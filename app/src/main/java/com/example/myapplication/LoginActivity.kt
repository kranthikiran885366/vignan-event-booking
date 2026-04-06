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
import com.example.myapplication.databinding.ActivityLoginBinding
import com.example.myapplication.util.UiValidator
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val credentialManager by lazy { CredentialManager.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { attemptEmailLogin() }
        binding.btnGuest.setOnClickListener { signInAnonymously() }
        binding.btnGoogleSignIn.setOnClickListener { signInWithGoogle() }
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.tvForgotPassword.setOnClickListener { sendPasswordReset() }
    }

    // ── Email / Password ──────────────────────────────────────────────────────

    private fun attemptEmailLogin() {
        val email    = UiValidator.normalizeEmail(binding.etEmail.text?.toString().orEmpty())
        val password = binding.etPassword.text?.toString().orEmpty()

        binding.tilEmail.error    = null
        binding.tilPassword.error = null
        var valid = true

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
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                lifecycleScope.launch {
                    val user = result.user ?: run { setLoading(false); showError("Auth failed"); return@launch }
                    saveFcmToken(user.uid)
                    val profile = fetchUserProfile(user.uid)
                    val name = profile?.getString("fullName")?.ifBlank { null }
                        ?: user.displayName
                        ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
                    setLoading(false)
                    goToMain(name, email)
                }
            }
            .addOnFailureListener { err ->
                setLoading(false)
                showError(friendlyAuthError(err.message))
            }
    }

    // ── Google Sign-In ────────────────────────────────────────────────────────

    private fun signInWithGoogle() {
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
                val result = credentialManager.getCredential(this@LoginActivity, request)
                val credential = result.credential

                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken.idToken, null)

                    val authResult = auth.signInWithCredential(firebaseCredential).await()
                    val user = authResult.user ?: run {
                        setLoading(false); showError("Google sign-in failed"); return@launch
                    }

                    val isNewUser = authResult.additionalUserInfo?.isNewUser == true
                    if (isNewUser) {
                        createUserProfile(
                            uid       = user.uid,
                            fullName  = user.displayName.orEmpty(),
                            email     = user.email.orEmpty(),
                            studentId = "",
                            photoUrl  = user.photoUrl?.toString()
                        )
                    }

                    saveFcmToken(user.uid)
                    setLoading(false)
                    goToMain(
                        name  = user.displayName ?: "User",
                        email = user.email.orEmpty()
                    )
                } else {
                    setLoading(false)
                    showError("Unsupported credential type")
                }
            } catch (e: GetCredentialException) {
                setLoading(false)
                showError("Google Sign-In failed: ${e.localizedMessage}")
            } catch (e: Exception) {
                setLoading(false)
                showError("Google Sign-In error: ${e.localizedMessage}")
            }
        }
    }

    // ── Anonymous / Guest ─────────────────────────────────────────────────────

    private fun signInAnonymously() {
        setLoading(true)
        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) {
                    lifecycleScope.launch { saveFcmToken(uid) }
                }
                setLoading(false)
                goToMain("Guest", "guest@vignan.ac.in")
            }
            .addOnFailureListener { err ->
                setLoading(false)
                showError(err.localizedMessage ?: "Guest login failed")
            }
    }

    // ── Password Reset ────────────────────────────────────────────────────────

    private fun sendPasswordReset() {
        val email = UiValidator.normalizeEmail(binding.etEmail.text?.toString().orEmpty())
        if (!UiValidator.isValidEmail(email)) {
            binding.tilEmail.error = getString(R.string.error_email_invalid)
            return
        }
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Snackbar.make(binding.root, "Password reset link sent to $email", Snackbar.LENGTH_LONG).show()
            }
            .addOnFailureListener { err ->
                showError(err.localizedMessage ?: "Failed to send reset email")
            }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun fetchUserProfile(uid: String) = runCatching {
        firestore.collection("users").document(uid).get().await()
    }.getOrNull()

    private fun createUserProfile(
        uid: String, fullName: String, email: String,
        studentId: String, photoUrl: String? = null
    ) {
        val data = hashMapOf(
            "uid"       to uid,
            "fullName"  to fullName,
            "email"     to email,
            "studentId" to studentId,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (!photoUrl.isNullOrBlank()) data["photoUrl"] = photoUrl
        firestore.collection("users").document(uid).set(data)
    }

    private fun saveFcmToken(uid: String) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            // Use set with merge to avoid "document not found" error
            firestore.collection("users").document(uid)
                .set(mapOf("fcmToken" to token), SetOptions.merge())
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnLogin.isEnabled        = !isLoading
        binding.btnGuest.isEnabled        = !isLoading
        binding.btnGoogleSignIn.isEnabled = !isLoading
        binding.progressLogin.visibility  = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.alpha            = if (isLoading) 0.6f else 1f
        binding.btnGoogleSignIn.alpha     = if (isLoading) 0.6f else 1f
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun goToMain(name: String, email: String) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_USER_NAME, name)
            putExtra(MainActivity.EXTRA_USER_EMAIL, email)
        })
        finish()
    }

    private fun friendlyAuthError(message: String?): String = when {
        message == null                          -> "Login failed. Try again."
        "no user record"   in message            -> "No account found with this email."
        "password is invalid" in message         -> "Incorrect password."
        "blocked"          in message            -> "Too many attempts. Try later."
        "network"          in message            -> "Network error. Check your connection."
        else                                     -> message
    }
}
