package com.example.alphabetlauncher

import org.junit.Assert.assertEquals
import org.junit.Test

class AlphabetBarLogicTest {

    @Test
    fun testLetterGrouping() {
        val apps = listOf(
            "Apple", "Banana", "cat", "Dog", "123App", "Elephant"
        )
        
        val grouped = apps.groupBy { app ->
            val firstChar = app.firstOrNull()?.uppercaseChar() ?: '#'
            if (firstChar in 'A'..'Z') firstChar else '#'
        }
        
        assertEquals(listOf("Apple"), grouped['A'])
        assertEquals(listOf("Banana"), grouped['B'])
        assertEquals(listOf("cat"), grouped['C'])
        assertEquals(listOf("Dog"), grouped['D'])
        assertEquals(listOf("Elephant"), grouped['E'])
        assertEquals(listOf("123App"), grouped['#'])
    }

    @Test
    fun testTouchPositionToLetter() {
        val alphabetSize = 26
        val columnHeight = 1000f
        
        // Items are 28 total (1 star + 26 letters + 1 dot)
        val totalItems = 28
        
        fun getLetterAtY(y: Float): Char {
            val rawIndex = (y / columnHeight * totalItems).toInt()
            val index = (rawIndex - 1).coerceIn(0, alphabetSize - 1)
            return ('A'..'Z').toList()[index]
        }
        
        // Top edge: y = 0
        // rawIndex = 0 -> index = -1 -> coerced to 0 ('A')
        assertEquals('A', getLetterAtY(0f))
        
        // Middle of column (y = 500f)
        // rawIndex = (500 / 1000 * 28) = 14
        // index = 13 ('N')
        assertEquals('N', getLetterAtY(500f))
        
        // Bottom edge (y = 1000f)
        // rawIndex = 28 -> index = 27 -> coerced to 25 ('Z')
        assertEquals('Z', getLetterAtY(1000f))
    }
}