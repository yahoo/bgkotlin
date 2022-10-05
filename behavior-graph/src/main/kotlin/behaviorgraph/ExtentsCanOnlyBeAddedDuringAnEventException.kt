//
// Copyright Yahoo 2021
//
package behaviorgraph

class ExtentsCanOnlyBeAddedDuringAnEventException(s: String, val extent: Extent<*>) : BehaviorGraphException("$s Extent=$extent")

