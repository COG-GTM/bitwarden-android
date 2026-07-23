package com.bitwarden.demoapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for the Password Playground demo app.
 *
 * A minimal sample application demonstrating how to build a standalone app on top of the
 * Bitwarden Android platform modules (`:core` and `:ui`), reusing the Bitwarden design system
 * and MVVM architecture patterns.
 */
@HiltAndroidApp
class DemoApplication : Application()
