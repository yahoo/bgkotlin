//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import com.yahoo.behaviorgraph.exception.BehaviorGraphException

abstract class BaseMoment(extent: Extent, debugName: String? = null) : Resource(extent, debugName),
    Transient {
    protected var _happened = false
    protected var _happenedWhen: Event? = null

    override val justUpdated: Boolean
        get() {
            assertValidAccessor()
            return _happened
        }


    val event: Event?
        get() {
            assertValidAccessor()
            return this._happenedWhen
        }

    override fun clear() {
        _happened = false
    }
}

class Moment(extent: Extent, debugName: String? = null): BaseMoment(extent, debugName) {
    fun update() {
        assertValidUpdater()
        _happened = true
        _happenedWhen = graph.currentEvent
        graph.resourceTouched(this)
        graph.trackTransient(this)
    }

    fun updateWithAction(debugName: String? = null) {
        graph.action(debugName) { update() }
    }

}

class TypedMoment<T>(extent: Extent, debugName: String? = null): BaseMoment(extent, debugName) {
    private var _happenedValue: T? = null

    val value: T
        get() {
            assertValidAccessor()
            if (!_happened) { throw BehaviorGraphException("Cannot access moment value when it did not update.") }
            return this._happenedValue!!
        }


    fun updateWithAction(value: T, debugName: String? = null) {
        graph.action(debugName) { update(value) }
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
        _happenedValue = null
        super.clear()
    }

    fun justUpdatedTo(value: T): Boolean {
        return this.justUpdated && this._happenedValue == value
    }

}