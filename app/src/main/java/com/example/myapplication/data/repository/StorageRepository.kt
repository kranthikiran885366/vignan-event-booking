package com.example.myapplication.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val storage: FirebaseStorage
) {
    suspend fun uploadEquipmentImage(equipmentId: String, imageUri: Uri): Result<String> {
        return try {
            val ref = storage.reference.child("equipment_images/$equipmentId.jpg")
            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getImageUrl(equipmentId: String): Result<String> {
        return try {
            val ref = storage.reference.child("equipment_images/$equipmentId.jpg")
            val url = ref.downloadUrl.await()
            Result.success(url.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
