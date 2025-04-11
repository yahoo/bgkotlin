package behaviorgraph

/**
 * [Demandable] types are objects that can be demanded. It can be one of two types.
 * The default **Reactive** type means that when the corresponding resource is updated,
 * the demanding behavior will be activated to run.
 * An **Order** type means the behavior will not be activated when that resource updates
 * but because it is still dependent on it is guaranteed to run afterwards if it activates
 * from another resource updating.
 */
enum class LinkType {
    Reactive,
    Order
}