package com.yahoo.behaviorgraph

import com.yahoo.behaviorgraph.exception.BehaviorGraphException
import com.yahoo.behaviorgraph.exception.ChildLifetimeCannotBeParent
import com.yahoo.behaviorgraph.exception.SameLifetimeMustBeEstablishedBeforeAddingToGraph

internal class ExtentLifetime(
    extent: Extent<*>
){
    var addedToGraphWhen: Long? = null
    val extents: MutableSet<Extent<*>> = mutableSetOf()
    var children: MutableSet<ExtentLifetime>? = null
    var parent: ExtentLifetime? = null

    init {
        extents.add(extent)
        if (extent.addedToGraphWhen != null) {
            addedToGraphWhen = extent.addedToGraphWhen
        }
    }

    fun unify(extent: Extent<*>) {
        if (extent.addedToGraphWhen != null) {
            val err = SameLifetimeMustBeEstablishedBeforeAddingToGraph(extent)
            throw err
        }
        if (extent.lifetime != null) {
            // merge existing lifetimes and children into one lifetime heirarchy
            // move children first
            extent.lifetime!!.children?.forEach {
                addChildLifetime(it)
            }
            // then make any extents in other lifetime part of this one
            extent.lifetime!!.extents.forEach {
                it.lifetime = this
                extents.add(it)
            }
        } else {
            extent.lifetime = this
            extents.add(extent)
        }
    }

    fun addChild(extent: Extent<*>) {
        if (extent.lifetime == null) {
            extent.lifetime = ExtentLifetime(extent)
        }
        addChildLifetime(extent.lifetime!!)
    }

    fun addChildLifetime(lifetime: ExtentLifetime) {
        var myLifetime: ExtentLifetime? = this
        while (myLifetime != null) {
            if (myLifetime == lifetime) {
                val err = BehaviorGraphException("Extent lifetime cannot be a child of itself")
                throw err
            }
            myLifetime = myLifetime.parent
        }
        lifetime.parent = this
        if (children == null) {
            children = mutableSetOf<ExtentLifetime>()
        }
        children?.add(lifetime)
    }

    fun hasCompatibleLifetime(lifetime: ExtentLifetime?): Boolean {
        if (this == lifetime) {
            // unified
            return true
        } else if (lifetime != null) {
            // parents
            if (parent != null) {
                return parent!!.hasCompatibleLifetime(lifetime)
            }
        }
        return false
    }

    fun getAllContainedExtents(): List<Extent<*>> {
        val resultExtents = mutableListOf<Extent<*>>()
        resultExtents.addAll(extents)
        children?.forEach { resultExtents.addAll(it.getAllContainedExtents()) }
        return resultExtents
    }

    fun getAllContainingExtents(): List<Extent<*>> {
        val resultExtents = mutableListOf<Extent<*>>()
        resultExtents.addAll(extents)
        parent?.let { resultExtents.addAll(it.getAllContainingExtents()) }
        return resultExtents
    }
}