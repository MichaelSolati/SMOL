package com.michaelsolati.smol

import android.app.Application
import com.michaelsolati.smol.util.FileUtil
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class SmolApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        FileUtil.cleanCompressedCache(this)
    }
}
