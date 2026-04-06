package com.example.myapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.myapplication.data.local.dao.EquipmentDao
import com.example.myapplication.data.local.entity.BookingEntity
import com.example.myapplication.data.local.entity.EquipmentEntity

@Database(
    entities = [EquipmentEntity::class, BookingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun equipmentDao(): EquipmentDao
}
