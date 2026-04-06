package com.example.myapplication.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.model.Category
import com.example.myapplication.ui.EquipmentListAdapter
import com.example.myapplication.ui.EquipmentStatusGridAdapter
import com.example.myapplication.ui.EquipmentViewModel
import com.example.myapplication.ui.NotificationHelper
import com.example.myapplication.ui.ReserveConfirmDialogFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment(), ReserveConfirmDialogFragment.ReserveDialogListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EquipmentViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            SavedStateViewModelFactory(requireActivity().application, requireActivity())
        )[EquipmentViewModel::class.java]
    }

    private val recyclerAdapter by lazy {
        EquipmentListAdapter { item, anchor ->
            lastAnchor = anchor
            viewModel.onReserveClicked(item)
        }
    }

    private lateinit var gridAdapter: EquipmentStatusGridAdapter
    private var lastAnchor: View? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearch()
        setupCategorySpinner()
        setupDateInput()
        setupRecyclerView()
        setupGrid()
        observeState()
        observeEvents()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onConfirmReserve(itemId: Int) {
        viewModel.state.value.allItems
            .firstOrNull { it.id == itemId }
            ?.let { viewModel.confirmBooking(it) }
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            viewModel.onSearchChanged(text?.toString().orEmpty())
        }
    }

    private fun setupCategorySpinner() {
        val categories = resources.getStringArray(R.array.category_options)
        binding.spinnerCategory.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spinnerCategory.setSelection(viewModel.state.value.selectedCategory.ordinal, false)
        binding.spinnerCategory.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long
                ) {
                    viewModel.onCategorySelected(Category.entries.getOrElse(pos) { Category.Audio })
                }
                override fun onNothingSelected(p: android.widget.AdapterView<*>?) {
                    viewModel.onCategorySelected(Category.Audio)
                }
            }
    }

    private fun setupDateInput() {
        // Keep state in sync while typing so reserve validation sees the latest date.
        binding.etDate.addTextChangedListener { text ->
            viewModel.onDateInputChanged(text?.toString().orEmpty())
        }

        if (binding.etDate.text.isNullOrBlank()) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            binding.etDate.setText(today)
            viewModel.onDateInputChanged(today)
        }

        binding.etDate.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) viewModel.onDateInputChanged(binding.etDate.text?.toString().orEmpty())
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerEquipment.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerEquipment.adapter = recyclerAdapter
    }

    private fun setupGrid() {
        gridAdapter = EquipmentStatusGridAdapter(requireContext(), emptyList()) { equipment ->
            viewModel.onReserveClicked(equipment)
        }
        binding.gridQuickStatus.adapter = gridAdapter
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                recyclerAdapter.submitList(state.filteredItems)
                gridAdapter.submitData(state.filteredItems)

                val isEmpty = state.filteredItems.isEmpty() && !state.isLoading
                binding.layoutEmpty.isVisible    = isEmpty
                binding.tvEmptyText.text         = state.emptyMessage ?: getString(R.string.no_equipment_available)
                binding.tilDate.error            = state.dateError

                if (binding.spinnerCategory.selectedItemPosition != state.selectedCategory.ordinal) {
                    binding.spinnerCategory.setSelection(state.selectedCategory.ordinal)
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.events.collectLatest { event ->
                when (event) {
                    is EquipmentViewModel.UiEvent.OpenConfirmDialog -> {
                        if (childFragmentManager.findFragmentByTag("reserve_dialog") == null) {
                            ReserveConfirmDialogFragment.newInstance(event.item.id)
                                .show(childFragmentManager, "reserve_dialog")
                        }
                    }
                    is EquipmentViewModel.UiEvent.ShowError ->
                        (activity as? MainActivity)?.showSnackbar(event.message)

                    is EquipmentViewModel.UiEvent.BookingSuccess -> {
                        NotificationHelper.showBookingNotification(
                            requireContext(), event.booking.bookingId
                        )
                        (activity as? MainActivity)?.showSnackbar(
                            getString(R.string.booking_successful),
                            getString(R.string.undo)
                        ) { viewModel.undoBooking() }
                        (activity as? MainActivity)?.navigateToBookings()
                    }
                }
            }
        }
    }
}
