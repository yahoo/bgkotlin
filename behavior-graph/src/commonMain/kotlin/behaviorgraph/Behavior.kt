//
// Copyright Yahoo 2021
//
package behaviorgraph

/**
 * A behavior is a block of code together with its dependency relationships (links). They are one of the two node types in a behavior graph. You define behaviors using the behavior() factory method of an Extent.
 *
 * Behaviors have both static and dynamic links. You provide static links when you create the behavior. Behavior Graph will update dynamic links per special methods on BehaviorBuilder or you can update them directly on a behavior.
 * @property extent A behavior always has an [Extent] with which it is created.
 */
class Behavior<T: Any>(
    val extent: Extent<T>, demands: List<Demandable>?, supplies: List<Resource>?,
    internal var thunk: ExtentThunk<T>
) : Comparable<Behavior<*>> {
    /**
     * The current set of all Resources which the behavior demands.
     */
    var demands: MutableSet<Resource>? = null
        internal set
    internal var orderingDemands: MutableSet<Resource>?  = null

    /**
     * The current set of all Resources which the behavior supplies.
     */
    var supplies: Set<Resource>? = null
        internal set
    internal var enqueuedWhen: Long? = null
    internal var removedWhen: Long? = null
    internal var orderingState = OrderingState.Untracked
    var order: Long = 0
        internal set

    internal var untrackedDemands: List<Demandable>?
    internal var untrackedDynamicDemands: List<Demandable>? = null
    internal var untrackedSupplies: List<Resource>?
    internal var untrackedDynamicSupplies: List<Resource>? = null

    init {
        extent.addBehavior(this)
        this.untrackedDemands = demands
        this.untrackedSupplies = supplies
    }

    override fun compareTo(other: Behavior<*>): Int {
        return order.compareTo(other.order)
    }

    override fun toString(): String {
        var rows = mutableListOf<String>("Behavior")
        supplies?.forEachIndexed { index, resource ->
            if (index == 0) {
                rows.add(" Supplies:")
            }
            rows.add("  " + resource.toString())
        }
        demands?.forEachIndexed { index, resource ->
            if (index == 0) {
                rows.add(" Demands:")
            }
            rows.add("  " + resource.toString())
        }
        return rows.joinToString("\n")
    }

    /**
     * Provide an array of Demandables. undefined is also an element type to make for easier use of optional chaining. Providing null is equivalent to saying there are no dynamic demands.
     */
    fun setDynamicDemands(vararg newDemands: Demandable) {
        setDynamicDemands(newDemands.asList())
    }

    /**
     * Provide an array of Demandables. undefined is also an element type to make for easier use of optional chaining. Providing null is equivalent to saying there are no dynamic demands.
     */
    fun setDynamicDemands(newDemands: List<Demandable?>?) {
        this.extent.graph.updateDemands(this, newDemands?.filterNotNull())
    }

    /**
     * Provide an array of Resources to supply. undefined is also an element type to make for easier use of optional chaining. Providing null is equivalent to saying there are no dynamic supplies.
     */
    fun setDynamicSupplies(vararg newSupplies: Resource) {
        setDynamicSupplies(newSupplies.asList())
    }

    /**
     * Provide an array of Resources to supply. undefined is also an element type to make for easier use of optional chaining. Providing null is equivalent to saying there are no dynamic supplies.
     */
    fun setDynamicSupplies(newSupplies: List<Resource?>?) {
        this.extent.graph.updateSupplies(this, newSupplies?.filterNotNull())
    }
}
