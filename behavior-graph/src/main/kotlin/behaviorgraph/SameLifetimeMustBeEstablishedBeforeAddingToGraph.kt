package behaviorgraph

class SameLifetimeMustBeEstablishedBeforeAddingToGraph(val ext: Extent<*>): BehaviorGraphException("Same lifetime relationship must be established before adding any extent to graph.")