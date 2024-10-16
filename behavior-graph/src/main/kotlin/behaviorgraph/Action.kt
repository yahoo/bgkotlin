//
// Copyright Yahoo 2021
//
package behaviorgraph

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

abstract class RunnableAction: Action, Future<Nothing?> {
    abstract fun runAction()
    private val semaphore: Semaphore = Semaphore(0)
    private var completed: Boolean = false
    private var underlyingProblem: Throwable? = null

    fun fail(underlyingProblem: Throwable) {
        this.underlyingProblem = underlyingProblem
        semaphore.release()
    }

    fun complete() {
        if (!completed) {
            completed = true
            semaphore.release()
        }
    }

    override fun cancel(p0: Boolean): Boolean {
        return false
    }

    override fun isCancelled(): Boolean {
        return false
    }

    override fun isDone(): Boolean {
        return completed
    }

    override fun get(): Nothing? {
        try {
            semaphore.acquire()
            if (underlyingProblem == null) {
                return null
            } else {
                throw ExecutionException(underlyingProblem)
            }
        } finally {
            semaphore.release()
        }
    }

    override fun get(p0: Long, p1: TimeUnit): Nothing? {
        if (semaphore.tryAcquire(p0, p1)) {
            try {
                if (underlyingProblem == null) {
                    return null
                } else {
                    throw ExecutionException(underlyingProblem)
                }
            } finally {
                semaphore.release()
            }
        } else {
            throw TimeoutException()
        }
    }
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
