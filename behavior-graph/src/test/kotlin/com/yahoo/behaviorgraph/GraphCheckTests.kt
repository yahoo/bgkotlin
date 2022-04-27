//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import com.yahoo.behaviorgraph.exception.BehaviorDependencyCycleDetectedException
import com.yahoo.behaviorgraph.exception.BehaviorGraphException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.util.ReflectionHelpers

class GraphCheckTests : AbstractBehaviorGraphTest() {
    /*
    @Test(expected = BehaviorGraphException::class)
    fun `check cannot add extent to graph outside event loop`() {
        g.addExtent(ext)
    }

    @Test
    fun `check no graph dependency cycles`() {
        val r_x = State(ext, 0, "r_x")
        val r_y = State(ext, 0, "r_y")
        ext.makeBehavior(listOf(r_y), listOf(r_x)) {}
        ext.makeBehavior(listOf(r_x), listOf(r_y)) {}
        var caught = false
        try {
            ext.addToGraphWithAction()
        } catch (e: BehaviorDependencyCycleDetectedException) {
            caught = true
            val cycle = e.cycle
            assertEquals(2, cycle.size)
            assertSame(r_x, cycle[0])
            assertSame(r_y, cycle[1])
        }
        assertTrue(caught)
    }

    @Test
    fun `check resource can only be supplied by one behavior`() {
        val r_x = State(ext, 0, "r_x")
        ext.makeBehavior(listOf(r_a), listOf(r_x)) {}
        ext.makeBehavior(listOf(r_a), listOf(r_x)) {}
        assertBehaviorGraphException { ext.addToGraphWithAction() }
    }

    @Test
    fun `check update demands and supplies only during event loop`() {
        val b_x = ext.makeBehavior(listOf(), listOf()) {}
        ext.addToGraphWithAction()

        assertBehaviorGraphException {
            b_x.setDemands(listOf(r_a))
        }

        assertBehaviorGraphException {
            b_x.setSupplies(listOf(r_b))
        }
    }

    @Test
    fun `check cannot update demands or supplies on behavior not in graph`() {
        val b_x = ext.makeBehavior(listOf(), listOf()) {}

        assertBehaviorGraphException {
            g.action("update") {
                b_x.setDemands(listOf(r_a))
            }
        }

        assertBehaviorGraphException {
            g.action("update") {
                b_x.setSupplies(listOf(r_a))
            }
        }
    }

    @Test
    fun `handled errors cancel any pending actions or effects but ready to run more`() {
        // |> Given a event with an error that is handled
        var innerAction = false
        var innerEffect = false
        var secondAction = false
        ext.addToGraphWithAction()
        assertExpectedException(SideEffectError::class) {
            g.action("throws") {
                ext.sideEffect("action") {
                    g.action("innerAction") {
                        innerAction = true
                    }
                }
                ext.sideEffect("innerEffect") {
                    throw SideEffectError()
                }
                ext.sideEffect("effect") {
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
    fun `handled throw in behavior should clear out queued up internals`() {
        val r1 = Moment<Unit>(ext, "r1")
        val r2 = Moment<Unit>(ext, "r2")
        val r3 = Moment<Unit>(ext, "r3")
        val b3 = ext.makeBehavior(listOf(r3), null) {
            // do nothing
        }

        ext.makeBehavior(listOf(r1), listOf(r2)) {
            if (r1.justUpdated) {
                r2.update(Unit)
            }
        }
        ext.makeBehavior(listOf(r2), listOf(r3)) {
            if (r2.justUpdated) {
                r3.update(Unit)
                b3.setDemands(listOf())
                b3.setSupplies(listOf())
                throw Exception()
            }
        }

        ext.addToGraphWithAction()
        assertExpectedException(Exception::class) {
            g.action("r1") {
                r1.update(Unit)
            }
        }

        assertNull(g.currentEvent)
        assertNull(g.currentBehavior)
        assertEquals(0, (ReflectionHelpers.getField(g, "activatedBehaviors") as Collection<*>).size)
        assertFalse(r1.justUpdated)
        assertEquals(
            0,
            (ReflectionHelpers.getField(g, "modifiedSupplyBehaviors") as Collection<*>).size
        )
        assertEquals(
            0,
            (ReflectionHelpers.getField(g, "modifiedDemandBehaviors") as Collection<*>).size
        )
    }

    @Test
    fun `handled error when adding extent does not leave dangling behaviors`() {
        // |> Given we are adding an extent
        ext.makeBehavior(null, null) {
            // do nothing
        }
        // |> When it throws when adding
        assertExpectedException(Exception::class) {
            g.action("add") {
                ext.addToGraph()
                throw Exception()
            }
        }
        // |> Then behaviors from that extent aren't waiting to be added
        assertEquals(0, (ReflectionHelpers.getField(g, "untrackedBehaviors") as Collection<*>).size)
    }
    
     */
}

class SideEffectError : Exception()
