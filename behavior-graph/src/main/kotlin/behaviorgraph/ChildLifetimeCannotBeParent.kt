package behaviorgraph

class ChildLifetimeCannotBeParent(val child: Extent<*>): BehaviorGraphException("Child lifetime cannot be a transitive parent.")