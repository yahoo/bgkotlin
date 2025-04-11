//
// Copyright Yahoo 2021
//
package behaviorgraph

import kotlin.test.*

class DynamicGraphChangesTest : AbstractBehaviorGraphTest() {

    @Test
    fun canAddAndUpdateInTheSameEvent() {
        val r_x: State<Long> = ext.state(0, "r_x")
        ext.behavior().demands(r_a).supplies(r_x).runs {
            r_x.update(r_a.value * 2)
        }

        g.action {
            r_a.update(2)
            ext.addToGraph()
        }

        assertEquals(r_x.value, 4)
    }

    @Test
    fun behaviorCanAddExtent() {
        // given a behavior that adds a extent when something happens

        // -- this is behavior that does the work
        val ext2 = TestExtent(g)
        setupExt.addChildLifetime(ext2)
        ext2.behavior().demands(r_b).supplies(r_c).runs {
            r_c.update(r_b.value + 1)
        }

        // -- this behavior adds the extent on event happening
        ext.behavior().demands(r_a).runs {
            g.addExtent(ext2)
        }
        ext.addToGraphWithAction()

        assertEquals(r_c.event.sequence, 0)

        // when that something happens
        r_a.updateWithAction(1)

        // then that extent should be added
        r_b.updateWithAction(2)

        assertEquals(r_c.value, 3)
    }

    @Test
    fun activatedBehaviorsCanReorderIfDemandsChange() {
        var counter = 0
        var whenX = 0
        var whenY = 0

        // create two behaviors such that a comes before b and they both come after a reordering step
        // each one keeps track of when it ran relative to the other
        val reordering: State<Unit> = ext.state(Unit, "reordering")
        var x_out = ext.state(0, "x_out")
        val x_bhv: Behavior<*> = ext.behavior().demands(r_a, reordering).supplies(x_out).runs {
            whenX = counter
            counter = counter + 1
        }
        val y_out = ext.state(0, "y_out")
        val y_bhv: Behavior<*> = ext.behavior().demands(r_a, reordering).supplies(y_out).runs {
            whenY = counter
            counter = counter + 1
        }

        ext.behavior().demands(r_a).supplies(reordering).runs {
            x_bhv.setDynamicDemands(y_out)
            y_bhv.setDynamicDemands()
        }

        ext.addToGraphWithAction()

        // this sets them one way
        g.action {
            x_bhv.setDynamicDemands()
            y_bhv.setDynamicDemands(x_out)
        }

        // when event that activates re-demand behavior happens
        r_a.updateWithAction(2)

        // X should be 3 and Y should be 2 (they are 0 and 1 when they get added)
        assertTrue(whenX > whenY)
    }

    @Test
    fun removedExtentsRemoveComponentsFromGraph() {
        // given an added behavior
        ext.behavior().demands(r_a).supplies(r_b).runs {
            r_b.update(r_a.value + 1)
        }
        ext.addToGraphWithAction()

        // when its extent is removed and its previous demand is updated
        ext.removeFromGraphWithAction()
        r_a.updateWithAction(1)

        // then it should not get run
        assertEquals(r_b.value, 0)

        // and be removed
        assertNull(ext.addedToGraphWhen)
    }

    @Test
    fun removedBehaviorsAreRemovedFromForeignSubsequents() {
        // |> Given we have a behavior which has foreign and local demands
        val ext2 = TestExtent(g)
        ext.addChildLifetime(ext2)
        val demanded1 = ext.moment("demanded1")
        val demanded2 = ext2.moment("demanded2")
        val ext2behavior = ext2.behavior().demands(demanded1, demanded2).runs {
        }
        g.action {
            ext.addToGraph()
            ext2.addToGraph()
        }
        assertEquals(demanded1.subsequents.size, 1)

        // |> When its owning extent is removed
        ext2.removeFromGraphWithAction()

        // |> Then the foreign demand will have this behavior removed as a subsequent
        assertEquals(demanded1.subsequents.size, 0)
        // But the local demand won't remove it (for performance)
        assertEquals(demanded2.subsequents.size, 1)
        // |> And all demands will be removed
        assertNull(ext2behavior.demands)
    }

    @Test
    fun removedBehaviorsAreRemovedAsForeignSuppliers() {
        // |> Given we have a behavior which supplies both foreign and local resources
        val ext2 = TestExtent(g)
        ext.addChildLifetime(ext2)
        val supplied1 = ext.moment("supplied1")
        val supplied2 = ext2.moment("supplied2")
        val ext2behavior = ext2.behavior().supplies(supplied1, supplied2).runs {
        }
        g.action {
            ext.addToGraph()
            ext2.addToGraph()
        }

        // |> When its owning extent is removed
        ext2.removeFromGraphWithAction()

        // |> Then the foreign supply will have this behavior removed as a supplied by
        assertNull(supplied1.suppliedBy)
        // |> But the local supply won't remove it
        assertNotNull(supplied2.suppliedBy)
        // |> And all supplies will be removed from behavior
        assertNull(ext2behavior.supplies)
    }

