//
// Copyright Yahoo 2021
//
package behaviorgraph

class BehaviorDependencyCycleDetectedException(s: String, val behavior: Behavior<*>, val cycle: List<Resource>) : BehaviorGraphException("$s Behavior=$behavior")

