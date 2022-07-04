package com.yahoo.behaviorgraph

import kotlin.test.*

class ExtentLifetimesTest : AbstractBehaviorGraphTest() {

    @Test
    fun `have same lifetime`() {
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
    fun `added in reverse have same lifetime`() {
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
    fun `must be established before adding`() {
        // |> Given two extents
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)

        // |> When unified after adding one extent
        // |> Then it will throw an error
        assertBehaviorGraphException {
            g.action {
                ext1.addToGraph()
                ext2.unifyLifetime(ext1)
                ext2.addToGraph()
            }
        }
    }

    @Test
    fun `merged other unified`() {
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
    fun `can link as static demands`() {
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
    fun `independent lifetimes cannot link as static demands`() {
        // |> Given two independent with foreign demand
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        val r1 = ext1.moment()
        ext2.behavior()
            .demands(r1)
            .runs {}

        // |> When they are added
        // |> Then it should raise an error
        assertBehaviorGraphException {
            g.action {
                ext1.addToGraph()
                ext2.addToGraph()
            }
        }
    }

    @Test
    fun `independent lifetimes cannot link as static supplies`() {
        // |> Given two independent extents with a foreign supply
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        val r1 = ext1.moment()
        ext2.behavior()
            .supplies(r1)
            .runs {}

        // |> When they are both added
        // |> Then it should raise an error
        assertBehaviorGraphException {
            g.action {
                ext1.addToGraph()
                ext2.addToGraph()
            }
        }
    }

    @Test
    fun `can remove each other`() {
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
    fun `cannot be added before`() {
        // |> Given parent extent
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        ext1.addChildLifetime(ext2)

        // |> When child is added first
        // |> Then it should raise an error
        assertBehaviorGraphException {
            ext2.addToGraphWithAction()
        }
    }

    @Test
    fun `can be added simultaneously`() {
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
    fun `can establish child relationship in subsequent events`() {
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
    fun `can supply and demand up lifetimes`() {
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
    fun `cannot static demand down lifetimes`() {
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
        assertBehaviorGraphException {
            g.action {
                ext1.addToGraph()
                ext2.addToGraph()
            }
        }
    }

    @Test
    fun `cannot static supply down lifetimes`() {
        // |> Given a parent relationship with supply in child
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        ext1.addChildLifetime(ext2)
        val r1 = ext2.moment()
        ext1.behavior().supplies(r1).runs {}

        // |> When they are added
        // |> Then it should raise an error
        assertBehaviorGraphException {
            g.action {
                ext1.addToGraph()
                ext2.addToGraph()
            }
        }
    }

    @Test
    fun `can remove with parent`() {
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
    fun `merge children of unified`() {
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
    fun `prevents circular children`() {
        // |> Given a parent relationship
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        ext1.addChildLifetime(ext2)

        // |> When we try to set reverse relationship
        // |> Then raise an error
        assertBehaviorGraphException {
            ext2.addChildLifetime(ext1)
        }
    }

    @Test
    fun `prevent circular children through unified`() {
        // |> Given a parent relationship and unified of child
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        val ext3 = TestExtent(g)
        ext1.addChildLifetime(ext2)
        ext2.unifyLifetime(ext3)

        // |> When that unified tries to become parent
        // |> Then raise an error
        assertBehaviorGraphException {
            ext3.addChildLifetime(ext1)
        }
    }

    @Test
    fun `prevent circular children when unifying`() {
        // |> Given parent relationship
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        val ext3 = TestExtent(g)
        ext2.addChildLifetime(ext3)
        ext3.addChildLifetime(ext1)
        // |> When parent becomes unified with child
        // |> Then raise an error
        assertBehaviorGraphException {
            ext1.unifyLifetime(ext2)
        }
    }

    @Test
    fun `can supply and demand up multiple generation lifetimes`() {
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
    fun `can supply and demand up and across generations lifetimes`() {
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
    fun `can remove multiple levels of children`() {
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
    fun `confirm containing lifetimes have been added`() {
        // |> Given we have removed one unified extent
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        ext1.unifyLifetime(ext2)

        // |> When event compvales without having removed other member of unified
        // |> Then raise an error
        assertBehaviorGraphException {
            ext1.addToGraphWithAction()
        }
    }

    @Test
    fun `confirm contained lifetimes have been removed`() {
        // |> Given we have removed a parent
        val ext1 = TestExtent(g)
        val ext2 = TestExtent(g)
        ext1.addChildLifetime(ext2)
        ext1.addToGraphWithAction()
        ext2.addToGraphWithAction()

        // |> When event ends without removing child
        // |> Then raise an error
        assertBehaviorGraphException {
            ext1.removeFromGraphWithAction()
        }
    }

    @Test
    fun `confirm dynamic demands are unwound`() {
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
        assertBehaviorGraphException {
            ext2.removeFromGraphWithAction()
        }
    }

    @Test
    fun `confirm dynamic supplies are unwound`() {
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
        assertBehaviorGraphException {
            ext2.removeFromGraphWithAction()
        }
    }

    @Test
    fun `can opt out of lifetime validations`() {
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
    fun `can opt out of static link lifetime checks`() {
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


}