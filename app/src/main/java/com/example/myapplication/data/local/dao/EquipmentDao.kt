package com.example.myapplication.data.local.dao

import androidx.room.*
import com.example.myapplication.data.local.entity.BookingEntity
import com.example.myapplication.data.local.entity.EquipmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentDao {

    @Query("SELECT * FROM equipment")
    fun getAllEquipment(): Flow<List<EquipmentEntity>>

    @Query("SELECT * FROM equipment WHERE category = :category")
    fun getEquipmentByCategory(category: String): Flow<List<EquipmentEntity>>

    @Query("SELECT * FROM equipment WHERE id = :id")
    suspend fun getEquipmentById(id: Long): EquipmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipment(equipment: EquipmentEntity)

    @Update
    suspend fun updateEquipment(equipment: EquipmentEntity)

    @Transaction
    suspend fun bookEquipment(booking: BookingEntity) {
        val equipment = getEquipmentById(booking.equipmentId)
        if (equipment != null && equipment.quantity > 0) {
            updateEquipment(equipment.copy(quantity = equipment.quantity - 1))
            insertBooking(booking)
        } else {
            throw IllegalStateException("Equipment out of stock or not found locally")
        }
    }

    @Insert
    suspend fun insertBooking(booking: BookingEntity): Long

    @Delete
    suspend fun deleteBooking(booking: BookingEntity)

    @Query("SELECT * FROM bookings WHERE equipmentId = :equipmentId AND bookingDate = :date")
    suspend fun getBooking(equipmentId: Long, date: String): BookingEntity?
}
