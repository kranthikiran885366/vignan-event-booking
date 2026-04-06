package com.example.myapplication.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.BookingUi
import com.example.myapplication.model.Category
import com.example.myapplication.model.EquipmentUi
import com.example.myapplication.util.UiValidator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EquipmentViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val auth      = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage   = FirebaseStorage.getInstance()

    private var clickGuardJob: Job? = null
    private var equipmentListener: ListenerRegistration? = null
    private var bookingsListener: ListenerRegistration? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    private val restoredCategory: Category = run {
        val name = savedStateHandle.get<String>(KEY_CATEGORY)
        Category.entries.firstOrNull { it.name == name } ?: Category.Audio
    }
    private val restoredDate: String   = savedStateHandle.get<String>(KEY_DATE)   ?: ""
    private val restoredScroll: Int    = savedStateHandle.get<Int>(KEY_SCROLL)    ?: 0
    private val restoredSearch: String = savedStateHandle.get<String>(KEY_SEARCH) ?: ""

    private val _state = MutableStateFlow(
        EquipmentUiState(
            allItems           = emptyList(),
            filteredItems      = emptyList(),
            selectedCategory   = restoredCategory,
            bookingDateInput   = restoredDate,
            searchQuery        = restoredSearch,
            bookingHistory     = emptyList(),
            listScrollPosition = restoredScroll,
            isLoading          = true
        )
    )
    val state: StateFlow<EquipmentUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        startRealtimeListeners()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun onCategorySelected(category: Category?) {
        val safe = category ?: Category.Audio
        _state.update {
            val filtered = applyFilters(it.allItems, safe, it.searchQuery)
            it.copy(
                selectedCategory = safe,
                filteredItems    = filtered,
                emptyMessage     = if (filtered.isEmpty()) "No equipment in this category" else null
            )
        }
        savedStateHandle[KEY_CATEGORY] = safe.name
    }

    fun onSearchChanged(query: String) {
        val sanitized = UiValidator.sanitize(query)
        _state.update {
            val filtered = applyFilters(it.allItems, it.selectedCategory, sanitized)
            it.copy(
                searchQuery   = sanitized,
                filteredItems = filtered,
                emptyMessage  = if (filtered.isEmpty()) "No results for \"$sanitized\"" else null
            )
        }
        savedStateHandle[KEY_SEARCH] = sanitized
    }

    fun onDateInputChanged(input: String) {
        val sanitized = UiValidator.sanitize(input)
        _state.update {
            it.copy(
                bookingDateInput = sanitized,
                dateError        = UiValidator.validateDateWindow(sanitized)
            )
        }
        savedStateHandle[KEY_DATE] = sanitized
    }

    fun recordScrollPosition(position: Int) {
        _state.update { it.copy(listScrollPosition = position) }
        savedStateHandle[KEY_SCROLL] = position
    }

    fun getSavedScrollPosition(): Int = _state.value.listScrollPosition

    fun getEquipmentName(equipmentId: Int): String =
        _state.value.allItems.firstOrNull { it.id == equipmentId }?.name
            ?: "Equipment #$equipmentId"

    /** Debounced reserve click — 500ms guard */
    fun onReserveClicked(equipment: EquipmentUi) {
        if (clickGuardJob?.isActive == true) return
        clickGuardJob = viewModelScope.launch {
            val error = validateBooking(equipment)
            if (error != null) _events.emit(UiEvent.ShowError(error))
            else               _events.emit(UiEvent.OpenConfirmDialog(equipment))
            delay(500)
        }
    }

    /**
     * Atomic Firestore transaction:
     * 1. Read equipment quantity
     * 2. Decrement quantity
     * 3. Write booking document
     * All in one transaction — if any step fails, everything rolls back.
     */
    fun confirmBooking(equipment: EquipmentUi) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                _events.emit(UiEvent.ShowError("Please sign in to book equipment"))
                return@launch
            }

            val error = validateBooking(equipment)
            if (error != null) { _events.emit(UiEvent.ShowError(error)); return@launch }

            val bookingDate = _state.value.bookingDateInput

            // Check duplicate in current state before hitting Firestore
            val alreadyBooked = _state.value.bookingHistory.any {
                it.equipmentId == equipment.id &&
                it.userId      == uid &&
                it.date        == bookingDate &&
                it.status      == "CONFIRMED"
            }
            if (alreadyBooked) {
                _events.emit(UiEvent.ShowError("You already have a booking for this equipment on $bookingDate"))
                return@launch
            }

            _state.update { it.copy(isLoading = true) }

            try {
                val newBookingId = firestore.runTransaction { txn ->
                    // Read current equipment state from Firestore
                    val equipRef  = firestore.collection(COLLECTION_EQUIPMENT).document(equipment.id.toString())
                    val equipSnap = txn.get(equipRef)
                    val currentQty = (equipSnap.getLong("quantity") ?: 0L).toInt()

                    // Read user profile to snapshot booking owner details in history.
                    val userRef = firestore.collection(COLLECTION_USERS).document(uid)
                    val userSnap = txn.get(userRef)
                    val userFullName = userSnap.getString("fullName")
                        .orEmpty()
                        .ifBlank { auth.currentUser?.displayName.orEmpty() }
                    val userStudentId = userSnap.getString("studentId").orEmpty()
                    val userEmail = userSnap.getString("email")
                        .orEmpty()
                        .ifBlank { auth.currentUser?.email.orEmpty() }

                    if (currentQty <= 0) {
                        throw FirebaseFirestoreException(
                            "This equipment is out of stock",
                            FirebaseFirestoreException.Code.ABORTED
                        )
                    }

                    // Create booking document reference
                    val bookingRef = firestore.collection(COLLECTION_BOOKINGS).document()

                    // Atomically decrement quantity and write booking
                    txn.update(equipRef, mapOf(
                        "quantity"    to (currentQty - 1),
                        "bookingDate" to bookingDate,
                        "available"   to (currentQty - 1 > 0)
                    ))
                    txn.set(bookingRef, mapOf(
                        "bookingId"   to bookingRef.id,
                        "equipmentId" to equipment.id,
                        "equipmentName" to equipment.name,
                        "userId"      to uid,
                        "userFullName" to userFullName,
                        "studentId"   to userStudentId,
                        "userEmail"   to userEmail,
                        "date"        to bookingDate,
                        "status"      to "CONFIRMED",
                        "location"    to equipment.location,
                        "category"    to equipment.category.name,
                        "createdAt"   to FieldValue.serverTimestamp()
                    ))

                    bookingRef.id
                }.await()

                _state.update { it.copy(isLoading = false) }

                val confirmedBooking = BookingUi(
                    bookingId   = newBookingId,
                    equipmentId = equipment.id,
                    userId      = uid,
                    equipmentName = equipment.name,
                    location      = equipment.location,
                    category      = equipment.category.name,
                    userFullName  = auth.currentUser?.displayName.orEmpty(),
                    userEmail     = auth.currentUser?.email.orEmpty(),
                    date        = bookingDate,
                    status      = "CONFIRMED"
                )
                _state.update { it.copy(pendingBooking = confirmedBooking) }
                _events.emit(UiEvent.BookingSuccess(confirmedBooking))

            } catch (ex: FirebaseFirestoreException) {
                _state.update { it.copy(isLoading = false) }
                _events.emit(UiEvent.ShowError(toUserFriendlyFirestoreError(ex)))
            } catch (ex: Exception) {
                _state.update { it.copy(isLoading = false) }
                _events.emit(UiEvent.ShowError(ex.localizedMessage ?: "Booking failed. Please try again."))
            }
        }
    }

    /** Undo last booking — cancels it in Firestore */
    fun undoBooking() {
        val booking = _state.value.pendingBooking ?: return
        cancelBookingById(booking.bookingId)
        _state.update { it.copy(pendingBooking = null) }
    }

    /**
     * Cancel booking — atomic transaction:
     * 1. Mark booking as CANCELLED in Firestore
     * 2. Restore equipment quantity
     */
    fun cancelBookingById(bookingId: String) {
        val booking = _state.value.bookingHistory.firstOrNull { it.bookingId == bookingId }
            ?: return
        if (booking.status == "CANCELLED") return

        viewModelScope.launch {
            try {
                firestore.runTransaction { txn ->
                    val bookingRef  = firestore.collection(COLLECTION_BOOKINGS).document(bookingId)
                    val bookingSnap = txn.get(bookingRef)

                    // Only cancel CONFIRMED bookings
                    val currentStatus = bookingSnap.getString("status") ?: ""
                    if (currentStatus != "CONFIRMED") return@runTransaction

                    val equipmentId = (bookingSnap.getLong("equipmentId")
                        ?: booking.equipmentId.toLong()).toInt()
                    val equipRef    = firestore.collection(COLLECTION_EQUIPMENT).document(equipmentId.toString())
                    val equipSnap   = txn.get(equipRef)
                    val currentQty  = (equipSnap.getLong("quantity") ?: 0L).toInt()

                    // Mark booking cancelled
                    txn.update(bookingRef, mapOf(
                        "status"      to "CANCELLED",
                        "cancelledAt" to FieldValue.serverTimestamp()
                    ))
                    // Restore quantity
                    txn.update(equipRef, mapOf(
                        "quantity"  to (currentQty + 1),
                        "available" to true
                    ))
                }.await()
            } catch (ex: Exception) {
                _events.emit(UiEvent.ShowError(ex.localizedMessage ?: "Failed to cancel booking"))
            }
        }
    }

    // ── Real-time Firestore Listeners ─────────────────────────────────────────

    private fun startRealtimeListeners() {
        // Equipment should always load on Home, even if auth restore is still in progress.
        startEquipmentListener()
        refreshBookingsListenerForCurrentUser()

        authStateListener = FirebaseAuth.AuthStateListener {
            refreshBookingsListenerForCurrentUser()
        }
        auth.addAuthStateListener(authStateListener!!)
    }

    private fun refreshBookingsListenerForCurrentUser() {
        bookingsListener?.remove()
        bookingsListener = null

        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _state.update { it.copy(bookingHistory = emptyList()) }
            return
        }
        startBookingsListener(uid)
    }

    private fun startEquipmentListener() {
        equipmentListener?.remove()
        equipmentListener = firestore.collection(COLLECTION_EQUIPMENT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    viewModelScope.launch {
                        val userMessage = when {
                            error.message?.contains("permission", ignoreCase = true) == true -> 
                                "Permission denied. Check Firestore rules in Firebase Console."
                            error.message?.contains("(default)", ignoreCase = true) == true ->
                                "Firestore database not created. See FIRESTORE_DATABASE_FIX.md"
                            else -> error.localizedMessage ?: "Failed to load equipment"
                        }
                        _events.emit(UiEvent.ShowError(userMessage))
                    }
                    android.util.Log.e("EquipmentViewModel", "Equipment query error: ${error.message}")
                    return@addSnapshotListener
                }

                viewModelScope.launch {
                    val remoteItems = snapshot?.documents.orEmpty().mapNotNull { mapEquipment(it) }
                    val items = if (remoteItems.isEmpty()) defaultEquipmentItems() else remoteItems
                    android.util.Log.d("EquipmentViewModel", "Loaded ${remoteItems.size} remote equipment items; showing ${items.size} items")
                    _state.update { current ->
                        val filtered = applyFilters(items, current.selectedCategory, current.searchQuery)
                        current.copy(
                            allItems      = items,
                            filteredItems = filtered,
                            isLoading     = false,
                            emptyMessage  = if (filtered.isEmpty()) "No equipment available" else null
                        )
                    }

                    if (remoteItems.isEmpty()) {
                        _events.emit(
                            UiEvent.ShowError(
                                "Firestore equipment is empty. Showing built-in defaults. Publish firestore.rules to enable real-time seeded data."
                            )
                        )
                    }
                }
            }
    }

    private fun startBookingsListener(uid: String) {
        bookingsListener?.remove()
        bookingsListener = firestore.collection(COLLECTION_BOOKINGS)
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    viewModelScope.launch {
                        _events.emit(UiEvent.ShowError(
                            error.localizedMessage ?: "Failed to load bookings"
                        ))
                    }
                    return@addSnapshotListener
                }

                val bookings = snapshot?.documents.orEmpty().mapNotNull { mapBooking(it) }
                    .sortedByDescending { it.status == "CONFIRMED" }  // Sort confirmed first, then cancelled
                _state.update { it.copy(bookingHistory = bookings) }
            }
    }

    // ── Firestore Document Mappers ────────────────────────────────────────────

    private fun mapEquipment(doc: DocumentSnapshot): EquipmentUi? {
        val id = parseIntField(doc, "id")
            ?: doc.id.toIntOrNull()
            ?: return null
        val categoryName = doc.getString("category").orEmpty().trim()
        val category = Category.entries.firstOrNull {
            it.name.equals(categoryName, ignoreCase = true)
        } ?: Category.Misc

        // Resolve image: stored URL first, then Firebase Storage path, then empty
        val storedUrl  = doc.getString("imageUrl").orEmpty()
        val imageUrl = if (storedUrl.isNotBlank()) storedUrl else null
        val quantity = parseIntField(doc, "quantity") ?: 0

        return EquipmentUi(
            id          = id,
            name        = doc.getString("name").orEmpty(),
            category    = category,
            quantity    = quantity,
            location    = doc.getString("location").orEmpty(),
            bookingDate = doc.getString("bookingDate").orEmpty(),
            imageUrl    = imageUrl
        )
    }

    private fun mapBooking(doc: DocumentSnapshot): BookingUi? {
        val userId = doc.getString("userId") ?: return null
        return BookingUi(
            bookingId   = doc.getString("bookingId").ifNullOrBlank { doc.id },
            equipmentId = (doc.getLong("equipmentId") ?: return null).toInt(),
            userId      = userId,
            equipmentName = doc.getString("equipmentName").orEmpty(),
            location      = doc.getString("location").orEmpty(),
            category      = doc.getString("category").orEmpty(),
            userFullName  = doc.getString("userFullName").orEmpty(),
            studentId     = doc.getString("studentId").orEmpty(),
            userEmail     = doc.getString("userEmail").orEmpty(),
            date        = doc.getString("date").orEmpty(),
            status      = doc.getString("status").ifNullOrBlank { "CONFIRMED" }
        )
    }

    private fun toUserFriendlyFirestoreError(ex: FirebaseFirestoreException): String {
        val msg = ex.message.orEmpty()
        return when {
            msg.contains("(default)", ignoreCase = true) &&
                msg.contains("does not exist", ignoreCase = true) -> {
                "❌ Firestore database not created.\n\n" +
                "Fix: Open Firebase Console → Firestore Database → Click 'Create Database' → " +
                "Choose your region → Start in test mode → Create.\n\n" +
                "Then apply security rules from docs/FIRESTORE_SETUP_DETAILED.md"
            }
            msg.contains("permission", ignoreCase = true) ||
                msg.contains("Failed to get document", ignoreCase = true) -> {
                "❌ Firestore permission denied.\n\n" +
                "Fix: Check Firestore Console → Rules tab → " +
                "Make sure rules from firestore.rules are published.\n\n" +
                "Or restart the app and try again."
            }
            msg.contains("UNAUTHENTICATED", ignoreCase = true) -> {
                "❌ Not authenticated with Firebase.\n\n" +
                "Fix: Make sure you're logged in with email/password or Google."
            }
            else -> msg.ifBlank { "Booking failed" }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun applyFilters(
        items: List<EquipmentUi>,
        category: Category,
        query: String
    ): List<EquipmentUi> {
        val byCategory = items.filter { it.category == category }
        // If category-filter produces nothing, fall back to all items so Home never looks broken.
        val base = if (byCategory.isEmpty() && items.isNotEmpty() && query.isBlank()) items else byCategory
        return base.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    }

    private fun parseIntField(doc: DocumentSnapshot, key: String): Int? {
        doc.getLong(key)?.toInt()?.let { return it }
        doc.getString(key)?.trim()?.toIntOrNull()?.let { return it }
        return null
    }

    private fun defaultEquipmentItems(): List<EquipmentUi> = listOf(
        EquipmentUi(1, "Projector X1", Category.Visual, 4, "Hall A", "", null),
        EquipmentUi(2, "Wireless Mic", Category.Audio, 2, "Studio B", "", null),
        EquipmentUi(3, "Speaker Unit", Category.Audio, 0, "Store C", "", null),
        EquipmentUi(4, "LED Panel", Category.Visual, 7, "Lab D", "", null),
        EquipmentUi(5, "Tripod Stand", Category.Misc, 5, "Room E", "", null),
        EquipmentUi(6, "HDMI Cable", Category.Misc, 1, "Store C", "", null),
        EquipmentUi(7, "Boom Mic", Category.Audio, 3, "Studio A", "", null),
        EquipmentUi(8, "LCD Screen", Category.Visual, 0, "Hall B", "", null),
        EquipmentUi(9, "Laser Pointer", Category.Misc, 6, "Room F", "", null),
        EquipmentUi(10, "PA System", Category.Audio, 2, "Auditorium", "", null)
    )

    private fun validateBooking(equipment: EquipmentUi): String? {
        if (!UiValidator.isNonBlank(equipment.name))     return "Equipment name is missing"
        if (!UiValidator.isNonBlank(equipment.location)) return "Equipment location is missing"
        if (equipment.quantity <= 0)                     return "This equipment is out of stock"
        return UiValidator.validateDateWindow(_state.value.bookingDateInput)
    }

    private fun String?.ifNullOrBlank(default: () -> String): String =
        if (this.isNullOrBlank()) default() else this

    override fun onCleared() {
        equipmentListener?.remove()
        bookingsListener?.remove()
        authStateListener?.let { auth.removeAuthStateListener(it) }
        super.onCleared()
    }

    // ── Events ────────────────────────────────────────────────────────────────

    sealed class UiEvent {
        data class OpenConfirmDialog(val item: EquipmentUi) : UiEvent()
        data class ShowError(val message: String) : UiEvent()
        data class BookingSuccess(val booking: BookingUi) : UiEvent()
    }

    companion object {
        private const val KEY_CATEGORY         = "sel_category"
        private const val KEY_DATE             = "booking_date"
        private const val KEY_SCROLL           = "scroll_pos"
        private const val KEY_SEARCH           = "search_query"
        const val COLLECTION_EQUIPMENT = "equipment"
        const val COLLECTION_BOOKINGS  = "bookings"
        const val COLLECTION_USERS     = "users"
    }
}
