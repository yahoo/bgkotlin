//
// Copyright Yahoo 2021
//
package behaviorgraph

class AllDemandsMustBeAddedToTheGraphExceptions(s: String, val currentBehavior: Behavior<*>, val untrackedDemand: Resource) : BehaviorGraphException("$s Behavior=$currentBehavior untrackedDemand=$untrackedDemand")
