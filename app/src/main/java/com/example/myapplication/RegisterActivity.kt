package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.databinding.ActivityRegisterBinding
import com.example.myapplication.util.UiValidator
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val credentialManager by lazy { CredentialManager.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener { attemptRegister() }
        binding.btnGoogleSignUp.setOnClickListener { signUpWithGoogle() }
        binding.tvLogin.setOnClickListener { finish() }
    }

    // ── Email / Password Register ─────────────────────────────────────────────

    private fun attemptRegister() {
        val name      = UiValidator.sanitize(binding.etFullName.text?.toString().orEmpty())
        val studentId = UiValidator.sanitize(binding.etStudentId.text?.toString().orEmpty())
        val email     = UiValidator.normalizeEmail(binding.etEmail.text?.toString().orEmpty())
        val password  = binding.etPassword.text?.toString().orEmpty()

        binding.tilFullName.error  = null
        binding.tilStudentId.error = null
        binding.tilEmail.error     = null
        binding.tilPassword.error  = null
        var valid = true

        if (!UiValidator.isValidFullName(name)) {
            binding.tilFullName.error = "Enter 2-50 letters (spaces . ' - allowed)"; valid = false
        }
        if (!UiValidator.isValidStudentId(studentId)) {
            binding.tilStudentId.error = "Student ID must be 4-20 chars (letters, numbers, -)"; valid = false
        }
        if (!UiValidator.isNonBlank(email)) {
            binding.tilEmail.error = getString(R.string.error_email_empty); valid = false
        } else if (!UiValidator.isValidEmail(email)) {
            binding.tilEmail.error = getString(R.string.error_email_invalid); valid = false
        }
        if (!UiValidator.isNonBlank(password)) {
            binding.tilPassword.error = getString(R.string.error_password_empty); valid = false
        } else if (!UiValidator.isStrongPassword(password)) {
            binding.tilPassword.error = "Use 8+ chars with upper, lower, number, special char"; valid = false
        }
        if (!valid) return

        setLoading(true)
        lifecycleScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user ?: run {
                    setLoading(false)
                    showError("Registration failed. Try again.")
                    return@launch
                }

                // Update Firebase Auth display name
                user.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(name).build()
                ).await()

                // Save profile to Firestore
                saveUserProfile(user.uid, name, studentId, email, null)

                // Save FCM token
                saveFcmToken(user.uid)

                setLoading(false)
                showSuccess(getString(R.string.register_success))
                goToMain(name, email)
            } catch (e: Exception) {
                setLoading(false)
                showError(e.localizedMessage ?: "Registration failed")
            }
        }
    }

    // ── Google Sign-Up ────────────────────────────────────────────────────────

    private fun signUpWithGoogle() {
        setLoading(true)
        val webClientId = getString(R.string.default_web_client_id)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@RegisterActivity, request)
                val credential = result.credential

                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken.idToken, null)
                    val authResult = auth.signInWithCredential(firebaseCredential).await()
                    val user = authResult.user ?: run {
                        setLoading(false); showError("Google sign-up failed"); return@launch
                    }

                    val isNew = authResult.additionalUserInfo?.isNewUser == true
                    if (isNew) {
                        saveUserProfile(
                            uid       = user.uid,
                            fullName  = user.displayName.orEmpty(),
                            studentId = "",
                            email     = user.email.orEmpty(),
                            photoUrl  = user.photoUrl?.toString()
                        )
                    }

                    saveFcmToken(user.uid)
                    setLoading(false)
                    goToMain(user.displayName ?: "User", user.email.orEmpty())
                } else {
                    setLoading(false)
                    showError("Unsupported credential type")
                }
            } catch (e: GetCredentialException) {
                setLoading(false)
                showError("Google Sign-Up failed: ${e.localizedMessage}")
            } catch (e: Exception) {
                setLoading(false)
                showError("Error: ${e.localizedMessage}")
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun saveUserProfile(
        uid: String, fullName: String, studentId: String,
        email: String, photoUrl: String?
    ) {
        val data = hashMapOf(
            "uid"       to uid,
            "fullName"  to fullName,
            "studentId" to studentId,
            "email"     to email,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (!photoUrl.isNullOrBlank()) data["photoUrl"] = photoUrl
        firestore.collection("users").document(uid).set(data).await()
    }

    private fun saveFcmToken(uid: String) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            // Use set with merge to avoid "document not found" error
            firestore.collection("users").document(uid).set(
                mapOf("fcmToken" to token),
                com.google.firebase.firestore.SetOptions.merge()
            )
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnRegister.isEnabled     = !isLoading
        binding.btnGoogleSignUp.isEnabled = !isLoading
        binding.progressRegister.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.alpha         = if (isLoading) 0.6f else 1f
    }

    private fun showError(message: String) =
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()

    private fun showSuccess(message: String) =
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()

    private fun goToMain(name: String, email: String) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_USER_NAME, name)
            putExtra(MainActivity.EXTRA_USER_EMAIL, email)
        })
        finishAffinity()
    }
}
