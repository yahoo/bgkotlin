//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

class Behavior(
    val extent: Extent<*>, demands: List<Resource>?, supplies: List<Resource>?,
    var block: (Extent<*>) -> Unit
) : Comparable<Behavior> {
    var demands: MutableSet<Resource>? = null
    var supplies: MutableSet<Resource>? = null
    var debugName: String? = null
    var enqueuedWhen: Long? = null
    var removedWhen: Long? = null
    var added = false
    internal var orderingState = OrderingState.Unordered
    var order: Long = 0
    var untrackedDemands: List<Resource>?
    var untrackedSupplies: List<Resource>?

    init {
        extent.addBehavior(this)
        this.untrackedDemands = demands
        this.untrackedSupplies = supplies
    }

    override fun compareTo(other: Behavior): Int {
        return order.compareTo(other.order)
    }

    override fun toString(): String {
        return "Behavior(debugName=$debugName)"
    }

    fun setDemands(newDemands: List<Resource>) {
        this.extent.graph.updateDemands(this, newDemands)
    }

    fun setSupplies(newSupplies: List<Resource>) {
        this.extent.graph.updateSupplies(this, newSupplies)
    }
}
