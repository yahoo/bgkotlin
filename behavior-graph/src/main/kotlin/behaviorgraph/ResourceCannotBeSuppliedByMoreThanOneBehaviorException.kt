//
// Copyright Yahoo 2021
//
package behaviorgraph

class ResourceCannotBeSuppliedByMoreThanOneBehaviorException(s: String, val alreadySupplied: Resource, val desiredSupplier: Behavior<*>) : BehaviorGraphException("$s alreadySupplied=$alreadySupplied desiredSupplier=$desiredSupplier")
