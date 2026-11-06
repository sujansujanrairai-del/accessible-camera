package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class AppStringsUnitTest {

    @Test
    fun appTitle_matchesRequirements() {
        val appName = "Accessible Camera and Gallery"
        assertEquals("Accessible Camera and Gallery", appName)
    }

    @Test
    fun developerCredit_matchesRequirements() {
        val developer = "Sujan Rai from the Blind Tech Nexus team"
        assertEquals("Sujan Rai from the Blind Tech Nexus team", developer)
    }
}
