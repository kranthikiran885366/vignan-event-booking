package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.myapplication.databinding.ActivityEquipmentDetailBinding
import com.example.myapplication.model.EquipmentStatus
import com.example.myapplication.model.EquipmentUi
import com.example.myapplication.ui.EquipmentViewModel
import com.example.myapplication.util.UiValidator
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EquipmentDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEquipmentDetailBinding

    private val viewModel: EquipmentViewModel by lazy {
        ViewModelProvider(
            this,
            SavedStateViewModelFactory(application, this)
        )[EquipmentViewModel::class.java]
    }

    private var equipmentId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEquipmentDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        equipmentId = intent.getIntExtra(EXTRA_EQUIPMENT_ID, -1)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        observeEquipment()
        observeEvents()
    }

    private fun observeEquipment() {
        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                val item = state.allItems.firstOrNull { it.id == equipmentId } ?: return@collectLatest
                bindItem(item)
            }
        }
    }

    private fun bindItem(item: EquipmentUi) {
        binding.collapsingToolbar.title = item.name
        binding.tvDetailName.text = item.name
        binding.tvDetailCategory.text = item.category.name
        binding.tvDetailQuantity.text = item.quantity.toString()
        binding.tvDetailLocation.text = item.location

        val imageRes = when (item.category.name) {
            "Audio"  -> R.drawable.img_audio
            "Visual" -> R.drawable.img_visual
            else     -> R.drawable.img_misc
        }
        binding.ivDetailImage.load(item.imageUrl) {
            crossfade(true)
            placeholder(imageRes)
            error(imageRes)
        }

        val (badgeText, badgeColor) = when (item.status) {
            EquipmentStatus.Available -> getString(R.string.status_available) to R.color.badge_available
            EquipmentStatus.Low       -> getString(R.string.status_low)       to R.color.badge_low
            EquipmentStatus.Out       -> getString(R.string.status_out)       to R.color.badge_out
        }
        binding.chipDetailStatus.text = badgeText
        binding.chipDetailStatus.setChipBackgroundColorResource(badgeColor)

        val enabled = item.quantity > 0
        binding.btnDetailReserve.isEnabled = enabled
        binding.btnDetailReserve.alpha = if (enabled) 1f else 0.5f

        binding.btnDetailReserve.setOnClickListener {
            val dateInput = binding.etDetailDate.text?.toString().orEmpty()
            val dateError = UiValidator.validateDateWindow(dateInput)
            if (dateError != null) {
                binding.tilDetailDate.error = dateError
                return@setOnClickListener
            }
            binding.tilDetailDate.error = null
            viewModel.onDateInputChanged(dateInput)
            viewModel.onReserveClicked(item)
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            viewModel.events.collectLatest { event ->
                when (event) {
                    is EquipmentViewModel.UiEvent.OpenConfirmDialog -> {
                        val item = viewModel.state.value.allItems.firstOrNull { it.id == event.item.id }
                            ?: return@collectLatest
                        viewModel.confirmBooking(item)
                    }
                    is EquipmentViewModel.UiEvent.BookingSuccess -> {
                        val equipmentName = viewModel.getEquipmentName(event.booking.equipmentId)
                        val intent = BookingSuccessActivity.newIntent(
                            this@EquipmentDetailActivity,
                            event.booking.bookingId,
                            equipmentName,
                            event.booking.date,
                            event.booking.status
                        )
                        startActivity(intent)
                        finish()
                    }
                    is EquipmentViewModel.UiEvent.ShowError ->
                        Snackbar.make(binding.root, event.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {
        const val EXTRA_EQUIPMENT_ID = "extra_equipment_id"

        fun newIntent(context: Context, equipmentId: Int): Intent =
            Intent(context, EquipmentDetailActivity::class.java)
                .putExtra(EXTRA_EQUIPMENT_ID, equipmentId)
    }
}
