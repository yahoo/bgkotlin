//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

internal enum class OrderingState {
    Untracked,
    NeedsOrdering,
    Clearing,
    Ordering,
    Ordered
}
