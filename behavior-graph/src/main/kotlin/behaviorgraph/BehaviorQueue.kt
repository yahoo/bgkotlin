package behaviorgraph

internal class BehaviorQueue {
    // Keeps track of activated behaviors and when asked
    // runs the next one with the lowest order.
    // Uses a binary heap

    private var behaviors: MutableList<Behavior<*>> = mutableListOf()

    fun add(behavior: Behavior<*>) {
        behaviors.add(behavior)
        heapUp(behaviors.size - 1)
    }

    fun peek(): Behavior<*>? {
        return behaviors.firstOrNull()
    }

    fun pop(): Behavior<*>? {
        if (behaviors.isEmpty()) return null

        val min = behaviors[0]
        // swap with last to remove from end
        val last = behaviors.removeAt(behaviors.size - 1)
        if (behaviors.isNotEmpty()) {
            behaviors[0] = last
            heapDown(0)
        }
        return min
    }

    private fun heapUp(index: Int) {
        var currentIndex = index
        var parentIndex = (currentIndex - 1) / 2

        while (currentIndex > 0 && behaviors[currentIndex] < behaviors[parentIndex]) {
            swap(currentIndex, parentIndex)
            currentIndex = parentIndex
            parentIndex = (currentIndex - 1) / 2
        }
    }

    private fun heapDown(index: Int) {
        var currentIndex = index

        while (true) {
            val leftChildIndex = (2 * currentIndex) + 1
            val rightChildIndex = (2 * currentIndex) + 2
            var smallestIndex = currentIndex

            if (leftChildIndex < behaviors.size && behaviors[leftChildIndex] < behaviors[smallestIndex]) {
                smallestIndex = leftChildIndex
            }

            if (rightChildIndex < behaviors.size && behaviors[rightChildIndex] < behaviors[smallestIndex]) {
                smallestIndex = rightChildIndex
            }

            if (smallestIndex == currentIndex) {
                break
            }

            swap(currentIndex, smallestIndex)
            currentIndex = smallestIndex
        }
    }

    private fun swap(i: Int, j: Int) {
        val temp = behaviors[i]
        behaviors[i] = behaviors[j]
        behaviors[j] = temp
    }

    fun reheap() {
        val oldBehaviors = behaviors
        behaviors = mutableListOf()
        for (b in oldBehaviors) {
            add(b)
        }
    }

    fun clear() {
        behaviors.clear()
    }

    val size: Int
        get() { return behaviors.size }
}