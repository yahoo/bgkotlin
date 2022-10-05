//
// Copyright Yahoo 2021
//
package behaviorgraph

class ExtentsCanOnlyBeRemovedDuringAnEventException(s: String, val extent: Extent<*>) : BehaviorGraphException("$s Extent=$extent")

