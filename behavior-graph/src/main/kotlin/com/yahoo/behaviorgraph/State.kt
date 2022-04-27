//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import com.yahoo.behaviorgraph.Event.Companion.InitialEvent

class State<T>(extent: Extent, initialState: T, debugName: String? = null) :
    Resource(extent, debugName),
    Transient {
    private var currentState = StateHistory(initialState, InitialEvent)
    private var priorStateDuringEvent: StateHistory<T>? = null
    val value: T
        get() {
            assertValidAccessor()
            return currentState.value
        }
    val event: Event
        get() {
            assertValidAccessor()
            return currentState.event
        }
    private val trace: StateHistory<T>
        get() = priorStateDuringEvent ?: currentState
    val traceValue: T
        get() = trace.value
    val traceEvent: Event
        get() = trace.event

    fun updateWithAction(newValue: T, debugName: String? = null) {
        graph.action({ update(newValue) }, debugName)
    }

    fun update(newValue: T) {
        if (newValue == currentState.value) {
            return
        }
        updateForce(newValue)
    }

    fun updateForce(newValue: T) {
        assertValidUpdater()
        if (graph.currentEvent != null && currentState.event.sequence < graph.currentEvent!!.sequence) {
            // this check prevents updating priorState if we are updated multiple times in same behavior
            priorStateDuringEvent = currentState
        }

        currentState = StateHistory(newValue, this.graph.currentEvent!!)
        this.graph.resourceTouched(this)
        this.graph.trackTransient(this)
    }

    override val justUpdated: Boolean
        get() {
            assertValidAccessor()
            return currentState.event == graph.currentEvent
        }

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



