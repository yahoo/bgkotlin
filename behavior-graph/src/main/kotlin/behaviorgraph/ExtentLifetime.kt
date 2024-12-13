package behaviorgraph

import java.lang.ref.WeakReference
import java.util.WeakHashMap

internal class ExtentLifetime(
    extent: Extent<*>
){
    var addedToGraphWhen: Long? = null
    // WeakHashMaps prevent extents from holding on to each other internally
    // otherwise we would need a hypothetical removeChildExtent when finished
    // in order for it to be fully released
    val extents: WeakHashMap<Extent<*>, Boolean> = WeakHashMap()
    var children: WeakHashMap<ExtentLifetime, Boolean>? = null
    var parent: WeakReference<ExtentLifetime>? = null

    init {
        extents[extent] = true
        if (extent.addedToGraphWhen != null) {
            addedToGraphWhen = extent.addedToGraphWhen
        }
    }

    fun unify(extent: Extent<*>) {
        if (extent.addedToGraphWhen != null) {
            assert(false) {
                "Same lifetime relationship must be established before adding any extent to graph. \nExtent=$extent"
            }
            // disabled asserts just allows this
        }
        if (extent.lifetime != null) {
            // merge existing lifetimes and children into one lifetime heirarchy
            // move children first
            extent.lifetime?.children?.forEach {(lifetime, _) ->
                addChildLifetime(lifetime)
            }
            // then make any extents in other lifetime part of this one
            extent.lifetime?.extents?.forEach { (it, _) ->
                it.lifetime = this
                extents[it] = true
            }
        } else {
            extent.lifetime = this
            extents[extent] = true
        }
    }

    fun addChild(extent: Extent<*>) {
        val thisLifetime = extent.lifetime
        if (thisLifetime == null) {
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
                assert(false) {
                    "Extent lifetime cannot be a child of itself."
                }
                return
            }
            myLifetime = myLifetime.parent?.get()
        }
        lifetime.parent = WeakReference(this)
        if (children == null) {
            children = WeakHashMap()
        }
        children?.put(lifetime, true)
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
                val refParent = thisParent.get()
                if (refParent != null) {
                    return refParent.hasCompatibleLifetime(lifetime)
                }
            }
        }
        return false
    }

    fun getAllContainedExtents(): List<Extent<*>> {
        val resultExtents = mutableListOf<Extent<*>>()
        resultExtents.addAll(extents.keys)
        children?.forEach { (childLifetime, _) -> resultExtents.addAll(childLifetime.getAllContainedExtents()) }
        return resultExtents
    }

    fun getAllContainingExtents(): List<Extent<*>> {
        val resultExtents = mutableListOf<Extent<*>>()
        resultExtents.addAll(extents.keys)
        parent?.let {
            it.get()?.let {
                resultExtents.addAll(it.getAllContainingExtents()) }
            }
        return resultExtents
    }
}