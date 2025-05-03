package behaviorgraph

import kotlin.jvm.JvmOverloads

fun interface DemandableLinks<T> {
    fun invoke(ctx: T, demands: MutableList<Demandable?>)
}

fun interface SuppliableLinks<T> {
    fun invoke(ctx: T, supplies: MutableList<Resource?>)
}


interface DynamicDemandable<T> : Demandable {
    val switchingResource: Resource // not used, just for compatibility with Demandable interface
    val relinkingOrder: RelinkingOrder? // not used, just for compatibility with Demandable interface
    fun appendDemands(graph: Graph, ctx: T, demands: MutableList<Demandable?>)

    override val resource: Resource
        get() = switchingResource

    override val type: LinkType
        get() = LinkType.Reactive


    fun addResultsToDemands(graph: Graph, results: Any?, demands: MutableList<Demandable?>) {
        results?.let {
            when (it) {
                is Iterable<*> -> {
                    for (resourceElement in it) {
                        if (resourceElement != null) {
                            demands.add(resourceElement as Demandable)
                        }
                    }
                }

                is Demandable -> {
                    demands.add(it)
                }

                else -> {
                    graph.bgassert(false, {
                        "DynamicDemandable must return an Iterable<Demandable> or Demandable from dynamicResources. \n" +
                                "Received: $it \n" +
                                "State: $switchingResource"
                    })
                }
            }
        }
    }
}

internal class GenericDynamicDemandable<T>(
    override val switchingResource: Resource,
    override val relinkingOrder: RelinkingOrder?,
    private val dynamicResources: (ctx: T, MutableList<Demandable?>) -> Unit
) : DynamicDemandable<T> {
    override fun appendDemands(graph: Graph, ctx: T, demands: MutableList<Demandable?>) {
        dynamicResources(ctx, demands)
    }
}

internal class SelectDynamicDemandable<T: Extent<*>>(
    override val switchingResource: State<T?>,
    override val relinkingOrder: RelinkingOrder?,
    private val dynamicResources: (T) -> Any?
) : DynamicDemandable<Any> {
    override fun appendDemands(graph: Graph, ctx: Any, demands: MutableList<Demandable?>) {
        switchingResource.value?.let {
            dynamicResources(it)?.let { results ->
                addResultsToDemands(graph, results, demands)
            }
        }
    }
}
internal class EachDynamicDemandable<T: Extent<*>>(
    override val switchingResource: State<out Iterable<T>>,
    override val relinkingOrder: RelinkingOrder?,
    private val dynamicResources: (T) -> Any?
) : DynamicDemandable<Any> {
    override fun appendDemands(graph: Graph, ctx: Any, demands: MutableList<Demandable?>) {
        switchingResource.value.forEach {
            dynamicResources(it)?.let { results ->
                addResultsToDemands(graph, results, demands)
            }
        }
    }
}

internal fun <T: Extent<*>> State<T?>.select(
    relinkingOrder: RelinkingOrder? = null,
    dynamicResources: (T) -> Any?
): DynamicDemandable<*> {
    return SelectDynamicDemandable(this, relinkingOrder, dynamicResources)
}

