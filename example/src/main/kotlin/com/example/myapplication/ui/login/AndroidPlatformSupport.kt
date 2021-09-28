//
// Copyright Yahoo 2021
//
package com.example.myapplication.ui.login

import android.os.Looper
import com.yahoo.behaviorgraph.platform.PlatformSupport

class AndroidPlatformSupport : PlatformSupport {
    override fun isMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }

    override fun getCurrentTimeMillis(): Long {
        return System.currentTimeMillis()
    }
}
