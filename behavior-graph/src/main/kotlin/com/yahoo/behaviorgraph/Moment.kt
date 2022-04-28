//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import com.yahoo.behaviorgraph.exception.BehaviorGraphException

class Moment<T>(extent: Extent, debugName: String? = null) : Resource(extent, debugName),
    Transient {
    private var _happened = false
    private var _happenedValue: T? = null
    private var _happenedWhen: Event? = null

    override val justUpdated: Boolean
        get() {
            assertValidAccessor()
            return _happened
        }


    val value: T
        get() {
            assertValidAccessor()
            if (!_happened) { throw BehaviorGraphException("Cannot access moment value when it did not update.") }
            return this._happenedValue!!
        }

    val event: Event?
        get() {
            assertValidAccessor()
            return this._happenedWhen
        }

    fun justUpdatedTo(value: T): Boolean {
        return this.justUpdated && this._happenedValue == value
    }

    fun updateWithAction(value: T, debugName: String? = null) {
        graph.action(debugName, { update(value) })
    }

    fun update(value: T) {
        assertValidUpdater()
        _happened = true
        _happenedValue = value
        _happenedWhen = graph.currentEvent
        graph.resourceTouched(this)
        graph.trackTransient(this)
    }

    override fun clear() {
        _happened = false
        _happenedValue = null
    }
}
