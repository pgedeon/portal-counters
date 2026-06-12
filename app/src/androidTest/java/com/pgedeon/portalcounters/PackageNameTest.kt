package com.pgedeon.portalcounters

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test verifying the application package name.
 */
@RunWith(AndroidJUnit4::class)
class PackageNameTest {
    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.pgedeon.portalcounters", appContext.packageName)
    }
}
