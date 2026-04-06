package com.example.myapplication.data.repository

import com.example.myapplication.data.local.dao.EquipmentDao
import com.example.myapplication.data.local.entity.BookingEntity
import com.example.myapplication.data.local.entity.EquipmentEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EquipmentRepositoryImpl @Inject constructor(
    private val equipmentDao: EquipmentDao,
    private val firestore: FirebaseFirestore
) {
    // 1. REAL-TIME CLOUD DATA SYNC
    fun syncEquipment(): Flow<List<EquipmentEntity>> = callbackFlow {
        val subscription = firestore.collection("equipment")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        EquipmentEntity(
                            id = doc.id.hashCode().toLong(), // Simple mapping docID -> Long for Room
                            name = doc.getString("name") ?: "",
                            category = doc.getString("category") ?: "Misc",
                            quantity = doc.getLong("quantity")?.toInt() ?: 0,
                            location = doc.getString("location") ?: "",
                            imageUrl = doc.getString("imageUrl")
                        )
                    }
                    trySend(list)
                }
            }
        awaitClose { subscription.remove() }
    }

    // 2. LOCAL DATA ACCESS (Offline First)
    fun getLocalEquipment(category: String? = null): Flow<List<EquipmentEntity>> {
        return if (category == null || category == "All") {
            equipmentDao.getAllEquipment()
        } else {
            equipmentDao.getEquipmentByCategory(category)
        }
    }

    // 3. ATOMIC BOOKING
    suspend fun bookEquipment(booking: BookingEntity): Result<Unit> {
        return try {
            // Check local stock first
            val localEquip = equipmentDao.getEquipmentById(booking.equipmentId)
                ?: return Result.failure(Exception("Equipment not found locally"))
            
            if (localEquip.quantity <= 0) {
                return Result.failure(Exception("Out of stock"))
            }

            // Perform Cloud Transaction (True Source of Truth for quantity)
            firestore.runTransaction { transaction ->
                // Note: We need the Firestore Doc ID. 
                // In a real app, EquipmentEntity would store the String Firestore ID.
                // For this demo, we'll assume the document name is the equipment name (sanitized)
                val docId = localEquip.name.replace(" ", "_").lowercase()
                val equipRef = firestore.collection("equipment").document(docId)
                
                val snapshot = transaction.get(equipRef)
                val currentQty = snapshot.getLong("quantity") ?: 0L
                
                if (currentQty > 0) {
                    transaction.update(equipRef, "quantity", currentQty - 1)
                    val newBookingRef = firestore.collection("bookings").document()
                    val bookingMap = hashMapOf(
                        "equipmentId" to booking.equipmentId,
                        "date" to booking.bookingDate,
                        "status" to booking.status,
                        "timestamp" to System.currentTimeMillis()
                    )
                    transaction.set(newBookingRef, bookingMap)
                } else {
                    throw Exception("Out of stock in cloud")
                }
            }.await()

            // If Cloud succeeds, update Local DB
            equipmentDao.bookEquipment(booking)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveToLocal(list: List<EquipmentEntity>) {
        list.forEach { equipmentDao.insertEquipment(it) }
    }
}
