package com.tomaspinho.atvr

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class AtvApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
    }

    companion object {
        @Volatile
        lateinit var INSTANCE: AtvApplication
            private set
    }
}