package com.example.myapplication.ui

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.myapplication.R

class ReserveConfirmDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val itemId = requireArguments().getInt(ARG_ITEM_ID)

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_reservation_title)
            .setMessage(R.string.confirm_reservation_message)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val listener = parentFragment as? ReserveDialogListener
                    ?: activity as? ReserveDialogListener
                listener?.onConfirmReserve(itemId)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    interface ReserveDialogListener {
        fun onConfirmReserve(itemId: Int)
    }

    companion object {
        private const val ARG_ITEM_ID = "arg_item_id"

        fun newInstance(itemId: Int): ReserveConfirmDialogFragment {
            return ReserveConfirmDialogFragment().apply {
                arguments = Bundle().apply { putInt(ARG_ITEM_ID, itemId) }
            }
        }
    }
}
