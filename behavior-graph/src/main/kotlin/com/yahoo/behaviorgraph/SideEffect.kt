//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

interface RunnableSideEffect {
    fun runSideEffect()
}

internal class SideEffect(val block: () -> Unit, val behavior: Behavior?, val debugName: String?): RunnableSideEffect {
    override fun runSideEffect() {
        block()
    }
}

internal class ExtentSideEffect(val block: (ext: Extent<*>) -> Unit, val extent: Extent<*>, val behavior: Behavior?, val debugName: String? = null): RunnableSideEffect {
    override fun runSideEffect() {
        block(extent)
    }
}