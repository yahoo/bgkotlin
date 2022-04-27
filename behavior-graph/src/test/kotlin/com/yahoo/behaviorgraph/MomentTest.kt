//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MomentTest : AbstractBehaviorGraphTest() {
    /*
    @Test
    fun `moment happens`() {
        // |> Given a moment in the graph
        val mr1 = Moment<Unit>(ext, "mr1")
        var afterUpdate = false
        ext.makeBehavior(listOf(mr1), null) {
            if (mr1.justUpdated) {
                afterUpdate = true
            }
        }
        ext.addToGraphWithAction()
        // |> When it is read in the graph (and was not updated)
        var beforeUpdate = false
        var happenedEvent: Event? = null
        ext.action("initial") {
            beforeUpdate = mr1.justUpdated
            mr1.update(Unit)
            happenedEvent = ext.graph.currentEvent
        }
        // |> Then it didn't happen
        assertFalse(beforeUpdate)
        // |> And when it updates
        // |> Then it did
        assertTrue(afterUpdate)
        // |> And outside an event loop
        // |> It does not happen
        assertFalse(mr1.justUpdated)
        // |> And event stays the same from when it last happened
        assertEquals(happenedEvent, mr1.event)
    }

    @Test
    fun `can have data`() {
        // Given a moment with data
        val mr1 = Moment<Int>(ext, "mr1")
        var afterUpdate: Int? = null
        ext.makeBehavior(listOf(mr1), null) {
            if (mr1.justUpdated) {
                afterUpdate = mr1.value
            }
        }
        ext.addToGraphWithAction()
        // |> When it happens
        mr1.updateWithAction(1)
        // |> Then the data is visible in subsequent behaviors
        assertEquals(1, afterUpdate)
        // but is an Exception outside event loop
        assertBehaviorGraphException { mr1.value }
    }

    @Test
    fun `non-supplied moment can happen when adding`() {
        val mr1 = Moment<Unit>(ext, "mr1")
        var didRun = false
        ext.makeBehavior(listOf(mr1), null) {
            if (mr1.justUpdated) {
                didRun = true
            }
        }

        g.action("adding") {
            mr1.update(Unit)
            ext.addToGraph()
        }
        assertTrue(didRun)
    }

    //checks below
    @Test
    fun `check happen requires graph`() {
        // |> Given a moment resource not part of the graph
        val mr1 = Moment<Unit>(ext, "mr1")
        // |> When it is updated
        // |> Then an error is raised
        assertBehaviorGraphException { mr1.update(Unit) }
    }

    @Test
    fun `check supplied moment catches wrong updater`() {
        // |> Given a supplied state resource
        val mr1 = Moment<Unit>(ext, "mr1")
        val mr2 = Moment<Unit>(ext, "mr2")
        ext.makeBehavior(listOf(mr1), listOf(mr2)) {
        }
        ext.makeBehavior(listOf(mr1), null) {
            if (mr1.justUpdated) {
                mr2.update(Unit)
            }
        }
        ext.addToGraphWithAction()
        // |> When it is updated by the wrong behavior
        // |> Then it should throw
        assertBehaviorGraphException { mr1.update(Unit) }
    }

    @Test
    fun `check measured moment catches wrong updater`() {
        // |> Given a measured moment resource
        val mr1 = Moment<Unit>(ext, "mr1")
        val mr2 = Moment<Unit>(ext, "mr2")
        ext.makeBehavior(listOf(mr1), null) {}
        ext.makeBehavior(listOf(mr1), null) {
            if (mr1.justUpdated) {
                mr2.update(Unit)
            }
        }
        ext.addToGraphWithAction()
        // |> When it is updated by the wrong behavior
        // |> Then it should throw
        assertBehaviorGraphException { mr1.update(Unit) }
    }

    @Test
    fun `check moment happens outside event loop is an error`() {
        val mr1 = Moment<Unit>(ext, "mr1")
        ext.addToGraphWithAction()
        assertBehaviorGraphException { mr1.update(Unit) }
    }

     */
}
