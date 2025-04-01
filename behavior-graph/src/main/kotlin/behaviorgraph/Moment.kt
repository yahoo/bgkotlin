//
// Copyright Yahoo 2021
//
package behaviorgraph

import kotlinx.coroutines.Job
import java.util.concurrent.Future

/**
 * A Moment is a type of resource is a type of Resource for tracking information that exists at a
 * single moment in time. A Button press is an example of a moment. It happens and then it is over.
 * Use [TypedMoment] if you wish to associate additional data with a Moment.
 */
class Moment @JvmOverloads constructor(extent: Extent<*>, debugName: String? = null): Resource(extent, debugName),
    Transient {
    private var _happened = false
    private var _happenedWhen: Event? = null

    /**
     * If this Moment has ever been update what was the last Event it was updated.
     * A behavior must demand this resource to access this property.
     */
    @get:JvmName("event")
    val event: Event?
        get() {
            assertValidAccessor()
            return this._happenedWhen
        }

    /**
     * Is there a current event and was this Moment resource updated during this event.
     * A behavior must demand this resource to access this property.
     */
    @get:JvmName("justUpdated")
    val justUpdated: Boolean
        get() {
            assertValidAccessor()
            return _happened
        }

    override val internalJustUpdated: Boolean get() = justUpdated

    /**
     * Mark this Moment resource as updated an activate any dependent behaviors.
     * A behavior must supply this resource in order to update it.
     */
    fun update() {
        assertValidUpdater()
        _happened = true
        _happenedWhen = graph.currentEvent
        graph.resourceTouched(this)
        graph.trackTransient(this)
    }

    /**
     * Create a new action and call [update].
     */
    @JvmOverloads
    fun updateWithAction(debugName: String? = null): Job {
        return graph.action(debugName) {
            update()
        }
    }

    override fun clear() {
        _happened = false
    }

    override fun toString(): String {
        return String.format("%s %s == %s (%s)", debugName ?: "", super.toString(), if (_happened) "Updated" else "Not Updated", _happenedWhen?.sequence)
    }

}