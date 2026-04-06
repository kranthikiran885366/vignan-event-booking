package com.example.myapplication.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.EquipmentDetailActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.ItemBookingBinding
import com.example.myapplication.model.BookingUi

class BookingsAdapter(
    private val getEquipmentName: (Int) -> String,
    private val onCancelClick: (BookingUi) -> Unit
) : ListAdapter<BookingUi, BookingsAdapter.BookingViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookingViewHolder(
        private val binding: ItemBookingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: BookingUi) {
            val ctx = binding.root.context
            binding.tvBookingId.text        = ctx.getString(R.string.booking_id_format, booking.bookingId)
            binding.tvBookingEquipment.text = booking.equipmentName.ifBlank { getEquipmentName(booking.equipmentId) }
            binding.tvBookingDate.text      = ctx.getString(R.string.booking_date_label, booking.date)
            val student = booking.studentId.ifBlank { "N/A" }
            val email = booking.userEmail.ifBlank { "N/A" }
            val location = booking.location.ifBlank { "N/A" }
            val category = booking.category.ifBlank { "N/A" }
            binding.tvBookingMeta.text = ctx.getString(
                R.string.booking_meta_format,
                booking.userFullName.ifBlank { "N/A" },
                student,
                email,
                category,
                location
            )
            binding.chipBookingStatus.text  = booking.status

            val chipColor = when (booking.status) {
                "CONFIRMED" -> R.color.status_available
                "PENDING"   -> R.color.status_low
                else        -> R.color.status_out
            }
            binding.chipBookingStatus.setChipBackgroundColorResource(chipColor)

            binding.btnCancelBooking.setOnClickListener { onCancelClick(booking) }

            binding.root.setOnClickListener {
                ctx.startActivity(EquipmentDetailActivity.newIntent(ctx, booking.equipmentId))
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<BookingUi>() {
        override fun areItemsTheSame(old: BookingUi, new: BookingUi) = old.bookingId == new.bookingId
        override fun areContentsTheSame(old: BookingUi, new: BookingUi) = old == new
    }
}
