package com.example.vehiclemaintenance.playstore

import android.content.Context
import android.content.Intent
import com.example.vehiclemaintenance.R

/**
 * The release application ID, written out rather than read from the package because debug builds
 * add a `.debug` suffix that has no Play listing.
 */
const val PLAY_STORE_APP_ID = "com.stevenmcgrew.vehiclemaintenancetrackerapp"

const val PLAY_STORE_LISTING_URL = "https://play.google.com/store/apps/details?id=$PLAY_STORE_APP_ID"

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
