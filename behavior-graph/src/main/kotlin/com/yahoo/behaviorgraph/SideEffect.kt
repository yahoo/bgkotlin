//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

data class SideEffect(val block: (extent: Extent<*>) -> Unit, val extent: Extent<*>, val behavior: Behavior?, val debugName: String?)
