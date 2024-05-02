package com.example.saveTheBunny

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.football.R

class LaunchPage1 : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch_page1)



        val newGameButton = findViewById<Button>(R.id.newGameButton)
        val exitButton = findViewById<Button>(R.id.exitButton)
        val settingButton = findViewById<Button>(R.id.settingsButton)



        newGameButton.setOnClickListener {
            // Start a new game
            startActivity(Intent(this, Game::class.java))
        }

        settingButton.setOnClickListener {
            // Open settings activity
            startActivity(Intent(this, Settings::class.java))
        }

        exitButton.setOnClickListener {
            // Exit the app
            finishAffinity()
        }
    }


}
