//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

internal class Action(val block: (extent: Extent<*>) -> Unit, val extent: Extent<*>)
