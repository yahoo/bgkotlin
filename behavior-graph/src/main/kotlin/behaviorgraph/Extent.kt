//
// Copyright Yahoo 2021
//
package behaviorgraph

import java.util.concurrent.Future

/**
 * An **Extent** is a collection of resources and behaviors. Extents allow us to
 * add (and remove) all those resources and behaviors to a graph at the same time.
 * You are guaranteed that all resources and behaviors in the same extent will be
 * part of the graph at the same time.
 *
 * You typically subclass (or delegate to) an Extent in order to define your
 * set of resources and behaviors for your program.
 * _Note that Extent has a type parameter._ This should be the type of the subclass.
 * This type parameterization provides a better Behavior Graph API.
 * This pattern is known as the [Curiously Recurring Template Pattern (CRTP)](https://en.wikipedia.org/wiki/Curiously_recurring_template_pattern)
 *
 * Example:
 * ```kotlin
 * class MyExtent(val graph: Graph): Extent<MyExtent>(graph) {
 *   val moment1: Moment = this.moment()
 *   val state1: State<Long> = this.state(0)
 *
 *   init {
 *     behavior()
 *       .supplies(state1)
 *       .demands(moment1)
 *       .runs {
 *         state1.update(state1.value + 1)
 *       }
 *   }
 * }
 *
 * val graph = Graph()
 * val mainExtent = MyExtent(graph)
 * mainExtent.addToGraphWithAction()
 * ```
 *
 */
open class Extent<ExtentContext: Any> @JvmOverloads constructor(val graph: Graph, var context: ExtentContext? = null) {
    var debugName: String = javaClass.simpleName
    internal var behaviors: MutableList<Behavior<ExtentContext>> = mutableListOf()
    internal var resources: MutableList<Resource> = mutableListOf()
    internal var addedToGraphWhen: Long? = null
    internal var didAddBehavior: Behavior<ExtentContext>
    internal var lifetime: ExtentLifetime? = null

    val didAdd: State<Boolean> = State(this, false)

    init {
        didAdd.debugName = "_didAdd_"
        didAddBehavior = behavior().supplies(didAdd).runs { this.didAdd.update(true) }
    }

    /**
     * Establish that the receiver will be around for the same period of time as the passed in extent.
     */
    fun unifyLifetime(extent: Extent<*>) {
        if (lifetime == null) {
            lifetime = ExtentLifetime(this)
        }
        lifetime!!.unify(extent)
    }

    /**
     * Establish that the receiver will be around for at least as long as the passed in child extent.
     */
    fun addChildLifetime(extent: Extent<*>) {
        if (this.lifetime == null) {
            lifetime = ExtentLifetime(this)
        }
        lifetime!!.addChild(extent)
    }

    internal fun hasCompatibleLifetime(extent: Extent<*>): Boolean {
        if (this == extent) {
            return true
        } else if (lifetime != null) {
            return lifetime!!.hasCompatibleLifetime(extent.lifetime)
        } else {
            return false
        }
    }

    internal fun addBehavior(behavior: Behavior<ExtentContext>) {
        this.behaviors.add(behavior)
    }

    internal fun addResource(resource: Resource) {
        this.resources.add(resource)
    }

    /**
     * Creates an Action on the graph and calls [addToGraph]
     */
    @JvmOverloads
    fun addToGraphWithAction(debugName: String? = null): Future<*> {
        return this.graph.action(debugName) {
            this.addToGraph()
        }
    }

    /**
     * An extent must be added to the graph before any of its behaviors will run or any of its resources will be linked to.
     */
    fun addToGraph() {
        if (graph.processingChangesOnCurrentThread) {
            if (graph.automaticResourceNaming) {
                nameResources()
            }
            graph.addExtent(this)
        } else {
            assert(false) {
                "addToGraph must be called within an event. \nAdding Extent=$this"
            }
        }
    }

