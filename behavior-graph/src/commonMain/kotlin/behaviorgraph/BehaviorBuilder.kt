package behaviorgraph

import kotlin.jvm.JvmOverloads

fun interface DynamicLinks<T> {
    fun invoke(ctx: T, links: MutableList<Linkable?>)
}


interface DynamicLinkable<T> : Linkable {
    val switchingResources: List<Linkable> // not used, just for compatibility with Demandable interface
    val relinkingOrder: RelinkingOrder? // not used, just for compatibility with Demandable interface
    fun appendDemands(graph: Graph, ctx: T, demands: MutableList<Linkable?>)

    override val resource: Resource
        get() = switchingResources[0].resource

    override val type: LinkType
        get() = LinkType.Reactive


    fun addResultsToDemands(graph: Graph, results: Any?, demands: MutableList<Linkable?>) {
        results?.let {
            when (it) {
                is Iterable<*> -> {
                    for (resourceElement in it) {
                        if (resourceElement != null) {
                            demands.add(resourceElement as Linkable)
                        }
                    }
                }

                is Linkable -> {
                    demands.add(it)
                }

                else -> {
                    graph.bgassert(false, {
                        "DynamicDemandable must return an Iterable<Demandable> or Demandable from dynamicResources. \n" +
                                "Received: $it \n" +
                                "State: $switchingResources[0]"
                    })
                }
            }
        }
    }
}

internal class GenericDynamicLinkable<T>(
    override val switchingResources: List<Linkable>,
    override val relinkingOrder: RelinkingOrder?,
    private val dynamicResources: (ctx: T, MutableList<Linkable?>) -> Unit
) : DynamicLinkable<T> {
    override fun appendDemands(graph: Graph, ctx: T, demands: MutableList<Linkable?>) {
        dynamicResources(ctx, demands)
    }
}

internal class SelectDynamicLinkable<T: Extent<*>>(
    override val switchingResources: List<State<T?>>,
    override val relinkingOrder: RelinkingOrder?,
    private val dynamicResources: (T) -> Any?
) : DynamicLinkable<Any> {
    override fun appendDemands(graph: Graph, ctx: Any, demands: MutableList<Linkable?>) {
        switchingResources[0].value?.let {
            dynamicResources(it)?.let { results ->
                addResultsToDemands(graph, results, demands)
            }
        }
    }
}
internal class EachDynamicLinkable<T: Extent<*>>(
    override val switchingResources: List<State<out Iterable<T>>>,
    override val relinkingOrder: RelinkingOrder?,
    private val dynamicResources: (T) -> Any?
) : DynamicLinkable<Any> {
    override fun appendDemands(graph: Graph, ctx: Any, demands: MutableList<Linkable?>) {
        switchingResources[0].value.forEach {
            dynamicResources(it)?.let { results ->
                addResultsToDemands(graph, results, demands)
            }
        }
    }
}

internal fun <T: Extent<*>> State<T?>.select(
    relinkingOrder: RelinkingOrder? = null,
    dynamicResources: (T) -> Any?
): DynamicLinkable<*> {
    return SelectDynamicLinkable(listOf(this), relinkingOrder, dynamicResources)
}

internal fun <T:Extent<*>, U: Iterable<T>> State<out U>.each(
    relinkingOrder: RelinkingOrder? = null,
    dynamicResources: (T) -> Any?
): DynamicLinkable<*> {
    return EachDynamicLinkable(listOf(this), relinkingOrder, dynamicResources)
}

/**
 * Provides a fluent API interface for creating a [Behavior].
 * Use the [Behavior] method on [Extent] to use.
 *
 * Example:
 * ```kotlin
 * extentInstance.behavior()
 *   .supplies(resource3)
 *   .demands(resource1, resource2)
 *   .runs {
 *     resource3.update(resource1.value + resource2.value)
 *   }
 * ```
 */
