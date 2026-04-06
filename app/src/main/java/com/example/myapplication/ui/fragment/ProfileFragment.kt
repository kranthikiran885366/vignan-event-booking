package com.example.myapplication.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.example.myapplication.LoginActivity
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentProfileBinding
import com.example.myapplication.ui.EquipmentViewModel
import com.example.myapplication.util.UiValidator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth      by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private var profileListener: ListenerRegistration? = null

    private val viewModel: EquipmentViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            SavedStateViewModelFactory(requireActivity().application, requireActivity())
        )[EquipmentViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set initial values from MainActivity intent
        val main = requireActivity() as MainActivity
        binding.tvUserName.text  = main.userName
        binding.tvUserEmail.text = main.userEmail

        // Load Google profile photo if available
        auth.currentUser?.photoUrl?.let { photoUri ->
            binding.ivAvatar.load(photoUri) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.img_app_logo_bg)
                error(R.drawable.img_app_logo_bg)
            }
        }

        startRealtimeProfileListener()
        observeBookingStats()

        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        binding.btnLogout.setOnClickListener {
            profileListener?.remove()
            auth.signOut()
            startActivity(
                Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
        }
    }

    override fun onDestroyView() {
        profileListener?.remove()
        super.onDestroyView()
        _binding = null
    }

    // ── Real-time Firestore profile listener ──────────────────────────────────

    private fun startRealtimeProfileListener() {
        val uid = auth.currentUser?.uid ?: return
        profileListener?.remove()
        profileListener = firestore
            .collection(EquipmentViewModel.COLLECTION_USERS)
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (!isAdded || _binding == null) return@addSnapshotListener
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val fullName  = snapshot.getString("fullName").orEmpty()
                val email     = snapshot.getString("email").orEmpty()
                val studentId = snapshot.getString("studentId").orEmpty()
                val photoUrl  = snapshot.getString("photoUrl")
                val createdAt = snapshot.getTimestamp("createdAt")

                if (fullName.isNotBlank())  binding.tvUserName.text  = fullName
                if (email.isNotBlank())     binding.tvUserEmail.text = email
                binding.tvStudentId.text   = studentId.ifBlank { "—" }
                binding.tvMemberSince.text = createdAt?.toDate()?.let {
                    SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(it)
                } ?: "—"

                // Load photo from Firestore if not already loaded from Auth
                if (!photoUrl.isNullOrBlank() && auth.currentUser?.photoUrl == null) {
                    binding.ivAvatar.load(photoUrl) {
                        crossfade(true)
                        transformations(CircleCropTransformation())
                        placeholder(R.drawable.img_app_logo_bg)
                        error(R.drawable.img_app_logo_bg)
                    }
                }
            }
    }

    // ── Booking stats from ViewModel (real-time Firestore) ────────────────────

    private fun observeBookingStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                val total  = state.bookingHistory.size
                val active = state.bookingHistory.count { it.status == "CONFIRMED" }
                binding.tvTotalBookings.text  = total.toString()
                binding.tvActiveBookings.text = active.toString()
            }
        }
    }

    // ── Edit Profile Dialog ────────────────────────────────────────────────────

    private fun showEditProfileDialog() {
        val uid = auth.currentUser?.uid ?: return

        // Get current values from Firestore or Auth
        val currentName = binding.tvUserName.text.toString()
        val currentStudentId = binding.tvStudentId.text.toString().takeIf { it != "—" } ?: ""

        // Build a clean vertical form for better readability and spacing.
        val density = resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }

        val etName = EditText(requireContext()).apply {
            hint = "Full Name"
            setText(currentName)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setSingleLine(true)
            setSelection(text.length)
        }

        val etStudentId = EditText(requireContext()).apply {
            hint = "Student ID"
            setText(currentStudentId)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setSingleLine(true)
        }

        val etEmail = EditText(requireContext()).apply {
            hint = "Email (read-only)"
            setText(auth.currentUser?.email ?: "")
            isEnabled = false
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setSingleLine(true)
        }

        val fieldParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(12) }

        container.addView(etName, fieldParams)
        container.addView(etStudentId, fieldParams)
        container.addView(etEmail, fieldParams)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(container)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newName = etName.text.toString().trim()
                val newStudentId = etStudentId.text.toString().trim()

                // Validate inputs
                if (!UiValidator.isValidFullName(newName)) {
                    etName.error = "Enter a valid full name"
                    return@setOnClickListener
                }

                if (newStudentId.isNotBlank() && !UiValidator.isValidStudentId(newStudentId)) {
                    etStudentId.error = "Invalid student ID format"
                    return@setOnClickListener
                }

                saveProfileChanges(uid, newName, newStudentId)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun saveProfileChanges(uid: String, newName: String, newStudentId: String) {
        val updates = mapOf(
            "fullName" to newName,
            "studentId" to newStudentId,
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        // Use .set() with merge to create doc if not exists, or update if exists
        firestore.collection("users").document(uid).set(updates, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                // Also update Auth display name
                   val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                       displayName = newName
                   }
                   auth.currentUser?.updateProfile(profileUpdates)?.addOnSuccessListener {
                       (activity as? MainActivity)?.showSnackbar("Profile updated successfully")
                   }?.addOnFailureListener { e ->
                       (activity as? MainActivity)?.showSnackbar("Failed to update Auth: ${e.message}")
                   }
            }
            .addOnFailureListener { e ->
                (activity as? MainActivity)?.showSnackbar("Failed to save profile: ${e.message}")
            }
    }
}
