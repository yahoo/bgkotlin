//
// Copyright Yahoo 2021
//
package behaviorgraph

internal enum class OrderingState {
    Untracked,
    NeedsOrdering,
    Clearing,
    Ordering,
    Ordered
}
