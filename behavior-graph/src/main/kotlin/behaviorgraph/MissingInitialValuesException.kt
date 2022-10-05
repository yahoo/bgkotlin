//
// Copyright Yahoo 2021
//
package behaviorgraph

class MissingInitialValuesException(s: String, val resource: Resource) : BehaviorGraphException("$s Resource=$resource")
