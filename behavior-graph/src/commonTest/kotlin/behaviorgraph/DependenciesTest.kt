//
// Copyright Yahoo 2021
//
package behaviorgraph

import kotlin.test.*

class DependenciesTest : AbstractBehaviorGraphTest() {
    @Test
    fun aActivatesB() {
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
    fun behaviorActivatedAncePerEventLoop() {
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
    fun duplicatesAreFilteredOut() {
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
    fun orderingResourcesArentCalled() {
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
    fun checkCanUpdateResourceInADifferentExtent() {
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

    @Test
    fun toStringWorksInBehaviorsThatDontDemandMentionedResources() {
        // We want to be able to debug things
        // So toString methods shouldn't trigger any demand verification checks

        val s1 = ext.state(0)
        val m1 = ext.moment()
        val tm1 = ext.typedMoment<Int>()
        val m2 = ext.moment()

        ext.behavior()
            .demands(s1)
            .supplies(m2)
            .performs {
                m2.update()
            }

        var s1_string = ""
        var m1_string = ""
        var tm1_string = ""
        ext.behavior()
            .demands(m2)
            .performs {
                s1_string = s1.toString()
                m1_string = m1.toString()
                tm1_string = tm1.toString()
            }

        ext.addToGraphWithAction()
        ext.action {
            s1.update(1)
            m1.update()
            tm1.update(1)
        }

        assertTrue(s1_string.isNotEmpty())
        assertTrue(m1_string.isNotEmpty())
        assertTrue(tm1_string.isNotEmpty())
    }
}