class BehaviorBuilder<T: Any>(
    internal val extent: Extent<T>
) {
    private var untrackedDemands: MutableList<Linkable> = mutableListOf()
    private var untrackedSupplies: MutableList<Linkable> = mutableListOf()
    private var dynamicDemandable: DynamicLinkable<T>? = null
    private var dynamicSuppliable: DynamicLinkable<T>? = null

    /**
     * Optional clause to include set of static (unchanging) demands this behavior will depend on.
     */
    fun demands(vararg demands: Linkable) = apply {
        internalAddDemands(demands.toList())
    }
    fun demands(demands: List<Linkable>) = apply {
        // lists make it easier to programmatically add multiple demands
        internalAddDemands(demands)
    }

    private fun internalAddDemands(demands: Iterable<Linkable>) {
        for (demand in demands) {
            if (demand is DynamicLinkable<*>) {
                extent.graph.bgassert(dynamicDemandable == null) {
                    "Only one dynamic demand clause allowed."
                }
                dynamicDemandable = demand as DynamicLinkable<T>
            } else {
                untrackedDemands.add(demand)
            }
        }
    }

    /**
     * Optional clause to include a set of static (unchanging) supplies this behavior will be responsible for.
     */
    fun supplies(vararg supplies: Linkable) = apply {
        internalAddSupplies(supplies.toList())
    }
    fun supplies(supplies: List<Linkable>) = apply {
        internalAddSupplies(supplies)
    }

    private fun internalAddSupplies(supplies: Iterable<Linkable>) {
        for (supply in supplies) {
            if (supply is DynamicLinkable<*>) {
                extent.graph.bgassert(dynamicSuppliable == null) {
                    "Only one dynamic supply clause allowed."
                }
                dynamicSuppliable = supply as DynamicLinkable<T>
            } else {
                untrackedSupplies.add(supply)
            }
        }
    }
    /**
     * Optional clause to include a set of demands which can change based on other resources changing.
     *
     * Example:
     * ```kotlin
     * extentInstance.behavior()
     *   .dynamicDemands(resource1, relinkingOrder = RelinkingOrder.RelinkingOrderSubsequent) { ctx, demands ->
     *     demands.add(resource2)
     *     demands.add(resource3)
     *   }
     *   .runs { ...
     * ```
     * @param switch When this resource changes, the `links` code block will run to determine which additional demands a behavior should depend on.
     * @param relinkingOrder Should the dynamic demands be set before or after the behavior is run. If in doubt choose `RelinkingOrderPrior`
     * @param links This anonymous function passes in an empty list of dynamic demands. You should add any additional demands the behavior will include. The `ctx` parameter points to the context object for the [Extent] this behaivor is created on.
     */
    @JvmOverloads
    fun dynamicDemands(
        switches: List<Linkable>,
        relinkingOrder: RelinkingOrder = RelinkingOrder.RelinkingOrderPrior,
        links: DynamicLinks<T>
    ) = apply {
        extent.graph.bgassert(dynamicDemandable == null) {
            "Only one dynamic demand clause allowed."
        }
        dynamicDemandable = GenericDynamicLinkable(switches, relinkingOrder) { ctx, demands ->
            links.invoke(ctx, demands)
        }
    }

    @JvmOverloads
    fun dynamicDemands(
        vararg switches: Linkable,
        relinkingOrder: RelinkingOrder = RelinkingOrder.RelinkingOrderPrior,
        links: DynamicLinks<T>
    ) = apply {
        extent.graph.bgassert(dynamicDemandable == null) {
            "Only one dynamic demand clause allowed."
        }
        dynamicDemandable = GenericDynamicLinkable(switches.toList(), relinkingOrder) { ctx, demands ->
            links.invoke(ctx, demands)
        }
    }
    /**
     * Optional clause to include a set of supplies which can change based on other resources changing
     *
     * Example:
     * ```kotlin
     * extentInstance.behavior()
     *   .dynamicSupplies(resource1, relinkingOrder = RelinkingOrder.RelinkingOrderSubsequent) { ctx, supplies ->
     *     supplies.add(resource2)
     *     supplies.add(resource3)
     *   }
     *   .runs { ...
     * ```
     *
     * @param switches When these resources change, the `links` code block will run to determine which additional supplies a behavior should be responsible for.
     * @param relinkingOrder Should the dynamic supplies be set before or after the behavior is run. If in doubt choose `RelinkingOrderPrior` (which is the default).
     * @param links This anonymous function passes in an empty list of dynamic supplies. You should add any additional supplies the behavior will include. The `ctx` parameter points to the context object for the [Extent] this behaivor is created on.
     */
    @JvmOverloads
    fun dynamicSupplies(
        switches: List<Linkable>,
        relinkingOrder: RelinkingOrder = RelinkingOrder.RelinkingOrderPrior,
        links: DynamicLinks<T>
    ) = apply {
        extent.graph.bgassert(dynamicSuppliable == null) {
            "Only one dynamic supply clause allowed."
        }
        dynamicSuppliable = GenericDynamicLinkable(switches, relinkingOrder) { ctx, demands ->
            links.invoke(ctx, demands)
        }
    }

    @JvmOverloads
    fun dynamicSupplies(
        vararg switches: Linkable,
        relinkingOrder: RelinkingOrder = RelinkingOrder.RelinkingOrderPrior,
        links: DynamicLinks<T>
    ) = apply {
        extent.graph.bgassert(dynamicSuppliable == null) {
            "Only one dynamic supply clause allowed."
        }
        dynamicSuppliable = GenericDynamicLinkable(switches.asList(), relinkingOrder) { ctx, demands ->
            links.invoke(ctx, demands)
        }
    }

    /**
     * Alternate form of `runs` for Kotlin because `.run()` is a builtin scope function
     * and is easily confused with Behavior Graph's `.runs()`.
     */
    fun performs(thunk: ExtentThunk<T>): Behavior<T> = runs(thunk)

    /**
     * Required clause which sets the block of code the behavior will run when one or more of its demands are updated.
     * The `ext` parameter passed in when the behavior is run is the [Extent] instance this behavior was created on.
     *
     * @return The behavior that was created. Typically the results are discarded unless you need direct access to the
     * behavior later.
     */
    fun runs(thunk: ExtentThunk<T>): Behavior<T> {
        // When we have dynamic demands/supplies we will create separate behaviors
        // to do that relinking. So first we will create some additional resources
        // that force the relinking behaviors to proprerly order themselves beofre or
        // after the main behavior. (We will need these resources when we create that
        // main behavior, so we figure that out first).

        // 1. Handle dynamic demand clause
        var dynamicDemandResource: Resource? = null
        dynamicDemandable?.let {
            val newDynamicDemandResource = extent.resource("(BG Dynamic Demand Resource)")
            if (it.relinkingOrder == RelinkingOrder.RelinkingOrderPrior || it.relinkingOrder == null) {

                validateSwitchingResourcesNotSupplied(it.switchingResources, "demands")

                // If relinking is prior, then we will demand the ordering resource to make sure our main behavior comes after
                untrackedDemands.add(newDynamicDemandResource)

            } else {
                // if subsequent, then we will supply the ordering resource to make sure our main behavior comes before
                untrackedSupplies.add(newDynamicDemandResource)
            }
            dynamicDemandResource = newDynamicDemandResource

            if (it is SelectDynamicLinkable<*> || it is EachDynamicLinkable<*>) {
                // The select/each dynamic linkables are designed for compactness so
                // we can assume we will also demand the switching resource becuase we almost
                // always need it in the main behavior to iterate over.
                // But we don't want to do this for the more expressive dynamic demand clauses
                // to enable full flexibility (like sometimes using trace demands to eliminate more complex cycles)
                untrackedDemands.addAll(it.switchingResources) // there will only be one
            }
        }

        // 2. Handle dynamic supply clause, similar to above
        var dynamicSupplyResource: Resource? = null
        dynamicSuppliable?.let {
            val newDynamicSupplyResource = extent.resource("(BG Dynamic Supply Resource)")
            if (it.relinkingOrder == RelinkingOrder.RelinkingOrderPrior || it.relinkingOrder == null) {
                validateSwitchingResourcesNotSupplied(it.switchingResources, "supplies")

                // if relinking is prior, then we will demand the ordering resource to make sure our main behavior comes after
                untrackedDemands.add(newDynamicSupplyResource)

            } else {
                // if relinking is subsequent, then our relinking behavior will come after the main behavior
                untrackedSupplies.add(newDynamicSupplyResource)
            }
            dynamicSupplyResource = newDynamicSupplyResource

            // see dynamic demands above for explanation
            if (it is SelectDynamicLinkable<*> || it is EachDynamicLinkable<*>) {
                untrackedDemands.addAll(it.switchingResources) // there will only be one
            }
        }

        // 3. Create the main behavior
        val mainBehavior = Behavior(extent, untrackedDemands, untrackedSupplies, thunk)
        extent.addBehavior(mainBehavior)


        // 4. Create behavior to do dynamic demand relinking
        dynamicDemandable?.let { dynamicDemandable ->
            var supplies: List<Linkable>? = null
            val demands: MutableList<Linkable> = mutableListOf()
            // relinking behavior activates on switching resources
            demands.addAll(dynamicDemandable.switchingResources)

            if (dynamicDemandable.relinkingOrder == RelinkingOrder.RelinkingOrderPrior || dynamicDemandable.relinkingOrder == null) {
                // if relinking is prior, then we supply the ordering resource to make sure we come first
                // (main behavior will demand it)
                dynamicDemandResource?.let { resource -> supplies = listOf(resource) }
            } else {
                // if subsequent, we demand it so we come after (main behavior will supply it)
                dynamicDemandResource?.let { resource -> demands.add(resource) }
            }
            // create the behavior that determines new demands each time it runs
            val orderingBehavior = Behavior(extent, demands, supplies) {
                val mutableListOfDemands = mutableListOf<Linkable?>()
                dynamicDemandable.appendDemands(extent.graph, it, mutableListOfDemands)
                mainBehavior.setDynamicDemands(mutableListOfDemands)
            }
            extent.addBehavior(orderingBehavior)
        }

        // 5. Create behavior to do dynamic supply relinking
        dynamicSuppliable?.let { dynamicSuppliable ->
            var supplies: List<Linkable>? = null
            val demands: MutableList<Linkable> = mutableListOf()
            demands.addAll(dynamicSuppliable.switchingResources)
            if (dynamicSuppliable.relinkingOrder == RelinkingOrder.RelinkingOrderPrior || dynamicSuppliable.relinkingOrder == null) {
                dynamicSupplyResource?.let { supplies = listOf(it) }
            } else {
                dynamicSupplyResource?.let { demands.add(it) }
            }
            val orderingBehavior = Behavior(extent, demands, supplies) {
                val mutableListOfSupplies = mutableListOf<Linkable?>()
                dynamicSuppliable.appendDemands(extent.graph, it, mutableListOfSupplies)
                mainBehavior.setDynamicSupplies(mutableListOfSupplies)
            }
            extent.addBehavior(orderingBehavior)
        }

        return mainBehavior
    }

    fun validateSwitchingResourcesNotSupplied(switchingResources: List<Linkable>, section: String) {
        // It is easy to supply a resource that we also depend on in our dynamic demands
        // This will create a cycle, but we can check for it here and give a better warning.
        var suppliesContainsSwitching = false
        for (resource in switchingResources) {
            if (untrackedSupplies.contains(resource)) {
                suppliesContainsSwitching = true
                break
            }
        }
        extent.graph.bgassert(!suppliesContainsSwitching) {
            "If a behavior has dynamic $section which are based on a supplied resource, then those dynamic $section must be RelinkingOrderSubsequent."
        }
    }
}

