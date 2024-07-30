package com.example.geminiunifiedreview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ScrollingView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginTop
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
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
    private lateinit var dynamicContentLayout: LinearLayout
    private lateinit var gridLayout: GridLayout
    private lateinit var loadingTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var resultsScroll: ScrollView

    data class ProductResponse(
        val product: String,
        val ratings: Map<String, String>,
        val pros: List<String>,
        val cons: List<String>
    )





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

    @SuppressLint("MissingInflatedId")
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
        gridLayout = findViewById(R.id.gridLayout)
        loadingTextView = findViewById(R.id.loadingTextView)
        progressBar = findViewById(R.id.progressBar)
        resultsScroll = findViewById(R.id.resultsScroll)

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
        progressBar.visibility = ProgressBar.VISIBLE
        loadingTextView.visibility = TextView.VISIBLE
        val config = loadConfig(this)
        val gemini_api_key = config.getProperty("GEMINI_API_KEY")
        val model = GenerativeModel(
            "gemini-1.5-pro",
            gemini_api_key
        )

        val bitmap1 = (captureIV1.drawable as? BitmapDrawable)?.bitmap

        val bitmap2 = (captureIV2.drawable as? BitmapDrawable)?.bitmap

        if(bitmap1 == null || bitmap2 == null){
            Log.e("ComparingActivity", "No Image Bitmap1")
            return
        }


        val input = content {
            text("Can you mention product names, their ratings from different websites and a few pros and cons depending or analyzing the reviews on different websites. Also, please Make Sure that the websites you find ratings from are same for product 1 and 2 which means, I want the ratings websites to be same for both products instead of you taking for different websites for both of them. I want the result to be in following structure and dont give any extra info or statements please\n" +
                    "[\n" +
                    "  {\n" +
                    "\t\"product\": \"1st product name\",\n" +
                    "  \t\"ratings\": {\n" +
                    "    \t  \"website1\": \"4.4/5.0\",\n" +
                    "    \t  \"website2\": \"4.3/5.0\",\n" +
                    "    \t  \"website3\": \"4.4/5.0\"\n" +
                    "  \t},\n" +
                    "  \t\"pros\": [\"point1\", \"point2\"],\n" +
                    "  \t\"cons\": [\"point1\", \"point2\"]\n" +
                    "  },\n" +
                    "  {\n" +
                    "\t\"product\": \"2nd product name\",\n" +
                    "  \t\"ratings\": {\n" +
                    "    \t  \"website1\": \"rating you found in the website/out of how much is the rating,\n" +
                    "    \t  \"website2\": \"rating you found in the website/out of how much is the rating\",\n" +
                    "    \t  \"website3\": \"rating you found in the website/out of how much is the rating\"\n" +
                    "  \t},\n" +
                    "  \t\"pros\": [\"point1\", \"point2\"],\n" +
                    "  \t\"cons\": [\"point1\", \"point2\"]\n" +
                    "  }\n" +
                    "] ")
            image(bitmap1)
            image(bitmap2)
        }

//        val intent = Intent(this, LoadingActivity::class.java)
//        startActivity(intent)

        // Launch a coroutine to call the suspend function
        lifecycleScope.launch {
            try {
                val response = model.generateContent(input)
                val resp = response.text
                Log.d("SuccessDCompareActivity", "worked, $resp")

//                 Parse the JSON response
                val gson = Gson()
                val productResponse: List<ProductResponse> = gson.fromJson(resp, object : TypeToken<List<ProductResponse>>() {}.type)

                // Convert the productResponse to JSON string
                val productResponseJson = gson.toJson(productResponse)

                Log.d("SuccessCompareActivity", "worked, $productResponseJson")

                triggerResultsData(productResponseJson)




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

    private fun triggerResultsData(productResponseJson: String) {
        try {
            val gson = Gson()
            val productResponses: List<ProductResponse> = gson.fromJson(productResponseJson, object : TypeToken<List<ProductResponse>>() {}.type)
            Log.d("TriggerSuccess", "worked, $productResponses")

            val data = listOf("Item 1", "Item 2")

            for (item in data) {
                if (item == "Item 1" || item == "Item 2") {
                    val index = if (item == "Item 1") 0 else 1
                    val productResponse = productResponses[index]

                    val linearLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = GridLayout.LayoutParams().apply {
                            width = 0
                            height = GridLayout.LayoutParams.WRAP_CONTENT
                            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 2f)
                            setMargins(8, 8, 8, 8)
                        }
                        setPadding(16, 16, 16, 16)
                    }

                    val productTextView = TextView(this).apply {
                        text = productResponse.product
                        setBackgroundColor(Color.parseColor("#e3c6f0"))
                        setTextColor(Color.BLACK)
                        setPadding(16, 16, 16, 16)
                        gravity = android.view.Gravity.CENTER
                    }
                    linearLayout.addView(productTextView)

                    // Ratings
                    for ((site, rating) in productResponse.ratings) {
                        val ratingLayout = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            setPadding(16, 8, 16, 8)
                        }

                        val imageView = ImageView(this).apply {
                            setImageResource(R.drawable.rating_icon) // Replace with your rating image resource
                            layoutParams = LinearLayout.LayoutParams(35.dpToPx(), 30.dpToPx()).apply {
                                marginEnd = 8.dpToPx()
                            }
                        }
                        ratingLayout.addView(imageView)

                        val ratingTextView = TextView(this).apply {
                            text = "$site: $rating"
                            setTextColor(Color.BLACK)
                        }
                        ratingLayout.addView(ratingTextView)

                        linearLayout.addView(ratingLayout)
                    }

                    // Pros
                    addImageTextSections(linearLayout, "Pros:", productResponse.pros)

                    // Cons
                    addImageTextSections(linearLayout, "Cons:", productResponse.cons)

                    gridLayout.addView(linearLayout)

                }
            }
            progressBar.visibility = ProgressBar.GONE
            loadingTextView.visibility = TextView.GONE
            resultsScroll.visibility = ScrollView.VISIBLE

        } catch (e: JSONException) {
            e.printStackTrace()
            Log.e("TriggerActivity", "Error man!!", e)
        }
    }

    // Helper function to add image and text sections
    private fun addImageTextSections(parentLayout: LinearLayout, title: String, items: List<String>) {
        val titleTextView = TextView(this).apply {
            text = title
            setTextColor(Color.BLACK)
            setPadding(16, 8, 16, 8)
            setTypeface(null, Typeface.BOLD)
        }
        parentLayout.addView(titleTextView)

        for (item in items) {
            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 8, 16, 8)
            }

            val itemImageView = ImageView(this).apply {
                if(title == "Cons:"){
                    setImageResource(R.drawable.cons)
                } else{
                    setImageResource(R.drawable.pros)
                }
                 // Replace with your pro/con image resource
                layoutParams = LinearLayout.LayoutParams(25.dpToPx(), 25.dpToPx()).apply {
                    marginEnd = 6.dpToPx()
                }
            }
            itemLayout.addView(itemImageView)

            val itemTextView = TextView(this).apply {
                text = item
                setTextColor(Color.BLACK)
            }
            itemLayout.addView(itemTextView)

            parentLayout.addView(itemLayout)
        }
    }

    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }



}