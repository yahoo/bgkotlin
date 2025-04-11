package behaviorgraph

/**
 * When removing an extent the default is to remove only that extent.
 * We can specify `ContainedLifetimes` to indicate that we want all extents with the same or
 * shorter lifetimes to be removed as well. This can be convenient when removing a subtree of extents.
 */
enum class ExtentRemoveStrategy {
    ExtentOnly,
    ContainedLifetimes
}
