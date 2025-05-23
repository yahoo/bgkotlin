//
// Copyright Yahoo 2021
//
package behaviorgraph

import kotlin.test.*

class StateTest : AbstractBehaviorGraphTest() {
    @Test
    fun initialState() {
        // |> When we create a new state resource
        val sr1 = ext.state<Long>(1, "sr1")

        // |> It has an initial value
        assertEquals(1, sr1.value)
    }

    @Test
    fun updates() {
        // |> Given a state in the graph
        val sr1 = ext.state(1, "sr1")
        ext.addToGraphWithAction()

        // |> When it is updated
        sr1.updateWithAction(2)

        assertEquals(2, sr1.value)
        assertEquals(g.lastEvent, sr1.event)
    }

    @Test
    fun filtersDuplicates() {
        // |> Given a state in the graph
        val sr1 = ext.state<Long>(1, "sr1")
        ext.addToGraphWithAction()
        // |> When updated with same value and filtering on
        val entered = sr1.event
        sr1.updateWithAction(1)

        // |> Then update doesn't happen
        assertNotEquals(g.lastEvent, sr1.event)
        assertEquals(entered, sr1.event)
    }

    @Test
    fun canOverrideDuplicateFilter() {
        // |> Given a state in the graph
        val sr1 = ext.state<Long>(1, "sr1");
        ext.addToGraphWithAction();

        // |> When updated with same value and filtering off
        g.action {
            sr1.updateForce(1);
        }

        // |> Then update does happen
        assertEquals(sr1.event, g.lastEvent)
    }

    @Test
    fun canBeANullableState() {
        // Motivation: nullable states are useful for modeling false/true with data
        // |> Given a nullable state
        val sr1 = ext.state<Int?>(null, "sr1")
        ext.addToGraphWithAction()

        // |> When updated
        sr1.updateWithAction(1)

        // |> Then it will have that new state
        assertEquals(1, sr1.value)

        // |> And updated to null
        sr1.updateWithAction(null)
        // |> Then it will have null state
        assertNull(sr1.value)
    }

    @Test
    fun worksInAction() {
        // |> Given a state
        val sr1 = ext.state<Int>(0, "sr1")
        ext.addToGraphWithAction()

        // |> When updated in action
        g.action {
            sr1.update(1)
        }

        // |> Then state is updated
        assertEquals(1, sr1.value)
    }

    @Test
    fun worksAsDemandAndSupply() {
        // |> Given state resources and behaviors
        val sr1 = ext.state<Long>(0, "sr1")
        val sr2 = ext.state<Long>(0, "sr2")
        var ran = false
        ext.behavior()
            .supplies(sr2)
            .demands(sr1)
            .runs {
                sr2.update(1)
            }
        ext.behavior()
            .demands(sr2)
            .runs {
                ran = true
            }
        ext.addToGraphWithAction()

        // |> When event is started
        sr1.updateWithAction(1)

        // |> Then subsequent behaviors are run
        assertTrue(ran)
    }

    @Test
    fun justChanged() {
        // |> Given a state resource
        val sr1 = ext.state<Long>(0, "sr1")
        var changed = false
        var changedTo = false
        var changedFrom = false
        var changedToFrom = false
        ext.behavior()
            .demands(sr1)
            .runs {
                changed = sr1.justUpdated
                changedTo = sr1.justUpdatedTo(1)
                changedFrom = sr1.justUpdatedFrom(0)
                changedToFrom = sr1.justUpdatedToFrom(1, 0)
            }
        ext.addToGraphWithAction()

        // |> When it updates
        sr1.updateWithAction(1)

        // |> Then its justChangedMethods work
        assertTrue(changed)
        assertTrue(changedTo)
        assertTrue(changedFrom)
        assertTrue(changedToFrom)
        // and they don't work outside an event
        assertFalse(sr1.justUpdated)
    }

