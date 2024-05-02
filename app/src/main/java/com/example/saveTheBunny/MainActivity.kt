package com.example.saveTheBunny


import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.example.football.R

class MainActivity : AppCompatActivity() {
    private lateinit var playButton: ImageView
    private lateinit var loadingProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playButton = findViewById(R.id.backgroundImageView)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)



        playButton.setOnClickListener {
            // Show loading indicator
            loadingProgressBar.visibility = View.VISIBLE

            // Simulate some loading process (Replace with your actual loading logic)
            simulateLoading()
        }
    }

    // Simulate loading process
    private fun simulateLoading() {
        // You can replace this with your actual loading logic.
        // For example, making a network request, loading data from a database, etc.
        // Here, we're just delaying for demonstration purposes.
        Thread {
            try {
                Thread.sleep(3000) // Simulating a 3-second loading process
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            // Hide loading indicator after loading is complete
            runOnUiThread {
                loadingProgressBar.visibility = View.GONE
                // Start LaunchPage1 activity
                startActivity(Intent(this, LaunchPage1::class.java))
            }
        }.start()
    }


}
