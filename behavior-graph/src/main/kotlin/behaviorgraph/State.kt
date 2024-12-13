//
// Copyright Yahoo 2021
//
package behaviorgraph

import behaviorgraph.Event.Companion.InitialEvent
import java.util.concurrent.Future

/**
 * A State is a type of resource for storing information over a period of time. Its value will persist into the future until it is updated.
 * All States must be given an initial value when created.
 */
class State<T> @JvmOverloads constructor(extent: Extent<*>, initialState: T, debugName: String? = null) :
    Resource(extent, debugName),
    Transient {
    private var currentState = StateHistory(initialState, InitialEvent)
    private var priorStateDuringEvent: StateHistory<T>? = null

    /**
     * The current underlying value.
     * A behavior must demand this State resource in order to access this property.
     */
    @get:JvmName("value")
    val value: T
        get() {
            assertValidAccessor()
            return currentState.value
        }

    /**
     * The last time the State was updated. Will return [Event.InitialEvent] for its initial value before it has been updated.
     * A behavior must demand this State resource in order to access this property.
     */
    @get:JvmName("event")
    val event: Event
        get() {
            assertValidAccessor()
            return currentState.event
        }
    private val trace: StateHistory<T>
        get() = priorStateDuringEvent ?: currentState

    /**
     * What was the value of the value property at the beginning of the current event.
     * If this resource has justUpdated it will return the previous value.
     * Otherwise it will return the current value.
     */
    @get:JvmName("traceValue")
    val traceValue: T
        get() = trace.value

    /**
     * What was the `event` property at the beginning of the current event.
     * If this resource has justUpdated it will return the previous event.
     * Otherwise it will return the current event.
     */
    @get:JvmName("traceEvent")
    val traceEvent: Event
        get() = trace.event


    /**
     * Create a new action and call [update].
     */
    @JvmOverloads
    fun updateWithAction(newValue: T, debugName: String? = null): Future<*> {
        return graph.action(debugName, { update(newValue) })
    }

    /**
     * Mark this State resource as updated, associate a value with that update and activate any dependent behaviors.
     * If the newValue is `==` to the current value, then the State resource will not update or activate the dependent
     * behavior.
     * A behavior must supply this resource in order to update it.
     */
    fun update(newValue: T) {
        if (newValue == currentState.value) {
            return
        }
        updateForce(newValue)
    }

    /**
     * Mark this State resource as updated, associate a value with that update and activate any dependent behaviors.
     * There is no `==` check. The State resource will always update and activate the dependent
     * behavior.
     * A behavior must supply this resource in order to update it.
     */
    fun updateForce(newValue: T) {
        assertValidUpdater()
        val thisSequence = graph.currentEvent?.sequence ?: 0
        if (graph.currentEvent != null && currentState.event.sequence < thisSequence) {
            // this check prevents updating priorState if we are updated multiple times in same behavior
            priorStateDuringEvent = currentState
        }

        this.graph.currentEvent?.let {
            currentState = StateHistory(newValue, it)
        }
        this.graph.resourceTouched(this)
        this.graph.trackTransient(this)
    }

    /**
     * Is there a current event and was this resource updated during this event.
     * A behavior must demand this resource to access this property.
     */
    @get:JvmName("justUpdated")
    val justUpdated: Boolean
        get() {
            assertValidAccessor()
            return currentState.event == graph.currentEvent
        }

    override val internalJustUpdated: Boolean get() = justUpdated

    /**
     * Checks if [justUpdated] and if the associated value is `==` to the passed in value.
     */
    fun justUpdatedTo(toValue: T): Boolean {
        return justUpdated &&
            (currentState.value == toValue)
    }

    /**
     * Checks if [justUpdated] and if the previous value is `==` to the passed in value.
     */
    fun justUpdatedFrom(fromValue: T): Boolean {
        return justUpdated &&
            (priorStateDuringEvent?.value == fromValue)
    }

    /**
     * Checks if [justUpdated] and if the current value and previous value both are `==` to the passed in values.
     */
    fun justUpdatedToFrom(toValue: T, fromValue: T): Boolean {
        return justUpdatedTo(toValue) && justUpdatedFrom(fromValue)
    }

    override fun clear() {
        priorStateDuringEvent = null
    }

    override fun toString(): String {
        return String.format("%s %s == %s (%s)", debugName ?: "", super.toString(), currentState.value, currentState.event.sequence)
    }

    private data class StateHistory<T>(val value: T, val event: Event)
}



