//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import com.yahoo.behaviorgraph.Event.Companion.InitialEvent
import com.yahoo.behaviorgraph.exception.AllDemandsMustBeAddedToTheGraphExceptions
import com.yahoo.behaviorgraph.exception.BehaviorDependencyCycleDetectedException
import com.yahoo.behaviorgraph.exception.BehaviorGraphException
import com.yahoo.behaviorgraph.exception.ExtentsCanOnlyBeAddedDuringAnEventException
import com.yahoo.behaviorgraph.exception.ExtentsCanOnlyBeRemovedDuringAnEventException
import com.yahoo.behaviorgraph.exception.ResourceCannotBeSuppliedByMoreThanOneBehaviorException
import com.yahoo.behaviorgraph.platform.PlatformSupport
import java.util.ArrayDeque
import java.util.ArrayList
import java.util.Deque
import java.util.HashSet
import java.util.PriorityQueue
import kotlin.math.max

class Graph constructor(private var platformSupport: PlatformSupport = PlatformSupport.platformSupport) {
    internal var currentEvent: Event? = null
    var lastEvent: Event
    private var activatedBehaviors: PriorityQueue<Behavior>
    internal var currentBehavior: Behavior? = null
    private var effects: ArrayDeque<RunnableSideEffect>
    private var actions: ArrayDeque<RunnableAction>
    private var untrackedBehaviors: MutableList<Behavior>
    private var modifiedDemandBehaviors: MutableList<Behavior>
    private var modifiedSupplyBehaviors: MutableList<Behavior>
    private var updatedTransients: MutableList<Transient>
    private var needsOrdering: MutableList<Behavior>
    private var eventLoopState: EventLoopState? = null
    private var extentsAdded: MutableList<Extent<*>> = mutableListOf()
    private var extentsRemoved: MutableList<Extent<*>> = mutableListOf()
    var validateDependencies: Boolean = true
    var validateLifetimes: Boolean = true

    init {
        effects = ArrayDeque()
        actions = ArrayDeque()
        activatedBehaviors = PriorityQueue()
        untrackedBehaviors = ArrayList()
        modifiedDemandBehaviors = ArrayList()
        modifiedSupplyBehaviors = ArrayList()
        updatedTransients = ArrayList()
        needsOrdering = ArrayList()
        lastEvent = InitialEvent
    }

    fun actionAsync(block: () -> Unit, debugName: String?) {
        val action = Action(block, debugName)
        asyncActionHelper(action)
    }

    fun action(block: () -> Unit, debugName: String? = null) {
        val action = Action(block, debugName)
        actionHelper(action)
    }

    internal fun actionHelper(action: RunnableAction) {
        if (eventLoopState != null && (eventLoopState!!.phase == EventLoopPhase.action || eventLoopState!!.phase == EventLoopPhase.updates)) {
            throw BehaviorGraphException("Action cannot be created directly inside another action or behavior. Consider wrapping it in a side effect block.")
        }
        actions.addLast(action)
        eventLoop()
    }

    internal fun asyncActionHelper(action: RunnableAction) {
        if (eventLoopState != null && (eventLoopState!!.phase == EventLoopPhase.action || eventLoopState!!.phase == EventLoopPhase.updates)) {
            throw BehaviorGraphException("Action cannot be created directly inside another action or behavior. Consider wrapping it in a side effect block.")
        }
        actions.addLast(action)
        if (currentEvent == null) {
            eventLoop()
        }
    }

