//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import com.yahoo.behaviorgraph.platform.PlatformSupport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// from typescript describe('Effects, Actions, Events')
class EffectsActionsEventsTest : AbstractBehaviorGraphTest() {
    @Test
    fun `happen after all behaviors`() {
        var happened = false
        // behavior a has a side effect and
        ext.makeBehavior(listOf(r_a), listOf(r_b)) { extent ->
            if (r_a.justUpdated) {
                extent.sideEffect("happen") {
                    happened = true
                }
                r_b.update(1, true)
            }
        }
        // b depends on a
        // check that side effect didn't happen during b's run
        ext.makeBehavior(listOf(r_b), null) { extent ->
            if (r_b.justUpdated) {
                assertFalse(happened)
                extent.sideEffect("after effect") {
                    assertTrue(happened)
                }
            }
        }

        ext.addToGraphWithAction()
        r_a.updateWithAction(1, true)
        // asserts are above in behavior
    }

    @Test
    fun `execute in the order they are pushed`() {
        var counter = 0
        var whenX = 0
        var whenY = 0
        ext.makeBehavior(listOf(r_a), listOf(r_b)) {
            if (r_a.justUpdated) {
                ext.sideEffect("first") {
                    whenX = counter
                    counter += 1
                }
                r_b.update(1, true)
            }
        }
        ext.makeBehavior(listOf(r_b), null) {
            if (r_b.justUpdated) {
                ext.sideEffect("second") {
                    whenY = counter
                    counter += 1
                }
            }
        }

        ext.addToGraphWithAction()
        r_a.updateWithAction(1, true)

        assertTrue(whenY > whenX)
    }

    @Test
    fun `transient values are cleared after effects are run`() {
        val r1 = Moment<Unit>(ext, "r1")
        ext.makeBehavior(listOf(r_a), listOf(r1)) { extent ->
            if (r_a.justUpdated) {
                r1.update(Unit)
                extent.sideEffect("after") {
                    assertTrue(r_a.justUpdatedTo(1))
                    assertTrue(r1.justUpdated)
                }
            }
        }
        ext.addToGraphWithAction()
        r_a.updateWithAction(1, true)

        assertFalse(r_a.justUpdatedTo(1))
        assertFalse(r1.justUpdated)
    }

    @Test
    fun `effects are sequenced`() {
        // |> Given an effect starts another event loop
        var effectCounter = 0
        var firstEffect: Int? = null
        var secondEffect: Int? = null
        ext.makeBehavior(listOf(r_a), null) { extent ->
            if (r_a.justUpdated) {
                extent.sideEffect("update b") {
                    ext.action("update b") {
                        // initiate another event loop
                        r_b.update(2, false)
                    }
                }
                extent.sideEffect("first effect") {
                    firstEffect = effectCounter
                    effectCounter += 1
                }
            }
        }

        ext.makeBehavior(listOf(r_b), null) { extent ->
            if (r_b.justUpdated) {
                extent.sideEffect("second effect") {
                    secondEffect = effectCounter
                    effectCounter += 1
                }
            }
        }

        ext.addToGraphWithAction()
        // |> When the side effects run
        r_a.updateWithAction(1, false)
        // |> Then the side effect from the second event loop will run
        // after the remaining effects from the first event loop
        assertEquals(0, firstEffect)
        assertEquals(1, secondEffect)
        assertEquals(2, effectCounter)
    }

    @Test
    fun `effects from first event loop complete before next event loop`() {
        // |> Given event loop with 2 effects
        var effectCounter = 0
        val m1 = Moment<Unit>(ext, "m1")
        var eventLoopOrder: Int? = null
        var effect2Order: Int? = null
        ext.makeBehavior(listOf(m1), null) { extent ->
            if (m1.justUpdated) {
                extent.sideEffect("effect 1") {
                    extent.graph.action("event loop2") {
                        eventLoopOrder = effectCounter++
                    }
                }
                extent.sideEffect("effect 2") {
                    effect2Order = effectCounter++
                }
            }
        }
        ext.addToGraphWithAction()
        // |> When the first effect initiates a new event loop
        m1.updateWithAction(Unit)
        // |> Then the second effect will run before the new event loop
        assertEquals(0, effect2Order)
        assertEquals(1, eventLoopOrder)
    }

