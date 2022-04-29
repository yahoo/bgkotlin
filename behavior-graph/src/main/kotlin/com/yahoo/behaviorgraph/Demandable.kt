package com.yahoo.behaviorgraph

interface Demandable {
    val resource: Resource
    val type: LinkType
}

data class DemandLink(override val resource: Resource, override val type: LinkType) : Demandable