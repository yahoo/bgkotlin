//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import com.yahoo.behaviorgraph.exception.BehaviorGraphException
import java.util.HashSet

open class Resource(val extent: Extent<*>, var debugName: String? = null): Demandable {
    val graph: Graph = extent.graph
    var added = false
    var subsequents: MutableSet<Behavior> = HashSet()
    var suppliedBy: Behavior? = null
    override val resource get() = this
    override val type get() = LinkType.reactive

    init {
        extent.addResource(this)
    }

    internal fun assertValidUpdater() {
        val currentBehavior = graph.currentBehavior
        val currentEvent = graph.currentEvent
        if (currentBehavior == null && currentEvent == null) {
            throw BehaviorGraphException("Resource $debugName must be updated inside a behavior or action")
        }
        if (this.suppliedBy != null && currentBehavior != this.suppliedBy) {
            throw BehaviorGraphException("Supplied resource $debugName can only be updated by its supplying behavior. CurrentBehavior = $currentBehavior")

        }
        if (this.suppliedBy == null && currentBehavior != null) {
            throw BehaviorGraphException("Unsupplied resource $debugName can only be updated in an action. CurrentBehavior=$currentBehavior")
        }
    }
}

/*
open class Resource<T> {
    var value: T? = null
    var event: Event? = null
    var debugName: String? = null
    var traced: Boolean = false
    var valuePersistence: ValuePersistence
    var previousValue: T? = null
    var previousEvent: Event? = null
    var graph: Graph
    var added: Boolean = false
    var extent: Extent? = null
    var subsequents: MutableSet<Behavior>
    var suppliedBy: Behavior? = null
    var capturedUpdate: (() -> Unit)? = null
    var traceValue: T?
        get() {
            if (!traced) {
                throw BehaviorGraphException("Accessing traced value for non traced resource. + $this")
            }
            return if (justHappened()) this.previousValue else this.value;
        }
    var traceEvent: Event?
        get() {
            if (!this.traced) {
                throw BehaviorGraphException("Accessing traced event for non traced resource: $this");
            } else {
                return if (this.justHappened()) this.previousEvent else this.event;
            }
        }

    init {
        valuePersistence = ValuePersistence.Persistent
        subsequents = HashSet()
        traceValue = null
        traceEvent = null
    }

    @JvmOverloads
    fun updateValue(newValue: T?, changesOnly: Boolean = false) {
        if (this.graph == null) {
            this.capturedUpdate = { this.updateValue(newValue, changesOnly) }
        } else {
            // Ensure valid graph structure
            if (this.graph?.currentEvent == null) {
                throw BehaviorGraphException("Added resources can only be updated during an event loop. Resource= $this")
            } else {
                if (this.suppliedBy != null && this.graph?.currentBehavior != this.suppliedBy) {
                    throw BehaviorGraphException("Resource can only be updated by its supplying behavior. Resource=$this currentBehavior= ${this.graph?.currentBehavior}");
                }

                if (changesOnly) {
                    if (this.value == newValue) {
                        return;
                    }
                }

                if (this.traced) {
                    val previousSequence = this.previousEvent?.sequence ?: 0
                    if (previousSequence < this.graph!!.currentEvent!!.sequence) {
                        this.previousValue = this.value;
                        this.previousEvent = this.event;
                    } else {
                        this.previousValue = null;
                        this.previousEvent = null;
                    }
                }

                this.value = newValue;
                this.event = this.graph!!.currentEvent;
                this.graph!!.resourceTouched(this);
            }
        }
    }

    fun justHappened(): Boolean {
        val anEvent = this.event
        val aCurrentEvent = this.graph?.currentEvent;

        return if (anEvent != null && aCurrentEvent != null) {
            anEvent.sequence == aCurrentEvent.sequence;
        } else {
            false;
        }
    }

    fun hasHappened(): Boolean {
        return this.event != null;
    }

    fun happenedSince(since: Resource): Boolean {
        val thisSequence = this.event?.sequence ?: -1;
        val sinceSequence = since.event?.sequence ?: 0;
        return thisSequence >= sinceSequence;
    }

    fun happenedBetween(since: Resource, until: Resource): Boolean {
        val thisSequence = this.event?.sequence ?: -1;
        val sinceSequence = since.event?.sequence ?: 0;
        val untilSequence = until.event?.sequence ?: Long.MAX_VALUE;
        return thisSequence >= sinceSequence && thisSequence < untilSequence;
    }

    override fun toString(): String {
        return "Resource(value=$value, debugName=$debugName, valuePersistence=$valuePersistence, extent=$extent)"
    }
}
*/
