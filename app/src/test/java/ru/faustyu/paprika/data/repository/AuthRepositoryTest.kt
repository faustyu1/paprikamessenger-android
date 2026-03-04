package ru.faustyu.paprika.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import ru.faustyu.paprika.data.PrefsManager
import ru.faustyu.paprika.data.network.ApiService
import ru.faustyu.paprika.data.network.AuthRequest
import ru.faustyu.paprika.data.network.AuthResponse
import ru.faustyu.paprika.util.Result

/**
 * Unit tests for AuthRepository
 */
class AuthRepositoryTest {
    
    private lateinit var apiService: ApiService
    private lateinit var prefsManager: PrefsManager
    private lateinit var authRepository: AuthRepository
    
    @Before
    fun setup() {
        apiService = mockk()
        prefsManager = mockk(relaxed = true)
        authRepository = AuthRepository(apiService, prefsManager)
    }
    
    @Test
    fun `login success returns token`() = runTest {
        // Given
        val username = "testuser"
        val password = "password123"
        val expectedToken = "test_token_123"
        
        coEvery {
            apiService.login(any())
        } returns Response.success(AuthResponse(expectedToken, null))
        
        // When
        val result = authRepository.login(username, password)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(expectedToken, (result as Result.Success).data)
        
        // Verify token was saved
        coVerify { prefsManager.token = expectedToken }
    }
    
    @Test
    fun `login failure returns error`() = runTest {
        // Given
        val username = "testuser"
        val password = "wrongpassword"
        
        coEvery {
            apiService.login(any())
        } returns Response.success(AuthResponse("", "Invalid credentials"))
        
        // When
        val result = authRepository.login(username, password)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message?.contains("Invalid credentials") == true)
    }
    
    @Test
    fun `register success saves token and returns it`() = runTest {
        // Given
        val username = "newuser"
        val password = "password123"
        val firstName = "Test"
        val lastName = "User"
        val publicKey = "public_key_hex"
        val expectedToken = "new_user_token"
        
        coEvery {
            apiService.register(any())
        } returns Response.success(AuthResponse(expectedToken, null))
        
        // When
        val result = authRepository.register(
            username, password, firstName, lastName, publicKey
        )
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(expectedToken, (result as Result.Success).data)
        coVerify { prefsManager.token = expectedToken }
    }
    
    @Test
    fun `isLoggedIn returns true when token exists`() {
        // Given
        coEvery { prefsManager.token } returns "some_token"
        
        // When
        val isLoggedIn = authRepository.isLoggedIn()
        
        // Then
        assertTrue(isLoggedIn)
    }
    
    @Test
    fun `isLoggedIn returns false when no token`() {
        // Given
        coEvery { prefsManager.token } returns null
        
        // When
        val isLoggedIn = authRepository.isLoggedIn()
        
        // Then
        assertFalse(isLoggedIn)
    }
    
    @Test
    fun `logout clears preferences`() {
        // When
        authRepository.logout()
        
        // Then
        coVerify { prefsManager.clear() }
    }
}
