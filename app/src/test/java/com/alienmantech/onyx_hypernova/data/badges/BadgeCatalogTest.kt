package com.alienmantech.onyx_hypernova.data.badges

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BadgeCatalogTest {

    @Test
    fun normalizeBadgeId_returnsStableIdForKnownBadge() {
        assertEquals("night", BadgeCatalog.normalizeBadgeId("night"))
        assertEquals("trimless", BadgeCatalog.normalizeBadgeId("trimless"))
    }

    @Test
    fun normalizeBadgeId_rejectsUnknownBadge() {
        assertNull(BadgeCatalog.normalizeBadgeId("unknown"))
    }

    @Test
    fun normalizeBadgeId_trimsAndLowercasesKnownBadge() {
        assertEquals("night", BadgeCatalog.normalizeBadgeId(" Night "))
    }
}
