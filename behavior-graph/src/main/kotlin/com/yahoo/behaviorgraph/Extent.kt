//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import com.yahoo.behaviorgraph.exception.BehaviorGraphException
import kotlin.system.measureTimeMillis

/**
 * A container for a group of related behaviors and resources
 */
open class Extent<SubclassType>(val graph: Graph) { //TODO restrict SubclassType to subclass of Extent
    var debugName: String = javaClass.simpleName
    internal var behaviors: MutableList<Behavior> = mutableListOf()
    internal var resources: MutableList<Resource> = mutableListOf()
    var addedToGraphWhen: Event? = null
        internal set

    fun addBehavior(behavior: Behavior) {
        this.behaviors.add(behavior)
    }

    fun addResource(resource: Resource) {
        this.resources.add(resource)
    }

    fun addToGraphWithAction() {
        this.graph.action("add extent: $debugName") { this.addToGraph() }
    }

    fun addToGraph() {
        if (graph.currentEvent != null) {
            nameResources()
            graph.addExtent(this)
        } else {
            throw BehaviorGraphException("addToGraph must be called within an event. Extent=$this")
        }
    }

    fun removeFromGraphWithAction() {
        this.graph.action("remove extent: $debugName") { this.removeFromGraph() }
    }

    fun removeFromGraph() {
        if (graph.currentEvent != null) {
            if (addedToGraphWhen != null) {
                graph.removeExtent(this)
            }
        } else {
            throw BehaviorGraphException("removeFromGraph must be called within an event. Extent=$this")
        }
    }

    fun makeBehavior(
        demands: List<Resource>?,
        supplies: List<Resource>?,
        block: (SubclassType) -> Unit
    ): Behavior {
        return Behavior(this, demands, supplies, block as (Extent<*>) -> Unit)
    }

    fun sideEffect(name: String?, block: (extent: SubclassType) -> Unit) {
        graph.sideEffect(this, name, block as (Extent<*>) -> Unit)
    }

    fun actionAsync(impulse: String?, action: () -> Unit) {
        if (this.addedToGraphWhen != null) {
            this.graph.actionAsync(impulse, action)
        } else {
            throw BehaviorGraphException("Action on extent requires it be added to the graph. Extent=$this")
        }
    }

    fun action(impulse: String?, action: () -> Unit) {
        if (this.addedToGraphWhen != null) {
            this.graph.action(impulse, action)
        } else {
            throw BehaviorGraphException("Action on extent requires it be added to the graph. Extent=$this")
        }
    }

    /**
     * Ensure all resources in self (not superclasses) have a debugName.
     * future: move to platformSupport since this is not portable beyond the jvm
     */
    private fun nameResources() {
        val timeMS = measureTimeMillis {
            javaClass.declaredFields.forEach { field ->
                if (field.type == Resource::class.java) {
                    val resource = field.get(this) as Resource
                    if (resource.debugName == null) {
                        println("setting debugName for ${field.name}")
                        resource.debugName = field.name
                    }
                }
            }
        }
        println("collectAndNameResources() time was $timeMS ms")
    }

    fun behavior(): BehaviorBuilder<Extent<*>> {
        return BehaviorBuilder(this)
    }

    fun resource(name: String? = null): Resource {
        return Resource(this, name)
    }


}
