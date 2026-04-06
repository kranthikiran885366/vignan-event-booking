package com.example.myapplication.ui

import com.example.myapplication.model.BookingUi
import com.example.myapplication.model.Category
import com.example.myapplication.model.EquipmentUi

data class EquipmentUiState(
    val allItems: List<EquipmentUi> = emptyList(),
    val filteredItems: List<EquipmentUi> = emptyList(),
    val selectedCategory: Category = Category.Audio,
    val searchQuery: String = "",
    val bookingDateInput: String = "",
    val dateError: String? = null,
    val emptyMessage: String? = null,
    val pendingBooking: BookingUi? = null,
    val bookingHistory: List<BookingUi> = emptyList(),
    val listScrollPosition: Int = 0,
    val isLoading: Boolean = false,
    val globalError: String? = null
)