    fun eventLoop() {
        while (true) {
            try {
                if (activatedBehaviors.size > 0 ||
                    untrackedBehaviors.size > 0 ||
                    modifiedDemandBehaviors.size > 0 ||
                    modifiedSupplyBehaviors.size > 0 ||
                    needsOrdering.size > 0
                ) {
                    eventLoopState!!.phase = EventLoopPhase.updates
                    val sequence = this.currentEvent!!.sequence
                    addUntrackedBehaviors(sequence)
                    addUntrackedSupplies(sequence)
                    addUntrackedDemands(sequence)
                    orderBehaviors()
                    runNextBehavior(sequence)

                    continue
                }

                if (validateLifetimes) {
                    if (extentsAdded.size > 0) {
                        validateAddedExtents()
                        extentsAdded.clear()
                    }
                    if (extentsRemoved.size > 0) {
                        validateRemovedExtents()
                        extentsRemoved.clear()
                    }
                }
                if (effects.isNotEmpty()) {
                    val effect = this.effects.removeFirst()
                    eventLoopState!!.phase = EventLoopPhase.sideEffects
                    eventLoopState!!.currentSideEffect = effect
                    effect.runSideEffect()
                    if (eventLoopState != null) {
                        // side effect could create a synchronous action which would create a nested event loop
                        // which would clear out any existing event loop states
                        eventLoopState?.currentSideEffect = null;
                    }
                    continue
                }

                currentEvent?.let { aCurrentEvent ->
                    clearTransients()
                    lastEvent = aCurrentEvent
                    currentEvent = null
                    eventLoopState = null
                    currentBehavior = null
                }

                if (actions.isNotEmpty()) {
                    val action = actions.removeFirst()
                    val newEvent = Event(
                        this.lastEvent.sequence + 1,
                        platformSupport.getCurrentTimeMillis()
                    )
                    this.currentEvent = newEvent
                    eventLoopState = EventLoopState(action)
                    eventLoopState!!.phase = EventLoopPhase.action
                    action.runAction()
                    continue
                }
            } catch (e: Exception) {
                //put graph into clean state and rethrow exception
                currentEvent = null
                eventLoopState = null
                actions.clear()
                effects.clear()
                currentBehavior = null
                activatedBehaviors.clear()
                clearTransients()
                modifiedDemandBehaviors.clear()
                modifiedSupplyBehaviors.clear()
                untrackedBehaviors.clear()
                extentsAdded.clear()
                extentsRemoved.clear()
                throw(e)
            }
            // no more tasks so we can exit the event loop
            break
        }
    }

    private fun validateAddedExtents() {
        // ensure extents with same lifetime also got added
        val needAdding: MutableSet<Extent<*>> = mutableSetOf()
        for (added in extentsAdded) {
            if (added.lifetime != null) {
                for (ext in added.lifetime!!.getAllContainingExtents()) {
                    if (ext.addedToGraphWhen == null) {
                        needAdding.add(ext)
                    }
                }
            }
        }
        if (needAdding.size > 0) {
            throw BehaviorGraphException("All extents with unified or parent lifetimes must be added during the same event. Extents=$needAdding")
        }
    }

