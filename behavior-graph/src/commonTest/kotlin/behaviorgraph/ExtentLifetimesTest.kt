package behaviorgraph

import kotlin.test.*

class ExtentLifetimesTest : AbstractBehaviorGraphTest() {

    @Test
    fun haveSameLifetime() {
        // |> Given two extents
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)

        // |> When they are unified
        ext1.unifyLifetime(ext2)

        // |> Then they will have same lifetime
        assertEquals(ext1.lifetime, ext2.lifetime)
        assertTrue(ext1.lifetime!!.extents.contains(ext1))
        assertTrue(ext1.lifetime!!.extents.contains(ext2))
    }

    @Test
    fun addedInReverseHaveSameLifetime() {
        // |> Given two extents
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)

        // |> When unified in reverse order
        ext2.unifyLifetime(ext1)

        // |> The will have same lifetime
        assertEquals(ext1.lifetime, ext2.lifetime)
        assertTrue(ext1.lifetime!!.extents.contains(ext1))
        assertTrue(ext1.lifetime!!.extents.contains(ext2))
    }

    @Test
    fun mustBeEstablishedBeforeAdding() {
        // |> Given two extents
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)

        // |> When unified after adding one extent
        // |> Then it will throw an error
        assertFails {
            g.action {
                ext1.addToGraph()
                ext2.unifyLifetime(ext1)
                ext2.addToGraph()
            }
        }
    }

    @Test
    fun mergedOtherUnified() {
        // |> Given two sets of unified lifetimes
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        ext1.unifyLifetime(ext2)
        val ext3 = TestExtent(g)
        val ext4 = TestExtent(g)
        ext3.unifyLifetime(ext4)

        // |> When one from each is unified
        ext1.unifyLifetime(ext3)

        // |> Then all 4 become unified
        assertEquals(ext2.lifetime, ext4.lifetime)
        assertTrue(ext1.lifetime!!.extents.contains(ext1))
        assertTrue(ext1.lifetime!!.extents.contains(ext2))
        assertTrue(ext1.lifetime!!.extents.contains(ext3))
        assertTrue(ext1.lifetime!!.extents.contains(ext4))
    }

    @Test
    fun canLinkAsStaticDemands() {
        // |> Given two unified lifetime extents with a foreign supply and demand
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        val r1 = ext1.moment()
        val r2 = ext1.moment()
        ext2.behavior()
            .supplies(r2)
            .demands(r1)
            .runs {}
        ext1.unifyLifetime(ext2)

        // |> When they are added
        // |> Then it should be allowed
        assertNoThrow {
            g.action {
                ext1.addToGraph()
                ext2.addToGraph()
            }

        }
    }

    @Test
    fun independentLifetimesCannotLinkAsStaticDemands() {
        // |> Given two independent with foreign demand
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        val r1 = ext1.moment()
        ext2.behavior()
            .demands(r1)
            .runs {}

        // |> When they are added
        // |> Then it should raise an error
        assertFails {
            g.action {
                ext1.addToGraph()
                ext2.addToGraph()
            }
        }
    }

    @Test
    fun independentLifetimesCannotLinkAsStaticSupplies() {
        // |> Given two independent extents with a foreign supply
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        val r1 = ext1.moment()
        ext2.behavior()
            .supplies(r1)
            .runs {}

        // |> When they are both added
        // |> Then it should raise an error
        assertFails {
            g.action {
                ext1.addToGraph()
                ext2.addToGraph()
            }
        }
    }

    @Test
    fun canRemoveEachOther() {
        // |> Given two unified extents
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        ext1.unifyLifetime(ext2)
        g.action {
            ext1.addToGraph()
            ext2.addToGraph()
        }

        // |> Then we remove one with flag for removing contained lifetimes
        g.action {
            ext1.removeFromGraph(ExtentRemoveStrategy.ContainedLifetimes)
        }

        // |> Then unified are also removed
        assertNull(ext2.addedToGraphWhen)
    }

    @Test
    fun cannotBeAddedBefore() {
        // |> Given parent extent
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        ext1.addChildLifetime(ext2)

        // |> When child is added first
        // |> Then it should raise an error
        assertFails {
            ext2.addToGraphWithAction()
        }
    }

    @Test
    fun canBeAddedSimultaneously() {
        // |> Given parent extent
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        ext1.addChildLifetime(ext2)

        // |> When they are added simultaneously
        // |> Then it should be fine
        assertNoThrow {
            g.action {
                ext1.addToGraph()
                ext2.addToGraph()
            }
        }
    }

    @Test
    fun canEstablishChildRelationshipInSubsequentEvents() {
        // |> Given an added extent
        val ext1 = TestExtent(g)
        ext1.addToGraphWithAction()

        // |> When we create and set a child relationship in a subsequent event
        // |> Then it should be fine
        val ext2 = TestExtent(g)
        assertNoThrow {
            ext1.addChildLifetime(ext2)
            ext2.addToGraphWithAction()
        }
    }

    @Test
    fun canSupplyAndDemandUpLifetimes() {
        // |> Given parent relationship with links up to parent
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        ext1.addChildLifetime(ext2)
        val r1 = ext1.moment()
        val r2 = ext1.moment()
        ext2.behavior().supplies(r2).demands(r1).runs {}
        ext1.addToGraphWithAction()

        // |> When we add child in subsequent event
        // |> Then it should be fine
        assertNoThrow {
            ext2.addToGraphWithAction()
        }
    }

    @Test
    fun cannotStaticDemandDownLifetimes() {
        // NOTE: Child lifetimes can be less so demands may no longer
        // exist. They must be dynamic

        // |> Given a parent relationship with demands in child
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        ext1.addChildLifetime(ext2)
        val r1 = ext2.moment()
        ext1.behavior().demands(r1).runs {}

        // |> When they are added
        // |> Then it should raise an error
        assertFails {
            g.action {
                ext1.addToGraph()
                ext2.addToGraph()
            }
        }
    }

    @Test
    fun cannotStaticSupplyDownLifetimes() {
        // |> Given a parent relationship with supply in child
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        ext1.addChildLifetime(ext2)
        val r1 = ext2.moment()
        ext1.behavior().supplies(r1).runs {}

        // |> When they are added
        // |> Then it should raise an error
        assertFails {
            g.action {
                ext1.addToGraph()
                ext2.addToGraph()
            }
        }
    }

    @Test
    fun canRemoveWithParent() {
        // |> Given parent lifetimes
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        val ext3 = TestExtent(g)
        ext1.addChildLifetime(ext2)
        ext1.addChildLifetime(ext3)
        g.action {
            ext1.addToGraph()
            ext2.addToGraph()
            ext3.addToGraph()
        }
        // |> When parent is removed with containedLifetime strategy
        // |> Then children (and their unified) are removed
        ext1.removeFromGraphWithAction(ExtentRemoveStrategy.ContainedLifetimes)
        assertNull(ext2.addedToGraphWhen)
        assertNull(ext3.addedToGraphWhen)
    }

    @Test
    fun mergeChildrenOfUnified() {
        // NOTE: unified get the same lifetime, so we need to retain
        // all children if they've already been defined.

        // |> Given two lifetimes with children
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        ext1.addChildLifetime(ext2)
        val ext3 = TestExtent(g)
        val ext4 = TestExtent(g)
        ext3.addChildLifetime(ext4)

        // |> When those lifetimes are merged
        ext1.unifyLifetime(ext3)

        // |> Then the children are merged
        assertTrue(ext1.lifetime!!.children!!.contains(ext2.lifetime))
        assertTrue(ext1.lifetime!!.children!!.contains(ext4.lifetime))
        assertTrue(ext3.lifetime!!.children!!.contains(ext2.lifetime))
        assertTrue(ext3.lifetime!!.children!!.contains(ext4.lifetime))
    }

    @Test
    fun preventsCircularChildren() {
        // |> Given a parent relationship
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        ext1.addChildLifetime(ext2)

        // |> When we try to set reverse relationship
        // |> Then raise an error
        assertFails {
            ext2.addChildLifetime(ext1)
        }
    }

    @Test
    fun preventCircularChildrenThroughUnified() {
        // |> Given a parent relationship and unified of child
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        val ext3 = TestExtent(g)
        ext1.addChildLifetime(ext2)
        ext2.unifyLifetime(ext3)

        // |> When that unified tries to become parent
        // |> Then raise an error
        assertFails {
            ext3.addChildLifetime(ext1)
        }
    }

    @Test
    fun preventCircularChildrenWhenUnifying() {
        // |> Given parent relationship
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        val ext3 = TestExtent(g)
        ext2.addChildLifetime(ext3)
        ext3.addChildLifetime(ext1)
        // |> When parent becomes unified with child
        // |> Then raise an error
        assertFails {
            ext1.unifyLifetime(ext2)
        }
    }

    @Test
    fun canSupplyAndDemandUpMultipleGenerationLifetimes() {
        // |> Given multiple generations
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        val ext3 = TestExtent(g)
        ext1.addChildLifetime(ext2)
        ext2.addChildLifetime(ext3)
        val r1 = ext1.moment()
        val r2 = ext1.moment()

        // |> When we link up multiple generations
        ext3.behavior().supplies(r2).demands(r1).runs {}
        ext1.addToGraphWithAction()
        ext2.addToGraphWithAction()

        // |> Then it should work fine
        assertNoThrow {
            ext3.addToGraphWithAction()
        }
    }

    @Test
    fun canSupplyAndDemandUpAndAcrossGenerationsLifetimes() {
        // |> Given multiple generations with unified lifetimes
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        val ext3 = TestExtent(g)
        ext1.unifyLifetime(ext2)
        ext2.addChildLifetime(ext3)
        val r1 = ext1.moment()
        val r2 = ext1.moment()

        // |> When we try to link up and across
        ext3.behavior().supplies(r2).demands(r1).runs {}
        g.action {
            ext1.addToGraph()
            ext2.addToGraph()
        }

        // |> Then it will also be fine
        assertNoThrow {
            ext3.addToGraphWithAction()
        }
    }

    @Test
    fun canRemoveMultipleLevelsOfChildren() {
        // |> Given multiple generations of children and unified
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        val ext3 = TestExtent(g)
        val ext4 = TestExtent(g)
        val ext5 = TestExtent(g)
        ext1.addChildLifetime(ext2)
        ext2.unifyLifetime(ext3)
        ext2.addChildLifetime(ext4)
        ext4.unifyLifetime(ext5)
        ext1.addToGraphWithAction()
        g.action {
            ext2.addToGraph()
            ext3.addToGraph()
        }
        g.action {
            ext4.addToGraph()
            ext5.addToGraph()
        }

        // |> When we remove parent with containedLifetimes strategy
        ext1.removeFromGraphWithAction(ExtentRemoveStrategy.ContainedLifetimes)

        // |> Then children and unified are recursively removed
        assertNull(ext3.addedToGraphWhen)
        assertNull(ext4.addedToGraphWhen)
        assertNull(ext5.addedToGraphWhen)
    }

    @Test
    fun confirmContainingLifetimesHaveBeenAdded() {
        // |> Given we have removed one unified extent
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        ext1.unifyLifetime(ext2)

        // |> When event compvales without having removed other member of unified
        // |> Then raise an error
        assertFails {
            ext1.addToGraphWithAction()
        }
    }

    @Test
    fun confirmContainedLifetimesHaveBeenRemoved() {
        // |> Given we have removed a parent
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        ext1.addChildLifetime(ext2)
        ext1.addToGraphWithAction()
        ext2.addToGraphWithAction()

        // |> When event ends without removing child
        // |> Then raise an error
        assertFails {
            ext1.removeFromGraphWithAction()
        }
    }

    @Test
    fun confirmDynamicDemandsAreUnwound() {
        // |> Given dynamic demands across foreign relationship
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        val r1 = ext2.moment()
        ext1.behavior()
            .dynamicDemands(ext1.didAdd) { _, demands -> demands.add(r1) }
            .runs {}
        g.action {
            ext1.addToGraph()
            ext2.addToGraph()
        }

        // |> When one extent is removed without fixing the dynamicDemand to that extent
        // |> Then raise an error
        assertFails {
            ext2.removeFromGraphWithAction()
        }
    }

    @Test
    fun confirmDynamicSuppliesAreUnwound() {
        // |> Given dynamic supply across foreign relationship
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        val r1 = ext2.moment()
        ext1.behavior()
            .dynamicSupplies(ext1.didAdd) { _, supplies -> supplies.add(r1) }
            .runs {}
        g.action {
            ext1.addToGraph()
            ext2.addToGraph()
        }

        // |> When one extent is removed without fixing the dynamicSupply to that extent
        // |> Then raise an error
        assertFails {
            ext2.removeFromGraphWithAction()
        }
    }

    @Test
    fun canOptOutOfLifetimeValidations() {
        // NOTE: Lifetime validations have to iterate through links which can be
        // slow and unnecessary in production

        // |> Given we turn off lifetime validations
        g.validateLifetimes = false
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        ext1.unifyLifetime(ext2)

        // |> When adding only one member of unified
        // |> Then don't throw
        assertNoThrow {
            ext1.addToGraphWithAction()
        }

        // |> And when removing only one member of unified
        ext2.addToGraphWithAction()

        // |> Then don't throw
        assertNoThrow {
            ext1.removeFromGraphWithAction()
        }
    }

    @Test
    fun canOptOutOfStaticLinkLifetimeChecks() {
        // |> Given we turn off lifetime validations
        g.validateLifetimes = false
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        val r1 = ext1.moment()
        val r2 = ext1.moment()

        // |> When we try to link staticly across incompatible lifetimes
        ext2.behavior().demands(r1).supplies(r2).runs {}

        // |> Then don't throw
        ext1.addToGraphWithAction()
        ext2.addToGraphWithAction()
    }

    @Test
    fun ifExtentsAreAddedRemovedTheyShouldRemovedFromCorrespondingLists() {
        // |> Given validate lifetimes is disabled
        g.validateLifetimes = false
        val ext1 = TestExtent(g)

        // |> When extent is added
        ext1.addToGraphWithAction()

        // |> Then it shouldn't be on added list after adding
        assertEquals(0, g.extentsAdded.size)

        // |> And when it is removed
        ext1.removeFromGraphWithAction()

        // |> Then it shouldn't be on removed list
        assertEquals(0, g.extentsRemoved.size)
    }

    @Test
    fun extentsAndLifetimesAreClearedAfterExtentRemoved() {
        // NOTE: We don't want lifetimes that are still around to
        // retain extents that are no longer needed and cause
        // memory leak

        // |> Given an extent is in a shared lifetime
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        ext1.unifyLifetime(ext2)
        g.action {
            ext1.addToGraph()
            ext2.addToGraph()
        }
        assertEquals(ext1.lifetime, ext2.lifetime)

        // |> When that extents are removed from graph
        val lifetime: ExtentLifetime = ext1.lifetime!!
        g.action {
            ext1.removeFromGraph()
            ext2.removeFromGraph()
        }

        // |> Then they should no longer have the lifetime
        // and the lifetime should no longer have them
        assertNull(ext1.lifetime)
        assertNull(ext2.lifetime)
        assertFalse(lifetime.extents.contains(ext1))
        assertFalse(lifetime.extents.contains(ext2))
    }

    @Test
    fun emptyLifetimeAfterRemovalsRemovesFromParent() {
        // NOTE: No need to keep around an empty lifetime as part of parent
        // child relationship

        // |> Given an extent in a child relationship
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        ext1.addChildLifetime(ext2)
        g.action {
            ext1.addToGraph()
            ext2.addToGraph()
        }

        // |> When that extent is removed
        val lifetime1 = ext1.lifetime!!
        val lifetime2 = ext2.lifetime!!
        ext2.removeFromGraphWithAction()

        // |> Then it should no longer be a child of the parent
        assertNull(lifetime2.parent)
        assertTrue(lifetime1.children?.isEmpty() ?: false)
    }

}