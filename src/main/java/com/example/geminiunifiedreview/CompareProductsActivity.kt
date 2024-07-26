package com.example.geminiunifiedreview

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.geminiunifiedreview.MainActivity.ImageHolder
import com.example.geminiunifiedreview.MainActivity.ProductResponse
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.Properties

class CompareProductsActivity : AppCompatActivity() {

    private lateinit var imageUrl1 : Uri
    private lateinit var imageUrl2 : Uri
    private lateinit var captureIV1 : ImageView
    private lateinit var placeholderLayout1: LinearLayout
    private lateinit var captureIV2 : ImageView
    private lateinit var placeholderLayout2: LinearLayout
    private lateinit var compareFunBtn: Button

    private val contract = registerForActivityResult(ActivityResultContracts.TakePicture()){

        val bitmap1 = uriToBitmap(imageUrl1)
        captureIV1.setImageBitmap(bitmap1)
        captureIV1.visibility = ImageView.VISIBLE
        placeholderLayout1.visibility = LinearLayout.GONE
    }

    private val contract2 = registerForActivityResult(ActivityResultContracts.TakePicture()){

        val bitmap2 = uriToBitmap(imageUrl2)
        captureIV2.setImageBitmap(bitmap2)
        captureIV2.visibility = ImageView.VISIBLE
        placeholderLayout2.visibility = LinearLayout.GONE
    }

    private val selectImageContract = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val bitmap = uriToBitmap(uri)
            captureIV1.setImageBitmap(bitmap)
            captureIV1.visibility = ImageView.VISIBLE
            placeholderLayout1.visibility = LinearLayout.GONE
        }
    }

    private val selectImageContract2 = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val bitmap = uriToBitmap(uri)
            captureIV2.setImageBitmap(bitmap)
            captureIV2.visibility = ImageView.VISIBLE
            placeholderLayout2.visibility = LinearLayout.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.statusBarColor = ContextCompat.getColor(this, R.color.ic_launcher_background)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compare_products)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.compareLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imageUrl1 = createImageUri(1)
        captureIV1 = findViewById(R.id.compareImageView1)
        placeholderLayout1 = findViewById(R.id.placeholderLayout1)
        imageUrl2 = createImageUri(2)
        captureIV2 = findViewById(R.id.compareImageView2)
        placeholderLayout2 = findViewById(R.id.placeholderLayout2)
        compareFunBtn = findViewById(R.id.compareButton)


        val captureImgBtn1 : ImageView = findViewById(R.id.captureImageButton1)
        captureImgBtn1.setOnClickListener {
            imageUrl1 = createImageUri(1)
            contract.launch(imageUrl1)
        }
        val captureImgBtn2 : ImageView = findViewById(R.id.captureImageButton2)
        captureImgBtn2.setOnClickListener {
            imageUrl2 = createImageUri(2)
            contract2.launch(imageUrl2)
        }

        val importImgBtn1: ImageView = findViewById(R.id.importImageButton1)
        importImgBtn1.setOnClickListener{
            selectImageContract.launch(arrayOf("image/*"))
        }

        val importImgBtn2: ImageView = findViewById(R.id.importImageButton2)
        importImgBtn2.setOnClickListener{
            selectImageContract2.launch(arrayOf("image/*"))
        }



        compareFunBtn.setOnClickListener{
            if (isNetworkConnected()) {
                comparingFunction()
            } else {
                showNetworkError()
            }
        }
    }

    private fun createImageUri(imageNum: Int): Uri {
        val image = if(imageNum == 1){
            File(filesDir, "compare_photo_1.png")
        } else {
            File(filesDir, "compare_photo_2.png")
        }

        return FileProvider.getUriForFile(this,
            "com.example.geminiunifiedreview.FileProvider",
            image)
    }

    private fun comparingFunction() {
        val config = loadConfig(this)
        val gemini_api_key = config.getProperty("GEMINI_API_KEY")
        val model = GenerativeModel(
            "gemini-1.5-flash",
            gemini_api_key
        )

        val bitmap1 = (captureIV1.drawable as? BitmapDrawable)?.bitmap

        val bitmap2 = (captureIV2.drawable as? BitmapDrawable)?.bitmap

        if(bitmap1 == null || bitmap2 == null){
            Log.e("ComparingActivity", "No Image Bitmap1")
            return
        }


        val input = content {
            text("Can you mention the product names in the 2 images and also give" +
                    " how much amazon rating does the products have out of 5?")
            image(bitmap1)
            image(bitmap2)
        }

        val intent = Intent(this, LoadingActivity::class.java)
        startActivity(intent)

        // Launch a coroutine to call the suspend function
        lifecycleScope.launch {
            try {
                val response = model.generateContent(input)
                val resp = response.text
                Log.d("SuccessCompareActivity", "worked, $resp")

                // Parse the JSON response
                val gson = Gson()
                val productResponse: ProductResponse = gson.fromJson(resp, object : TypeToken<ProductResponse>() {}.type)

                // Convert the productResponse to JSON string
                val productResponseJson = gson.toJson(productResponse)

                Log.d("SuccessCompareActivity", "worked, $productResponseJson")

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("CompareMainActivity", "Error during content generation", e)
            }
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    private fun showNetworkError() {
        Toast.makeText(this, "No internet connection.\nPlease check your network settings.", Toast.LENGTH_LONG).show()
        Log.e("NetworkActivity", "No Internet Connection")

    }

    private fun loadConfig(context: Context): Properties {
        val properties = Properties()
        try {
            val inputStream = context.assets.open("config.properties")
            properties.load(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return properties
    }
}