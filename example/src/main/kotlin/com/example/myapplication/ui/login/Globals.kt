//
// Copyright Yahoo 2021
//
package com.example.myapplication.ui.login

import com.yahoo.behaviorgraph.Graph

object Globals {
    init {
    }

    var graph = Graph()

    init {
        reinit()
    }

    @JvmStatic
    fun reinit() {
        graph = Graph()
    }
}
