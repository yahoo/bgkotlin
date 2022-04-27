//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph.exception

import com.yahoo.behaviorgraph.Extent

class ExtentsCanOnlyBeAddedDuringAnEventException(s: String, val extent: Extent) : BehaviorGraphException("$s Extent=$extent")

