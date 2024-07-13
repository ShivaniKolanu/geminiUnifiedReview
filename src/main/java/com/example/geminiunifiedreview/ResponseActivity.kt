package com.example.geminiunifiedreview

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File

class ResponseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        window.statusBarColor = ContextCompat.getColor(this, R.color.ic_launcher_background)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_response)

        val responseTextView: TextView = findViewById(R.id.responseTextView)
        val response = intent.getStringExtra("response")
        val responseIV: ImageView = findViewById(R.id.responseImageView)
        val isCapturedImage = intent.getBooleanExtra("isCapturedImage", true)

        val bitmap = MainActivity.ImageHolder.bitmap
        if (bitmap != null) {
            responseIV.setImageBitmap(bitmap)
        }
        if (response != null) {
            responseTextView.text = response
        }
        val backButton: Button = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}
