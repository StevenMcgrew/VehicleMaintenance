package com.example.vehiclemaintenance.playstore

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import com.example.vehiclemaintenance.R

/**
 * The release application ID, written out rather than read from the package because debug builds
 * add a `.debug` suffix that has no Play listing.
 */
const val PLAY_STORE_APP_ID = "com.stevenmcgrew.vehiclemaintenancetrackerapp"

const val PLAY_STORE_LISTING_URL = "https://play.google.com/store/apps/details?id=$PLAY_STORE_APP_ID"

const val PLAY_STORE_MARKET_URI = "market://details?id=$PLAY_STORE_APP_ID"

private const val PLAY_STORE_PACKAGE = "com.android.vending"

/** Opens the share sheet with a pre-written recommendation and the Play listing link. */
fun shareApp(context: Context) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_app_subject))
        putExtra(
            Intent.EXTRA_TEXT,
            context.getString(R.string.share_app_message, PLAY_STORE_LISTING_URL),
        )
    }
    context.startActivity(Intent.createChooser(send, null))
}

/**
 * Opens the listing in the Play Store app, where the user can rate it, falling back to the web
 * page on devices without the Play Store.
 */
fun openPlayStoreListing(context: Context) {
    val playStore = Intent(Intent.ACTION_VIEW, PLAY_STORE_MARKET_URI.toUri())
        .setPackage(PLAY_STORE_PACKAGE)
    val browser = Intent(Intent.ACTION_VIEW, PLAY_STORE_LISTING_URL.toUri())
    try {
        context.startActivity(playStore)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(browser)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.rate_app_unavailable, Toast.LENGTH_SHORT).show()
        }
    }
}
