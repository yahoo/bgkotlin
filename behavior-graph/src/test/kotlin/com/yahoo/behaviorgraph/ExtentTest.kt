//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import kotlin.test.*

class ExtentTest : AbstractBehaviorGraphTest() {
    class TestExtentLocal(g: Graph) : Extent(g) {
        val r1 = this.state<Int>(0, "r1")
        var r2 = this.state<Int>(0, "custom_r2")
        val b1 = this.behavior().demands(r1).supplies(r2).runs {
            r2.update(r1.value * 2)
        }

        fun injectNumber(num: Int) {
            r1.updateWithAction(num)
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
        assertSame(e, e.b1.extent)
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

    @Test
    fun `added resource is updated on adding`() {
        val e = Extent(g)
        var runOnAdd = false
        e.behavior().demands(e.didAdd).runs {
            runOnAdd = true
        }
        e.addToGraphWithAction()

        assertTrue(runOnAdd)
    }

    //checks below
    @Test
    fun `check cannot add extent to graph multiple times`() {
        assertBehaviorGraphException { setupExt.addToGraphWithAction() }
    }

    @Test
    fun `check extent cannot be added to graph outside event`() {
        val e = TestExtentLocal(g)
        assertBehaviorGraphException {
            e.addToGraph()
        }
    }

    @Test
    fun `check extent cannot be removed from graph outside event`() {
        val e = TestExtentLocal(g)
        e.addToGraphWithAction()
        assertBehaviorGraphException {
            e.removeFromGraph()
        }
    }
}