    @Test
    fun traceTracksBeforeAndAfterValues() {
        // |> Given a behavior that updates a value
        val sr1 = ext.state<Int>(0, "sr1")
        val mr1 = ext.moment("mr1")
        var before: Int? = null
        var after: Int? = null
        var afterEntered: Event? = null
        ext.behavior()
            .supplies(sr1)
            .demands(mr1)
            .runs {
                before = sr1.traceValue
                sr1.update(1)
                after = sr1.traceValue
                afterEntered = sr1.traceEvent
            }
        val beforeEvent = sr1.event
        ext.addToGraphWithAction()

        // |> When trace is accessed before the update
        mr1.updateWithAction()

        // |> Then that value is the current value
        assertEquals(0, before)
        assertEquals(1, sr1.value)
        assertEquals(0, after)
        assertEquals(beforeEvent, afterEntered)
    }

    @Test
    fun traceIsValueFromStartOfEventNotPreviousValue() {
        // |> Given a state resource in the graph
        val sr1 = ext.state(0)
        ext.addToGraphWithAction()

        // |> When it is updated multiple times in action (or behavior)
        var traceValue: Int? = null
        var traceEvent: Event? = null
        g.action {
            sr1.update(1)
            sr1.update(2)
            g.sideEffect {
                traceValue = sr1.traceValue
                traceEvent = sr1.traceEvent
            }
        }

        // |> Then trace is still the value from beginning of
        assertEquals(traceValue, 0)
        assertEquals(traceEvent, Event.InitialEvent)
    }

    @Test
    fun startStateIsTransientAfterUpdates() {
        // |> Given a state resource
        val sr1 = ext.state<Long>(0, "sr1")
        val mr1 = ext.moment("mr1")
        ext.behavior()
            .supplies(sr1)
            .demands(mr1)
            .runs {
                sr1.update(1)
            }
        ext.addToGraphWithAction()

        // |> When it is updated
        mr1.updateWithAction()

        // |> Then the start state is no longer available after the event
        assertNull(sr1.priorStateDuringEvent)
    }

    @Test
    fun canUpdateStateForNonSuppliedResourceWhenAdding() {
        val sr1 = ext.state<Long>(0, "sr1")
        var didRun = false
        ext.behavior()
            .demands(sr1)
            .runs {
                didRun = true
            }

        g.action {
            sr1.update(1)
            ext.addToGraph()
        }

        assertTrue(didRun)
    }

    @Test
    fun checkSuppliedStateIsUpdatedBySupplier() {
        // |> Given a supplied state resource
        val sr1 = ext.state<Long>(0, "sr1")
        val mr1 = ext.moment("mr1")
        ext.behavior()
            .supplies(sr1)
            .demands(mr1)
            .runs {}
        ext.behavior()
            .demands(mr1)
            .runs {
                sr1.update(1)
            }
        ext.addToGraphWithAction()

        // |> When it is updated by the wrong behavior
        // |> Then it should throw
        assertFails { mr1.update() }
    }

    @Test
    fun checkNonSuppliedStateIsUpdatedByAction() {
        // |> Given a state resource that is not supplied
        val sr1 = ext.state<Long>(0, "sr1")
        val mr1 = ext.moment("mr1")
        ext.behavior()
            .demands(mr1)
            .runs {
                sr1.update(1)
            }
        ext.addToGraphWithAction()

        // |> When it is updated by a behavior
        // |> Then it should throw
        assertFails { mr1.updateWithAction() }
    }

    @Test
    fun checkUpdateOutsideEventIsAnError() {
        val sr1 = ext.state<Long>(0, "mr1")
        ext.addToGraphWithAction()
        assertFails { sr1.update(2) }
    }

    @Test
    fun updateWhenSuppliedByAnotherBehaviorIsAnError() {
        val sr1 = ext.state<Long>(0, "sr1")
        val mr1 = ext.moment("mr1")
        ext.behavior().demands(mr1).runs {
            sr1.update(2)
        }
        ext.behavior().supplies(sr1).runs {
            sr1.update(3)
        }
        ext.addToGraphWithAction();

        assertFails {
            mr1.updateWithAction()
        }
    }

