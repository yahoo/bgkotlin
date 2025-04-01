//
// Copyright Yahoo 2021
//
package behaviorgraph

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * SideEffects are blocks of code that are guaranteed to run at the end of the current event.
 * Use them to create output to external APIs.
 */
interface SideEffect {
    val debugName: String?
    val behavior: Behavior<*>?
    val dispatcher: CoroutineDispatcher?
}

internal interface RunnableSideEffect: SideEffect, Runnable {
}

internal class GraphSideEffect(
    val thunk: Thunk,
    override val behavior: Behavior<*>?,
    override val debugName: String? = null,
    override val dispatcher: CoroutineDispatcher? = null
): RunnableSideEffect {
    override fun run() {
        thunk.invoke()
    }
}

internal class ExtentSideEffect<T: Any>(
    val thunk: ExtentThunk<T>,
    val context: T,
    override val behavior: Behavior<T>?,
    override val debugName: String? = null,
    override val dispatcher: CoroutineDispatcher? = null
): RunnableSideEffect {
    override fun run() {
        thunk.invoke(context)
    }
}