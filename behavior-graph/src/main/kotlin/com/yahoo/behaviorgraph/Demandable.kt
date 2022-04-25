package com.yahoo.behaviorgraph

interface Demandable {
    val resource: Resource
    val type: LinkType
}

data class DemandLink(val resource: Resource, val type: LinkType)