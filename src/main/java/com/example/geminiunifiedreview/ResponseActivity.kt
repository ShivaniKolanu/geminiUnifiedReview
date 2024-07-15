package com.example.geminiunifiedreview

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import android.widget.LinearLayout

class ResponseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        window.statusBarColor = ContextCompat.getColor(this, R.color.ic_launcher_background)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_response)

        val responseIV: ImageView = findViewById(R.id.responseImageView)
        val isCapturedImage = intent.getBooleanExtra("isCapturedImage", true)


        // Retrieve the product response JSON string
        val productResponseJson = intent.getStringExtra("productResponse")
        val productResponse = Gson().fromJson(productResponseJson, MainActivity.ProductResponse::class.java)

        Log.d("ProductActivity", "working, $productResponse")

        findViewById<TextView>(R.id.productNameTextView).text = productResponse.product

        val ratingsContainer = findViewById<GridLayout>(R.id.ratingsContainer)

        // Adding ratings dynamically
        productResponse.ratings.forEach { (website, rating) ->
            val ratingItem = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
                }

                val imageView = ImageView(this@ResponseActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(45.dpToPx(), 40.dpToPx()).apply {
                        marginEnd = 8.dpToPx()
                    }
                    setImageResource(R.drawable.rating_icon)
                }

                val textView = TextView(this@ResponseActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = 12.dpToPx()
                    }
                    text = "$website: $rating"
                    textSize = 18f
                }

                addView(imageView)
                addView(textView)
            }

            ratingsContainer.addView(ratingItem)
        }


        val prosContainer = findViewById<LinearLayout>(R.id.prosContainer)
        productResponse.pros.forEach { pro ->
            val proItem = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
                }

                val imageView = ImageView(this@ResponseActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(35.dpToPx(), 35.dpToPx()).apply {
                        marginEnd = 8.dpToPx()
                    }
                    setImageResource(R.drawable.pros)
                }

                val textView = TextView(this@ResponseActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    text = pro
                    textSize = 18f
                }

                addView(imageView)
                addView(textView)
            }

            prosContainer.addView(proItem)
        }

        val consContainer = findViewById<LinearLayout>(R.id.consContainer)
        productResponse.cons.forEach { con ->
            val conItem = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
                }

                val imageView = ImageView(this@ResponseActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(35.dpToPx(), 35.dpToPx()).apply {
                        marginEnd = 8.dpToPx()
                    }
                    setImageResource(R.drawable.cons)
                }

                val textView = TextView(this@ResponseActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    text = con
                    textSize = 18f
                }

                addView(imageView)
                addView(textView)
            }

            consContainer.addView(conItem)
        }



        val bitmap = MainActivity.ImageHolder.bitmap
        if (bitmap != null) {
            responseIV.setImageBitmap(bitmap)
        }
        val backButton: Button = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        // Optional: Clear the back stack to ensure no previous activities remain
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // Call this if you want to finish the current activity
    }

}



