package com.example.playersampleapp

import android.app.Application
import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class PlayerSampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@PlayerSampleApplication)
            modules(appModule(this@PlayerSampleApplication))
        }
    }

    companion object {
        private var instance: PlayerSampleApplication? = null

        fun getContext(): Context {
            return instance!!.applicationContext
        }
    }
}