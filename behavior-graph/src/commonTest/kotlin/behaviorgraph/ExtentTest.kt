//
// Copyright Yahoo 2021
//
package behaviorgraph

import kotlin.test.*

class ExtentTest : AbstractBehaviorGraphTest() {
    class TestExtentLocal(g: Graph) : TestExtent(g) {
        val r1 = this.state<Int>(0)
        var r2 = this.state<Int>(0, "custom_r2")
        val b1 = this.behavior().demands(r1).supplies(r2).runs {
            r2.update(r1.value * 2)
        }

        fun injectNumber(num: Int) {
            r1.updateWithAction(num)
        }
    }

    @Test
    fun getsClassName() {
        val e = TestExtentLocal(g)
        assertEquals("TestExtentLocal", e.debugName)
    }

    @Test
    fun containedComponentsPickedUp() {
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
    fun containedComponentsNamedIfNeeded() {
        val e = TestExtentLocal(g)
        e.addToGraphWithAction()

        // we use regex because kotlin name mangling modifies the field name
        // when converting to js

        // property names
        assertTrue(e.r1.debugName?.contains(Regex("r1")) == true)
        // custom name not overridden
        assertTrue(e.r2.debugName?.contains(Regex("custom_r2")) == true)
    }

    @Test
    fun automaticNamingCanBeDisabled() {
        g.automaticResourceNaming = false
        val e = TestExtentLocal(g)
        e.addToGraphWithAction()

        // won't get automatic name
        assertEquals(null, e.r1.debugName)
        // custom name still works
        assertEquals("custom_r2", e.r2.debugName)
    }

    @Test
    fun addedResourceIsUpdatedOnAdding() {
        val e = TestExtent(g)
        var runOnAdd = false
        e.behavior().demands(e.didAdd).runs {
            runOnAdd = true
        }
        e.addToGraphWithAction()

        assertTrue(runOnAdd)
    }

    //checks below
    @Test
    fun checkCannotAddExtentToGraphMultipleTimes() {
        assertFails { setupExt.addToGraphWithAction() }
    }

    @Test
    fun checkExtentCannotBeAddedToGraphOutsideEvent() {
        val e = TestExtentLocal(g)
        assertFails {
            e.addToGraph()
        }
    }

    @Test
    fun checkExtentCannotBeRemovedFromGraphOutsideEvent() {
        val e = TestExtentLocal(g)
        e.addToGraphWithAction()
        assertFails {
            e.removeFromGraph()
        }
    }

    class NonSubclass(g: Graph) {
        val extent = Extent<NonSubclass>(g, this)
        val r1 = extent.state<Int>(0)
        var r2 = extent.state<Int>(0)

        init {
            extent.behavior().demands(r1).supplies(r2).runs {
                r2.update(r1.value * 2)
            }
        }

        fun injectNumber(num: Int) {
            r1.updateWithAction(num)
        }
    }

    @Test
    fun nonExtentSubclassAlsoWorks() {
        val nonSubclass = NonSubclass(g)
        nonSubclass.extent.addToGraphWithAction()
        nonSubclass.injectNumber(2)
        assertEquals(nonSubclass.r2.value, 4)
    }

    @Test
    fun extentMethodsReturnContextObjects() {
        val nonSubclass = NonSubclass(g)
        // add a behavior to test that nonSubclass is the one that's run
        nonSubclass.extent.behavior()
            .demands(nonSubclass.extent.didAdd)
            .runs {
                assertEquals(it, nonSubclass)
                it.extent.sideEffect {
                    assertEquals(it, nonSubclass)
                    it.extent.action {
                        assertEquals(it, nonSubclass)
                    }
                    it.extent.action {
                        assertEquals(it, nonSubclass)
                    }
                }
            }
        nonSubclass.extent.addToGraphWithAction()
    }

    @Test
    fun contextObjectNamesItsResources() {
        val nonSubclass = NonSubclass(g)
        nonSubclass.extent.addToGraphWithAction()

        // we use regex because kotlin name mangling modifies the field name
        // when converting to js
        assertEquals(nonSubclass.r1.debugName?.contains(Regex("r1")), true)
        assertEquals(nonSubclass.r2.debugName?.contains(Regex("r2")), true)
    }
}
