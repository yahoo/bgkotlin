package behaviorgraph

import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmName

/**
 * A TypedMoment is a [Moment] resource that has associated data.
 * It is a type of Resource for tracking information that exists at a
 * single moment in time. A network call returning is an example of a TypedMoment. It happens,
 * when it happens it contains the results of the network call. At then end of the current event
 * it is no longer relevant.
 * Use [Moment] if you have no additional information.
 */
class TypedMoment<T> @JvmOverloads constructor(extent: Extent<*>, debugName: String? = null): Resource(extent, debugName),
    Transient {
    private var _happened = false
    private var _happenedWhen: Event? = null
    private var _happenedValue: T? = null

    /**
     * Is there a current event and if the moment updated then what is the associated data.
     * Will return null if the moment did not update this event.
     * Be careful: It is possible that T is also an optional type itself
     * So this typedMoment could be called with .update(null).
     * In that case justUpdated will be true and value will also be null.
     * A behavior must demand this resource to access its value.
     */
    @get:JvmName("value")
    val value: T?
        get() {
            assertValidAccessor()
            return this._happenedValue
        }

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
     * Create a new action and call [update].
     */
    @JvmOverloads
    fun updateWithAction(value: T, debugName: String? = null) {
        graph.action(debugName) { update(value) }
    }

    /**
     * Mark this TypedMoment resource as updated, associate a value with that update and activate any dependent behaviors.
     * A behavior must supply this resource in order to update it.
     */
    fun update(value: T) {
        assertValidUpdater()
        _happened = true
        _happenedValue = value
        _happenedWhen = graph.currentEvent
        graph.resourceTouched(this)
        graph.trackTransient(this)
    }

    override fun clear() {
        _happenedValue = null
        _happened = false
    }

    override fun toString(): String {
        val localDebugName = debugName ?: ""
        val localType = super.toString()
        val localUpdated = if (_happened) _happenedValue else "NA"
        val localSequence = _happenedWhen?.sequence ?: "NA"
        return "$localDebugName $localType == $localUpdated ($localSequence)"
    }

    /**
     * Is there a current event and was this Moment resource updated during this event.
     * A behavior must demand this resource to access this property.
     */
    @get:JvmName("justUpdated")
    val justUpdated: Boolean get() {
        assertValidAccessor()
        return _happened
    }

    override val internalJustUpdated: Boolean get() = justUpdated

    /**
     * Checks if [justUpdated] and if the associated value is `==` to the passed in value.
     */
    fun justUpdatedTo(value: T): Boolean {
        return this.justUpdated && this._happenedValue == value
    }

}