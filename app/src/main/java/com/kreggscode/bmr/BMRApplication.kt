package com.kreggscode.bmr

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BMRApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
