//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

interface RunnableSideEffect {
    val debugName: String?
    val behavior: Behavior?
    fun runSideEffect()
}

internal class SideEffect(val block: () -> Unit, override val behavior: Behavior?, override val debugName: String?): RunnableSideEffect {
    override fun runSideEffect() {
        block()
    }
}

internal class ExtentSideEffect(val block: (ext: Extent) -> Unit, val extent: Extent, override val behavior: Behavior?, override val debugName: String? = null): RunnableSideEffect {
    override fun runSideEffect() {
        block(extent)
    }
}