package behaviorgraph

/**
 * A Graph [Event] saves the current time when it runs.
 * This optional interface lets this time be overridden, typically for testing purposes.
 */
interface DateProvider {
    fun now(): Long
}