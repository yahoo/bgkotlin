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
    data class Happened<T>(val value: T, val event: Event)
    private var _happened: Happened<T>? = null
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
            return this._happened?.value
        }

    /**
     * If this Moment has ever been update what was the last Event it was updated.
     * A behavior must demand this resource to access this property.
     */
    @get:JvmName("event")
    val event: Event?
        get() {
            assertValidAccessor()
            return this._happened?.event
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
        graph.currentEvent?.let {
            _happened = Happened(value, it)
            graph.resourceTouched(this)
            graph.trackTransient(this)
        }
    }

    override fun clear() {
        _happened = null
    }

    override fun toString(): String {
        val localDebugName = debugName ?: ""
        val localType = super.toString()
        val localUpdated = _happened?.value ?: "NA"
        val localSequence = _happened?.event?.sequence ?: "NA"
        return "$localDebugName $localType == $localUpdated ($localSequence)"
    }

    /**
     * Is there a current event and was this Moment resource updated during this event.
     * A behavior must demand this resource to access this property.
     */
    @get:JvmName("justUpdated")
    val justUpdated: Boolean get() {
        assertValidAccessor()
        return _happened != null
    }

    override val internalJustUpdated: Boolean get() = justUpdated

    /**
     * Checks if [justUpdated] and if the associated value is `==` to the passed in value.
     */
    fun justUpdatedTo(value: T): Boolean {
        return this.justUpdated && this._happened?.value == value
    }

    fun observeUpdates(onUpdated: (Pair<T, Event>) -> Unit): Behavior<*> {
        val extent = this.extent as Extent<Any>
        val observer = extent.behavior()
            .demands(this)
            .runs { _ ->
                this._happened?.let { happened ->
                    this.extent.sideEffect {
                        onUpdated(Pair(happened.value, happened.event))
                    }
                }
            }
        if (this.extent.addedToGraphWhen != null) {
            this.extent.graph.addLateBehavior(observer)
        }
        return observer
    }
}