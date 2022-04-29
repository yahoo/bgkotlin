//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

class Behavior(
    val extent: Extent, demands: List<Demandable>?, supplies: List<Resource>?,
    var block: (Extent) -> Unit
) : Comparable<Behavior> {
    var demands: MutableSet<Resource>? = null
    var orderingDemands: MutableSet<Resource>?  = null
    var supplies: Set<Resource>? = null
    var enqueuedWhen: Long? = null
    var removedWhen: Long? = null
    internal var orderingState = OrderingState.Untracked
    var order: Long = 0

    var untrackedDemands: List<Demandable>?
    var untrackedDynamicDemands: List<Demandable>? = null
    var untrackedSupplies: List<Resource>?
    var untrackedDynamicSupplies: List<Resource>? = null

    init {
        extent.addBehavior(this)
        this.untrackedDemands = demands
        this.untrackedSupplies = supplies
    }

    override fun compareTo(other: Behavior): Int {
        return order.compareTo(other.order)
    }

    override fun toString(): String {
        return "Behavior()"
    }

    fun setDynamicDemands(vararg newDemands: Demandable) {
        setDynamicDemands(newDemands.asList())
    }

    fun setDynamicDemands(newDemands: List<Demandable?>?) {
        this.extent.graph.updateDemands(this, newDemands?.filterNotNull())
    }

    fun setDynamicSupplies(vararg newSupplies: Resource) {
        setDynamicSupplies(newSupplies.asList())
    }

    fun setDynamicSupplies(newSupplies: List<Resource?>?) {
        this.extent.graph.updateSupplies(this, newSupplies?.filterNotNull())
    }
}