internal fun <T:Extent<*>, U: Iterable<T>> State<out U>.each(
    relinkingOrder: RelinkingOrder? = null,
    dynamicResources: (T) -> Any?
): DynamicDemandable<*> {
    return EachDynamicDemandable(this, relinkingOrder, dynamicResources)
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
    private var untrackedDemands: MutableList<Demandable> = mutableListOf()
    private var untrackedSupplies: MutableList<Resource> = mutableListOf()
    private var dynamicDemandable: DynamicDemandable<T>? = null
    private var dynamicSupplySwitches: List<Demandable>? = null
    private var dynamicSupplyLinks: SuppliableLinks<T>? = null
    private var dynamicSupplyRelinkingOrder: RelinkingOrder = RelinkingOrder.RelinkingOrderPrior

    /**
     * Optional clause to include set of static (unchanging) demands this behavior will depend on.
     */
    fun demands(vararg demands: Demandable) = apply {
        for (demand in demands) {
            if (demand is DynamicDemandable<*>) {
                untrackedDemands.add(demand.switchingResource)
                dynamicDemandable = demand as DynamicDemandable<T>
            } else {
                untrackedDemands.add(demand)
            }
        }
    }
    fun demands(demands: List<Demandable>) = apply {
        for (demand in demands) {
            if (demand is DynamicDemandable<*>) {
                untrackedDemands.add(demand.switchingResource)
                dynamicDemandable = demand as DynamicDemandable<T>
            } else {
                untrackedDemands.add(demand)
            }
        }
    }

    /**
     * Optional clause to include a set of static (unchanging) supplies this behavior will be responsible for.
     */
    fun supplies(vararg supplies: Resource) = apply { untrackedSupplies.addAll(supplies) }
    fun supplies(supplies: List<Resource>) = apply { untrackedSupplies.addAll(supplies) }

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
        switch: Demandable,
        relinkingOrder: RelinkingOrder = RelinkingOrder.RelinkingOrderPrior,
        links: DemandableLinks<T>
    ) = apply {
        dynamicDemandable = GenericDynamicDemandable(switch as Resource, relinkingOrder) { ctx, demands ->
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
        switches: List<Demandable>,
        relinkingOrder: RelinkingOrder = RelinkingOrder.RelinkingOrderPrior,
        links: SuppliableLinks<T>
    ) = apply {
        dynamicSupplySwitches = switches
        dynamicSupplyLinks = links
        dynamicSupplyRelinkingOrder = relinkingOrder
    }
    @JvmOverloads
    fun dynamicSupplies(
        vararg switches: Demandable,
        relinkingOrder: RelinkingOrder = RelinkingOrder.RelinkingOrderPrior,
        links: SuppliableLinks<T>
    ) = apply {
        dynamicSupplySwitches = switches.asList()
        dynamicSupplyLinks = links
        dynamicSupplyRelinkingOrder = relinkingOrder
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
        var dynamicDemandResource: Resource? = null
        dynamicDemandable?.let {
            val newDynamicDemandResource = extent.resource("(BG Dynamic Demand Resource)")
            if (it.relinkingOrder == RelinkingOrder.RelinkingOrderPrior || it.relinkingOrder == null) {
                untrackedDemands.add(newDynamicDemandResource)
            } else {
                untrackedSupplies.add(newDynamicDemandResource)
            }
            dynamicDemandResource = newDynamicDemandResource
        }

        var dynamicSupplyResource: Resource? = null
        if (dynamicSupplySwitches != null) {
            dynamicSupplyResource = extent.resource("(BG Dynamic Supply Resource)")
            if (dynamicSupplyRelinkingOrder == RelinkingOrder.RelinkingOrderPrior) {
                untrackedDemands.add(dynamicSupplyResource)
            } else {
                untrackedSupplies.add(dynamicSupplyResource)
            }
        }

        val mainBehavior = Behavior(extent, untrackedDemands, untrackedSupplies, thunk)
        extent.addBehavior(mainBehavior)

        dynamicDemandable?.let { dynamicDemandable ->
            var supplies: List<Resource>? = null
            val demands: MutableList<Demandable> = mutableListOf()
            demands.add(dynamicDemandable.switchingResource)
            if (dynamicDemandable.relinkingOrder == RelinkingOrder.RelinkingOrderPrior || dynamicDemandable.relinkingOrder == null) {
                dynamicDemandResource?.let { resource -> supplies = listOf(resource) }
            } else {
                dynamicDemandResource?.let { resource -> demands.add(resource) }
            }
            val orderingBehavior = Behavior(extent, demands, supplies) {
                val mutableListOfDemands = mutableListOf<Demandable?>()
                dynamicDemandable.appendDemands(extent.graph, it, mutableListOfDemands)
                mainBehavior.setDynamicDemands(mutableListOfDemands)
            }
            extent.addBehavior(orderingBehavior)
        }

        if (dynamicSupplySwitches != null) {
            var supplies: List<Resource>? = null
            val demands: MutableList<Demandable> = mutableListOf()
            dynamicSupplySwitches?.let { demands.addAll(it) }
            if (dynamicSupplyRelinkingOrder == RelinkingOrder.RelinkingOrderPrior) {
                dynamicSupplyResource?.let { supplies = listOf(it) }
            } else {
                dynamicSupplyResource?.let { demands.add(it) }
            }
            val orderingBehavior = Behavior(extent, demands, supplies) {
                val mutableListOfSupplies = mutableListOf<Resource?>()
                dynamicSupplyLinks?.invoke(it, mutableListOfSupplies)
                mainBehavior.setDynamicSupplies(mutableListOfSupplies)
            }
            extent.addBehavior(orderingBehavior)
        }

        return mainBehavior
    }
}

