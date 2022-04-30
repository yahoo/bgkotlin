//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

interface RunnableAction {
    val debugName: String?
    fun runAction()
}

class Action(val block: () -> Unit, override val debugName: String? = null): RunnableAction {
    override fun runAction() {
        block()
    }
}

class ExtentAction(val block: (extent: Extent) -> Unit, val extent: Extent, override val debugName: String? = null): RunnableAction {
    override fun runAction() {
        block(extent)
    }
}