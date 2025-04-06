//
// Copyright Yahoo 2021
//
package behaviorgraph

import behaviorgraph.Event.Companion.InitialEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlin.math.max


/**
 * The core construct that represents the graph of behavior and resource nodes.
 * As many graphs can exist in the same program as you like; however nodes in one graph cannot directly link to nodes in another graph.
 * @param dateProvider Let's us offer an alternate source of timestamps for an [Event] typically used in testing.
 */

class Graph @JvmOverloads constructor(
    private val dateProvider: DateProvider? = null,
) {
    /**
     * The current event if one is currently running.
     */
    var currentEvent: Event? = null
        private set

    /**
     * The last completed event (ie all behaviors and side effects have run)
     */
    var lastEvent: Event
        private set
    private var activatedBehaviors: BehaviorQueue = BehaviorQueue()

    /**
     * The current running behavior if one is running.
     */
    var currentBehavior: Behavior<*>? = null
        private set
    private var untrackedBehaviors: MutableList<Behavior<*>> = mutableListOf()
    private var modifiedDemandBehaviors: MutableList<Behavior<*>> = mutableListOf()
    private var modifiedSupplyBehaviors: MutableList<Behavior<*>> = mutableListOf()
    private var updatedTransients: MutableList<Transient> = mutableListOf()
    private var needsOrdering: MutableList<Behavior<*>> = mutableListOf()
    internal var eventLoopState: EventLoopState? = null
    internal var extentsAdded: MutableList<Extent<*>> = mutableListOf()
    internal var extentsRemoved: MutableList<Extent<*>> = mutableListOf()
    internal val processingChangesOnCurrentThread: Boolean get() = eventLoopState?.runningOnCurrentThread == true && eventLoopState?.phase?.processingChanges == true
    var defaultSideEffectDispatcher: CoroutineDispatcher? = Dispatchers.Main
    private var processingMutex: Mutex = Mutex(false)
    private var actionQueueMutex: Mutex = Mutex(false)
    private var actionQueue: MutableList<RunnableAction> = mutableListOf()
    private var effectQueue: MutableList<RunnableSideEffect> = mutableListOf()


    /**
     * Validating dependencies between nodes in the graph can take some additional time.
     * This should be set to true to help you prevent errors while developing.
     * Set this to false if you wish to squeeze additional performance out of your production application.
     */
    var validateDependencies: Boolean = true

    /**
     * Validating behaviors are linked via resources that are still around can take some additional time.
     * This should be set to true to help you prevent errors while developing.
     * Set this to false if you wish to squeeze additional performance out of your production application.
     */
    var validateLifetimes: Boolean = true

    /**
     * System uses reflection to automatically name resources as extents are added to the graph.
     * This makes debugging significantly easier; however it does come with some small cost.
     * You may disable this here.
     */
    var automaticResourceNaming: Boolean = true

    /**
     * The current action may update one or more resources. Inspecting this list lets us
     * identify which action initiated the current event.
     */
    val actionUpdates: List<Resource>? get() = eventLoopState?.actionUpdates

    /**
     * The action belonging to the current event if one is running.
     */
    val currentAction: Action? get() = eventLoopState?.action

    /**
     * The current side effect if one is running.
     */
    val currentSideEffect: SideEffect? get() = eventLoopState?.currentSideEffect

    init {
        lastEvent = InitialEvent
    }

    /**
     * Creates a new action. It will always run the passed in function and subsequent graph event
     * before continuing to the next line.
     *
     * Example:
     * ```kotlin
     * graph.actionAsync { resource1.update() }
     * afterFunction()
     * ```
     *
     * In the above example `resource1.update()` and associated event will always run before `afterFunction()`
     * is called.
     *
     * @param debugName lets us add additional context to an action for debugging
     */
    @JvmOverloads
    fun action(
        debugName: String? = null,
        thunk: Thunk
    ): Job {
        val graphAction = GraphAction(thunk, debugName)
        return this.actionInternal(graphAction)
    }

    internal fun actionInternal(action: RunnableAction): Job {
        var actionThrowable: Throwable? = null
        action.job.invokeOnCompletion { cause: Throwable? ->
            actionThrowable = cause
        }
        eventLoopState?.let {
            val wrongAction = it.thread == Thread.currentThread() &&
                    (it.phase == EventLoopPhase.Action || it.phase == EventLoopPhase.Updates)
            assert(
                !wrongAction,
                { "Action cannot be created directly inside another action or behavior. Consider wrapping it in a side effect block." })
        }
        if (processingMutex.tryLock()) {
            try {
                CoroutineScope(Dispatchers.Unconfined).launch {
                    var nextAction: RunnableAction? = action
                    while(true) {
                        if (nextAction == null) {
                            break
                        }
                        nextAction?.let {
                            internalRunAction(it)
                        }
                        actionQueueMutex.lock()
                        nextAction = actionQueue.removeFirstOrNull()
                        actionQueueMutex.unlock()
                    }
                }
                actionThrowable?.let { throw it }
            } finally {
                processingMutex.unlock()
            }
        } else {
            runBlocking {
                try {
                    actionQueueMutex.lock()
                    actionQueue.add(action)
                } finally {
                    actionQueueMutex.unlock()
                }
            }
        }
        return action.job
    }

    private suspend fun internalRunAction(action: RunnableAction) {
        try {
            val newEvent = Event(
                this.lastEvent.sequence + 1, dateProvider?.now() ?: 0
            )
            this.currentEvent = newEvent
            eventLoopState = EventLoopState(action)
            eventLoopState?.phase = EventLoopPhase.Action
            action.runAction()
            while (true) {
                if (activatedBehaviors.size > 0 ||
                    untrackedBehaviors.size > 0 ||
                    modifiedDemandBehaviors.size > 0 ||
                    modifiedSupplyBehaviors.size > 0 ||
                    needsOrdering.size > 0
                ) {
                    eventLoopState?.phase = EventLoopPhase.Updates
                    val sequence: Long = this.currentEvent?.sequence ?: 0
                    addUntrackedBehaviors()
                    addUntrackedSupplies()
                    addUntrackedDemands(sequence)
                    orderBehaviors()
                    runNextBehavior(sequence)

                    continue
                }

                if (validateLifetimes) {
                    if (extentsAdded.size > 0) {
                        validateAddedExtents()
                    }
                    if (extentsRemoved.size > 0) {
                        validateRemovedExtents()
                    }
                }
                extentsAdded.clear()
                for (removed in extentsRemoved) {
                    removed.lifetime?.clearExtentRelationship(removed)
                }
                extentsRemoved.clear()

                if (effectQueue.isNotEmpty()) {
                    val effect = this.effectQueue.removeAt(0)
                    eventLoopState?.phase = EventLoopPhase.SideEffects
                    eventLoopState?.currentSideEffect = effect
                    withContext(effect.dispatcher ?: defaultSideEffectDispatcher ?: Dispatchers.Unconfined) {
                        effect.run()
                    }
                    if (eventLoopState != null) {
                        // side effect could create a synchronous action which would create a nested event loop
                        // which would clear out any existing event loop states
                        eventLoopState?.currentSideEffect = null
                    }
                    continue
                }

                currentEvent?.let { aCurrentEvent ->
                    val eventAction = eventLoopState?.action
                    clearTransients()
                    lastEvent = aCurrentEvent
                    currentEvent = null
                    eventLoopState = null
                    currentBehavior = null
                    eventAction?.job?.complete()
                }
                break
            }
        } catch (e: Throwable) {
            //put graph into clean state and rethrow exception
            currentEvent = null
            eventLoopState?.action?.job?.completeExceptionally(e)
            eventLoopState = null
            actionQueue.clear()
            effectQueue.clear()
            currentBehavior = null
            activatedBehaviors.clear()
            clearTransients()
            modifiedDemandBehaviors.clear()
            modifiedSupplyBehaviors.clear()
            untrackedBehaviors.clear()
            extentsAdded.clear()
            extentsRemoved.clear()
        }
    }

    private fun validateAddedExtents() {
        // ensure extents with same lifetime also got added
        val needAdding: MutableSet<Extent<*>> = mutableSetOf()
        for (added in extentsAdded) {
            if (added.lifetime != null) {
                for (ext in added.lifetime?.getAllContainingExtents() ?: listOf()) {
                    if (ext.addedToGraphWhen == null) {
                        needAdding.add(ext)
                    }
                }
            }
        }
        assert(
            needAdding.size == 0,
            { "All extents with unified or parent lifetimes must be added during the same event. Extents=$needAdding" })
    }

    private fun validateRemovedExtents() {
        // validate extents with contained lifetimes are also removed
        val needRemoving: MutableSet<Extent<*>> = mutableSetOf()
        for (removed in extentsRemoved) {
            if (removed.lifetime != null) {
                for (ext in removed.lifetime?.getAllContainedExtents() ?: listOf()) {
                    if (ext.addedToGraphWhen != null) {
                        needRemoving.add(ext)
                    }
                }
            }
        }
        assert(
            needRemoving.size == 0,
            { "All extents with unified or child lifetimes must be removed during the same event. Extents=$needRemoving" })

        // validate removed resources are not still linked to remaining behaviors
        for (removed in extentsRemoved) {
            for (resource in removed.resources) {
                for (demandedBy in resource.subsequents) {
                    if (demandedBy.extent.addedToGraphWhen != null) {
                        assert(false) {
                            "Remaining behaviors should remove dynamicDemands to removed resources. \nRemaining Behavior=$demandedBy \nRemoved Resource=$resource"
                        }
                    }
                }
                if (resource.suppliedBy != null && resource.suppliedBy?.extent?.addedToGraphWhen != null) {
                    assert(false) {
                        "Remaining behaviors should remove dynamicSupplies to removed resources. \nRemaining Behavior=${resource.suppliedBy} \nRemoved resource=$resource"
                    }
                }
            }
        }
    }

    private fun clearTransients() {
        updatedTransients.forEach {
            it.clear()
        }
        updatedTransients.clear()
    }

    internal fun trackTransient(resource: Transient) {
        updatedTransients.add(resource)
    }

    internal fun resourceTouched(resource: Resource) {
        this.currentEvent?.let { aCurrentEvent ->
            if (eventLoopState != null && eventLoopState?.phase == EventLoopPhase.Action) {
                eventLoopState?.actionUpdates?.add(resource)
            }
            for (subsequent in resource.subsequents) {
                val isOrderingDemand =
                    subsequent.orderingDemands != null && subsequent.orderingDemands?.contains(resource) ?: false
                if (!isOrderingDemand) {
                    activateBehavior(subsequent, aCurrentEvent.sequence)
                }
            }
        }
    }

    private fun activateBehavior(behavior: Behavior<*>, sequence: Long) {
        if (behavior.enqueuedWhen == null || (behavior.enqueuedWhen ?: 0) < sequence) {
            behavior.enqueuedWhen = sequence
            activatedBehaviors.add(behavior)
        }
    }

    private fun runNextBehavior(sequence: Long) {
        val behavior: Behavior<Any>? = activatedBehaviors.pop() as Behavior<Any>?
        behavior?.let {
            if (it.removedWhen != sequence) {
                currentBehavior = it
                behavior.thunk.invoke(it.extent.context ?: it.extent)
                currentBehavior = null
            }
        }
    }

    /**
     * Creates a [SideEffect] and adds it to the queue.
     * All side effects in the queue will be run in order at the end of the current event.
     */
    @JvmOverloads
    fun sideEffect(debugName: String? = null, block: () -> Unit) {
        sideEffectHelper(GraphSideEffect(block, currentBehavior, debugName))
    }

    internal fun sideEffectHelper(sideEffect: RunnableSideEffect) {
        if (this.currentEvent == null) {
            assert(false) { "Effects can only be added during an event loop." }
        } else if (eventLoopState?.runningOnCurrentThread != true) {
            assert(false) {
                "You've created a side effect from an alternate thread while another is running. Side effects should be created inside behaviors only."
            }
        } else if (eventLoopState?.phase == EventLoopPhase.SideEffects) {
            assert(false) {
                "You've created a side effect inside another side effect. Side effects should be created inside behaviors. Is this a mistake?"
            }
        } else {
            this.effectQueue.add(sideEffect)
        }

    }

    private fun addUntrackedBehaviors() {
        for (behavior in untrackedBehaviors) {
            modifiedDemandBehaviors.add(behavior)
            modifiedSupplyBehaviors.add(behavior)
        }
        untrackedBehaviors.clear()
    }

    private fun addUntrackedSupplies() {
        modifiedSupplyBehaviors.forEach { behavior ->
            if (validateLifetimes) {
                behavior.untrackedSupplies?.forEach { existingSupply ->
                    if (!behavior.extent.hasCompatibleLifetime(existingSupply.extent)) {
                        assert(false) {
                            "Behavior can only supply resources on extents with the unified or parent lifetimes. \nSupplying Behavior=$behavior \nStatic Supply=$existingSupply"
                        }
                    }
                }
            }

            val allUntrackedSupplies: MutableSet<Resource> = mutableSetOf()
            behavior.untrackedSupplies?.let {
                allUntrackedSupplies.addAll(it)
            }
            behavior.untrackedDynamicSupplies?.let {
                allUntrackedSupplies.addAll(it)
            }
            behavior.supplies?.forEach { it.suppliedBy = null }

            behavior.supplies = allUntrackedSupplies
            behavior.supplies?.forEach { newSupply: Resource ->
                if (newSupply.suppliedBy != null && newSupply.suppliedBy !== behavior) {
                    assert(false) {
                        "Resource cannot be supplied by more than one behavior. Supplied Resource=$newSupply \nSupplying Behavior=$newSupply.suppliedBy \nAdditional Behavior=$behavior"
                    }
                    // with asserts off, will just switch to new supplying behavior
                }
                newSupply.suppliedBy = behavior
            }

            // technically this doesn't need reordering but its subsequents will
            // set this to reorder will also adjust its subsequents if necessary
            // in the sortDFS code
            if (behavior.orderingState != OrderingState.NeedsOrdering) {
                behavior.orderingState = OrderingState.NeedsOrdering
                this.needsOrdering.add(behavior)
            }
        }
        this.modifiedSupplyBehaviors.clear()
    }

    private fun addUntrackedDemands(sequence: Long) {
        modifiedDemandBehaviors.forEach { behavior ->
            if (validateLifetimes) {
                behavior.untrackedDemands?.forEach { demand ->
                    if (!behavior.extent.hasCompatibleLifetime(demand.resource.extent)) {
                        assert(false) {
                            "Static demands can only be assigned across extents with a unified or parent lifetime. Demand=$demand \nDemanding Behavior=$behavior"
                        }
                        // disabled asserts will just allow incompatible lifetimes
                    }
                }
            }

            val allUntrackedDemands: MutableSet<Demandable> = mutableSetOf()
            behavior.untrackedDemands?.let {
                allUntrackedDemands.addAll(it)
            }
            behavior.untrackedDynamicDemands?.let {
                allUntrackedDemands.addAll(it)
            }

            var removedDemands: MutableList<Resource>? = null
            behavior.demands?.forEach { demand ->
                if (!allUntrackedDemands.contains(demand)) {
                    if (removedDemands == null) {
                        removedDemands = mutableListOf()
                    }
                    removedDemands?.add(demand)
                }
            }

            var addedDemands: MutableList<Resource>? = null
            for (link in allUntrackedDemands) {
                val untrackedDemand = link.resource
                if (untrackedDemand.extent.addedToGraphWhen == null) {
                    assert(false) {
                        "Cannot demand a resource that hasn't been added to the graph. Demanding behavior=$behavior \nDemand=$untrackedDemand"
                    }
                }
                if (behavior.demands == null || !(behavior.demands?.contains(untrackedDemand) ?: false)) {
                    if (addedDemands == null) {
                        addedDemands = mutableListOf()
                    }
                    addedDemands.add(untrackedDemand)
                }
            }
            var needsRunning = false

            removedDemands?.forEach { it.subsequents.remove(behavior) }
            var orderBehavior = behavior.orderingState != OrderingState.Ordered

            addedDemands?.forEach { demand ->
                demand.subsequents.add(behavior)
                if (demand.internalJustUpdated) {
                    needsRunning = true
                }
                if (!orderBehavior) {
                    val prior = demand.suppliedBy
                    if (prior != null && prior.orderingState == OrderingState.Ordered && prior.order >= behavior.order) {
                        orderBehavior = true
                    }
                }
            }

            var newDemands: MutableSet<Resource>? = null
            var orderingDemands: MutableSet<Resource>? = null
            allUntrackedDemands.forEach { link ->
                if (newDemands == null) {
                    newDemands = mutableSetOf()
                }
                newDemands?.add(link.resource)
                if (link.type == LinkType.Order) {
                    if (orderingDemands == null) {
                        orderingDemands = mutableSetOf()
                    }
                    orderingDemands?.add(link.resource)
                }
            }
            behavior.demands = newDemands
            behavior.orderingDemands = orderingDemands

            if (orderBehavior) {
                if (behavior.orderingState != OrderingState.NeedsOrdering) {
                    behavior.orderingState = OrderingState.NeedsOrdering
                    needsOrdering.add(behavior)
                }
            }
            if (needsRunning) {
                this.activateBehavior(behavior, sequence)
            }
        }
        this.modifiedDemandBehaviors.clear()
    }

    /**
     * find all behaviors that need ordering and their
    // subsequents and mark them all as needing ordering
     */
    private fun orderBehaviors() {
        if (needsOrdering.isEmpty()) {
            return
        }
        val localNeedsOrdering = mutableListOf<Behavior<*>>()

        var x = 0
        while (x < needsOrdering.size) {
            var behavior = needsOrdering[x]

            if (behavior.orderingState == OrderingState.NeedsOrdering) {
                behavior.orderingState = OrderingState.Clearing
                localNeedsOrdering.add(behavior)
                behavior.supplies?.forEach { supply ->
                    supply.subsequents.forEach { subsequent ->
                        if (subsequent.orderingState == OrderingState.Ordered) {
                            subsequent.orderingState = OrderingState.NeedsOrdering
                            needsOrdering.add(subsequent)
                        }
                    }
                }
            }
            x++
        }
        needsOrdering.clear()

        val needsReheap = mutableListOf(false) // this allows out parameter
        for (behavior in localNeedsOrdering) {
            sortDFS(behavior, needsReheap)
        }

        if (needsReheap[0]) {
            // if orders have changed, we wll need to make sure any existing activated
            // behaviors are properly sorted
            activatedBehaviors.reheap()
        }
    }

    private fun sortDFS(behavior: Behavior<*>, needsReheap: MutableList<Boolean>) {
        if (behavior.orderingState == OrderingState.Ordering) {
            assert(false) {
                val cycleString = debugCycleForBehavior(behavior)
                "Behavior dependency cycle detected. Behavior=$behavior \nCycle=\n$cycleString"
            }
            // give up on this path if we fail the assertion. Essentially ordering is broken.
            behavior.orderingState = OrderingState.Ordered
            return
        }

        if (behavior.orderingState == OrderingState.Clearing) {
            behavior.orderingState = OrderingState.Ordering
            var order = 0L
            behavior.demands?.forEach { demand ->
                demand.suppliedBy?.let { prior ->
                    if (prior.orderingState != OrderingState.Ordered) {
                        sortDFS(prior, needsReheap)
                    }
                    order = max(order, prior.order + 1)
                }
            }

            behavior.orderingState = OrderingState.Ordered

            if (order != behavior.order) {
                behavior.order = order
                // TODO: a small optimization may be to skip
                // reheap if it's not activated. So if we
                // have a bunch of elements in behavior queue but only
                // one non activated behavior changes, there's
                // no need to resort those
                needsReheap[0] = true
            }
        }
    }

    fun debugCycleForBehavior(behavior: Behavior<*>): List<Resource> {
        val stack = mutableListOf<Resource>() //we'll "push" and "pop" from the end
        if (cycleDFS(behavior, behavior, stack)) {
            var output = mutableListOf<Resource>()
            while (stack.isNotEmpty()) {
                var rez = stack.removeAt(stack.size - 1)
                output.add(rez)
            }
            return output
        } else {
            return mutableListOf()
        }
    }

    private fun cycleDFS(
        currentBehavior: Behavior<*>,
        target: Behavior<*>,
        stack: MutableList<Resource>
    ): Boolean {
        currentBehavior.demands?.forEach { aResource ->
            stack.add(aResource)
            var b = aResource.suppliedBy
            if (b != null) {
                if (b == target) {
                    return true
                }
                if (cycleDFS(b, target, stack)) {
                    return true
                }
                stack.removeAt(stack.size - 1)
            }
        }

        return false
    }

    private fun addBehavior(behavior: Behavior<*>) {
        this.untrackedBehaviors.add(behavior)
    }

    internal fun updateDemands(behavior: Behavior<*>, newDemands: List<Demandable>?) {
        if (behavior.extent.addedToGraphWhen == null) {
            assert(false) {
                "Behavior must belong to graph before updating demands. \nDemanding Behavior=$behavior"
            }
            return
        } else if (!processingChangesOnCurrentThread) {
            assert(false) {
                "Demands can only be updated during an event loop. \nDemanding Behavior=$behavior"
            }
            return
        }
        behavior.untrackedDynamicDemands = newDemands
        modifiedDemandBehaviors.add(behavior)
    }

    internal fun updateSupplies(behavior: Behavior<*>, newSupplies: List<Resource>?) {
        if (behavior.extent.addedToGraphWhen == null) {
            assert(false) {
                "Behavior must belong to graph before updating supplies. \nDemanding Behavior=$behavior"
            }
        } else if (!processingChangesOnCurrentThread) {
            assert(false) {
                "Supplies can only be updated during an event loop. \nDemanding Behavior=$behavior"
            }
        }
        behavior.untrackedDynamicSupplies = newSupplies
        modifiedSupplyBehaviors.add(behavior)
    }

    private fun removeBehavior(behavior: Behavior<*>, sequence: Long) {
        // If we demand a foreign resource then we should be
        // removed from its list of subsequents
        var removed = false
        behavior.demands?.forEach { demand ->
            if (demand.extent != behavior.extent) {
                demand.subsequents.remove(behavior)
                removed = true
            }
        }
        // and remove foreign demands
        // its faster to erase the whole list than pick out the foreign ones
        if (removed) {
            behavior.demands = null
        }

        // any foreign resources should no longer be supplied by this behavior
        removed = false
        behavior.supplies?.forEach { supply ->
            if (supply.extent != behavior.extent) {
                supply.suppliedBy = null
                removed = true
            }
        }
        // and clear out those foreign supplies
        // its faster to clear whole list than pick out individual foreign ones
        if (removed) {
            behavior.supplies = null
        }

        behavior.removedWhen = sequence
    }

    internal fun addExtent(extent: Extent<*>) {
        if (extent.addedToGraphWhen != null) {
            assert(false) {
                "Extent $extent has already been added to the graph: ${extent.graph}"
            }
            return
        }
        if (!processingChangesOnCurrentThread) {
            assert(false) {
                "Extents can only be added inside an action or behavior running on the current thread. \nExtent=$extent"
            }
        }

        if (validateLifetimes) {
            if (extent.lifetime != null) {
                if (extent.lifetime?.addedToGraphWhen == null) {
                    extent.lifetime?.addedToGraphWhen = currentEvent?.sequence
                }
            }
            val refParent = extent.lifetime?.parent
            if (refParent != null && refParent.addedToGraphWhen == null) {
                assert(false) {
                    "Extent with child lifetime must be added after parent."
                }
                // disabled asserts will mean we can still add it (just no lifetime tracking)
            }
        }

        extent.addedToGraphWhen = currentEvent?.sequence
        extentsAdded.add(extent)
        activateBehavior(extent.didAddBehavior, currentEvent?.sequence ?: 0)
        for (behavior in extent.behaviors) {
            addBehavior(behavior)
        }
    }

    internal fun removeExtent(extent: Extent<*>) {
        if (!processingChangesOnCurrentThread) {
            assert(false) {
                "Extents can only be removed during an event. \nExtent=$extent"
            }
            return
        }
        extentsRemoved.add(extent)
        for (behavior in extent.behaviors) {
            removeBehavior(behavior, currentEvent?.sequence ?: 0)
        }
        extent.addedToGraphWhen = null
    }

    override fun toString(): String {
        return buildString {
            if (currentEvent != null) {
                append(String.format("Current Event: %d\n", currentEvent?.sequence ?: 0))
            } else {
                append("No current event")
            }
            eventLoopState?.let {
                append(it.toString())
                append("\n")
            }
            currentBehavior?.let {
                append(it.toString())
            }
        }
    }
}
