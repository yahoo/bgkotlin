//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import com.yahoo.behaviorgraph.platform.PlatformSupport
import org.junit.Assert.*
import org.junit.Test

class EffectsActionsEventsTest : AbstractBehaviorGraphTest() {

    @Test
    fun `happen after all behaviors`() {
        var happened = false
        // behavior a has a side effect and
        ext.behavior()
            .supplies(r_b)
            .demands(r_a)
            .runs { extent ->
                extent.sideEffect {
                    happened = true
                }
                r_b.update(1)
            }
        // b depends on a
        // check that side effect didn't happen during b's run
        ext.behavior()
            .demands(r_b)
            .runs { extent ->
                assertFalse(happened)
                extent.sideEffect("after effect") {
                    assertTrue(happened)
                }
            }

        ext.addToGraphWithAction()
        r_a.updateWithAction(1)
        // asserts are above in behavior
    }

    @Test
    fun `execute in the order they are pushed`() {
        var counter = 0
        var whenX = 0
        var whenY = 0
        ext.behavior()
            .supplies(r_b)
            .demands(r_a)
            .runs {
                ext.sideEffect("first") {
                    whenX = counter
                    counter += 1
                }
                r_b.update(1)
            }
        ext.behavior()
            .demands(r_b)
            .runs {
                ext.sideEffect("second") {
                    whenY = counter
                    counter += 1
                }
            }

        ext.addToGraphWithAction()
        r_a.updateWithAction(1)

        assertTrue(whenY > whenX)
    }

    @Test
    fun `transient values are cleared after effects are run`() {
        val r1 = Moment<Unit>(ext, "r1")
        ext.behavior()
            .supplies(r1)
            .demands(r_a)
            .runs { extent ->
                r1.update(Unit)
                extent.sideEffect("after") {
                    assertTrue(r_a.justUpdatedTo(1))
                    assertTrue(r1.justUpdated)
                }
            }
        ext.addToGraphWithAction()
        r_a.updateWithAction(1)

        assertFalse(r_a.justUpdatedTo(1))
        assertFalse(r1.justUpdated)
    }


    @Test
    fun `effects from first event loop complete before next event loop`() {
        // |> Given event loop with 2 effects
        var effectCounter = 0
        val m1 = Moment<Unit>(ext, "m1")
        var eventLoopOrder: Int? = null
        var effect2Order: Int? = null
        ext.behavior()
            .demands(m1)
            .runs { extent ->
                    extent.sideEffect("effect 1") {
                        extent.graph.action("event loop2") {
                            eventLoopOrder = effectCounter++
                        }
                    }
                    extent.sideEffect("effect 2") {
                        effect2Order = effectCounter++
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
    fun `actionAsync runs immediately if no current events`() {
        var effectIsRun= false
        ext.addToGraphWithAction()
        ext.actionAsync {
            ext.sideEffect {
                effectIsRun = true
            }
        }

        assertTrue(effectIsRun)
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
        r1.updateWithAction(1)
        assertEquals(99, r1.event.timestamp)
    }

    @Test
    fun `effects can only be run during an event`() {
        ext.addToGraphWithAction()
        assertBehaviorGraphException {
            ext.sideEffect {  }
        }
    }

    @Test
    fun `actions have knowledge of changes for debugging`() {
        val m1 = ext.moment<Unit>()
        val m2 = ext.moment<Unit>()
        val m3 = ext.moment<Unit>()

        var actionUpdatesDuring: List<Resource>? = null
        ext.behavior().demands(m1).supplies(m3).runs {
            m3.update(Unit)
            actionUpdatesDuring = ext.graph.actionUpdates
        }
        ext.addToGraphWithAction()

        // |> When action updates multiple resources
        g.action {
            m1.update(Unit)
            m2.update(Unit)
        }

        // |> Then that information is available during the current event
        assertTrue(actionUpdatesDuring?.contains(m1) ?: false)
        assertTrue(actionUpdatesDuring?.contains(m2) ?: false)

    }

    @Test
    fun `actions have debugName`() {
        var m1 = ext.moment<Unit>();
        var s1 = ext.state<Long>(1);
        var lastActionName: String? = null
        ext.behavior().demands(ext.didAdd, m1, s1).runs {
            lastActionName = ext.graph.currentAction!!.debugName;
        }
        ext.addToGraphWithAction("added");
        assertEquals(lastActionName, "added")

        g.action("1") {
            m1.update(Unit);
        }

        assertEquals(lastActionName, "1")

        g.actionAsync("2") {
            m1.update(Unit);
        }

        assertEquals(lastActionName, "2")

        m1.updateWithAction(Unit,"3")

        assertEquals(lastActionName, "3")

        s1.updateWithAction(2, "4");
        assertEquals(lastActionName, "4")

        ext.action("5") {
            m1.update(Unit)
        }

        assertEquals(lastActionName, "5")

        ext.actionAsync("6") {
            m1.update(Unit)
        }

        assertEquals(lastActionName, "6")
    }

    @Test
    fun `sideEffects have debugName`() {
        val m1 = ext.moment<Unit>()
        val m2 = ext.moment<Unit>()
        var firstSideEffectName: String? = null
        var secondSideEffectName: String? = null
        ext.behavior().supplies(m2).demands(m1).runs {
            m2.update(Unit)
            ext.sideEffect("1") {
                firstSideEffectName = ext.graph.currentSideEffect?.debugName
            }
        }
        ext.behavior().demands(m2).runs {
            ext.sideEffect {
                secondSideEffectName = ext.graph.currentSideEffect?.debugName
            }
        }
        ext.addToGraphWithAction()
        m1.updateWithAction(Unit)

        assertEquals(firstSideEffectName, "1")
        assertNull(secondSideEffectName)
    }

    @Test
    fun `can create side effects with graph object`() {
        var valueAfter = 0
        var sideEffectName: String? = null
        g.action {
            g.sideEffect("sideEffect1") {
                valueAfter = 1
                sideEffectName = g.currentSideEffect?.debugName
            }
        }
        assertEquals(valueAfter, 1)
        assertEquals(sideEffectName, "sideEffect1")
    }

    @Test
    fun `defining behavior visible inside side effect`() {
        var m1 = ext.moment<Unit>()
        var defininingBehavior: Behavior? = null
        var createdBehavior = ext.behavior().demands(m1).runs {
            ext.sideEffect {
                defininingBehavior = ext.graph.currentSideEffect?.behavior
            }
        }

        ext.addToGraphWithAction()
        m1.updateWithAction(Unit)

        assertEquals(defininingBehavior, createdBehavior)
    }

    @Test
    fun `action inside sideEffect has extent`() {
        val m1 = ext.moment<Unit>()
        var insideExtent: Extent? = null
        ext.behavior().demands(m1).runs {
            ext.sideEffect {
                ext.action {
                    insideExtent = it
                }
            }
        }
        ext.addToGraphWithAction()
        m1.updateWithAction(Unit)

        assertEquals(insideExtent, ext)
    }

    @Test
    fun `nested actions are disallowed`() {
        assertBehaviorGraphException {
            g.action {
                g.action {

                }
            }
        }
    }

    @Test
    fun `actions directly inside behaviors are disallowed`() {
        ext.behavior().demands(ext.didAdd).runs {
            ext.action {

            }
        }

        assertBehaviorGraphException {
            ext.addToGraphWithAction()
        }
    }

    @Test
    fun `sideEffect in sideEffect doesnt make sense`() {
        assertBehaviorGraphException {
            g.action {
                g.sideEffect {
                    g.sideEffect {

                    }
                }
            }
        }
    }
}

