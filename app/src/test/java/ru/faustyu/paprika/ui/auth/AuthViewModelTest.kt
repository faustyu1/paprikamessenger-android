package ru.faustyu.paprika.ui.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ru.faustyu.paprika.data.repository.AuthRepository
import ru.faustyu.paprika.util.Constants
import ru.faustyu.paprika.util.Result

/**
 * Unit tests for AuthViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: AuthViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk()
        viewModel = AuthViewModel(authRepository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `authenticate with empty username shows error`() {
        // Given
        var successCalled = false
        
        // When
        viewModel.authenticate(
            isLogin = true,
            username = "",
            password = "password",
            onSuccess = { successCalled = true }
        )
        
        // Then
        assertEquals("Username and password cannot be empty", viewModel.error)
        assertFalse(successCalled)
    }
    
    @Test
    fun `authenticate with empty password shows error`() {
        // Given
        var successCalled = false
        
        // When
        viewModel.authenticate(
            isLogin = true,
            username = "user",
            password = "",
            onSuccess = { successCalled = true }
        )
        
        // Then
        assertEquals("Username and password cannot be empty", viewModel.error)
        assertFalse(successCalled)
    }
    
    @Test
    fun `register with short password shows error`() {
        // Given
        var successCalled = false
        
        // When
        viewModel.authenticate(
            isLogin = false,
            username = "newuser",
            password = "123",
            firstName = "Test",
            onSuccess = { successCalled = true }
        )
        
        // Then
        assertTrue(viewModel.error?.contains("${Constants.MIN_PASSWORD_LENGTH} characters") == true)
        assertFalse(successCalled)
    }
    
    @Test
    fun `register with short username shows error`() {
        // Given
        var successCalled = false
        
        // When
        viewModel.authenticate(
            isLogin = false,
            username = "ab",
            password = "password123",
            firstName = "Test",
            onSuccess = { successCalled = true }
        )
        
        // Then
        assertTrue(viewModel.error?.contains("${Constants.MIN_USERNAME_LENGTH} characters") == true)
        assertFalse(successCalled)
    }
    
    @Test
    fun `register with username starting with number shows error`() {
        // Given
        var successCalled = false
        
        // When
        viewModel.authenticate(
            isLogin = false,
            username = "123user",
            password = "password123",
            firstName = "Test",
            onSuccess = { successCalled = true }
        )
        
        // Then
        assertEquals("Username cannot start with a number", viewModel.error)
        assertFalse(successCalled)
    }
    
    @Test
    fun `login success calls onSuccess callback`() = runTest {
        // Given
        val token = "test_token"
        var receivedToken: String? = null
        
        coEvery {
            authRepository.login(any(), any())
        } returns Result.Success(token)
        
        // When
        viewModel.authenticate(
            isLogin = true,
            username = "user",
            password = "password",
            onSuccess = { receivedToken = it }
        )
        
        advanceUntilIdle()
        
        // Then
        assertEquals(token, receivedToken)
        assertFalse(viewModel.isLoading)
        assertNull(viewModel.error)
    }
    
    @Test
    fun `login failure shows error`() = runTest {
        // Given
        val errorMessage = "Invalid credentials"
        
        coEvery {
            authRepository.login(any(), any())
        } returns Result.Error(Exception(errorMessage))
        
        // When
        viewModel.authenticate(
            isLogin = true,
            username = "user",
            password = "wrongpassword",
            onSuccess = {}
        )
        
        advanceUntilIdle()
        
        // Then
        assertEquals(errorMessage, viewModel.error)
        assertFalse(viewModel.isLoading)
    }
    
    @Test
    fun `register without firstName shows error`() {
        // Given
        var successCalled = false
        
        // When
        viewModel.authenticate(
            isLogin = false,
            username = "newuser",
            password = "password123",
            firstName = "",
            onSuccess = { successCalled = true }
        )
        
        // Then
        assertEquals("First name is required", viewModel.error)
        assertFalse(successCalled)
    }
}
