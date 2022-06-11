//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

/**
 * SideEffects are blocks of code that are guaranteed to run at the end of the current event.
 * Use them to create output to external APIs.
 */
interface SideEffect {
    val debugName: String?
    val behavior: Behavior<*>?
}

internal interface RunnableSideEffect: SideEffect {
    fun runSideEffect()
}

internal class GraphSideEffect(val thunk: Thunk, override val behavior: Behavior<*>?, override val debugName: String?): RunnableSideEffect {
    override fun runSideEffect() {
        thunk.invoke()
    }
}

internal class ExtentSideEffect<T>(val thunk: ExtentThunk<T>, val context: T, override val behavior: Behavior<T>?, override val debugName: String? = null): RunnableSideEffect {
    override fun runSideEffect() {
        thunk.invoke(context)
    }
}