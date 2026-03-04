package ru.faustyu.paprika.ui.base

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * Base ViewModel with common state management
 */
abstract class BaseViewModel : ViewModel() {
    
    var isLoading by mutableStateOf(false)
        protected set
    
    var error by mutableStateOf<String?>(null)
        protected set
    
    /**
     * Show loading indicator
     */
    protected fun showLoading() {
        isLoading = true
        error = null
    }
    
    /**
     * Hide loading indicator
     */
    protected fun hideLoading() {
        isLoading = false
    }
    
    /**
     * Show error message
     */
    protected fun showError(message: String) {
        isLoading = false
        error = message
    }
    
    /**
     * Clear error message
     */
    protected fun clearError() {
        error = null
    }
    
    /**
     * Handle exception and show error
     */
    protected fun handleException(e: Exception, defaultMessage: String = "An error occurred") {
        showError(e.message ?: defaultMessage)
    }
}
