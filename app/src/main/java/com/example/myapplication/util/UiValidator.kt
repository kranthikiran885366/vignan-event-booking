package com.example.myapplication.util

import android.util.Patterns
import java.text.SimpleDateFormat
import java.util.*

object UiValidator {

    private const val MIN_PASSWORD_LENGTH = 8
    private const val MAX_NAME_LENGTH = 50
    private const val MIN_NAME_LENGTH = 2
    private val NAME_REGEX = Regex("^[A-Za-z][A-Za-z .'-]{1,49}$")
    private val STUDENT_ID_REGEX = Regex("^[A-Za-z0-9][A-Za-z0-9-]{3,19}$")
    private val STRICT_DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    fun sanitize(input: String): String {
        return input
            .replace(Regex("[\\u0000-\\u001F]"), "")
            .trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[<>\"'%&;]"), "")
    }

    fun normalizeEmail(input: String): String {
        return sanitize(input).lowercase(Locale.US)
    }

    fun isNonBlank(input: String?): Boolean {
        return !input.isNullOrBlank()
    }

    fun isValidEmail(email: String): Boolean {
        val value = normalizeEmail(email)
        return value.length in 5..254 && Patterns.EMAIL_ADDRESS.matcher(value).matches()
    }

    fun isValidFullName(name: String): Boolean {
        val value = sanitize(name)
        return value.length in MIN_NAME_LENGTH..MAX_NAME_LENGTH && NAME_REGEX.matches(value)
    }

    fun isValidStudentId(studentId: String): Boolean {
        val value = sanitize(studentId)
        return STUDENT_ID_REGEX.matches(value)
    }

    fun isStrongPassword(password: String): Boolean {
        if (password.length < MIN_PASSWORD_LENGTH || password.any { it.isWhitespace() }) return false
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        return hasUpper && hasLower && hasDigit && hasSpecial
    }

    /**
     * Validates if date is YYYY-MM-DD, not in past, and within 30 days.
     * Returns error message or null if valid.
     */
    fun validateDateWindow(dateStr: String): String? {
        val value = sanitize(dateStr)
        if (value.isBlank()) return "Date cannot be empty"
        if (!STRICT_DATE_REGEX.matches(value)) return "Invalid format (YYYY-MM-DD)"

        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.isLenient = false
            val date = sdf.parse(value) ?: return "Invalid format (YYYY-MM-DD)"

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val today = calendar.time

            if (date.before(today)) {
                return "Cannot book for a past date"
            }

            calendar.add(Calendar.DAY_OF_YEAR, 30)
            if (date.after(calendar.time)) {
                return "Maximum 30 days in advance"
            }

            null
        } catch (e: Exception) {
            "Invalid format (YYYY-MM-DD)"
        }
    }
}
