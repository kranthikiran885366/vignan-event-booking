package com.example.myapplication.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.example.myapplication.R
import com.example.myapplication.databinding.ItemStatusGridBinding
import com.example.myapplication.model.EquipmentStatus
import com.example.myapplication.model.EquipmentUi

class EquipmentStatusGridAdapter(
    private val context: Context,
    private var items: List<EquipmentUi>,
    private val onClick: (EquipmentUi) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): EquipmentUi = items[position]

    override fun getItemId(position: Int): Long = items[position].id.toLong()

    fun submitData(newItems: List<EquipmentUi>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding: ItemStatusGridBinding
        val view: View

        if (convertView == null) {
            binding = ItemStatusGridBinding.inflate(LayoutInflater.from(context), parent, false)
            view = binding.root
            view.tag = binding
        } else {
            view = convertView
            binding = convertView.tag as ItemStatusGridBinding
        }

        val item = getItem(position)
        binding.tvGridName.text = item.name
        binding.tvGridQty.text = context.getString(R.string.quantity_format, item.quantity)

        val (label, colorRes) = when (item.status) {
            EquipmentStatus.Available -> Pair(R.string.status_available, R.color.badge_available)
            EquipmentStatus.Low -> Pair(R.string.status_low, R.color.badge_low)
            EquipmentStatus.Out -> Pair(R.string.status_out, R.color.badge_out)
        }
        binding.tvGridBadge.text = context.getString(label)
        binding.tvGridBadge.setBackgroundResource(R.drawable.bg_badge)
        binding.tvGridBadge.background.setTint(context.getColor(colorRes))

        view.isEnabled = item.quantity > 0
        view.alpha = if (item.quantity > 0) 1f else 0.55f
        view.setOnClickListener {
            if (item.quantity > 0) {
                onClick(item)
            }
        }

        return view
    }
}
