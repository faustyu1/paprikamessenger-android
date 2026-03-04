package ru.faustyu.paprika.util

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for utility functions
 */
class ExtensionsTest {
    
    @Test
    fun `toFullImageUrl with http URL returns same URL`() {
        // Given
        val url = "http://example.com/image.jpg"
        
        // When
        val result = url.toFullImageUrl()
        
        // Then
        assertEquals(url, result)
    }
    
    @Test
    fun `toFullImageUrl with https URL returns same URL`() {
        // Given
        val url = "https://example.com/image.jpg"
        
        // When
        val result = url.toFullImageUrl()
        
        // Then
        assertEquals(url, result)
    }
    
    @Test
    fun `toFullImageUrl with null returns null`() {
        // Given
        val url: String? = null
        
        // When
        val result = url.toFullImageUrl()
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `withCacheBuster adds timestamp parameter`() {
        // Given
        val url = "http://example.com/image.jpg"
        
        // When
        val result = url.withCacheBuster()
        
        // Then
        assertTrue(result.startsWith(url))
        assertTrue(result.contains("?t="))
    }
    
    @Test
    fun `withCacheBuster appends to existing query string`() {
        // Given
        val url = "http://example.com/image.jpg?size=large"
        
        // When
        val result = url.withCacheBuster()
        
        // Then
        assertTrue(result.startsWith(url))
        assertTrue(result.contains("&t="))
    }
    
    @Test
    fun `parseToUnixSeconds with valid ISO string returns timestamp`() {
        // Given
        val isoString = "2024-01-01T12:00:00Z"
        
        // When
        val result = isoString.parseToUnixSeconds()
        
        // Then
        assertTrue(result > 0)
    }
    
    @Test
    fun `parseToUnixSeconds with invalid string returns fallback`() {
        // Given
        val invalidString = "not a date"
        val fallback = 123456L
        
        // When
        val result = invalidString.parseToUnixSeconds(fallback)
        
        // Then
        assertEquals(fallback, result)
    }
}

class ConstantsTest {
    
    @Test
    fun `ChatType fromValue returns correct type`() {
        assertEquals(ChatType.PRIVATE, ChatType.fromValue(0))
        assertEquals(ChatType.GROUP, ChatType.fromValue(1))
        assertEquals(ChatType.CHANNEL, ChatType.fromValue(2))
    }
    
    @Test
    fun `ChatType fromValue with invalid returns PRIVATE as default`() {
        assertEquals(ChatType.PRIVATE, ChatType.fromValue(999))
    }
    
    @Test
    fun `MessageType fromValue returns correct type`() {
        assertEquals(MessageType.TEXT, MessageType.fromValue("text"))
        assertEquals(MessageType.IMAGE, MessageType.fromValue("image"))
        assertEquals(MessageType.VIDEO, MessageType.fromValue("video"))
        assertEquals(MessageType.FILE, MessageType.fromValue("file"))
    }
    
    @Test
    fun `MessageStatus fromValue returns correct status`() {
        assertEquals(MessageStatus.SENT, MessageStatus.fromValue("sent"))
        assertEquals(MessageStatus.DELIVERED, MessageStatus.fromValue("delivered"))
        assertEquals(MessageStatus.READ, MessageStatus.fromValue("read"))
        assertEquals(MessageStatus.FAILED, MessageStatus.fromValue("failed"))
    }
}

class ResultTest {
    
    @Test
    fun `Success isSuccess returns true`() {
        val result = Result.Success("data")
        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertFalse(result.isLoading)
    }
    
    @Test
    fun `Error isError returns true`() {
        val result = Result.Error(Exception("error"))
        assertFalse(result.isSuccess)
        assertTrue(result.isError)
        assertFalse(result.isLoading)
    }
    
    @Test
    fun `Loading isLoading returns true`() {
        val result = Result.Loading
        assertFalse(result.isSuccess)
        assertFalse(result.isError)
        assertTrue(result.isLoading)
    }
    
    @Test
    fun `getOrNull returns data for Success`() {
        val data = "test data"
        val result = Result.Success(data)
        assertEquals(data, result.getOrNull())
    }
    
    @Test
    fun `getOrNull returns null for Error`() {
        val result = Result.Error(Exception())
        assertNull(result.getOrNull())
    }
    
    @Test
    fun `exceptionOrNull returns exception for Error`() {
        val exception = Exception("test")
        val result = Result.Error(exception)
        assertEquals(exception, result.exceptionOrNull())
    }
    
    @Test
    fun `exceptionOrNull returns null for Success`() {
        val result = Result.Success("data")
        assertNull(result.exceptionOrNull())
    }
}
