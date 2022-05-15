//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

/**
 * An __Action__ is a block of code which initiates a Behavior Graph [Event].
 * You create actions with the [action], [actionAsync] methods on an [Extent].
 * You can also use [Graph.action], [Graph.actionAsync] on your [Graph] instance.
 * You do not create an action directly.
 * The block of code in actions is run by the Behavior Graph runtime.
 */
interface Action {
    val debugName: String?
}

internal interface RunnableAction: Action {
    fun runAction()
}

internal class GraphAction(val thunk: Thunk, override val debugName: String? = null): RunnableAction {
    override fun runAction() {
        thunk.invoke()
    }
}

internal class ExtentAction<T>(val thunk: ExtentThunk<T>, val extent: T, override val debugName: String? = null): RunnableAction {
    override fun runAction() {
        thunk.invoke(extent)
    }
}
