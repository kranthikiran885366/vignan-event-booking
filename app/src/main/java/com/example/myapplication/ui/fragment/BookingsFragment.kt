package com.example.myapplication.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentBookingsBinding
import com.example.myapplication.ui.BookingsAdapter
import com.example.myapplication.ui.EquipmentViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BookingsFragment : Fragment() {

    private var _binding: FragmentBookingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EquipmentViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            SavedStateViewModelFactory(requireActivity().application, requireActivity())
        )[EquipmentViewModel::class.java]
    }

    private val bookingsAdapter by lazy {
        BookingsAdapter(
            getEquipmentName = { id -> viewModel.getEquipmentName(id) },
            onCancelClick    = { booking ->
                viewModel.cancelBookingById(booking.bookingId)
                (activity as? MainActivity)?.showSnackbar(getString(R.string.snack_booking_undone))
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerBookings.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerBookings.adapter = bookingsAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                val bookings = state.bookingHistory
                bookingsAdapter.submitList(bookings)

                val hasBookings = bookings.isNotEmpty()
                binding.recyclerBookings.isVisible    = hasBookings
                binding.layoutEmptyBookings.isVisible = !hasBookings && !state.isLoading

                // Update count badge
                binding.tvBookingsCount.text = getString(R.string.bookings_count_format, bookings.size)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
