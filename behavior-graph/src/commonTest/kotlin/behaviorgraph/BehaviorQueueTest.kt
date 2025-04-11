package behaviorgraph

import kotlin.test.*

internal class BehaviorQueueTest {

    lateinit var q: BehaviorQueue
    lateinit var g: Graph
    lateinit var ext: Extent<Any>

    @BeforeTest
    fun setup() {
        q = BehaviorQueue()
        g = Graph()
        ext = Extent(g)
    }

    fun makeBehavior(order: Long): Behavior<Any> {
        val b = Behavior(ext, null, null, {})
        b.order = order
        return b
    }

    @Test
    fun behaviorQueueIsEmpty() {
        assertEquals(0, q.size)
        assertNull(q.pop())
    }

    @Test
    fun behaviorQueueCanAddAndPop() {
        val b1 = makeBehavior(0)
        q.add(b1)
        assertEquals(b1, q.pop())
    }

    @Test
    fun poppingMeansItMovesToTheNext() {
        val b1 = makeBehavior(1)
        q.add(b1)
        assertEquals(b1, q.pop())
        assertNull(q.pop())
    }

    @Test
    fun popsLowestOrderFirst() {
        val b1 = makeBehavior(1)
        val b2 = makeBehavior(0)
        q.add(b1)
        q.add(b2)
        assertEquals(b2, q.pop())
        assertEquals(b1, q.pop())
    }

    @Test
    fun stopSearchingWhenWeHaveFoundMinimumOrder() {
        val b1 = makeBehavior(7)
        val b2 = makeBehavior(8)
        q.add(b1)
        q.add(b2)
        q.pop()
        val b3 = makeBehavior(3)
        q.add(b3)
        assertEquals(b3, q.pop())
    }

    @Test
    fun clearingRestsEverything() {
        // |> Given we have queue that has been used
        val b1 = makeBehavior(1)
        val b2 = makeBehavior(1)
        q.add(b1)
        q.add(b2)

        // |> When we clear it
        q.clear()

        // |> Then it should be empty
        assertNull(q.pop())
        assertEquals(0, q.size)

        // |> And when we add more items
        q.add(b2)
        val popped = q.pop()

        // |> Then we should get first new item
        assertEquals(b2, popped)
    }

    @Test
    fun reheapMakesSureOrderIsCorrect() {
        val b1 = makeBehavior(9)
        val b2 = makeBehavior(9)
        val b3 = makeBehavior(9)
        q.add(b1)
        q.add(b2)
        q.add(b3)

        b2.order = 5
        q.reheap()

        assertEquals(q.pop(), b2)
    }

}