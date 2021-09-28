//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicGraphChangesTest : AbstractBehaviorGraphTest() {
    @Test
    fun `added behaviors are run`() {
        ext.makeBehavior(null, listOf(r_b)) {
            r_b.update(5, true)
        }

        ext.addToGraphWithAction()

        assertEquals(5L, r_b.value)
    }

    @Test
    fun `can add and update in the same event loop`() {
        val r_x = State(ext, 0L, "r_x")
        ext.makeBehavior(listOf(r_a), listOf(r_x)) {
            if (r_a.justUpdated) {
                r_x.update(r_a.value * 2, true)
            }
        }

        g.action("add") {
            r_a.update(2, true)
            ext.addToGraph()
        }

        assertEquals(4L, r_x.value)
    }

    @Test
    fun `behavior can add extent`() {
        // given a behavior that adds a new extent when something happens
        // -- this is new behavior that does the work
        val ext2 = TestExtent(g)
        ext2.makeBehavior(listOf(r_b), listOf(r_c)) {
            r_c.update(r_b.value + 1, true)
        }
        // -- this behavior adds the new extent on event happening
        ext.makeBehavior(listOf(r_a), null) {
            if (r_a.justUpdated) {
                g.addExtent(ext2)
            }
        }

        ext.addToGraphWithAction()

        assertEquals(0, r_c.event.sequence)
        // when that something happens
        r_a.updateWithAction(1, true)
        // then that new extent should be added
        r_b.updateWithAction(2, true)

        assertEquals(3L, r_c.value)
    }

    @Test
    fun `activated behaviors can reorder if demands change`() {
        var counter = 0
        var whenX = 0
        var whenY = 0
        // create two behaviors such that a comes before b and they both come after a reordering step
        // each one keeps track of when it ran relative to the other
        val reordering = State(ext, 0, "reordering")
        val x_out = State(ext, 0, "x_out")
        val x_bhv = ext.makeBehavior(listOf(r_a, reordering), listOf(x_out)) {
            whenX = counter
            counter += 1
        }
        val y_out = State(ext, 0, "y_out")
        val y_bhv = ext.makeBehavior(listOf(r_a, reordering, x_out), listOf(y_out)) {
            whenY = counter
            counter += 1
        }

        ext.makeBehavior(listOf(r_a), listOf(reordering)) {
            if (r_a.justUpdated) {
                x_bhv.setDemands(listOf(r_a, reordering, y_out))
                y_bhv.setDemands(listOf(r_a, reordering))
            }
        }

        ext.addToGraphWithAction()
        // when event that activates re-demand behavior happens
        r_a.updateWithAction(2, true)
        // X should be 3 and Y should be 2 (they are 0 and 1 when they get added)
        assertTrue(whenX > whenY)
    }

    @Test
    fun `removed extents remove components from graph`() {
        // given an added behavior
        val r_x = State(ext, 0, "r_x")
        val b_a = Behavior(ext, listOf(r_a), listOf(r_b)) {
            if (r_a.justUpdated) {
                r_b.update(r_a.value + 1, true)
            }
        }
        ext.addToGraphWithAction()
        // when its extent is removed and its previous demand is updated
        ext.removeFromGraphWithAction()
        r_a.updateWithAction(1, true)
        // then it should not get run
        assertEquals(0, r_b.value)
        // and be removed
        assertFalse(b_a.added)
        assertFalse(r_x.added)
        assertNull(ext.addedToGraphWhen)
    }

    //start friday
    @Test
    fun `activated then removed behaviors dont run`() {
        // given a behavior that is added
        val remover = State(ext, null, "y_out")
        val ext2 = TestExtent(g)
        val didRun = State(ext2, false, "didRun")
        ext2.makeBehavior(listOf(r_a, remover), listOf(didRun)) {
            if (r_a.justUpdated) {
                didRun.update(true, true)
            }
        }
        ext.makeBehavior(listOf(r_a), listOf(remover)) {
            if (r_a.justUpdated) {
                ext2.removeFromGraph()
            }
        }

        g.action("add") {
            ext.addToGraphWithAction()
            ext2.addToGraphWithAction()
        }
        // when it is both activated and removed in the same event loop
        r_a.updateWithAction(1, true)
        // then it will not run
        assertFalse(didRun.value)
    }

    @Test
    fun `can supply a resource by a behavior in a different extent after its subsequent is added`() {
        // ext has resource a and process that depends on it and then it is added
        val r_z = State(ext, 0, "r_z")
        val r_y = State(ext, 0, "r_y")
        ext.makeBehavior(listOf(r_y), listOf(r_z)) {
            if (r_y.justUpdated) {
                r_z.update(r_y.value, true)
            }
        }
        ext.addToGraphWithAction()
        // then a new extent is added that supplies it by a new behavior, it could just pass along the value
        val ext2 = TestExtent(g)
        val r_x = State(ext2, 0, "r_x")
        ext2.makeBehavior(listOf(r_x), listOf(r_y)) {
            if (r_x.justUpdated) {
                r_y.update(r_x.value, true)
            }
        }
        ext2.addToGraphWithAction()
        // then update the trigger which should pass it along to the end
        r_x.updateWithAction(1, true)
        assertEquals(1, r_z.value)
    }

    @Test
    fun `updating post-add demands changes them`() {
        val b1 = ext.makeBehavior(listOf(), listOf()) {}
        ext.addToGraphWithAction()

        g.action("update") {
            b1.setDemands(listOf(r_a))
        }

        assertTrue(b1.demands!!.contains(r_a))
    }

    @Test
    fun `updating post-add supplies changes them`() {
        val b1 = ext.makeBehavior(listOf(), listOf()) {}
        ext.addToGraphWithAction()

        g.action("update") {
            b1.setSupplies(listOf(r_a))
        }

        assertTrue(b1.supplies!!.contains(r_a))
    }

    @Test
    fun `adding a post-add supply will reorder activated behaviors`() {
        // first add a behavior that demands an unsupplied resource
        val r_y = State(ext, 0L, "r_y")
        val r_x = State(ext, 0L, "r_x")
        ext.makeBehavior(listOf(r_a, r_x), listOf(r_y)) {
            if (r_x.justUpdated) {
                r_y.update(r_a.value, true)
            }
        }
        ext.addToGraphWithAction()
        // then add another behavior that (will) supply the resource
        // b_a behavior should be reordered to come after b_b
        val ext2 = TestExtent(g)
        val b_b = ext2.makeBehavior(listOf(r_a), null) {
            if (r_a.justUpdated) {
                r_x.update(newValue = r_a.value, changesOnly = true)
            }
        }
        ext2.addToGraphWithAction()
        // update the supply to accommodate
        g.action("supply r_x") {
            b_b.setSupplies(listOf(r_x))
        }
        // when action initiates updates we should get them run in order
        r_a.updateWithAction(newValue = 3, changesOnly = true)
        // if they don't get reordered then b_a will still run first since
        // both demand r_a which gets run. And that would be wrong because
        // b_a now is subsequent to b_b
        assertEquals(3, r_y.value)
    }
}
