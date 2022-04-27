//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.util.ReflectionHelpers

class StateTest : AbstractBehaviorGraphTest() {
    @Test
    fun `initial state`() {
        // |> When we create a new state resource
        val sr1 = ext.state<Long>(1, "sr1")

        // |> It has an initial value
        assertEquals(1, sr1.value)
    }

    @Test
    fun updates() {
        // |> Given a state in the graph
        val sr1 = ext.state(1,"sr1")
        ext.addToGraphWithAction()

        // |> When it is updated
        sr1.updateWithAction(2)

        assertEquals(2, sr1.value)
        assertEquals(g.lastEvent, sr1.event)
    }

    @Test
    fun `filters duplicates`() {
        // |> Given a state in the graph
        val sr1 = ext.state<Long>(1, "sr1")
        ext.addToGraphWithAction()
        // |> When updated with same value and filtering on
        val entered = sr1.event
        sr1.updateWithAction(1)

        // |> Then update doesn't happen
        assertNotEquals(g.lastEvent, sr1.event)
        assertEquals(entered, sr1.event)
    }

    @Test
    fun `can override duplicate filter`() {
        // |> Given a state in the graph
        val sr1 = ext.state<Long>(1, "sr1");
        ext.addToGraphWithAction();

        // |> When updated with same value and filtering off
        val entered = sr1.event;
        g.action({
            sr1.updateForce(1);
        })

        // |> Then update does happen
        assertEquals(sr1.event, g.lastEvent)
    }

    @Test
    fun `can be a nullable state`() {
        // Motivation: nullable states are useful for modeling false/true with data
        // |> Given a nullable state
        val sr1 = ext.state<Int?>(null, "sr1")
        ext.addToGraphWithAction()

        // |> When updated
        sr1.updateWithAction(1)

        // |> Then it will have that new state
        assertEquals(1, sr1.value)

        // |> And updated to null
        sr1.updateWithAction(null)
        // |> Then it will have null state
        assertNull(sr1.value)
    }

    @Test
    fun `works in action`() {
        // |> Given a state
        val sr1 = ext.state<Int>(0, "sr1")
        ext.addToGraphWithAction()

        // |> When updated in action
        g.action({
            sr1.update(1)
        })

        // |> Then state is updated
        assertEquals(1, sr1.value)
    }
/*
    @Test
    fun `works as demand and supply`() {
        // |> Given state resources and behaviors
        val sr1 = State<Long>(ext, 0, "sr1")
        val sr2 = State<Long>(ext, 0, "sr2")
        var ran = false
        ext.makeBehavior(listOf(sr1), listOf(sr2)) {
            if (sr1.justUpdated) {
                sr2.update(1, false)
            }
        }
        ext.makeBehavior(listOf(sr2), null) {
            if (sr2.justUpdated) {
                ran = true
            }
        }
        ext.addToGraphWithAction()
        // |> When event is started
        sr1.updateWithAction(1, false)
        // |> Then subsequent behaviors are run
        assertTrue(ran)
    }

    @Test
    fun justChanged() {
        // |> Given a state resource
        val sr1 = State<Long>(ext, 0, "sr1")
        var changed = false
        var changedTo = false
        var changedFrom = false
        var changedToFrom = false
        ext.makeBehavior(listOf(sr1), null) {
            changed = sr1.justUpdated
            changedTo = sr1.justUpdatedTo(1)
            changedFrom = sr1.justUpdatedFrom(0)
            changedToFrom = sr1.justUpdatedToFrom(1, 0)
        }
        ext.addToGraphWithAction()
        // |> When it updates
        sr1.updateWithAction(1, false)
        // |> Then its justChangedMethods work
        assertTrue(changed)
        assertTrue(changedTo)
        assertTrue(changedFrom)
        assertTrue(changedToFrom)
        // and they don't work outside an event
        assertFalse(sr1.justUpdated)
    }

    @Test
    fun `trace tracks before and after values`() {
        // |> Given a behavior that updates a value
        val sr1 = State<Int>(ext, 0, "sr1")
        val mr1 = Moment<Unit>(ext, "mr1")
        var before: Int? = null
        var after: Int? = null
        var afterEntered: Event? = null
        ext.makeBehavior(listOf(mr1), listOf(sr1)) {
            if (mr1.justUpdated) {
                before = sr1.traceValue
                sr1.update(1, false)
                after = sr1.traceValue
                afterEntered = sr1.traceEvent
            }
        }
        val beforeEvent = sr1.event
        ext.addToGraphWithAction()
        // |> When trace is accessed before the update
        mr1.updateWithAction(Unit)
        // |> Then that value is the current value
        assertEquals(0, before)
        assertEquals(1, sr1.value)
        assertEquals(0, after)
        assertEquals(beforeEvent, afterEntered)
    }

    @Test
    fun `start state is transient after updates`() {
        // |> Given a state resource
        val sr1 = State<Long>(ext, 0, "sr1")
        val mr1 = Moment<Unit>(ext, "mr1")
        ext.makeBehavior(listOf(mr1), listOf(sr1)) {
            if (mr1.justUpdated) {
                sr1.update(1, false)
            }
        }
        ext.addToGraphWithAction()
        // |> When it is updated
        mr1.updateWithAction(Unit)
        // |> Then the start state is no longer available after the event
        assertNull(ReflectionHelpers.getField(sr1, "priorStateDuringEvent"))
    }

    @Test
    fun `can update state for non-supplied resource when adding`() {
        val sr1 = State<Long>(ext, 0, "sr1")
        var didRun = false
        ext.makeBehavior(listOf(sr1), null) {
            if (sr1.justUpdated) {
                didRun = true
            }
        }

        g.action("adding") {
            sr1.update(1, false)
            ext.addToGraph()
        }

        assertTrue(didRun)
    }

    //STATE Checks below
    @Test
    fun `check update state needs state resource to be part of graph`() {
        // |> Given a state resource not part of the graph
        val sr1 = State<Int>(ext, 0, "sr1")
        // |> When it is updated
        // |> Then an error is raised
        assertBehaviorGraphException { sr1.update(1, false) }
    }

    @Test
    fun `check supplied state is updated by supplier`() {
        // |> Given a supplied state resource
        val sr1 = State<Long>(ext, 0, "sr1")
        val mr1 = Moment<Unit>(ext, "mr1")
        ext.makeBehavior(listOf(mr1), listOf(sr1)) {}
        ext.makeBehavior(listOf(mr1), null) {
            if (mr1.justUpdated) {
                sr1.update(1, false)
            }
        }
        ext.addToGraphWithAction()
        // |> When it is updated by the wrong behavior
        // |> Then it should throw
        assertBehaviorGraphException { mr1.update(Unit) }
    }

    @Test
    fun `check measured state is update by push event`() {
        // |> Given a state resource that is not supplied
        val sr1 = State<Long>(ext, 0, "sr1")
        val mr1 = Moment<Unit>(ext, "mr1")
        ext.makeBehavior(listOf(mr1), null) {
            if (mr1.justUpdated) {
                sr1.update(1, false)
            }
        }
        ext.addToGraphWithAction()
        // |> When it is updated by a behavior
        // |> Then it should throw
        assertBehaviorGraphException { mr1.update(Unit) }
    }

    @Test
    fun `check update outside eventis an error`() {
        val sr1 = State<Long>(ext, 0, "mr1")
        ext.addToGraphWithAction()
        assertBehaviorGraphException { sr1.update(2, false) }
    }

 */
}
