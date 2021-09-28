//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph.exception

import com.yahoo.behaviorgraph.Behavior
import com.yahoo.behaviorgraph.Resource

class AllDemandsMustBeAddedToTheGraphExceptions(s: String, val currentBehavior: Behavior, val untrackedDemand: Resource) : BehaviorGraphException("$s Behavior=$currentBehavior untrackedDemand=$untrackedDemand")
