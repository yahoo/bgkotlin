//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

interface RunnableAction {
    fun runAction()
}

internal class Action(val block: () -> Unit, val debugName: String? = null): RunnableAction {
    override fun runAction() {
        block()
    }
}

internal class ExtentAction(val block: (extent: Extent<*>) -> Unit, val extent: Extent<*>, val debugName: String? = null): RunnableAction {
    override fun runAction() {
        block(extent)
    }
}