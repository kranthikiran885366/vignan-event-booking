package com.example.myapplication.domain.usecase

import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class BookEquipmentUseCase @Inject constructor() {
    
    operator fun invoke(date: String): Result<Unit> {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.isLenient = false
            val bookingDate = sdf.parse(date) ?: return Result.failure(Exception("Invalid date format"))

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val today = calendar.time

            if (bookingDate.before(today)) {
                return Result.failure(Exception("Cannot book for a past date"))
            }

            calendar.add(Calendar.DAY_OF_YEAR, 30)
            if (bookingDate.after(calendar.time)) {
                return Result.failure(Exception("Cannot book more than 30 days in advance"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Invalid date format (YYYY-MM-DD)"))
        }
    }
}