    @Test
    fun `behaviors from first event loop complete before next event loop`() {
        // Note: Initiating side effects directly from within a behavior can lead to accessing
        // and thus depending on state without being explicit about that dependency. However
        // it does not mean it is strictly an error. We should be able to synchronously initiate
        // a new event loop without necessarily breaking the graph.
        // |> Given we are in the middle of an event loop with multiple
        var effectCounter = 0
        var eventLoopOrder: Int? = null
        var behavior2Order: Int? = null
        val m1 = Moment<Unit>(ext, "m1")
        val m2 = Moment<Unit>(ext, "m2")
        ext.makeBehavior(listOf(m1), listOf(m2)) { extent ->
            if (m1.justUpdated) {
                m2.update(Unit)
                extent.action("inside side effect") {
                    eventLoopOrder = effectCounter++
                }
            }
        }
        ext.makeBehavior(listOf(m2), null) {
            if (m2.justUpdated) {
                behavior2Order = effectCounter++
            }
        }
        ext.addToGraphWithAction()
        // |> When an event loop is initiated from a prior behavior
        m1.updateWithAction(Unit)
        // |> Then the subsequent behavior will run before the next event loop
        assertEquals(0, behavior2Order)
        assertEquals(1, eventLoopOrder)
    }

    @Test
    fun `check updating a behavior after a side effect should throw`() {
        // Note: Here again, side effects shouldn't come directly from inside a
        // behavior. So if one does create a new event loop and there's still
        // more resources to update there we won't be able to do that
        val m1 = Moment<Unit>(ext, "m1")
        val m2 = Moment<Unit>(ext, "m2")
        ext.makeBehavior(listOf(m1), listOf(m2)) { extent ->
            if (m1.justUpdated) {
                extent.action("inside side effect") {
                    // this will force event loop to finish when it runs synchronously
                }
                // event loop should have finished by the time we return up the stack
                // causing this to fail
                assertBehaviorGraphException { m2.update(Unit) }
            }
        }
        ext.addToGraphWithAction()

        m1.updateWithAction(Unit)
    }

    @Test
    fun `actions are run synchronously by default when there is only one`() {
        // |> Given there are no running events
        var counter = 0
        ext.addToGraphWithAction()
        // |> When an action is added
        ext.action("action") {
            counter += 1
        }
        // |> Then it will be run synchronously
        assertEquals(1, counter)
    }

    @Test
    fun `actions are synchronous by default`() {
        // |> Given there is a running event
        var counter = 1
        var effectIsRun = 0
        var actionIsRun = 0
        ext.addToGraphWithAction()
        // |> When a new action is added
        ext.action("existing") {
            ext.sideEffect("side effect") {
                ext.action("new") {
                    actionIsRun = counter
                    counter += 1
                }
                effectIsRun = counter
                counter += 1
            }
        }
        // |> Then it will be run after first event completes entirely
        assertEquals(2, effectIsRun)
        assertEquals(1, actionIsRun)
    }

    @Test
    fun `actions can opt into async`() {
        // |> Given there is a running event
        var counter = 1
        var effectIsRun = 0
        var actionIsRun = 0
        ext.addToGraphWithAction()
        // |> When a new action is added asynchronously
        ext.action("existing") {
            ext.sideEffect("side effect") {
                ext.actionAsync("new") {
                    actionIsRun = counter
                    counter += 1
                }
                effectIsRun = counter
                counter += 1
            }
        }
        // |> Then it will be run after first event completes entirely
        assertEquals(1, effectIsRun)
        assertEquals(2, actionIsRun)
    }

    @Test
    fun `PlatformSupport gives an alternate time`() {
        val tp = object : PlatformSupport {

            override fun isMainThread(): Boolean {
                TODO("Not yet implemented")
            }

            override fun getCurrentTimeMillis(): Long {
                return 99
            }
        }
        val g2 = Graph(tp)
        val ext3 = TestExtent(g2)
        val r1 = State(ext3, 0, "r1")
        ext3.addToGraphWithAction()
        r1.updateWithAction(1, true)
        assertEquals(99, r1.event.timestamp)
    }
}

