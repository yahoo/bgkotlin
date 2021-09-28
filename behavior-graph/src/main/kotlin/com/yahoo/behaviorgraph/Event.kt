//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

data class Event(val sequence: Long, val timestamp: Long,  val impulse: String?) {
    companion object {
        val InitialEvent: Event = Event(0, 0, "InitialEvent")
    }
}
