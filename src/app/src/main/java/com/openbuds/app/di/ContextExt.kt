package com.openbuds.app.di

import android.content.Context
import com.openbuds.app.OpenBudsApp

/** The app-wide dependency container, or null when the context is not hosted by [OpenBudsApp]. */
val Context.appContainer: AppContainer?
    get() = (applicationContext as? OpenBudsApp)?.container
