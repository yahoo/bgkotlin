//
// Copyright Yahoo 2021
//
package behaviorgraph

import java.util.concurrent.Future
import kotlin.test.*

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
        val r1 = Moment(ext, "r1")
        ext.behavior()
            .supplies(r1)
            .demands(r_a)
            .runs { extent ->
                r1.update()
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
        val m1 = Moment(ext, "m1")
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
        m1.updateWithAction()

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
    fun `new actions run after side effects`() {
        // |> Given there is a running event
        var counter = 1
        var effectIsRun = 0
        var actionIsRun = 0
        ext.addToGraphWithAction()

        // |> When a new action is added
        var f: Future<*>? = null
        var futureIsDoneBeforeSideEffect: Boolean = false
        ext.action("existing") {
            ext.sideEffect("side effect") {
                f = ext.action("new") {
                    actionIsRun = counter
                    counter += 1
                }
                futureIsDoneBeforeSideEffect = f!!.isDone
                effectIsRun = counter
                counter += 1
            }
        }

        // |> Then it will be run after event completes entirely
        assertEquals(1, effectIsRun)
        assertEquals(2, actionIsRun)
        assertFalse(futureIsDoneBeforeSideEffect)
        assertTrue(f!!.isDone)
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
                ext.action("new") {
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
        val tp = object : DateProvider {
            override fun now(): Long {
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
        val m1 = ext.moment()
        val m2 = ext.moment()
        val m3 = ext.moment()

        var actionUpdatesDuring: List<Resource>? = null
        ext.behavior().demands(m1).supplies(m3).runs {
            m3.update()
            actionUpdatesDuring = ext.graph.actionUpdates
        }
        ext.addToGraphWithAction()

        // |> When action updates multiple resources
        g.action {
            m1.update()
            m2.update()
        }

        // |> Then that information is available during the current event
        assertTrue(actionUpdatesDuring?.contains(m1) ?: false)
        assertTrue(actionUpdatesDuring?.contains(m2) ?: false)

    }

    @Test
    fun `actions have debugName`() {
        var m1 = ext.moment();
        var s1 = ext.state<Long>(1);
        var lastActionName: String? = null
        ext.behavior().demands(ext.didAdd, m1, s1).runs {
            lastActionName = ext.graph.currentAction!!.debugName;
        }
        ext.addToGraphWithAction("added");
        assertEquals(lastActionName, "added")

        g.action("1") {
            m1.update();
        }

        assertEquals(lastActionName, "1")

        val f = g.action("2") {
            m1.update();
        }
        f.get()

        assertEquals(lastActionName, "2")

        m1.updateWithAction("3")

        assertEquals(lastActionName, "3")

        s1.updateWithAction(2, "4");
        assertEquals(lastActionName, "4")

        ext.action("5") {
            m1.update()
        }

        assertEquals(lastActionName, "5")

        ext.action("6") {
            m1.update()
        }

        assertEquals(lastActionName, "6")
    }

    @Test
    fun `sideEffects have debugName`() {
        val m1 = ext.moment()
        val m2 = ext.moment()
        var firstSideEffectName: String? = null
        var secondSideEffectName: String? = null
        ext.behavior().supplies(m2).demands(m1).runs {
            m2.update()
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
        m1.updateWithAction()

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
        var m1 = ext.moment()
        var defininingBehavior: Behavior<*>? = null
        var createdBehavior = ext.behavior().demands(m1).runs {
            ext.sideEffect {
                defininingBehavior = ext.graph.currentSideEffect?.behavior
            }
        }

        ext.addToGraphWithAction()
        m1.updateWithAction()

        assertEquals(defininingBehavior, createdBehavior)
    }

    @Test
    fun `action inside sideEffect has extent`() {
        val m1 = ext.moment()
        var insideExtent: TestExtent? = null
        ext.behavior().demands(m1).runs {
            ext.sideEffect {
                ext.action {
                    insideExtent = it
                }
            }
        }
        ext.addToGraphWithAction()
        m1.updateWithAction()

        assertEquals(insideExtent, ext)
    }

    @Test
    fun `nested actions are disallowed`() {
        assertFails {
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

        assertFails {
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

