package ru.faustyu.paprika.ui.contacts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.faustyu.paprika.data.network.AddContactRequest
import ru.faustyu.paprika.data.network.ContactEntry
import ru.faustyu.paprika.data.network.NetworkModule

class ContactsViewModel : ViewModel() {

    var contacts by mutableStateOf<List<ContactEntry>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)

    fun showError(msg: String?) { error = msg }

    // Search query filtering the contacts list
    var searchQuery by mutableStateOf("")

    val filteredContacts: List<ContactEntry>
        get() {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) return contacts.sortedByDescending { it.last_seen }
            return contacts.filter { c ->
                c.username.lowercase().contains(q) ||
                c.first_name?.lowercase()?.contains(q) == true ||
                c.last_name?.lowercase()?.contains(q) == true
            }.sortedByDescending { it.last_seen }
        }

    init {
        loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val resp = NetworkModule.api.getContacts()
                if (resp.isSuccessful) {
                    contacts = resp.body() ?: emptyList()
                } else {
                    error = "Failed to load contacts"
                }
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun addContact(username: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val resp = NetworkModule.api.addContact(AddContactRequest(username))
                if (resp.isSuccessful) {
                    resp.body()?.let { contacts = contacts + it }
                    onSuccess()
                } else {
                    onError("User not found")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Network error")
            }
        }
    }

    fun deleteContact(userId: Long) {
        viewModelScope.launch {
            try {
                NetworkModule.api.deleteContact(userId)
                contacts = contacts.filter { it.user_id != userId }
            } catch (_: Exception) {}
        }
    }
}
