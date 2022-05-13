//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import kotlin.test.*

class DependenciesTest : AbstractBehaviorGraphTest() {
    @Test
    fun `a Activates b`() {
        ext.behavior()
            .supplies(r_b)
            .demands(r_a)
            .runs {
                r_b.update(2 * r_a.value)
            }
        ext.addToGraphWithAction()
        r_a.updateWithAction(1)

        assertEquals(2L, r_b.value)
        assertEquals(r_b.event, r_a.event)
    }

    @Test
    fun `behavior activated once per event loop`() {
        var called = 0
        ext.behavior()
            .supplies(r_c)
            .demands(r_a, r_b)
            .runs {
                called += 1
            }
        ext.addToGraphWithAction()

        g.action {
            r_a.update(1)
            r_b.update(2)
        }

        assertEquals(1, called) //once for initial add and once for adds
    }

    @Test
    fun `duplicates are filtered out`() {
        val b1 = ext.behavior()
            .supplies(r_b, r_b)
            .demands(r_a, r_a)
            .runs {}
        ext.addToGraphWithAction()

        assertEquals(1, b1.demands!!.size)
        assertEquals(1, b1.supplies!!.size)
        assertEquals(1, r_a.subsequents.size)
    }

    @Test
    fun`ordering resources arent called`() {
        // |> Given a behavior with an ordering demand
        var run = false
        ext.behavior().demands(r_a, r_b.order).runs {
            run = true
        }
        ext.addToGraphWithAction()

        // |> When that demand is updated
        r_b.updateWithAction(1)

        // |> Then that behavior doesn't run
        assertFalse(run)
    }

    @Test
    fun `check can update resource in a different extent`() {
        val parentExt = TestExtent(g)
        val ext2 = TestExtent(g)
        parentExt.addChildLifetime(ext2)

        val parent_r: State<Long> = parentExt.state(0, "parent_r")
        val parent_r2: State<Long> = parentExt.state(0, "parent_r2")
        val ext2_r1: State<Long> = ext2.state(0, "ext2_r1")

        parentExt.behavior()
            .supplies(parent_r2)
            .demands(parent_r)
            .runs {
                parent_r2.update(parent_r.value)
            }

        ext2.behavior()
            .supplies(parent_r)
            .demands(ext2_r1)
            .runs {
                parent_r.update(ext2_r1.value)
            }

        parentExt.addToGraphWithAction()
        ext2.addToGraphWithAction()

        g.action("update ext2_r1") {
            ext2_r1.update(33)
        }

        assertEquals(33L, parent_r2.value)
    }
}
