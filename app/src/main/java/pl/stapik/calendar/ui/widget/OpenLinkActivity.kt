package pl.stapik.calendar.ui.widget

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

class OpenLinkActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(EXTRA_URL)

        if (!url.isNullOrBlank()) {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            )
        }

        finish()
    }

    companion object {
        const val EXTRA_URL = "entry_link"
    }
}