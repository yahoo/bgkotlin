//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph.exception

import com.yahoo.behaviorgraph.Behavior
import com.yahoo.behaviorgraph.Resource

class ResourceCannotBeSuppliedByMoreThanOneBehaviorException(s: String, val alreadySupplied: Resource, val desiredSupplier: Behavior) : BehaviorGraphException("$s alreadySupplied=$alreadySupplied desiredSupplier=$desiredSupplier")
