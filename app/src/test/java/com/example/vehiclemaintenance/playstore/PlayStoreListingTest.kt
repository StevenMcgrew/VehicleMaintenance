package com.example.vehiclemaintenance.playstore

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayStoreListingTest {

    @Test
    fun listingUrlPointsAtTheReleasePackage() {
        assertEquals(
            "https://play.google.com/store/apps/details?id=com.stevenmcgrew.vehiclemaintenancetrackerapp",
            PLAY_STORE_LISTING_URL,
        )
    }

    @Test
    fun marketUriPointsAtTheReleasePackage() {
        assertEquals(
            "market://details?id=com.stevenmcgrew.vehiclemaintenancetrackerapp",
            PLAY_STORE_MARKET_URI,
        )
    }
}
