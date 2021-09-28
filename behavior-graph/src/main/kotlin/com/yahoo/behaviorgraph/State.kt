//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import com.yahoo.behaviorgraph.Event.Companion.InitialEvent

class State<T>(extent: Extent<*>, initialState: T, debugName: String? = null) :
    Resource(extent, debugName),
    Transient {
    private var currentState = StateHistory(initialState, InitialEvent)
    private var priorStateDuringEvent: StateHistory<T>? = null
    val value: T
        get() = currentState.value
    val event: Event
        get() = currentState.event
    private val trace: StateHistory<T>
        get() = priorStateDuringEvent ?: currentState
    val traceValue: T
        get() = trace.value
    val traceEvent: Event
        get() = trace.event

    fun updateWithAction(newValue: T, changesOnly: Boolean) {
        graph.action(getImpulse()) { update(newValue, changesOnly) }
    }

    fun update(newValue: T, changesOnly: Boolean) {
        this.assertValidUpdater()

        if (changesOnly) {
            if (newValue == currentState.value)
                return
        }
        priorStateDuringEvent = currentState
        currentState = StateHistory(newValue, this.graph.currentEvent!!)
        this.graph.resourceTouched(this)
        this.graph.trackTransient(this)
    }

    private fun getImpulse(): String? {
        return if (this.debugName != null) {
            "Impulse From happen(): $this)"
        } else null
    }

    val justUpdated: Boolean
        get() = currentState.event == graph.currentEvent

    fun justUpdatedTo(toValue: T): Boolean {
        return justUpdated &&
            (currentState.value == toValue)
    }

    fun justUpdatedFrom(fromValue: T): Boolean {
        return justUpdated &&
            (priorStateDuringEvent!!.value == fromValue)
    }

    fun justUpdatedToFrom(toValue: T, fromValue: T): Boolean {
        return justUpdatedTo(toValue) && justUpdatedFrom(fromValue)
    }

    override fun clear() {
        priorStateDuringEvent = null
    }

    data class StateHistory<T>(val value: T, val event: Event)
}



