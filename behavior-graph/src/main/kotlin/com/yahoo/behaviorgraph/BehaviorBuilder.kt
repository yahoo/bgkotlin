package com.yahoo.behaviorgraph

class BehaviorBuilder<T: Extent<*>>(
    val extent: T
) {
    var untrackedDemands: MutableList<Demandable> = mutableListOf()
    var untrackedSupplies: MutableList<Resource> = mutableListOf()
    var dynamicDemandSwitches: Array<out Demandable>? = null
    var dynamicDemandLinks: ((ext: T) -> List<Demandable?>?)? = null
    var dynamicDemandRelinkingOrder: RelinkingOrder = RelinkingOrder.relinkingOrderPrior
    var dynamicSupplySwitches: Array<out Demandable>? = null
    var dynamicSupplyLinks: ((ext: T) -> List<Resource?>?)? = null
    var dynamicSupplyRelinkingOrder: RelinkingOrder = RelinkingOrder.relinkingOrderPrior

    fun demands(vararg demands: Demandable) = apply { untrackedDemands.addAll(demands) }
    fun supplies(vararg supplies: Resource) = apply { untrackedSupplies.addAll(supplies) }
    fun dynamicDemands(vararg switches: Demandable, links: ((ext: T) -> List<Demandable?>?), relinkingOrder: RelinkingOrder = RelinkingOrder.relinkingOrderPrior) = apply {
        dynamicDemandSwitches = switches
        dynamicDemandLinks = links
        dynamicDemandRelinkingOrder = relinkingOrder
    }
    fun dynamicSupplies(vararg switches: Demandable, links: ((ext: T) -> List<Resource?>?), relinkingOrder: RelinkingOrder = RelinkingOrder.relinkingOrderPrior) = apply {
        dynamicSupplySwitches = switches
        dynamicSupplyLinks = links
        dynamicSupplyRelinkingOrder = relinkingOrder
    }

    fun runs(block: (ext: T) -> Unit): Behavior {
        var dynamicDemandResource: Resource? = null
        if (dynamicDemandSwitches != null) {
            dynamicDemandResource = extent.resource("(BG Dynamic Demand Resource)")
            if (dynamicDemandRelinkingOrder == RelinkingOrder.relinkingOrderPrior) {
                untrackedDemands.add(dynamicDemandResource!!)
            } else {
                untrackedSupplies.add(dynamicDemandResource!!)
            }
        }

        var dynamicSupplyResource: Resource? = null
        if (dynamicSupplySwitches != null) {
            dynamicSupplyResource = extent.resource("(BG Dynamic Supply Resource)")
            if (dynamicSupplyRelinkingOrder == RelinkingOrder.relinkingOrderPrior) {
                untrackedDemands.add(dynamicSupplyResource)
            } else {
                untrackedSupplies.add(dynamicSupplyResource)
            }
        }

        val mainBehavior = Behavior(extent, untrackedDemands, untrackedSupplies, block as (Extent<*>) -> Unit)

        if (dynamicDemandSwitches != null) {
            var supplies: List<Resource>? = null
            val demands: MutableList<Demandable> = mutableListOf()
            dynamicDemandSwitches?.let { demands.addAll(it) }
            if (dynamicDemandRelinkingOrder == RelinkingOrder.relinkingOrderPrior) {
                supplies = listOf(dynamicDemandResource!!)
            } else {
                demands.add(dynamicDemandResource!!)
            }
            Behavior(extent, demands, supplies) {
                val demandLinks = dynamicDemandLinks!!(it as T)
                mainBehavior.setDynamicDemands(demandLinks)
            }
        }

        if (dynamicSupplySwitches != null) {
            var supplies: List<Resource>? = null
            val demands: MutableList<Demandable> = mutableListOf()
            dynamicSupplySwitches?.let { demands.addAll(it) }
            if (dynamicSupplyRelinkingOrder == RelinkingOrder.relinkingOrderPrior) {
                supplies = listOf(dynamicSupplyResource!!)
            } else {
                demands.add(dynamicSupplyResource!!)
            }
            Behavior(extent, demands, supplies) {
                var supplyLinks = dynamicSupplyLinks!!(it as T)
                mainBehavior.setDynamicSupplies(supplyLinks)
            }
        }

        return mainBehavior
    }
}

