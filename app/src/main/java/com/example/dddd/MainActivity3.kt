package com.example.dddd

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity3 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)

        val explicitButton4 = findViewById<Button>(R.id.button4)
        explicitButton4.setOnClickListener {
            val explicitIntent = Intent(this, MainActivity2::class.java)
            startActivity(explicitIntent)

        }
        val explicitbutton5 = findViewById<Button>(R.id.button5)
        explicitbutton5.setOnClickListener {
            val explicitIntent = Intent(this, end1::class.java)
            startActivity(explicitIntent)

        }

    }
}