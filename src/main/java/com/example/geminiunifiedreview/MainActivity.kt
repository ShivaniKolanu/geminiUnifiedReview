package com.example.geminiunifiedreview

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.IOException
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Log
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import java.io.ByteArrayOutputStream
import java.util.Dictionary
import java.util.Properties
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var captureIV : ImageView
    private lateinit var imageUrl : Uri
    private lateinit var triggerFunctionBtn: Button
    private lateinit var placeholderLayout: LinearLayout
    private var isCapturedImage = false

    object ImageHolder {
        var bitmap: Bitmap? = null
    }

    data class Rating(
        val website: String,
        val rating: String
    )

    data class ProductResponse(
        val product: String,
        val ratings: Map<String, String>,
        val pros: List<String>,
        val cons: List<String>
    )



    private val contract = registerForActivityResult(ActivityResultContracts.TakePicture()){

        val bitmap = uriToBitmap(imageUrl)
        captureIV.setImageBitmap(bitmap)
        triggerFunctionBtn.visibility = Button.VISIBLE
        captureIV.visibility = ImageView.VISIBLE
        placeholderLayout.visibility = LinearLayout.GONE
    }

    private val selectImageContract = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val bitmap = uriToBitmap(uri)
            captureIV.setImageBitmap(bitmap)
            triggerFunctionBtn.visibility = Button.VISIBLE
            captureIV.visibility = ImageView.VISIBLE
            placeholderLayout.visibility = LinearLayout.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.statusBarColor = ContextCompat.getColor(this, R.color.ic_launcher_background)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        imageUrl = createImageUri()
        captureIV = findViewById(R.id.captureImageView)
        triggerFunctionBtn = findViewById(R.id.triggerFunctionButton)
        placeholderLayout = findViewById(R.id.placeholderLayout)
        val saveIcon: ImageView = findViewById(R.id.saveIcon)

        saveIcon.setOnClickListener {
            showSavedFunction()
        }

        val captureImgBtn = findViewById<Button>(R.id.captureImageButton)
        captureImgBtn.setOnClickListener{
            isCapturedImage = true
            imageUrl = createImageUri() // Update image URI each time before capturing
            contract.launch(imageUrl)

        }

        val importImgBtn = findViewById<Button>(R.id.importImageButton)
        importImgBtn.setOnClickListener {
            isCapturedImage = false
            selectImageContract.launch(arrayOf("image/*"))
        }

        triggerFunctionBtn.setOnClickListener {
            if (isNetworkConnected()) {
                triggerFunction()
            } else {
                showNetworkError()
            }
        }
    }

    private fun createImageUri(): Uri {
        val image = File(filesDir, "camera_photos.png")
        return FileProvider.getUriForFile(this,
            "com.example.geminiunifiedreview.FileProvider",
            image)
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

    private fun showSavedFunction() {
        Log.d("SavedClickActivity", "Saved Click Happened")
        val intent = Intent(this, LoadingActivity::class.java)
        startActivity(intent)

        Handler(Looper.getMainLooper()).postDelayed({
            val responseIntent = Intent(this, SavedSearchesActivity::class.java)
            startActivity(responseIntent)
            finish()
        }, 2000)
    }


    private fun triggerFunction() {

        val config = loadConfig(this)
        val gemini_api_key = config.getProperty("GEMINI_API_KEY")

        Log.d("APIActivity", "working, $gemini_api_key")

        val model = GenerativeModel(
            "gemini-1.0-pro-vision-latest",
            gemini_api_key
        )

        val bitmap = (captureIV.drawable as? BitmapDrawable)?.bitmap

        if (bitmap == null) {
            showToastWithLongMessage("Failed to get bitmap from ImageView.")
            return
        }


        val input = content {
            text("From the given image, can you tell me which product is it and also mention " +
                    "what rating it has in amazon and any other 3 relevant websites out of 5. " +
                    "Product: the product name\n " +
                    "Amazon Rating: value out of 5 \n Name of website - Rating: value out of 5\n " +
                    "Name of website - Rating: value out of 5\n Name of website - Rating: value out of 5\n  " +
                    "Pros: 2-3 pros\n Cons: 2-3 c   ons if any?\n the structure i want is as below\n" +
                    "{\n" +
                    "  \"product\": \"product name\",\n" +
                    "  \"ratings\": {\n" +
                    "    \"website1\": \"rating you found in the website/out of how much is the rating\",\n" +
                    "    \"website2\": \"rating you found in the website/out of how much is the rating\",\n" +
                    "    \"website3\": \"rating you found in the website/out of how much is the rating\"\n" +
                    "    \"website4\": \"rating you found in the website/out of how much is the rating\"\n" +
                    "  },\n" +
                    "  \"pros\": [\"point1\", \"point2\"],\n" +
                    "  \"cons\": [\"point1\", \"point2\"]\n" +
                    "}\n"

            )
            image(bitmap)
        }

        val intent = Intent(this, LoadingActivity::class.java)
        startActivity(intent)

        // Launch a coroutine to call the suspend function
        lifecycleScope.launch {
            try {
                val response = model.generateContent(input)
                val byteImage = bitmapToByteArray(bitmap)
                val resp = response.text
                Log.d("SuccessActivity", "worked, $resp")

                // Parse the JSON response
                val gson = Gson()
                val productResponse: ProductResponse = gson.fromJson(resp, object : TypeToken<ProductResponse>() {}.type)

                // Convert the productResponse to JSON string
                val productResponseJson = gson.toJson(productResponse)

                Log.d("SuccessActivity", "worked, $productResponseJson")

                val intent_1 = Intent(this@MainActivity, ResponseActivity::class.java)
                intent_1.putExtra("response", resp)
                intent_1.putExtra("productResponse", productResponseJson)
                ImageHolder.bitmap = bitmap
                intent_1.putExtra("isCapturedImage", isCapturedImage)
                startActivity(intent_1)

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("MainActivity", "Error during content generation", e)
                showToastWithLongMessage("Error: ${e.message}")
            }
        }
    }

    private fun showToastWithLongMessage(message: String) {
        val maxToastLength = 2000 // Maximum length for a single Toast message
        if (message.length > maxToastLength) {
            val chunkCount = message.length / maxToastLength
            for (i in 0..chunkCount) {
                val start = i * maxToastLength
                val end = if ((i + 1) * maxToastLength > message.length) message.length else (i + 1) * maxToastLength
                Toast.makeText(this, message.substring(start, end), Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    // Utility method to check network connectivity
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

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
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