    @Test
    fun unsuppliedResourceThrowsIfNotFromAction() {
        val sr1 = ext.state<Long>(0, "sr1")
        val mr1 = ext.moment("mr1")
        ext.behavior().demands(mr1).runs {
            sr1.update(2)
        }
        ext.addToGraphWithAction()

        assertFails {
            mr1.update()
        }
    }

    @Test
    fun cannotAccessValueInsideBehaviorIfNotSupplyOrDemand() {
        val sr1 = ext.state(1)
        val sr2 = ext.state(1)
        val sr3 = ext.state(1)
        val sr4 = ext.state(1)
        val sr5 = ext.state(1)

        // |> Given resource that are supplied and demanded
        ext.behavior().demands(sr1).supplies(sr2).runs {
            sr1.value
            sr1.event
            sr1.justUpdated
            sr2.value
            sr2.event
            sr2.justUpdated
        }
        ext.addToGraphWithAction()

        // |> When they are accessed inside a behavior during an event
        // |> Then it will succeed
        sr1.updateWithAction(2)

        // |> And when they are accessed outside an event or behavior
        // |> Then it will succeed
        sr1.value
        sr1.event
        sr1.justUpdated

        // |> And when we access a non-supplied resource inside an action
        // |> Then it will succeed
        g.action {
            sr1.value
        }

        // |> But Given behaviors that access value, event, or justUpdated for a resource
        // that is not supplied or demanded
        val ext2 = TestExtent(g)
        ext.addChildLifetime(ext2)
        ext2.behavior().demands(sr3).runs {
            sr2.value
        }

        ext2.behavior()
            .demands(sr4)
            .runs {
                sr2.event
            }

        ext2.behavior().demands(sr5).runs {
            sr2.justUpdated
        }
        ext2.addToGraphWithAction();

        // |> Then it will fail
        assertFails {
            sr3.updateWithAction(2)
        }

        assertFails {
            sr4.updateWithAction(2)
        }

        assertFails {
            sr5.updateWithAction(2)
        }

        // |> And when we access a supplied resource from an action
        // |> Then it will fail
        assertFails {
            sr2.updateWithAction(2)
        }
    }

    @Test
    fun canObserveStateChanges() {
        // |> Given a state resource
        val sr1 = ext.state<Long>(0, "sr1")
        val sr2 = ext.state<Long>(0, "sr2")
        ext.behavior()
            .supplies(sr2)
            .demands(sr1)
            .runs {
                sr2.update(sr1.value * 2)
            }
        ext.addToGraphWithAction()


        // |> When we observe changes to the state resource
        var lastValue = 0L
        val observer = sr2.observeUpdates { newValue ->
            lastValue = newValue
        }

        // |> And we update the state resource
        sr1.updateWithAction(1)
        assertEquals(2L, lastValue)
        sr1.updateWithAction(2)
        assertEquals(4L, lastValue)

        observer.removeEarly()
        sr1.updateWithAction(3)
        assertEquals(4L, lastValue) // No change after observer removed
    }

    @Test
    fun canObserveBeforeExtentAdded() {
        // this will just add the behavior to the extent and will get added when extents add
        // the point is to not making observers strictly dependent on when the extent is added to the graph

        // |> Given a state resource
        val sr1 = ext.state<Long>(0, "sr1")
        val sr2 = ext.state<Long>(0, "sr2")
        ext.behavior()
            .supplies(sr2)
            .demands(sr1)
            .runs {
                sr2.update(sr1.value * 2)
            }
        // |> When we observe changes to the state resource
        var lastValue = 0L
        val observer = sr2.observeUpdates { newValue ->
            lastValue = newValue
        }
        // |> And we add the extent to the graph
        ext.addToGraphWithAction()
        // |> And we update the state resource
        sr1.updateWithAction(1)
        assertEquals(2L, lastValue)
    }
}
