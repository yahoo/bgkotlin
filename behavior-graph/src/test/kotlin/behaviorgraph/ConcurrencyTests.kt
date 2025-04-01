package behaviorgraph

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.asCompletableFuture
import kotlin.test.*
import java.lang.Thread
import java.util.concurrent.*

class ConcurrencyTests : AbstractBehaviorGraphTest() {
    @Test
    fun runsOnActionThreadIfNotBusy() {
        val sr1 = ext.state(1)
        var runningThread: String? = null
        val sem = Semaphore(0)
        ext.behavior()
            .demands(sr1)
            .performs {
                runningThread = Thread.currentThread().name
                sem.release()
            }
        ext.addToGraphWithAction()

        val t1 = Thread {
            sr1.updateWithAction(2)
        }
        t1.name = "thisthread"
        t1.start()
        sem.acquire()
        assert(runningThread?.contains(Regex("thisthread")) ?: false) // contains because coroutines can alter thread name some
    }

    @Test
    fun secondThreadDispatchesAndContinuesWhileFirstIsBusy() {
        // This test sets up two threads so the first can block
        // the second, and we use the test running thread
        // to orchestrate.
        // The semaphores lead us through the steps so that we can
        // test what we want.
        val sem1 = Semaphore(0)
        val sem2 = Semaphore(0)

        val sr1 = ext.state(0)
        val sr2 = ext.state(0)
        ext.behavior()
            .demands(sr1)
            .performs {
                sem1.release()
                sem2.acquire()
            }
        ext.addToGraphWithAction()


        var f1: Future<*>? = null
        var f2: Future<*>? = null

        val t1 = Thread {
            f1 = sr1.updateWithAction(1).asCompletableFuture()
        }.start()
        // first thread running bg (intentionally blocked in behavior)
        sem1.acquire()

        val t2 = Thread {
            f2 = sr2.updateWithAction(2).asCompletableFuture()
            sem1.release()
        }.start()
        // thread 2 will be blocked and return a future
        sem1.acquire()
        assertFalse(f2!!.isDone) // still running
        assertFalse(f2!!.isCancelled) // always false
        // let t1 finish
        sem2.release()
        // now we can wait for t2 to finish with future
        f2!!.get()
        assertTrue(f2!!.isDone)
        assertEquals(1, sr1.value)
        assertEquals(2, sr2.value)
    }

    @Test
    fun exceptionOnBackgroundThreadBubblesUpToFuture() {
        // We have access to future in the thread that creates the action;
        // however the exception may happen on the internally created background
        // thread that ends up running the action if it blocks.
        // This tests that this exception gets put into the Future which
        // we throws when we call get() with pointer to the original exception.
        //
        // See note above about using multiple threads and semaphores
        val sem1 = Semaphore(0)
        val sem2 = Semaphore(0)

        val sr1 = ext.state(0)
        val sr2 = ext.state(0)
        ext.behavior()
            .demands(sr1)
            .performs {
                sem1.release()
                sem2.acquire()
            }
        ext.addToGraphWithAction()


        var f1: Future<*>? = null
        var f2: Future<*>? = null

        val t1 = Thread {
            f1 = sr1.updateWithAction(1).asCompletableFuture()
        }.start()
        // first thread running bg (intentionally blocked in behavior)
        sem1.acquire()

        val t2 = Thread {
            f2 = ext.action {
                // action inside action throws
                // @SAL 10/16/2024-- I'm not sure if this is an annoying way to do this.
                // This will hit an assert and throws which shows up in the console,
                // but because it is in a background thread the other threads will continue.
                ext.action { }
            }.asCompletableFuture()
            sem1.release()
        }.start()
        // thread 2 will be blocked and return a future
        sem1.acquire()
        sem2.release()

        assertFails {
            f2!!.get()
        }
    }

    @Test
    fun futureImplementsTimeout() {
        // Calling get() with a timeout should throw an error if
        // we don't unblock in time.
        //
        // See note above about using multiple threads and semaphores

        val sem1 = Semaphore(0)
        val sem2 = Semaphore(0)

        val sr1 = ext.state(0)
        val sr2 = ext.state(0)
        ext.behavior()
            .demands(sr1)
            .performs {
                sem1.release()
                sem2.acquire()
            }
        ext.addToGraphWithAction()


        var f1: Future<*>? = null
        var f2: Future<*>? = null

        val t1 = Thread {
            f1 = sr1.updateWithAction(1).asCompletableFuture()
        }.start()
        // first thread running bg (intentionally blocked in behavior)
        sem1.acquire()

        val t2 = Thread {
            f2 = ext.action {
            }.asCompletableFuture()
            sem1.release()
        }.start()
        // thread 2 will be blocked and return a future
        sem1.acquire()

        // we don't unblock the first thread ever,
        // so second action never runs
        assertFails {
            f2!!.get(100, TimeUnit.MILLISECONDS)
        }
    }

    @Test
    fun canAccessResourcesOnSecondThread() {
        // |> Given we are in the middle of running an event
        val sem = Semaphore(0)
        val sem2 = Semaphore(0)
        val sr1 = ext.state(1)
        val sr2 = ext.state(2)
        ext.behavior()
            .demands(sr1)
            .performs {
                sem2.release()
                sem.acquire()
            }
        ext.addToGraphWithAction()

        val t1 = Thread {
            sr1.updateWithAction(2)
        }

        t1.start()
        sem2.acquire()


        // |> When we access a resource from another thread
        // |> Then we should be able to access the value without
        // hitting an assert statement
        var currentValue = 0
        assertNoThrow {
            currentValue = sr2.value
        }
        assertEquals(2, currentValue)
    }

    @Test
    fun canRunSideEffectsOnSpecifiedThread() {
        // |> Given we have provided a different dispatcher
        //g.defaultSideEffectDispatcher = Dispatchers.IO
        //g.sideEffectExecutor = Executors.newSingleThreadExecutor()

        val sem = Semaphore(0)
        val sr1 = ext.state(1)
        var sideEffectThread: Thread? = null
        ext.behavior()
            .demands(sr1)
            .performs {
                it.sideEffect(dispatcher = Dispatchers.IO) {
                    sideEffectThread = Thread.currentThread()
                    sem.release()
                }
            }
        ext.addToGraphWithAction()

        // |> When we run action from background thread
        var backgroundThread: Thread? = null
        Thread {
            backgroundThread = Thread.currentThread()
            sr1.updateWithAction(2)
        }.start()
        sem.acquire()

        // |> Then Side effects still happen on main thread
        assertNotEquals(sideEffectThread, backgroundThread)
    }

}