//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph.exception

import com.yahoo.behaviorgraph.Behavior
import com.yahoo.behaviorgraph.Resource

class BehaviorDependencyCycleDetectedException(s: String, val behavior: Behavior<*>, val cycle: List<Resource>) : BehaviorGraphException("$s Behavior=$behavior")

