package com.example.myapplication.util

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object FirestoreSeeder {

    private const val COLLECTION_EQUIPMENT = "equipment"

    // Real equipment data — stored permanently in Firestore
    private val equipmentSeedData = listOf(
        mapOf(
            "id" to 1, "name" to "Projector X1", "category" to "Visual",
            "quantity" to 4, "location" to "Hall A", "bookingDate" to "",
            "imageUrl" to "", "description" to "High-lumen projector for large halls",
            "available" to true
        ),
        mapOf(
            "id" to 2, "name" to "Wireless Mic", "category" to "Audio",
            "quantity" to 2, "location" to "Studio B", "bookingDate" to "",
            "imageUrl" to "", "description" to "UHF wireless microphone system",
            "available" to true
        ),
        mapOf(
            "id" to 3, "name" to "Speaker Unit", "category" to "Audio",
            "quantity" to 0, "location" to "Store C", "bookingDate" to "",
            "imageUrl" to "", "description" to "200W portable PA speaker",
            "available" to false
        ),
        mapOf(
            "id" to 4, "name" to "LED Panel", "category" to "Visual",
            "quantity" to 7, "location" to "Lab D", "bookingDate" to "",
            "imageUrl" to "", "description" to "RGB LED panel for stage lighting",
            "available" to true
        ),
        mapOf(
            "id" to 5, "name" to "Tripod Stand", "category" to "Misc",
            "quantity" to 5, "location" to "Room E", "bookingDate" to "",
            "imageUrl" to "", "description" to "Adjustable camera/mic tripod stand",
            "available" to true
        ),
        mapOf(
            "id" to 6, "name" to "HDMI Cable", "category" to "Misc",
            "quantity" to 1, "location" to "Store C", "bookingDate" to "",
            "imageUrl" to "", "description" to "4K HDMI 2.0 cable, 5m length",
            "available" to true
        ),
        mapOf(
            "id" to 7, "name" to "Boom Mic", "category" to "Audio",
            "quantity" to 3, "location" to "Studio A", "bookingDate" to "",
            "imageUrl" to "", "description" to "Directional boom microphone with stand",
            "available" to true
        ),
        mapOf(
            "id" to 8, "name" to "LCD Screen", "category" to "Visual",
            "quantity" to 0, "location" to "Hall B", "bookingDate" to "",
            "imageUrl" to "", "description" to "55-inch LCD display screen",
            "available" to false
        ),
        mapOf(
            "id" to 9, "name" to "Laser Pointer", "category" to "Misc",
            "quantity" to 6, "location" to "Room F", "bookingDate" to "",
            "imageUrl" to "", "description" to "Green laser pointer with presenter remote",
            "available" to true
        ),
        mapOf(
            "id" to 10, "name" to "PA System", "category" to "Audio",
            "quantity" to 2, "location" to "Auditorium", "bookingDate" to "",
            "imageUrl" to "", "description" to "Complete PA system with mixer and speakers",
            "available" to true
        )
    )

    /**
     * Seeds equipment data to Firestore only if the collection is empty.
     * Called once on app start from CollegeEventApp.
     */
    fun seedIfEmpty(db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
        android.util.Log.e("FIRESTORE_SEEDER", "===== seedIfEmpty() CALLED =====")
        db.collection(COLLECTION_EQUIPMENT)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                android.util.Log.e("FIRESTORE_SEEDER", "Query success. Snapshot isEmpty: ${snapshot.isEmpty}")
                if (snapshot.isEmpty) {
                    android.util.Log.e("FIRESTORE_SEEDER", "Equipment collection empty - STARTING SEED with ${equipmentSeedData.size} items")
                    writeAllEquipment(db)
                } else {
                    android.util.Log.e("FIRESTORE_SEEDER", "Equipment collection already has ${snapshot.size()} document(s). Skipping seed.")
                }
            }
            .addOnFailureListener { ex ->
                android.util.Log.e("FIRESTORE_SEEDER", "Query FAILED: ${ex.message} | ${ex.javaClass.simpleName}", ex)
            }
    }

    /**
     * Force re-seed — use this to reset equipment data in Firestore.
     */
    fun forceSeed(db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
        writeAllEquipment(db)
    }

    private fun writeAllEquipment(db: FirebaseFirestore) {
        android.util.Log.e("FIRESTORE_SEEDER", "writeAllEquipment() called - preparing batch with ${equipmentSeedData.size} items")
        val batch = db.batch()
        equipmentSeedData.forEach { item ->
            val docId = (item["id"] as Int).toString()
            val ref = db.collection(COLLECTION_EQUIPMENT).document(docId)
            @Suppress("UNCHECKED_CAST")
            batch.set(ref, item as Map<String, Any>, SetOptions.merge())
        }
        android.util.Log.e("FIRESTORE_SEEDER", "Batch prepared. Committing ${equipmentSeedData.size} items to Firestore...")
        batch.commit()
            .addOnSuccessListener {
                android.util.Log.e("FIRESTORE_SEEDER", "✓ BATCH COMMIT SUCCESS! Seeded ${equipmentSeedData.size} equipment items to Firestore")
            }
            .addOnFailureListener { ex ->
                android.util.Log.e("FIRESTORE_SEEDER", "✗ BATCH COMMIT FAILED: ${ex.message} | ${ex.javaClass.simpleName}", ex)
            }
    }
}
