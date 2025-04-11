package behaviorgraph

import kotlin.jvm.JvmOverloads

fun interface DemandableLinks<T> {
    fun invoke(ctx: T, demands: MutableList<Demandable?>)
}

fun interface SuppliableLinks<T> {
    fun invoke(ctx: T, supplies: MutableList<Resource?>)
}

/**
 * Provides a fluent API interface for creating a [Behavior].
 * Use the [behavior] method on [Extent] to use.
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
    private var dynamicDemandSwitches: List<Demandable>? = null
    private var dynamicDemandLinks: DemandableLinks<T>? = null
    private var dynamicDemandRelinkingOrder: RelinkingOrder = RelinkingOrder.RelinkingOrderPrior
    private var dynamicSupplySwitches: List<Demandable>? = null
    private var dynamicSupplyLinks: SuppliableLinks<T>? = null
    private var dynamicSupplyRelinkingOrder: RelinkingOrder = RelinkingOrder.RelinkingOrderPrior

    /**
     * Optional clause to include set of static (unchanging) demands this behavior will depend on.
     */
    fun demands(vararg demands: Demandable) = apply { untrackedDemands.addAll(demands) }
    fun demands(demands: List<Demandable>) = apply { untrackedDemands.addAll(demands) }

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
     * @param switches When these resources change, the `links` code block will run to determine which additional demands a behavior should depend on.
     * @param relinkingOrder Should the dynamic demands be set before or after the behavior is run. If in doubt choose `RelinkingOrderPrior`
     * @param links This anonymous function passes in an empty list of dynamic demands. You should add any additional demands the behavior will include. The `ctx` parameter points to the context object for the [Extent] this behaivor is created on.
     */
    @JvmOverloads
    fun dynamicDemands(
        switches: List<Demandable>,
        relinkingOrder: RelinkingOrder = RelinkingOrder.RelinkingOrderPrior,
        links: DemandableLinks<T>
    ) = apply {
        dynamicDemandSwitches = switches
        dynamicDemandLinks = links
        dynamicDemandRelinkingOrder = relinkingOrder
    }

    @JvmOverloads
    fun dynamicDemands(
        vararg switches: Demandable,
        relinkingOrder: RelinkingOrder = RelinkingOrder.RelinkingOrderPrior,
        links: DemandableLinks<T>
    ) = apply {
        dynamicDemandSwitches = switches.asList()
        dynamicDemandLinks = links
        dynamicDemandRelinkingOrder = relinkingOrder
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
        if (dynamicDemandSwitches != null) {
            dynamicDemandResource = extent.resource("(BG Dynamic Demand Resource)")
            if (dynamicDemandRelinkingOrder == RelinkingOrder.RelinkingOrderPrior) {
                untrackedDemands.add(dynamicDemandResource)
            } else {
                untrackedSupplies.add(dynamicDemandResource)
            }
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

        if (dynamicDemandSwitches != null) {
            var supplies: List<Resource>? = null
            val demands: MutableList<Demandable> = mutableListOf()
            dynamicDemandSwitches?.let { demands.addAll(it) }
            if (dynamicDemandRelinkingOrder == RelinkingOrder.RelinkingOrderPrior) {
                dynamicDemandResource?.let { supplies = listOf(it) }
            } else {
                dynamicDemandResource?.let { demands.add(it) }
            }
            Behavior(extent, demands, supplies) {
                val mutableListOfDemands = mutableListOf<Demandable?>()
                dynamicDemandLinks?.invoke(it, mutableListOfDemands)
                mainBehavior.setDynamicDemands(mutableListOfDemands)
            }
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
            Behavior(extent, demands, supplies) {
                val mutableListOfSupplies = mutableListOf<Resource?>()
                dynamicSupplyLinks?.invoke(it, mutableListOfSupplies)
                mainBehavior.setDynamicSupplies(mutableListOfSupplies)
            }
        }

        return mainBehavior
    }
}

