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
    private var effects: Deque<SideEffect>
    private var actions: Deque<Action>
    private var untrackedBehaviors: MutableList<Behavior>
    private var modifiedDemandBehaviors: MutableList<Behavior>
    private var modifiedSupplyBehaviors: MutableList<Behavior>
    private var updatedTransients: MutableList<Transient>
    private var needsOrdering: MutableList<Behavior>

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

    //*****
    fun actionAsync(impulse: String?, block: () -> Unit) {
        val action = Action(impulse, block)
        this.actions.addLast(action)
        if (this.currentEvent == null) {
            this.eventLoop()
        }
    }

    fun action(impulse: String?, block: () -> Unit) {
        val action = Action(impulse, block)
        this.actions.addLast(action)
        this.eventLoop()
    }

    fun eventLoop() {
        while (true) {
            try {
                if (this.activatedBehaviors.size > 0 ||
                    this.untrackedBehaviors.size > 0 ||
                    this.modifiedDemandBehaviors.size > 0 ||
                    this.modifiedSupplyBehaviors.size > 0 ||
                    this.needsOrdering.size > 0
                ) {
                    val sequence = this.currentEvent!!.sequence
                    this.addUntrackedBehaviors(sequence)
                    this.addUntrackedSupplies(sequence)
                    this.addUntrackedDemands(sequence)
                    this.orderBehaviors()
                    this.runNextBehavior(sequence)

                    continue
                }

                if (effects.isNotEmpty()) {
                    val effect = this.effects.removeFirst()
                    effect.block(effect.extent)
                    continue
                }

                currentEvent?.let { aCurrentEvent ->
                    this.clearTransients()
                    this.lastEvent = aCurrentEvent
                    this.currentEvent = null
                    this.currentBehavior = null
                }

                if (actions.isNotEmpty()) {
                    val action = actions.removeFirst()
                    val newEvent = Event(
                        this.lastEvent.sequence + 1,
                        platformSupport.getCurrentTimeMillis(),
                        action.impulse
                    )
                    this.currentEvent = newEvent
                    action.block()
                    continue
                }
            } catch (e: Exception) {
                //put graph into clean state and rethrow exception
                this.currentEvent = null
                this.actions.clear()
                this.effects.clear()
                this.currentBehavior = null
                this.activatedBehaviors.clear()
                this.clearTransients()
                this.modifiedDemandBehaviors.clear()
                this.modifiedSupplyBehaviors.clear()
                this.untrackedBehaviors.clear()
                throw(e)
            }
            // no more tasks so we can exit the event loop
            break
        }
    }

    private fun clearTransients() {
        updatedTransients.forEach {
            it.clear()
        }
        updatedTransients.clear()
    }

    internal fun trackTransient(resource: Transient) {
        this.updatedTransients.add(resource)
    }

    internal fun resourceTouched(resource: Resource) {
        this.currentEvent?.let { aCurrentEvent ->
            for (subsequent in resource.subsequents) {
                this.activateBehavior(subsequent, aCurrentEvent.sequence)
            }
        }
    }

    private fun activateBehavior(behavior: Behavior, sequence: Long) {
        if (behavior.enqueuedWhen == null || behavior.enqueuedWhen!! < sequence) {
            behavior.enqueuedWhen = sequence
            //note addLast() versus the javascript push(), which here would add first and in javascript appends
            this.activatedBehaviors.add(behavior)
        }
    }

    private fun runNextBehavior(sequence: Long) {
        if (this.activatedBehaviors.isEmpty()) {
            return
        }
        val behavior = this.activatedBehaviors.remove()
        if (behavior.removedWhen != sequence) {
            this.currentBehavior = behavior
            behavior.block(behavior.extent!!)
            this.currentBehavior = null
        }
    }

    internal fun sideEffect(extent: Extent<*>, name: String?, block: (extent: Extent<*>) -> Unit) {
        if (this.currentEvent == null) {
            throw BehaviorGraphException("Effects can only be added during an event loop.")
        } else {
            this.effects.addLast(SideEffect(name, block, extent))
        }
    }

    private fun addUntrackedBehaviors(sequence: Long) {
        for (behavior in this.untrackedBehaviors) {
            this.activateBehavior(behavior, sequence)
            this.modifiedDemandBehaviors.add(behavior)
            this.modifiedSupplyBehaviors.add(behavior)
        }
        this.untrackedBehaviors.clear()
    }

    //Note: parameter sequence not used. We'll keep because typescript uses it.
    private fun addUntrackedSupplies(sequence: Long) {
        modifiedSupplyBehaviors.forEach { behavior ->
            behavior.untrackedSupplies?.let { behaviorUntrackedSupplies ->
                behavior.supplies?.forEach { existingSupply ->
                    existingSupply.suppliedBy = null
                }
                behavior.supplies = HashSet(behaviorUntrackedSupplies)
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
                behavior.untrackedSupplies = null
                // technically this doesn't need reordering but its subsequents will
                // setting this to reorder will also adjust its subsequents if necessary
                // in the sortDFS code
                this.needsOrdering.add(behavior)
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
