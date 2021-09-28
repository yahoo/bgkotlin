//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import com.yahoo.behaviorgraph.exception.BehaviorGraphException

class Moment<T>(extent: Extent<*>, debugName: String? = null) : Resource(extent, debugName),
    Transient {
    private var _happened = false
    private var _happenedValue: T? = null
    private var _happenedWhen: Event? = null

    /**
     * return if we've just been updated
     */
    val justUpdated: Boolean
        get() = this._happened

    /**
     * @throws  BehaviorGraphException if not justUpdated())
     */
    val value: T
        get() {
            if (!justUpdated) {
                throw BehaviorGraphException("Cannot access value unless it has been justUpdated()")
            }
            return this._happenedValue!!
        }
    val event: Event?
        get() = this._happenedWhen

    fun updateWithAction(value: T) {
        graph.action(getImpulse()) { update(value) }
    }

    private fun getImpulse(): String? {
        return if (this.debugName != null) {
            "Impulse From happen(): $this)"
        } else null
    }

    fun update(value: T) {
        this.assertValidUpdater()
        this._happened = true
        this._happenedValue = value
        this._happenedWhen = this.graph.currentEvent
        this.graph.resourceTouched(this)
        this.graph.trackTransient(this)
    }

    override fun clear() {
        this._happened = false
        this._happenedValue = null
    }
}
