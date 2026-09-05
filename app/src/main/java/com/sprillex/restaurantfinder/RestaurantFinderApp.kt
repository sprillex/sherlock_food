package com.sprillex.restaurantfinder

import android.app.Application
import com.sprillex.restaurantfinder.diagnostics.CrashReporter

class RestaurantFinderApp : Application() {
    override fun onCreate() {
        // Must be called prior to initializing DI, databases, or third-party libraries
        CrashReporter.install(this)
        super.onCreate()
    }
}
