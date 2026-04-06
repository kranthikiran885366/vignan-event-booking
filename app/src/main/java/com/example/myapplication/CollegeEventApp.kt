package com.example.myapplication

import android.app.Application
import com.example.myapplication.util.FirestoreSeeder
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CollegeEventApp : Application() {

    companion object {
        init {
            android.util.Log.e("COLLEGE_EVENT_APP_COMPANION", "===== COMPANION OBJECT INIT BLOCK =====")
        }
    }

    override fun onCreate() {
        super.onCreate()

        println("[COLLEGE APP] onCreate() called - Thread: ${Thread.currentThread().name}")
        android.util.Log.e("COLLEGE_EVENT_APP","===== CollegeEventApp.onCreate() STARTED =====")

        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
            android.util.Log.e("COLLEGE_EVENT_APP", "Firebase initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("COLLEGE_EVENT_APP", "Firebase init failed: ${e.message}")
        }

        // Enable Firestore offline persistence (cache unlimited)
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = settings
            android.util.Log.e("COLLEGE_EVENT_APP", "Firestore settings configured")
        } catch (e: Exception) {
            android.util.Log.e("COLLEGE_EVENT_APP", "Firestore settings failed: ${e.message}")
        }

        // Enable Realtime Database offline persistence
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
            android.util.Log.e("COLLEGE_EVENT_APP", "Realtime DB persistence enabled")
        } catch (e: Exception) {
            android.util.Log.e("COLLEGE_EVENT_APP", "Realtime DB persistence failed: ${e.message}")
        }

        // Seed initial equipment data to Firestore if collection is empty
        try {
            android.util.Log.e("COLLEGE_EVENT_APP", "Starting to seed Firestore equipment...")
            FirestoreSeeder.seedIfEmpty()
            android.util.Log.e("COLLEGE_EVENT_APP", "Seeding request sent (async)")
        } catch (ex: Exception) {
            android.util.Log.e("COLLEGE_EVENT_APP", "Seeding exception: ${ex.message}", ex)
        }
        
        android.util.Log.e("COLLEGE_EVENT_APP", "===== CollegeEventApp.onCreate() COMPLETED =====")
    }
}
