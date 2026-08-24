package com.example.retail360

import android.app.Application
import com.example.retail360.util.Graph

/**
 * Custom Application class for Retail360.
 * Initialises the dependency graph and background sync.
 */
class Retail360App : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.provide(this)
    }
}