    /**
     * Creates an Action on the graph and calls [removeFromGraph]
     */
    @JvmOverloads
    fun removeFromGraphWithAction(strategy: ExtentRemoveStrategy = ExtentRemoveStrategy.ExtentOnly, debugName: String? = null) {
        this.graph.action(debugName, { this.removeFromGraph(strategy) })
    }

    /**
     * Remove this Extent from its associated Graph instance. Must be called from inside an Action or Behavior.
     * @param strategy Optional parameter to automatically remove all extents with the same or shorter lifetimes
     * as established by [addChildLifetime] and [unifyLifetime].
     */
    @JvmOverloads
    fun removeFromGraph(strategy: ExtentRemoveStrategy = ExtentRemoveStrategy.ExtentOnly) {
        if (graph.processingChangesOnCurrentThread) {
            if (addedToGraphWhen != null) {
                if (strategy == ExtentRemoveStrategy.ExtentOnly || this.lifetime == null) {
                    graph.removeExtent(this)
                } else {
                    lifetime?.getAllContainedExtents()?.forEach { graph.removeExtent(it) }
                }
            }
        } else {
            assert(false) {
                "removeFromGraph must be called within an event. \nRemoving Extent=$this"
            }
        }
    }

    /**
     * Ensure all resources in self (not superclasses) have a debugName.
     * future: move to platformSupport since this is not portable beyond the jvm
     */
    private fun nameResources() {
        // context object or this extent if none
        val focus: Any = context ?: this
        try {
            focus.javaClass.declaredFields.forEach { field ->
                // iterate through each field and see if its a resource subclass
                if ((Resource::class.java as Class).isAssignableFrom(field.type)) {
                    // sometimes fields aren't accessible to reflection, try enabling that
                    field.isAccessible = true // throws error if not possible
                    val resource = field.get(focus) as Resource
                    if (resource.debugName == null) {
                        resource.debugName = field.name
                    }
                }
            }
        } catch (ex: Exception) {
            // throws error if we cannot make fields accessible for security reasons
            // catching the error is fine here, it just means we won't get debug names
        }
    }

    /**
     * Create a [Resource] instance associated with this [Extent]
     */
    @JvmOverloads
    fun resource(debugName: String? = null): Resource {
        return Resource(this, debugName)
    }

    /**
     * Create a [TypedMoment] instance associated with this [Extent]
     */
    @JvmOverloads
    fun <T> typedMoment(debugName: String? = null): TypedMoment<T> {
        return TypedMoment<T>(this, debugName)
    }

    /**
     * Creates a [Moment] instance associated with this [Extent]
     */
    @JvmOverloads
    fun moment(debugName: String? = null): Moment {
        return Moment(this, debugName)
    }

    /**
     * Creates a [State] instance associated with this [Extent].
     */
    @JvmOverloads
    fun <T> state(initialState: T, debugName: String? = null): State<T> {
        return State<T>(this, initialState, debugName)
    }

    /**
     * Creates a [BehaviorBuilder] to create a [Behavior] associated with this [Extent]
     */
    fun behavior(): BehaviorBuilder<ExtentContext> {
        return BehaviorBuilder(this)
    }

    /**
     * Calls [Graph.sideEffect] on the Graph instance associated with this [Extent].
     */
    @JvmOverloads
    fun sideEffect(debugName: String? = null, thunk: ExtentThunk<ExtentContext>) {
        val sideEffect = ExtentSideEffect(thunk, (context ?: this) as ExtentContext, this.graph.currentBehavior as Behavior<ExtentContext>?, debugName)
        graph.sideEffectHelper(sideEffect)
    }

    /**
     * Calls [Graph.action] on the Graph instance associated with this [Extent].
     */
    @JvmOverloads
    fun action(debugName: String? = null, thunk: ExtentThunk<ExtentContext>): Future<*> {
        val action = ExtentAction(thunk, (context ?: this) as ExtentContext, debugName)
        return graph.actionHelper(action)
    }

    override fun toString(): String {
        return super.toString() + "\n" + graph.toString()
    }

}