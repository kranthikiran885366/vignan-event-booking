package com.example.myapplication.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.myapplication.EquipmentDetailActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.ItemEquipmentBinding
import com.example.myapplication.model.EquipmentStatus
import com.example.myapplication.model.EquipmentUi

class EquipmentListAdapter(
    private val onReserveClick: (EquipmentUi, View) -> Unit
) : ListAdapter<EquipmentUi, EquipmentListAdapter.EquipmentViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EquipmentViewHolder {
        val binding = ItemEquipmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EquipmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EquipmentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EquipmentViewHolder(
        private val binding: ItemEquipmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EquipmentUi) {
            binding.tvName.text     = item.name
            binding.tvQuantity.text = binding.root.context.getString(R.string.quantity_format, item.quantity)
            binding.tvLocation.text = binding.root.context.getString(R.string.location_format, item.location)
            binding.tvDate.text     = binding.root.context.getString(
                R.string.booking_date_format,
                if (item.bookingDate.isBlank()) "N/A" else item.bookingDate
            )

            val (badgeRes, badgeColor) = when (item.status) {
                EquipmentStatus.Available -> R.string.status_available to R.color.badge_available
                EquipmentStatus.Low       -> R.string.status_low       to R.color.badge_low
                EquipmentStatus.Out       -> R.string.status_out       to R.color.badge_out
            }
            binding.chipStatus.text = binding.root.context.getString(badgeRes)
            binding.chipStatus.setChipBackgroundColorResource(badgeColor)

            val enabled = item.quantity > 0
            binding.btnReserve.isEnabled = enabled
            binding.btnReserve.alpha     = if (enabled) 1f else 0.5f
            binding.btnReserve.setOnClickListener { onReserveClick(item, binding.btnReserve) }

            // Load image: Firebase Storage URL if available, else local drawable fallback
            val fallbackRes = when (item.category.name) {
                "Audio"  -> R.drawable.img_audio
                "Visual" -> R.drawable.img_visual
                else     -> R.drawable.img_misc
            }
            if (!item.imageUrl.isNullOrBlank()) {
                binding.ivEquipment.load(item.imageUrl) {
                    crossfade(true)
                    placeholder(fallbackRes)
                    error(fallbackRes)
                }
            } else {
                binding.ivEquipment.setImageResource(fallbackRes)
            }

            // Both card click and View Details button open detail screen
            val openDetail = View.OnClickListener {
                binding.root.context.startActivity(
                    EquipmentDetailActivity.newIntent(binding.root.context, item.id)
                )
            }
            binding.root.setOnClickListener(openDetail)
            binding.btnViewDetails.setOnClickListener(openDetail)
        }
    }

    private object Diff : DiffUtil.ItemCallback<EquipmentUi>() {
        override fun areItemsTheSame(oldItem: EquipmentUi, newItem: EquipmentUi) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: EquipmentUi, newItem: EquipmentUi) = oldItem == newItem
    }
}
