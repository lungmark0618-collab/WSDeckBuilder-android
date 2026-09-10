package com.mark.wsdeck.ui.shared

import org.junit.Assert.*
import org.junit.Test

class BackSwipePolicyTest {
    @Test fun onlyDeliberateLeftSwipeReturns() {
        assertTrue(shouldNavigateBack(-120f, 15f, 80f))
        assertFalse(shouldNavigateBack(120f, 0f, 80f))
        assertFalse(shouldNavigateBack(-30f, 0f, 80f))
        assertFalse(shouldNavigateBack(-100f, 110f, 80f))
        assertFalse(shouldNavigateBack(0f, -150f, 80f))
    }
}
