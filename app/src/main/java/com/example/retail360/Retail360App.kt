package com.example.retail360

import android.app.Application
import com.cloudinary.android.MediaManager
import com.example.retail360.util.Graph

/**
 * App entry point. Wires up Cloudinary and the dependency Graph once, at process start.
 */
class Retail360App : Application() {

    override fun onCreate() {
        super.onCreate()

        // ---- Cloudinary (unsigned uploads via an upload preset) ----
        val config = hashMapOf(
            "cloud_name" to "pwgntjrp"
            // secure=true by default; using an unsigned preset means no api_key/secret on device
        )
        MediaManager.init(this, config)

        // ---- Service locator: builds Room DB + repositories ----
        Graph.provide(this)
    }
}
