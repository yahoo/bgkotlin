//
// Copyright Yahoo 2021
//
package com.example.myapplication.ui.login

import com.yahoo.behaviorgraph.Graph
import com.yahoo.behaviorgraph.platform.PlatformSupport

object Globals {
    init {
        PlatformSupport.platformSupport = AndroidPlatformSupport()
    }

    var graph = Graph()

    init {
        reinit()
    }

    @JvmStatic
    fun action(impulse: String, action: () -> Unit) {
        graph.action(impulse, action)
    }

    @JvmStatic
    fun reinit() {
        graph = Graph()
    }
}
