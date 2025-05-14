package behaviorgraph

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
            extent.graph.bgassert(false) {
                "Same lifetime relationship must be established before adding any extent to graph. \nExtent=$extent"
            }
            // disabled asserts just allows this
        }
        if (extent.lifetime != null) {
            // merge existing lifetimes and children into one lifetime heirarchy
            // move children first
            extent.lifetime?.children?.forEach {lifetime ->
                addChildLifetime(lifetime)
            }
            // then make any extents in other lifetime part of this one
            extent.lifetime?.extents?.forEach { it ->
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
        extent.lifetime?.let {
            addChildLifetime(it)
        }
    }

    fun addChildLifetime(lifetime: ExtentLifetime) {
        var myLifetime: ExtentLifetime? = this
        while (myLifetime != null) {
            // check up the chain of parents to prevent circular lifetime
            if (myLifetime == lifetime) {
                val extent = myLifetime.extents.first()
                if (extent != null) {
                    extent.graph.bgassert(false) {
                        "Extent lifetime cannot be a child of itself."
                    }
                }
                return
            }
            myLifetime = myLifetime.parent
        }
        lifetime.parent = this
        if (children == null) {
            children = mutableSetOf()
        }
        children?.add(lifetime)
    }

    fun hasCompatibleLifetime(lifetime: ExtentLifetime?): Boolean {
        if (this == lifetime) {
            // unified
            return true
        } else if (lifetime != null) {
            // parents
            val thisParent = parent
            if (thisParent != null) {
                // parent is a weak reference so we get it here
                val refParent = thisParent
                if (refParent != null) {
                    return refParent.hasCompatibleLifetime(lifetime)
                }
            }
        }
        return false
    }

    fun getAllContainedExtents(): List<Extent<*>> {
        val resultExtents = mutableListOf<Extent<*>>()
        resultExtents.addAll(extents)
        children?.forEach { childLifetime -> resultExtents.addAll(childLifetime.getAllContainedExtents()) }
        return resultExtents
    }

    fun getAllContainingExtents(): List<Extent<*>> {
        val resultExtents = mutableListOf<Extent<*>>()
        resultExtents.addAll(extents)
        parent?.let {
            resultExtents.addAll(it.getAllContainingExtents())
        }
        return resultExtents
    }

    fun clearExtentRelationship(removedExtent: Extent<*>) {
        // Removed extents no longer participate in their lifetime
        // Unwind those to prevent memory leaks

        // removed extent is no longer part of it's lifetime
        extents.remove(removedExtent)
        // and empty lifetimes are no longer part of a parent child relationship
        if (extents.isEmpty()) {
            parent?.children?.remove(this)
            parent = null
        }
        removedExtent.lifetime = null
    }
}