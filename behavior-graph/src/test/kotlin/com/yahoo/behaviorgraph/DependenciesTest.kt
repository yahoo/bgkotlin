//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import org.junit.Assert.assertEquals
import org.junit.Test

class DependenciesTest: AbstractBehaviorGraphTest() {
    /*
    @Test
    fun `a Activates b`() {
        ext.makeBehavior(listOf(r_a), listOf(r_b)) {
            if (r_a.justUpdated) {
                r_b.update(2 * r_a.value, false)
            }
        }
        ext.addToGraphWithAction()
        r_a.updateWithAction(1, false)

        assertEquals(2L, r_b.value)
        assertEquals(r_b.event, r_a.event)
    }

    @Test
    fun `behavior activated once per event loop`() {
        var called = 0
        ext.makeBehavior(listOf(r_a, r_b), listOf(r_c)) {
            called += 1
        }

        ext.addToGraphWithAction()

        g.action("test") {
            r_a.update(1, true)
            r_b.update(2, true)
        }

        assertEquals(2, called) //once for initial add and once for adds
    }

    @Test
    fun `duplicates are filtered out`() {
        val b1 = ext.makeBehavior(listOf(r_a, r_a), listOf(r_b, r_b)) {}
        ext.addToGraphWithAction()

        assertEquals(1, b1.demands!!.size)
        assertEquals(1, b1.supplies!!.size)
        assertEquals(1, r_a.subsequents.size)
    }

    @Test
    fun `check can update resource in a different extent`() {
        val parentExt = TestExtent(g)
        val ext2 = TestExtent(g)
        val parent_r: State<Long> = State(parentExt, 0, "parent_r")
        val parent_r2: State<Long> = State(parentExt, 0, "parent_r2")
        val ext2_r1: State<Long> = State(ext2, 0, "ext2_r1")

        parentExt.makeBehavior(listOf(parent_r), listOf(parent_r2)) {
            parent_r2.update(parent_r.value, false)
        }

        ext2.makeBehavior(listOf(ext2_r1), listOf(parent_r)) {
            parent_r.update(ext2_r1.value, false)
        }

        ext2.addToGraphWithAction()
        parentExt.addToGraphWithAction()

        g.action("update ext2_r1") {
            ext2_r1.update(33, false)
        }

        assertEquals(33L, parent_r2.value)
    }
*/
}
