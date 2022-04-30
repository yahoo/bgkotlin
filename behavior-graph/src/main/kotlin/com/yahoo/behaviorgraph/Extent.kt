//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import com.yahoo.behaviorgraph.exception.BehaviorGraphException
import kotlin.system.measureTimeMillis

open class Extent(val graph: Graph) {
    var debugName: String = javaClass.simpleName
    internal var behaviors: MutableList<Behavior> = mutableListOf()
    internal var resources: MutableList<Resource> = mutableListOf()
    internal var addedToGraphWhen: Long? = null
    var didAdd: State<Boolean>
    internal var didAddBehavior: Behavior
    internal var lifetime: ExtentLifetime? = null

    init {
        didAdd = State(this, false)
        didAddBehavior = behavior().supplies(didAdd).runs { it.didAdd.update(true) }
    }

    fun unifyLifetime(extent: Extent) {
        if (lifetime == null) {
            lifetime = ExtentLifetime(this)
        }
        lifetime!!.unify(extent)
    }

    fun addChildLifetime(extent: Extent) {
        if (this.lifetime == null) {
            lifetime = ExtentLifetime(this)
        }
        lifetime!!.addChild(extent)
    }

    internal fun hasCompatibleLifetime(extent: Extent): Boolean {
        if (this == extent) {
            return true
        } else if (lifetime != null) {
            return lifetime!!.hasCompatibleLifetime(extent.lifetime)
        } else {
            return false
        }
    }

    internal fun addBehavior(behavior: Behavior) {
        this.behaviors.add(behavior)
    }

    internal fun addResource(resource: Resource) {
        this.resources.add(resource)
    }

    fun addToGraphWithAction(debugName: String? = null) {
        this.graph.action(debugName, {
            this.addToGraph()
        })
    }

    fun addToGraph() {
        if (graph.currentEvent != null) {
            nameResources()
            graph.addExtent(this)
        } else {
            throw BehaviorGraphException("addToGraph must be called within an event. Extent=$this")
        }
    }

    fun removeFromGraphWithAction(strategy: ExtentRemoveStrategy = ExtentRemoveStrategy.extentOnly, debugName: String? = null) {
        this.graph.action(debugName, { this.removeFromGraph(strategy) })
    }

    fun removeFromGraph(strategy: ExtentRemoveStrategy = ExtentRemoveStrategy.extentOnly) {
        if (graph.currentEvent != null) {
            if (addedToGraphWhen != null) {
                if (strategy == ExtentRemoveStrategy.extentOnly || this.lifetime == null) {
                    graph.removeExtent(this)
                } else {
                    lifetime?.getAllContainedExtents()?.forEach { graph.removeExtent(it) }
                }
            }
        } else {
            throw BehaviorGraphException("removeFromGraph must be called within an event. Extent=$this")
        }
    }

    /**
     * Ensure all resources in self (not superclasses) have a debugName.
     * future: move to platformSupport since this is not portable beyond the jvm
     */
    private fun nameResources() {
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

    fun resource(name: String? = null): Resource {
        return Resource(this, name)
    }

    fun <T> typedMoment(name: String? = null): TypedMoment<T> {
        return TypedMoment<T>(this, name)
    }

    fun moment(name: String? = null): Moment {
        return Moment(this, name)
    }

    fun <T> state(initialState: T, name: String? = null): State<T> {
        return State<T>(this, initialState, name)
    }
}

fun <T: Extent> T.behavior(): BehaviorBuilder<T> {
    return BehaviorBuilder(this)
}

fun <T: Extent> T.sideEffect(debugName: String? = null, block: (ext: T) -> Unit) {
    val sideEffect = ExtentSideEffect(block as (Extent) -> Unit, this, this.graph.currentBehavior, debugName)
    graph.sideEffectHelper(sideEffect)
}

fun <T: Extent> T.actionAsync(debugName: String? = null, action: (ext: T) -> Unit) {
    val action = ExtentAction(action as (Extent) -> Unit, this, debugName)
    graph.asyncActionHelper(action)
}

fun <T: Extent> T.action(debugName: String? = null, action: (ext: T) -> Unit) {
    val action = ExtentAction(action as (Extent) -> Unit, this, debugName)
    graph.actionHelper(action)
}
