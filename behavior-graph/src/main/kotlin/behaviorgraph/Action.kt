//
// Copyright Yahoo 2021
//
package behaviorgraph

import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

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

abstract class RunnableAction: Action {
    abstract fun runAction()
    internal val job: CompletableJob = Job()
}

internal class GraphAction(val thunk: Thunk, override val debugName: String? = null): RunnableAction() {
    override fun runAction() {
        thunk.invoke()
    }
}

internal class ExtentAction<T>(val thunk: ExtentThunk<T>, val context: T, override val debugName: String? = null):
    RunnableAction() {
    override fun runAction() {
        thunk.invoke(context)
    }
}