    @Test
    fun activatedThenRemovedBehaviorsDontRun() {
        // given a behavior that is added
        val remover = ext.state(null, "y_out")

        val ext2: TestExtent = TestExtent(g)
        ext.addChildLifetime(ext2)
        val didRun: State<Boolean> = ext2.state(false, "didRun")
        ext2.behavior().demands(r_a, remover).supplies(didRun).runs {
            if (r_a.justUpdated) {
                didRun.update(true)
            }
        }
        ext.behavior().demands(r_a).supplies(remover).runs {
            ext2.removeFromGraph()
        }

        g.action {
            ext.addToGraph()
            ext2.addToGraph()
        }

        // when it is both activated and removed in the same event
        r_a.updateWithAction(1)

        // then it will not run
        assertFalse(didRun.value)
    }

    @Test
    fun canSupplyAResourceDifferentExtentAfterSubsequentAdded() {
        // ext has resource a and process that depends on it and then it is added
        val r_z: State<Long> = ext.state(0, "r_z")
        val r_y: State<Long> = ext.state(0, "r_y")
        ext.behavior().demands(r_y).supplies(r_z).runs {
            r_z.update(r_y.value)
        }
        ext.addToGraphWithAction()

        // then a extent is added that supplies it by a behavior, it could just pass along the value
        val ext2: TestExtent = TestExtent(g)
        ext.addChildLifetime(ext2)
        val r_x: State<Long> = ext2.state(0, "r_x")
        ext2.behavior().demands(r_x).supplies(r_y).runs {
            r_y.update(r_x.value)
        }
        ext2.addToGraphWithAction()

        // then update the trigger which should pass it along to the end
        r_x.updateWithAction(1)
        assertEquals(r_z.value, 1)
    }

    @Test
    fun updatingPostAddDemandsChangesThem() {
        val b1 = ext.behavior().runs {
        }
        ext.addToGraphWithAction()

        g.action {
            b1.setDynamicDemands(r_a)
        }

        assertTrue(b1.demands!!.contains(r_a))
    }

    @Test
    fun changingToDemandResourceThatAlreadyBeenUpdatedThisEventActivatesBehavior() {
        // |> Given we have a behavior that doesn't demand r_a
        var run = false
        val b1 = ext.behavior().runs {
            run = true
        }
        ext.behavior().demands(r_a).runs {
            b1.setDynamicDemands(r_a)
        }
        ext.addToGraphWithAction()

        // |> When we update the behavior to demand r_a in the same event that r_a has already run
        r_a.updateWithAction(1)

        // |> Then our behavior will activate
        assertTrue(run)
    }

    @Test
    fun setDynamicCanIncludeUndefined() {
        // NOTE: This makes it easier to include a set of resources on an
        // foreign extent that may not be there with nullish coalescing

        // |> Given a behavior with dynamic demands/supplies
        val r1 = ext.moment()
        val r2 = ext.moment()
        val r3 = ext.moment()
        var didRun = false
        ext.behavior()
            .dynamicDemands(r1) { _, demands ->
                demands.add(r2)
                demands.add(null)
            }
            .dynamicSupplies(r1) { _, supplies ->
                supplies.add(r3)
            }
            .runs {
                didRun = true
                r3.update()
            }
        ext.addToGraphWithAction()

        // |> When demands/supplies relink with undefined in the list of links
        r1.updateWithAction()
        r2.updateWithAction()

        // |> Then behavior should run as expected with undefined filtered out
        assertTrue(didRun)
    }


    @Test
    fun updatingPostAddSuppliesChangesThem() {
        val b1 = ext.behavior().runs {
        }
        ext.addToGraphWithAction()

        g.action {
            b1.setDynamicSupplies(r_a)
        }

        assertTrue(b1.supplies!!.contains(r_a))
    }

    @Test
    fun addingPostAddSupplyReordersActivatedBehaviors() {

        // first add a behavior that demands an unsupplied resource
        val r_y: State<Long> = ext.state(0, "r_y")
        val r_x: State<Long> = ext.state(0, "r_x")
        ext.behavior().demands(r_a, r_x).supplies(r_y).runs {
            if (r_x.justUpdated) {
                r_y.update(r_a.value)
            }
        }
        ext.addToGraphWithAction()

        // then add another behavior that (will) supply the resource
        // b_a behavior should be reordered to come after b_b
        val ext2: TestExtent = TestExtent(g)
        ext.addChildLifetime(ext2)
        val b_b = ext2.behavior().demands(r_a).runs {
            r_x.update(r_a.value)
        }
        ext2.addToGraphWithAction()

        // update the supply to accommodate
        g.action {
            b_b.setDynamicSupplies(r_x)
        }

        // when action initiates updates we should get them run in order
        r_a.updateWithAction(3)

        // if they don't get reordered then b_a will still run first since
        // both demand r_a which gets run. And that would be wrong because
        // b_a now is subsequent to b_b
        assertEquals(r_y.value, 3)
    }

