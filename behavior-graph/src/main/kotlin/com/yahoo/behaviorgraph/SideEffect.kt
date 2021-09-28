//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

data class SideEffect(val debugName: String?, val block: (extent: Extent<*>) -> Unit, val extent: Extent<*>) //todo contrain Extent more than "*"?
