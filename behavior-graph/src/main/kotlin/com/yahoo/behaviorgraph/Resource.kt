//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import com.yahoo.behaviorgraph.exception.BehaviorGraphException
import java.util.HashSet

open class Resource(val extent: Extent, var debugName: String? = null): Demandable {
    val graph: Graph = extent.graph
    var subsequents: MutableSet<Behavior> = mutableSetOf()
    var suppliedBy: Behavior? = null

    override val resource get() = this
    override val type get() = LinkType.reactive

    init {
        extent.addResource(this)
    }

    val order: Demandable get() = DemandLink(this, LinkType.order)

    internal fun assertValidUpdater() {
        val currentBehavior = graph.currentBehavior
        val currentEvent = graph.currentEvent
        if (currentBehavior == null && currentEvent == null) {
            throw BehaviorGraphException("Resource $debugName must be updated inside a behavior or action")
        }
        if (!graph.validateDependencies) { return }
        if (suppliedBy != null && currentBehavior != suppliedBy) {
            throw BehaviorGraphException("Supplied resource $debugName can only be updated by its supplying behavior. CurrentBehavior = $currentBehavior")

        }
        if (suppliedBy == null && currentBehavior != null) {
            throw BehaviorGraphException("Unsupplied resource $debugName can only be updated in an action. CurrentBehavior=$currentBehavior")
        }
    }

    internal fun assertValidAccessor() {
        if (!graph.validateDependencies) { return }
        val currentBehavior = graph.currentBehavior
        if (currentBehavior != null && currentBehavior != suppliedBy && !(currentBehavior.demands?.contains(this) ?: false)) {
            throw BehaviorGraphException("Cannot access the value or event of a resource inside a behavior unless it is supplied or demanded.")
        }
    }

    open val justUpdated: Boolean get() {
        assertValidAccessor()
        return false
    }
}
