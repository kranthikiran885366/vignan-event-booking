package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "equipment",
    indices = [Index(value = ["name"], unique = true)]
)
data class EquipmentEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val category: String,
    val quantity: Int,
    val location: String,
    val imageUrl: String? = null
)
