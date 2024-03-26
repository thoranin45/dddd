package com.example.dddd

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val explicitButton = findViewById<Button>(R.id.button)
        explicitButton.setOnClickListener {
            val explicitIntent = Intent(this, MainActivity3::class.java)
            startActivity(explicitIntent)

        }
        val explicitButton4 = findViewById<Button>(R.id.button4)
        explicitButton4.setOnClickListener {
            val explicitIntent = Intent(this, wifi::class.java)
            startActivity(explicitIntent)

        }

        }
    }
