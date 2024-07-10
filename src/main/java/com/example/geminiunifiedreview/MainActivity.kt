package com.example.geminiunifiedreview

import android.content.ContentValues
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


class MainActivity : AppCompatActivity() {

    private lateinit var captureIV : ImageView
    private lateinit var imageUrl : Uri
    private lateinit var triggerFunctionBtn: Button
    private lateinit var placeholderLayout: LinearLayout


    private val contract = registerForActivityResult(ActivityResultContracts.TakePicture()){
//
//        captureIV.setImageURI(null)
//        captureIV.setImageURI(imageUrl)
        val bitmap = uriToBitmap(imageUrl)
        captureIV.setImageBitmap(bitmap)
        triggerFunctionBtn.visibility = Button.VISIBLE
        captureIV.visibility = ImageView.VISIBLE
        placeholderLayout.visibility = LinearLayout.GONE
    }

    private val selectImageContract = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
//            captureIV.setImageURI(uri)
            val bitmap = uriToBitmap(uri)
            captureIV.setImageBitmap(bitmap)
            triggerFunctionBtn.visibility = Button.VISIBLE
            captureIV.visibility = ImageView.VISIBLE
            placeholderLayout.visibility = LinearLayout.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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

        val captureImgBtn = findViewById<Button>(R.id.captureImageButton)
        captureImgBtn.setOnClickListener{
            imageUrl = createImageUri() // Update image URI each time before capturing
            contract.launch(imageUrl)

        }

        val importImgBtn = findViewById<Button>(R.id.importImageButton)
        importImgBtn.setOnClickListener {
            selectImageContract.launch(arrayOf("image/*"))
        }

        triggerFunctionBtn.setOnClickListener {
            triggerFunction()
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



    private fun triggerFunction() {
        // Placeholder function
        println("Trigger function executed.")
//        Toast.makeText(this, "Trigger!!.", Toast.LENGTH_SHORT).show()

        val model = GenerativeModel(
            "gemini-1.0-pro-vision-latest",
            "AIzaSyCMNKgS52h40uEoFkBHptuCdV8ZAEwJ-M8"
        )

        val bitmap = (captureIV.drawable as? BitmapDrawable)?.bitmap

        if (bitmap == null) {
            showToastWithLongMessage("Failed to get bitmap from ImageView.")
            return
        }

        val input = content {
            text("From the given image, can you tell me which product is it and also mention what rating it has in amazon out of 5. you can mention in this structure. Product: the product name\n Amazon Rating: value out of 5 \n Pros: 2-3 pros\n Cons: 2-3 cons if any?\n")
            image(bitmap)
        }

        // Launch a coroutine to call the suspend function
        lifecycleScope.launch {
            try {
                val response = model.generateContent(input)

                // Get the first text part of the first candidate
                println(response.text)
                val resp = response.text
                Log.d("SuccessActivity", "worked, $resp")
                println(response.candidates.first().content.parts.first().toString())
                Toast.makeText(applicationContext, "$resp", Toast.LENGTH_SHORT).show()

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



}