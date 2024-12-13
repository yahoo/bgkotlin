//
// Copyright Yahoo 2021
//
package behaviorgraph

/**
 * A Resource is a node which connects between Behaviors. It can be demanded or supplied.
 * This is the base class for [Moment] [TypedMoment] and [State] resources.
 * In almost all scenarios you are interested in one of those.
 *
 * You may wish to use this Resource to enforce ordering between two behaviors without
 * implying any other relationship.
 */
open class Resource @JvmOverloads constructor(val extent: Extent<*>, var debugName: String? = null): Demandable {
    val graph: Graph = extent.graph
    internal var subsequents: MutableSet<Behavior<*>> = mutableSetOf()
    var suppliedBy: Behavior<*>? = null
        internal set

    override val resource get() = this
    override val type get() = LinkType.Reactive

    init {
        extent.addResource(this)
    }

    @get:JvmName("order")
    val order: Demandable get() = DemandLink(this, LinkType.Order)

    internal open val internalJustUpdated: Boolean get() = false

    internal fun assertValidUpdater() {
        val currentBehavior = graph.currentBehavior
        val currentEvent = graph.currentEvent
        if (!graph.processingChangesOnCurrentThread) {
            assert(false) {
                "Resource must be updated inside a behavior or action. \nResource=$this"
            }
        }
        if (!graph.validateDependencies) { return }
        if (suppliedBy != null && currentBehavior != suppliedBy) {
            assert(false) {
                "Supplied resource can only be updated by its supplying behavior. \nResource=$this \nBehavior Trying to Update=$currentBehavior"
            }
        }
        if (suppliedBy == null && currentBehavior != null) {
            assert(false) {
                "Unsupplied resource can only be updated in an action. \nResource=$this \nBehaviorTrying to Update=$currentBehavior"
            }
        }
    }

    internal fun assertValidAccessor() {
        if (!graph.validateDependencies) { return }
        // allow access to state from alternate threads while running
        val eventLoopThread = graph.eventLoopState?.thread
        if (graph.eventLoopState != null && eventLoopThread != Thread.currentThread()) { return }
        val currentBehavior = graph.currentBehavior
        if (currentBehavior != null && currentBehavior != suppliedBy && !(currentBehavior.demands?.contains(this) ?: false)) {
            assert(false) {
                "Cannot access the value or event of a resource inside a behavior unless it is supplied or demanded. \nResource=$this \nAccessing Behavior=$currentBehavior"
            }
        }
    }
}