    @Test
    fun changingSuppliedWillUnsupplyOldResources() {
        // |> Given we have a resource supplied by a behavior
        val m1 = ext.moment()
        val b1 = ext.behavior().runs {
            // do nothing
        }
        ext.addToGraphWithAction()
        ext.action {
            b1.setDynamicSupplies(m1)
        }
        assertNotNull(m1.suppliedBy)

        // |> When that behavior no longer supplies that original resource
        ext.action {
            b1.setDynamicSupplies(null)
        }

        // |> Then that resource should free to be supplied by another behavior
        assertNull(m1.suppliedBy)
    }

    @Test
    fun dynamicDemandsClauseUpdatesDemands() {
        // |> Given a behavior with dynamicDemands
        val m1 = ext.moment()
        val m2 = ext.moment()
        val m3 = ext.moment()
        var runCount = 0
        var relinkBehaviorOrder: Long = 0
        var behaviorOrder: Long = 0
        ext.behavior()
            .demands(m1)
            .dynamicDemands(m2) { _, demands ->
                relinkBehaviorOrder = ext.graph.currentBehavior!!.order
                demands.add(m3)
            }
            .runs {
                runCount++
                behaviorOrder = ext.graph.currentBehavior!!.order
            }
        ext.addToGraphWithAction()

        // |> When net yet demanded resource is updated
        m3.updateWithAction()

        // |> Then behavior is not run
        assertEquals(runCount, 0)

        // |> And when an update activates a relink and we update
        m2.updateWithAction()
        m3.updateWithAction()

        // |> Then behavior will be run
        assertEquals(runCount, 1)

        // |> And when we update original static resource
        m1.updateWithAction()

        // |> Then we expect behavior to also run
        assertEquals(runCount, 2)

        // |> Relink behavior should be a prior to its behavior
        // This ensures that relinking happens before behavior is run
        assertTrue(behaviorOrder > relinkBehaviorOrder)
    }

    @Test
    fun dynamicDemandsClauseWithNoStaticDemandsGetsOrderCorrect() {
        // |> Given a behavior with dynamic demands and no static demands
        val m1 = ext.moment()
        var relinkingOrder: Long? = null
        var behaviorOrder: Long? = null
        ext.behavior()
            .dynamicDemands(m1) { _, demands ->
                relinkingOrder = ext.graph.currentBehavior!!.order
                demands.add(m1)
            }
            .runs {
                behaviorOrder = ext.graph.currentBehavior!!.order
            }
        ext.addToGraphWithAction()

        // |> When resource causes activation on both
        g.action {
            m1.update()
        }

        // |> Expect the relinking behavior to come first
        assertTrue(behaviorOrder!! > relinkingOrder!!)
    }

    @Test
    fun dynamicSuppliesClauseUpdatesSupplies() {
        // |> Given a behavior with dynamicSupplies
        val m1 = ext.moment()
        val m2 = ext.moment()
        val m3 = ext.moment()

        var relinkingBehaviorOrder: Long = 0
        var behaviorOrder: Long = 0
        ext.behavior()
            .demands(m1)
            .dynamicSupplies(m2) { ctx, supplies ->
                relinkingBehaviorOrder = ctx.graph.currentBehavior!!.order
                supplies.add(m3)
            }
            .runs {
                behaviorOrder = ext.graph.currentBehavior!!.order
                m3.update()
            }
        ext.addToGraphWithAction()

        // |> When behavior activated before relink is activated
        // |> Then action should throw because behavior does not supply that resource
        assertFails {
            m1.update()  // cannot update unsupplied resource
        }

        // |> And when behavior has its supplies relinked
        m2.updateWithAction()

        // |> Then the behavior can activate and update the newly supplied resource
        g.action {
            m1.update()
            g.sideEffect {
                assertTrue(m3.justUpdated)
            }
        }

        // |> And behavior is ordered greater than the relinking behavior to ensure
        // it is updated before running
        assertTrue(behaviorOrder > relinkingBehaviorOrder)
    }

