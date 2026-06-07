package com.example.campuslife

import android.os.Bundle
import android.util.Log // ✨ Added for Logcat debugging
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {

    // Define a stable logging tag for filtering in your Logcat console
    private val TAG = "CampusLifeAuth"

    private lateinit var auth: FirebaseAuth
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        auth = FirebaseAuth.getInstance()

        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)

        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Welcome back! 🎉", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        // 🟥 Log the complete error trace directly to the console stack
                        Log.e(TAG, "Login execution encountered an exception", task.exception)

                        Toast.makeText(this, "Authentication Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        btnRegister.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Account created successfully! ✨", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        // 🟥 Log the complete error trace directly to the console stack
                        Log.e(TAG, "Registration execution encountered an exception", task.exception)

                        Toast.makeText(this, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        val txtForgotPassword = findViewById<TextView>(R.id.txtForgotPassword)

        txtForgotPassword.setOnClickListener {
            val email = edtEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email address first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Reset link sent! Check your inbox 📬", Toast.LENGTH_LONG).show()
                        Log.d(TAG, "Password reset email sent successfully to: $email")
                    } else {
                        Log.e(TAG, "Failed to send reset email", task.exception)
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}