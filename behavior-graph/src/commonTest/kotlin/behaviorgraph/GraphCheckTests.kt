//
// Copyright Yahoo 2021
//
package behaviorgraph

import kotlin.test.*

class GraphCheckTests : AbstractBehaviorGraphTest() {

    @Test
    fun checkNoGraphDependencyCycles() {
        val r_x = ext.state(0, "r_x")
        val r_y = ext.state(0, "r_y")
        val r_z = ext.state(0, "r_z")
        ext.behavior().supplies(r_z).runs {
            // non cycle behavior
        }
        ext.behavior()
            .supplies(r_x)
            .demands(r_y, r_z)
            .runs {}
        ext.behavior()
            .supplies(r_y)
            .demands(r_x)
            .runs {}

        var caught = false
        try {
            ext.addToGraphWithAction()
        } catch (e: AssertionError) {
            caught = true
            //val cycle = e.cycle
            //assertEquals(2, cycle.size)
            //assertSame(r_x, cycle[0])
            //assertSame(r_y, cycle[1])
        }
        assertTrue(caught)
    }

    @Test
    fun checkResourceCanOnlyBeSuppliedByOneBehavior() {
        val r_x = ext.state(0, "r_x")
        ext.behavior()
            .supplies(r_x)
            .demands(r_a)
            .runs {}
        ext.behavior()
            .supplies(r_x)
            .demands(r_a)
            .runs {}
        assertFails { ext.addToGraphWithAction() }
    }

    @Test
    fun checkUpdateDemandsAndSuppliesOnlyDuringEventLoop() {
        val b_x = ext.behavior()
            .supplies()
            .demands()
            .runs {}
        ext.addToGraphWithAction()

        assertFails {
            b_x.setDynamicDemands(listOf(r_a))
        }

        assertFails {
            b_x.setDynamicSupplies(listOf(r_b))
        }
    }

    @Test
    fun checkCannotUpdateDemandsOrSuppliesOnBehaviorNotInGraph() {
        val b_x = ext.behavior()
            .supplies()
            .demands()
            .runs {}

        assertFails {
            g.action("update") {
                b_x.setDynamicDemands(listOf(r_a))
            }
        }

        assertFails {
            g.action("update") {
                b_x.setDynamicSupplies(listOf(r_a))
            }
        }
    }

    @Test
    fun handledErrorsCancelAnyPendingActionsOrEffectsButReadyToRunMore() {
        // |> Given a event with an error that is handled
        var innerAction = false
        var innerEffect = false
        var secondAction = false
        ext.addToGraphWithAction()
        assertFails {
            g.action {
                ext.sideEffect("action side effect") {
                    g.action {
                        innerAction = true
                    }
                }
                ext.sideEffect("throw effect") {
                    throw SideEffectError()
                }
                ext.sideEffect("inner effect") {
                    innerEffect = true
                }
            }
        }
        // |> When trying to run another event
        g.action("works") {
            secondAction = true
        }
        // |> Then the next should work
        assertFalse(innerAction)
        assertFalse(innerEffect)
        assertTrue(secondAction)
    }

    @Test
    fun handledThrowInBehaviorShouldClearOutQueuedUpInternals() {
        val r1 = ext.moment("r1")
        val r2 = ext.moment("r2")
        val r3 = ext.moment("r3")
        var b3: Behavior<*>? = null

        ext.behavior().supplies(r2).demands(r1).runs {
            r2.update()
        }

        ext.behavior().supplies(r3).demands(r2).runs {
            r3.update()
            b3!!.setDynamicDemands(listOf())
            b3!!.setDynamicSupplies(listOf())
            throw Exception()
        }

        b3 = ext.behavior().demands(r3).runs {
            // do nothing
        }

        ext.addToGraphWithAction()
        assertFails {
            g.action("r1") {
                r1.update()
            }
        }

        assertNull(g.currentEvent)
        assertNull(g.currentBehavior)
        assertEquals(0, g.activatedBehaviors.size)
        assertFalse(r1.justUpdated)
        assertEquals(
            0,
            g.modifiedSupplyBehaviors.size
        )
        assertEquals(
            0, g.modifiedDemandBehaviors.size)
    }

    @Test
    fun handledErrorWhenAddingExtentDoesNotLeaveDanglingBehaviors() {
        // |> Given we are adding an extent
        ext.behavior().runs { }
        // |> When it throws when adding
        assertFails {
            g.action("add") {
                ext.addToGraph()
                throw Exception()
            }
        }
        // |> Then behaviors from that extent aren't waiting to be added
        assertEquals(0, g.untrackedBehaviors.size)
    }

    @Test
    fun checkCannotDemandResourceFromExtentThatHasNotBeenAddedToGraph() {
        val ext3 = TestExtent(g)
        val mr1 = ext3.moment()
        ext.behavior().demands(mr1).runs {

        }
        assertFails {
            ext.addToGraphWithAction()
        }
    }
}

class SideEffectError : Exception()
