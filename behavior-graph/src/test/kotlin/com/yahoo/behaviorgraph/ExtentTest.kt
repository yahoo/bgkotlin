//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtentTest : AbstractBehaviorGraphTest() {
    class TestExtentLocal(g: Graph) : Extent<TestExtentLocal>(g) {
        val r1 = State<Int>(this, 0, "r1")
        var r2 = State<Int>(this, 0, "custom_r2")

        init {
            r2.debugName = "custom_r2"
        }

        val b1 = Behavior(this, listOf(r1), listOf(r2)) {
            if (r1.justUpdated) {
                r2.update(r1.value * 2, true)
            }
        }

        fun injectNumber(num: Int) {
            this.r1.updateWithAction(num, true)
        }
    }

    @Test
    fun `gets class name`() {
        val e = TestExtentLocal(g)
        assertEquals("TestExtentLocal", e.debugName)
    }

    @Test
    fun `contained components picked up`() {
        val e = TestExtentLocal(g)
        e.addToGraphWithAction()

        assertSame(g, e.r1.graph)
        assertTrue(e.r1.added)
        assertSame(e, e.b1.extent)
        assertTrue(e.b1.added)
        assertEquals(0, e.r2.value)

        e.injectNumber(2)

        assertEquals(2, e.r1.value)
        assertEquals(4, e.r2.value)
    }

    @Test
    fun `contained components named if needed`() {
        val e = TestExtentLocal(g)
        e.addToGraphWithAction()
        // property names
        assertEquals("r1", e.r1.debugName)
        // custom name not overridden
        assertEquals("custom_r2", e.r2.debugName)
    }

    //checks below
    @Test
    fun `check cannot add extent to graph multiple times`() {
        assertBehaviorGraphException { setupExt.addToGraphWithAction() }
    }

    @Test
    fun `check actions on unadded extents are errors`() {
        val e = TestExtent(g)
        assertBehaviorGraphException { e.action("impulse1") {} }
        assertBehaviorGraphException { e.actionAsync("impulse2") {} }
    }
}
