package com.yahoo.behaviorgraph

/**
 * A behavior demands a set of **Demandables**.
 * A [Resource], [Moment], and [State] are all Demandables.
 * By default these Demandables have a [LinkType] of `Reactive` which means a demanding behavior will
 * run when the resource is updated. An `Order` type will let the behavior access the demanded resource
 * but it will not necessarily reun when that resource is updated.
 */
interface Demandable {
    val resource: Resource
    val type: LinkType
}

data class DemandLink(override val resource: Resource, override val type: LinkType) : Demandable