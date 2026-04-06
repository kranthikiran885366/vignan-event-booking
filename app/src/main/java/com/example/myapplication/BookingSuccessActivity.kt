package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityBookingSuccessBinding

class BookingSuccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingSuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bookingId     = intent.getStringExtra(EXTRA_BOOKING_ID)     ?: ""
        val equipmentName = intent.getStringExtra(EXTRA_EQUIPMENT_NAME) ?: ""
        val date          = intent.getStringExtra(EXTRA_DATE)           ?: ""
        val status        = intent.getStringExtra(EXTRA_STATUS)         ?: "CONFIRMED"

        binding.tvSuccessBookingId.text  = bookingId
        binding.tvSuccessEquipment.text  = equipmentName
        binding.tvSuccessDate.text       = date
        binding.tvSuccessStatus.text     = status

        binding.btnViewBookings.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_NAVIGATE_TO, MainActivity.NAV_BOOKINGS)
            }
            startActivity(intent)
            finish()
        }

        binding.btnBackHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            finish()
        }
    }

    companion object {
        const val EXTRA_BOOKING_ID     = "extra_booking_id"
        const val EXTRA_EQUIPMENT_NAME = "extra_equipment_name"
        const val EXTRA_DATE           = "extra_date"
        const val EXTRA_STATUS         = "extra_status"

        fun newIntent(
            context: Context,
            bookingId: String,
            equipmentName: String,
            date: String,
            status: String
        ): Intent = Intent(context, BookingSuccessActivity::class.java).apply {
            putExtra(EXTRA_BOOKING_ID,     bookingId)
            putExtra(EXTRA_EQUIPMENT_NAME, equipmentName)
            putExtra(EXTRA_DATE,           date)
            putExtra(EXTRA_STATUS,         status)
        }
    }
}
