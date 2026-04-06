package com.example.myapplication.model

enum class Category {
    Audio,
    Visual,
    Misc
}

enum class EquipmentStatus {
    Available,
    Low,
    Out
}

data class EquipmentUi(
    val id: Int,
    val name: String,
    val category: Category,
    val quantity: Int,
    val location: String,
    val bookingDate: String,
    val imageUrl: String? = null
) {
    val status: EquipmentStatus
        get() = when {
            quantity <= 0 -> EquipmentStatus.Out
            quantity < 3 -> EquipmentStatus.Low
            else -> EquipmentStatus.Available
        }
}

data class BookingUi(
    val bookingId: String,
    val equipmentId: Int,
    val userId: String,
    val equipmentName: String = "",
    val location: String = "",
    val category: String = "",
    val userFullName: String = "",
    val studentId: String = "",
    val userEmail: String = "",
    val date: String,
    val status: String
)
