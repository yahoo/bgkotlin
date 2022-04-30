//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MomentTest : AbstractBehaviorGraphTest() {
    @Test
    fun `moment happens`() {
        // |> Given a moment in the graph
        val mr1 = ext.moment("mr1")
        var afterUpdate = false
        ext.behavior()
            .demands(mr1)
            .runs {
                afterUpdate = true
            }
        ext.addToGraphWithAction()

        // |> When it is read in the graph (and was not updated)
        var beforeUpdate = false
        var happenedEvent: Event? = null
        ext.action("initial") {
            beforeUpdate = mr1.justUpdated
            mr1.update()
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
        val mr1 = ext.typedMoment<Int>("mr1")
        var afterUpdate: Int? = null
        var updatedToOne: Boolean = false
        ext.behavior()
            .demands(mr1)
            .runs {
                afterUpdate = mr1.value
                updatedToOne = mr1.justUpdatedTo(1)
            }
        ext.addToGraphWithAction()

        // |> When it happens
        mr1.updateWithAction(1)

        // |> Then the data is visible in subsequent behaviors
        assertEquals(1, afterUpdate)
        assertTrue(updatedToOne)
        // but is an Exception outside event loop
        assertBehaviorGraphException { mr1.value }
    }

    @Test
    fun `non-supplied moment can happen when adding`() {
        val mr1 = ext.moment("mr1")
        var didRun = false
        ext.behavior()
            .demands(mr1)
            .runs {
                didRun = true
            }

        g.action("adding") {
            mr1.update()
            ext.addToGraph()
        }
        assertTrue(didRun)
    }

    //checks below
    @Test
    fun `check happen requires graph`() {
        // |> Given a moment resource not part of the graph
        val mr1 = ext.moment("mr1")
        // |> When it is updated
        // |> Then an error is raised
        assertBehaviorGraphException { mr1.update() }
    }

    @Test
    fun `check supplied moment catches wrong updater`() {
        // |> Given a supplied state resource
        val mr1 = ext.moment("mr1")
        val mr2 = ext.moment("mr2")
        ext.behavior()
            .supplies(mr2)
            .demands(mr1)
            .runs {
            }
        ext.behavior()
            .demands(mr1)
            .runs {
                mr2.update()
            }
        ext.addToGraphWithAction()

        // |> When it is updated by the wrong behavior
        // |> Then it should throw
        assertBehaviorGraphException { mr1.update() }
    }

    @Test
    fun `check non-supplied moment catches wrong updater`() {
        // |> Given a measured moment resource
        val mr1 = ext.moment("mr1")
        val mr2 = ext.moment("mr2")
        ext.behavior()
            .demands(mr1)
            .runs {}
        ext.behavior()
            .demands(mr1)
            .runs {
                mr2.update()
            }
        ext.addToGraphWithAction()

        // |> When it is updated by the wrong behavior
        // |> Then it should throw
        assertBehaviorGraphException { mr1.update() }
    }

    @Test
    fun `check moment happens outside event loop is an error`() {
        val mr1 = ext.moment("mr1")
        ext.addToGraphWithAction()
        assertBehaviorGraphException { mr1.update() }
    }

    @Test
    fun`cannot access value inside behavior if not supply or demand`() {
        val mr1 = ext.typedMoment<Int>()
        val mr2 = ext.typedMoment<Int>()
        val mr3 = ext.moment()
        val mr4 = ext.moment()
        val mr5 = ext.moment()

        // |> Given resource that are supplied and demanded
        ext.behavior().demands(mr1).supplies(mr2).runs {
            mr1.value
            mr1.event
            mr1.justUpdated
            mr2.event
            mr2.justUpdated
        }
        ext.addToGraphWithAction()

        // |> When they are accessed inside a behavior during an event
        // |> Then it will succeed
        mr1.updateWithAction(1)

        // |> And when they are accessed outside an event or behavior
        // |> Then it will succeed
        mr1.event
        mr1.justUpdated

        // |> And when we access a non-supplied resource inside an action
        // |> Then it will succeed
        g.action {
            mr1.event
            mr1.justUpdated
        }

        // |> But Given behaviors that access value, event, or justUpdated for a resource
        // that is not supplied or demanded
        val ext2 = Extent(g)
        ext.addChildLifetime(ext2)
        ext2.behavior().demands(mr3).runs {
            mr2.value
        }
        ext2.behavior().demands(mr4).runs {
            mr2.event
        }
        ext2.behavior().demands(mr5).runs {
            mr2.justUpdated
        }
        ext2.addToGraphWithAction()

        // |> Then it will fail
        assertBehaviorGraphException {
            mr3.updateWithAction()
        }
        assertBehaviorGraphException {
            mr4.updateWithAction()
        }
        assertBehaviorGraphException {
            mr5.updateWithAction()
        }

        // |> And when we access a supplied resource from an action
        // |> Then it will fail
        assertBehaviorGraphException {
            mr2.updateWithAction(2)
        }
    }

}
