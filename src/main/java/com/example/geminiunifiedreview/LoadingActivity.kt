package com.example.geminiunifiedreview

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class LoadingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        window.statusBarColor = ContextCompat.getColor(this, R.color.ic_launcher_background)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        val response = intent.getStringExtra("response")

        if (response != null) {
            val intent = Intent(this, ResponseActivity::class.java)
            intent.putExtra("response", response)
            startActivity(intent)
            finish()
        }
    }
}