    private fun validateRemovedExtents() {
        // validate extents with contained lifetimes are also removed
        val needRemoving: MutableSet<Extent<*>> = mutableSetOf()
        for (removed in extentsRemoved) {
            if (removed.lifetime != null) {
                for (ext in removed.lifetime!!.getAllContainedExtents()) {
                    if (ext.addedToGraphWhen != null) {
                        needRemoving.add(ext)
                    }
                }
            }
        }
        if (needRemoving.size > 0) {
            throw BehaviorGraphException("All extents with unified or child lifetimes must be removed during the same event. Extents=$needRemoving")
        }

        // validate removed resources are not still linked to remaining behaviors
        for (removed in extentsRemoved) {
            for (resource in removed.resources) {
                for (demandedBy in resource.subsequents) {
                    if (demandedBy.extent.addedToGraphWhen != null) {
                        throw BehaviorGraphException("Remaining behaviors must remove dynamicDemands to removed resources. Behavior=$demandedBy Resource=$resource")
                    }
                }
                if (resource.suppliedBy != null && resource.suppliedBy!!.extent.addedToGraphWhen != null) {
                    throw BehaviorGraphException("Remaining behaviors must remove dynamicSupplies to removed resources. Remaining Behavior=${resource.suppliedBy} Removed resource=$resource")
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
            if (eventLoopState != null && eventLoopState!!.phase == EventLoopPhase.action) {
                eventLoopState!!.actionUpdates.add(resource)
            }
            for (subsequent in resource.subsequents) {
                val isOrderingDemand = subsequent.orderingDemands != null && subsequent.orderingDemands!!.contains(resource)
                if (!isOrderingDemand) {
                    activateBehavior(subsequent, aCurrentEvent.sequence)
                }
            }
        }
    }

    private fun activateBehavior(behavior: Behavior, sequence: Long) {
        if (behavior.enqueuedWhen == null || behavior.enqueuedWhen!! < sequence) {
            behavior.enqueuedWhen = sequence
            activatedBehaviors.add(behavior)
        }
    }

    private fun runNextBehavior(sequence: Long) {
        if (activatedBehaviors.isEmpty()) {
            return
        }
        val behavior = activatedBehaviors.remove()
        if (behavior.removedWhen != sequence) {
            currentBehavior = behavior
            behavior.block(behavior.extent)
            currentBehavior = null
        }
    }

    fun sideEffect(block: () -> Unit, debugName: String? = null) {
        sideEffectHelper(SideEffect(block, currentBehavior, debugName))
    }

    internal fun sideEffectHelper(sideEffect: RunnableSideEffect) {
        if (this.currentEvent == null) {
            throw BehaviorGraphException("Effects can only be added during an event loop.")
        } else if (eventLoopState!!.phase == EventLoopPhase.sideEffects) {
            throw BehaviorGraphException("Nested side effects are disallowed.")
        } else {
            this.effects.addLast(sideEffect)
        }

    }

    private fun addUntrackedBehaviors(sequence: Long) {
        for (behavior in untrackedBehaviors) {
            activateBehavior(behavior, sequence)
            modifiedDemandBehaviors.add(behavior)
            modifiedSupplyBehaviors.add(behavior)
        }
        untrackedBehaviors.clear()
    }

    private fun addUntrackedSupplies() {
        modifiedSupplyBehaviors.forEach { behavior ->
            behavior.untrackedSupplies?.let { behaviorUntrackedSupplies ->
                behavior.supplies?.forEach { existingSupply ->
                    if (validateLifetimes && !behavior.extent.hasCompatibleLifetime(existingSupply.extent)) {
                        throw BehaviorGraphException("Static supplies can only be with extents with the unified or parent lifetimes. Supply=$existingSupply")
                    }
                }

                val allUntrackedSupplies: MutableSet<Resource> = mutableSetOf()
                if (behavior.untrackedSupplies != null) {
                    allUntrackedSupplies.addAll(behavior.untrackedSupplies!!)
                }
                behavior.untrackedSupplies = null
                if (behavior.untrackedDynamicSupplies != null) {
                    allUntrackedSupplies.addAll(behavior.untrackedDynamicSupplies!!)
                }
                behavior.untrackedDynamicSupplies = null

                behavior.supplies?.forEach { it.suppliedBy = null }

                behavior.supplies = allUntrackedSupplies
                behavior.supplies?.forEach { newSupply: Resource ->
                    if (newSupply.suppliedBy != null && newSupply.suppliedBy !== behavior) {
                        throw ResourceCannotBeSuppliedByMoreThanOneBehaviorException(
                            "Resource cannot be supplied by more than one behavior",
                            newSupply,
                            behavior
                        )
                    }
                    newSupply.suppliedBy = behavior
                }

                // technically this doesn't need reordering but its subsequents will
                // setting this to reorder will also adjust its subsequents if necessary
                // in the sortDFS code
                if (behavior.orderingState != OrderingState.NeedsOrdering) {
                    behavior.orderingState = OrderingState.NeedsOrdering
                    this.needsOrdering.add(behavior)
                }
            }
        }
        this.modifiedSupplyBehaviors.clear()
    }

    private fun addUntrackedDemands(sequence: Long) {
        modifiedDemandBehaviors.forEach { behavior ->
            behavior.untrackedDemands?.let { untrackedDemands ->
                var removedDemands: MutableList<Resource>? = null
                behavior.demands?.forEach { demand ->
                    if (!untrackedDemands.contains(demand)) {
                        if (removedDemands == null) {
                            removedDemands = ArrayList()
                        }
                        removedDemands?.add(demand)
                    }
                }
                var addedDemands: MutableList<Resource>? = null

                for (untrackedDemand in untrackedDemands) {
                    if (!untrackedDemand.added) {
                        throw AllDemandsMustBeAddedToTheGraphExceptions(
                            "All demands must be added to the graph.",
                            behavior,
                            untrackedDemand
                        )
                    }
                    if (behavior.demands == null || !behavior.demands!!.contains(untrackedDemand)) {
                        if (addedDemands == null) {
                            addedDemands = ArrayList()
                        }
                        addedDemands.add(untrackedDemand)
                    }
                }
                var needsRunning = false

                removedDemands?.let { localRemovedDemands ->
                    needsRunning = true
                    for (demand in localRemovedDemands) {
                        demand.subsequents.remove(behavior)
                    }
                }
                var orderBehavior = behavior.orderingState == OrderingState.Unordered

                addedDemands?.let { localAddedDemands ->
                    needsRunning = true
                    for (demand in localAddedDemands) {
                        demand.subsequents.add(behavior)

                        if (!orderBehavior) {
                            val prior = demand.suppliedBy
                            if (prior != null && prior.orderingState == OrderingState.Ordered && prior.order >= behavior.order) {
                                orderBehavior = true
                            }
                        }
                    }
                }

                behavior.demands = HashSet(behavior.untrackedDemands)
                behavior.untrackedDemands = null

                if (orderBehavior) {
                    this.needsOrdering.add(behavior)
                }
                if (needsRunning) {
                    this.activateBehavior(behavior, sequence)
                }
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
        val localNeedsOrdering = ArrayList<Behavior>()
        val traversalQueue = ArrayDeque<Behavior>()
        // first get behaviors that need ordering and mark them as
        // ordered so they will be traversed when first encountered
        for (behavior in needsOrdering) {
            behavior.orderingState = OrderingState.Ordered
            traversalQueue.addLast(behavior)
        }

        needsOrdering.clear()

        while (traversalQueue.isNotEmpty()) {
            var behavior = traversalQueue.removeFirst()

            if (behavior.orderingState == OrderingState.Ordered) {
                behavior.orderingState = OrderingState.Unordered
                localNeedsOrdering.add(behavior)
                behavior.supplies?.forEach { aResource ->
                    aResource.subsequents.forEach { aSubsequentBehavior ->
                        traversalQueue.push(aSubsequentBehavior)
                    }
                }
            }
        }
        //TODO is there a kotlin idiom for the following?
        val needsReheap = mutableListOf(false) // this allows out parameter
        for (behavior in localNeedsOrdering) {
            this.sortDFS(behavior, needsReheap)
        }

        if (needsReheap.first()) {
            //we've invalidated our current activatedBehaviors by changing the priority of
            //some of the behaviors, so resort.
            val newActivatedBehaviors = PriorityQueue<Behavior>()
            activatedBehaviors.forEach {
                newActivatedBehaviors.add(it)
            }
            activatedBehaviors = newActivatedBehaviors
        }
    }

    private fun sortDFS(behavior: Behavior, needsReheap: MutableList<Boolean>) {
        if (behavior.orderingState == OrderingState.Ordering) {
            throw BehaviorDependencyCycleDetectedException(
                "Behavior dependency cycle detected.", behavior,
                cycleForBehavior(behavior)
            )
        }

        if (behavior.orderingState == OrderingState.Unordered) {
            behavior.orderingState = OrderingState.Ordering
            var order = 0L
            behavior.demands?.forEach { localResource ->
                localResource.suppliedBy?.let { localBehavior ->
                    if (localBehavior.orderingState != OrderingState.Ordered) {
                        this.sortDFS(localBehavior, needsReheap)
                    }
                    order = max(order, localBehavior.order + 1)
                }
            }

            behavior.orderingState = OrderingState.Ordered

            if (order != behavior.order) {
                behavior.order = order
                needsReheap[0] = true
            }
        }
    }

    private fun cycleForBehavior(behavior: Behavior): List<Resource> {
        var stack = ArrayList<Resource>() //we'll "push" and "pop" from the end
        if (cycleDFS(behavior, behavior, stack)) {
            var output = ArrayList<Resource>()
            while (stack.isNotEmpty()) {
                var rez = stack.removeAt(stack.size - 1)
                output.add(rez)
            }
            return output
        } else {
            return ArrayList()
        }
    }

    private fun cycleDFS(
        currentBehavior: Behavior,
        target: Behavior,
        stack: MutableList<Resource>
    ): Boolean {
        currentBehavior.demands?.forEach { aResource ->
            stack.add(aResource)
            var b = aResource.suppliedBy
            if (b != null) {
                if (b == target) {
                    return true
                }
                if (this.cycleDFS(b, target, stack)) {
                    return true
                }
                stack.removeAt(stack.size - 1)
            }
        }

        return false
    }

    private fun addBehavior(behavior: Behavior) {
        behavior.added = true
        this.untrackedBehaviors.add(behavior)
    }

    fun updateDemands(behavior: Behavior, newDemands: List<Demandable>?) {
        if (!behavior.added) {
            throw BehaviorGraphException("Behavior must belong to graph before updating demands: $behavior")
        } else if (this.currentEvent == null) {
            throw BehaviorGraphException("Demands can only be updated during an event loop. Behavior=$behavior")
        }
        behavior.untrackedDemands = newDemands
        this.modifiedDemandBehaviors.add(behavior)
    }

    fun updateSupplies(behavior: Behavior, newSupplies: List<Resource>?) {
        if (!behavior.added) {
            throw BehaviorGraphException("Behavior must belong to graph before updating supplies. Behavior=$behavior")
        }

        this.currentEvent
            ?: throw BehaviorGraphException("Supplies can only be updated during an event loop. Behavior=$behavior")

        behavior.untrackedSupplies = newSupplies
        this.modifiedSupplyBehaviors.add(behavior)
    }

    private fun removeBehavior(behavior: Behavior, sequence: Long) {
        // remove all behaviors supplies from subsequents demands
        behavior.supplies?.forEach { supply ->
            supply.subsequents.forEach { subsequent ->
                subsequent.demands?.remove(supply)
            }
            supply.subsequents.clear()
        }

        behavior.demands?.forEach { demand ->
            demand.subsequents.remove(behavior)
        }
        behavior.demands?.clear()


        behavior.removedWhen = sequence
        behavior.added = false
    }

    private fun addResource(resource: Resource) {
        resource.added = true
    }

    fun addExtent(extent: Extent<*>) {
        if (extent.addedToGraphWhen != null) {
            throw BehaviorGraphException("Extent $extent has already been added to the graph: ${extent.graph}")
        }

        this.currentEvent?.let { localCurrentEvent ->
            extent.addedToGraphWhen = localCurrentEvent
            extent.resources.forEach {
                this.addResource(it)
            }
            extent.behaviors.forEach {
                addBehavior(it)
            }
        } ?: run {
            throw ExtentsCanOnlyBeAddedDuringAnEventException(
                "Extents can only be added during an event.",
                extent
            )
        }
    }

    fun removeExtent(extent: Extent<*>) {
        this.currentEvent?.let { localCurrentEvent ->
            extent.resources.forEach { resource ->
                resource.added = false
            }
            extent.behaviors.forEach { behavior ->
                removeBehavior(behavior, localCurrentEvent.sequence)
            }

            extent.addedToGraphWhen = null
        } ?: run {
            throw ExtentsCanOnlyBeRemovedDuringAnEventException(
                "Extents can only be removed during an event loop.",
                extent
            )
        }
    }
}
