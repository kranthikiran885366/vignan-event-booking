package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = auth.currentUser
            if (currentUser == null) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else if (currentUser.isAnonymous) {
                startActivity(Intent(this, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_USER_NAME, "Guest")
                    putExtra(MainActivity.EXTRA_USER_EMAIL, "guest@vignan.ac.in")
                })
                finish()
            } else {
                // Fetch profile from Firestore for returning users
                firestore.collection("users").document(currentUser.uid).get()
                    .addOnSuccessListener { doc ->
                        val name = doc.getString("fullName")?.ifBlank { null }
                            ?: currentUser.displayName
                            ?: currentUser.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                            ?: "User"
                        val email = doc.getString("email")?.ifBlank { null }
                            ?: currentUser.email
                            ?: ""
                        startActivity(Intent(this, MainActivity::class.java).apply {
                            putExtra(MainActivity.EXTRA_USER_NAME, name)
                            putExtra(MainActivity.EXTRA_USER_EMAIL, email)
                        })
                        finish()
                    }
                    .addOnFailureListener {
                        // Fallback to auth data
                        startActivity(Intent(this, MainActivity::class.java).apply {
                            putExtra(MainActivity.EXTRA_USER_NAME, currentUser.displayName ?: "User")
                            putExtra(MainActivity.EXTRA_USER_EMAIL, currentUser.email ?: "")
                        })
                        finish()
                    }
            }
        }, 2500)
    }
}
