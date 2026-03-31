package com.example.campuslife

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    // Non-cringe "vibe" messages
    private val loadingMessages = arrayOf(
        "Organizing your desk...",
        "Loading the notes...",
        "Prepping the study session...",
        "Reviewing the syllabus...",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.splashLogo)
        val statusText = findViewById<TextView>(R.id.txtStatus)

        // Pop-in animation for logo
        logo.startAnimation(AnimationUtils.loadAnimation(this, R.anim.overshoot_pop))

        // Change text halfway through for a "live" feel
        Handler(Looper.getMainLooper()).postDelayed({
            statusText.text = loadingMessages.random()
        }, 1500)

        // Launch MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 3000) // 3 seconds total for splash
    }
}