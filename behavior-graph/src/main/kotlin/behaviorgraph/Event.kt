//
// Copyright Yahoo 2021
//
package behaviorgraph

/**
 * An **Event** is a single run pass through a [Graph] instance. It starts with
 * an [Action], proceeds through running each relevant [Behavior]. And completes
 * after the last [GraphSideEffect] is run. Every resource that is updated during this
 * pass will point to the same `Event` instance.
 *
 * You do not create Events explicitly. The Graph instance will create one when it runs.
 *
 * `InitialEvent` is the Zero event which occurs _before_ all other events.
 *
 */
class Event internal constructor(sequence: Long, timestamp: Long) {
    /**
     * Each Event is assigned a monotonically increasing number. You can use this information to quickly determine the order in which resources update.
     */
    val sequence: Long

    /**
     * Each event is given a timestamp. By default this is the current time in milliseconds but can be overridden with the [DateProvider] instance passed on [Graph] construction.
     */
    val timestamp: Long

    init {
        this.sequence = sequence
        this.timestamp = timestamp
    }

    companion object {
        /**
         * `InitialEvent` is the Zero event which occurs _before_ all other events.
         * New [State] resources automatically are given this event to pair with their initial values.
         */
        val InitialEvent: Event = Event(0, 0)
    }
}
