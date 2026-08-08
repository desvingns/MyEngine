package dev.myengine.android

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AndroidSmokeInstrumentationTest {
    @Test
    fun testTargetApplicationContextIsAvailable() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

        assertNotNull("Instrumentation must expose the target context", targetContext)
        assertEquals("dev.myengine.android", targetContext.packageName)
    }
}