    @Test
    fun setDynamicDemandsRetainsStatics() {
        // |> Given a behavior with static demands
        val m1 = ext.moment()
        val m2 = ext.moment()
        var run = false
        val b1 = ext.behavior().demands(m1).runs {
            run = true
        }
        ext.addToGraphWithAction()

        // |> When dynamicDemands are set
        g.action {
            b1.setDynamicDemands(m2)
        }

        // |> Then behavior runs on newly added dynamicDemand
        m2.updateWithAction()
        assertTrue(run)

        // |> And when static demand is updated
        run = false
        m1.updateWithAction()

        // |> Then it will also update
        assertTrue(run)
    }

    @Test
    fun setDynamicSuppliesRetainsStatics() {
        // |> Given behavior that supplies one resource
        val m1 = ext.moment()
        val m2 = ext.moment()
        val m3 = ext.moment()
        val b1 = ext.behavior().demands(m1).supplies(m2).runs {
            m2.update()
            m3.update()
        }
        ext.addToGraphWithAction()

        // |> When I setDynamicSupplies to supply both and activate it
        g.action {
            b1.setDynamicSupplies(m3)
        }

        // |> Then behavior updates both successfully
        g.action {
            m1.update()
            g.sideEffect {
                assertTrue(m2.justUpdated)
                assertTrue(m3.justUpdated)
            }
        }
    }

    @Test
    fun updatingDemandsOnBehaviorThatHasAlreadyRunWillAffectFutureEvents() {
        // |> Given a behavior that demands one resource
        val m1 = ext.moment()
        val m2 = ext.moment()
        var run = false
        ext.behavior().demands(m1).runs {
            ext.graph.currentBehavior!!.setDynamicDemands(m2)
            run = true
        }
        ext.addToGraphWithAction()

        // It doesn't activate on other resource
        m2.updateWithAction()
        assertFalse(run)

        // |> When behavior updates demands on itself to include m2
        m1.updateWithAction()
        run = false

        // |> Then m2 updating will activate the behavior
        m2.updateWithAction()
        assertTrue(run)
    }

    @Test
    fun canRelinkDynamicDemandsAfterBehaviorRuns() {
        // |> Given a behavior with subsequent relinking that demands m2
        val m1 = ext.moment("m1")
        val m2 = ext.moment("m2")

        var didRun = false

        var relinkingOrder: Long? = null
        var behaviorOrder: Long? = null
        ext.behavior()
            .dynamicDemands(m1, ext.didAdd, relinkingOrder = RelinkingOrder.RelinkingOrderSubsequent) { _, demands ->
                if (ext.didAdd.justUpdated) {
                    demands.add(m2)
                } else {
                    relinkingOrder = ext.graph.currentBehavior!!.order
                }
            }
            .runs {
                behaviorOrder = ext.graph.currentBehavior!!.order
                didRun = true
            }
        ext.addToGraphWithAction()

        // |> When m2 is removed but still demanded
        g.action {
            m1.update()
            m2.update()
        }

        // |> Then behavior is run first
        assertTrue(didRun)
        assertTrue(relinkingOrder!! > behaviorOrder!!)

        // |> And when that resource is updated in a future event
        didRun = false
        m2.updateWithAction()

        // |> Then it is no longer demanded
        assertFalse(didRun)
    }

    @Test
    fun canRelinkDynamicSuppliesAfterBehaviorRuns() {
        // NOTE: As an example, pressing a button may say, "update state current item and move to next time"
        // So our behavior supplies the current item's state which we update when a button press
        // but afterwards we want to supply a one for the next button press.
        // Updating supplies after would apply in this situation.

        // |> Given a behavior that doesn't supply anything but will dynamically afterwards
        val s1 = ext.state<Long>(0)
        val m1 = ext.moment()
        ext.behavior()
            .dynamicSupplies(m1, relinkingOrder = RelinkingOrder.RelinkingOrderSubsequent) { _, supplies ->
                supplies.add(s1)
            }
            .demands(m1)
            .runs {
                if (g.currentBehavior!!.supplies?.contains(s1) ?: false) {
                    s1.update(1)
                }
            }
        ext.addToGraphWithAction()

        // |> When we update m1
        m1.updateWithAction()

        // |> Then we should get no output the first time (it was just resupplied after)
        assertEquals(s1.value, 0)

        // |> And when we update m1 again
        m1.updateWithAction()

        // |> Then we should expect the previous resupplying vals us update
        assertEquals(s1.value, 1)
    }

    @Test
    fun dynamicDemandsMustBeInTheGraph() {
        // |> Given an extent with foreign demands that haven't been added
        val ext1 = TestExtent(g)
        val r1 = ext1.moment()
        val ext2 = TestExtent(g)
        ext2.behavior().dynamicDemands(ext2.didAdd) { _, demands -> demands.add(r1) }.runs {}
        // |> When that extent is added
        // |> Then it should raise an error

        assertFails {
            val f = ext2.addToGraphWithAction()
        }
    }
}
