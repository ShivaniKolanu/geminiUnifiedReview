package com.example.geminiunifiedreview

import android.content.ContentValues
import android.graphics.Bitmap
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

class MainActivity : AppCompatActivity() {

    private lateinit var captureIV : ImageView
    private lateinit var imageUrl : Uri
    private lateinit var triggerFunctionBtn: Button

    private val contract = registerForActivityResult(ActivityResultContracts.TakePicture()){
//
//        captureIV.setImageURI(null)
//        captureIV.setImageURI(imageUrl)
        val bitmap = uriToBitmap(imageUrl)
        captureIV.setImageBitmap(bitmap)
        triggerFunctionBtn.visibility = Button.VISIBLE
        updateLayout()
    }

    private val selectImageContract = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
//            captureIV.setImageURI(uri)
            val bitmap = uriToBitmap(uri)
            captureIV.setImageBitmap(bitmap)
            triggerFunctionBtn.visibility = Button.VISIBLE
            updateLayout()
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
//        println("Trigger function executed.")
        Toast.makeText(this, "Trigger!!.", Toast.LENGTH_SHORT).show()

    }

    private fun updateLayout() {
        captureIV.visibility = ImageView.VISIBLE

        val constraintLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main)
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)

        constraintSet.clear(R.id.captureImageButton, ConstraintSet.TOP)
        constraintSet.clear(R.id.importImageButton, ConstraintSet.TOP)
        constraintSet.connect(R.id.captureImageButton, ConstraintSet.TOP, R.id.captureImageView, ConstraintSet.BOTTOM)
        constraintSet.connect(R.id.importImageButton, ConstraintSet.TOP, R.id.captureImageButton, ConstraintSet.BOTTOM)

        constraintSet.applyTo(constraintLayout)
    }


}