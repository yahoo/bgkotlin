//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph.exception

import com.yahoo.behaviorgraph.Resource

class MissingInitialValuesException(s: String, val resource: Resource) : BehaviorGraphException("$s Resource=$resource")
