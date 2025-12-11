package com.frzterr.app

import android.app.Application
import com.frzterr.app.data.remote.supabase.SupabaseManager

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        SupabaseManager.initialize(this)
    }
}
