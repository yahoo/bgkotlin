package behaviorgraph

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
internal actual fun makePlatformSpecific(): PlatformSpecific {
    return object : PlatformSpecific {
        override fun assert(condition: Boolean, lazyMessage: () -> String) {
            kotlin.assert(condition, lazyMessage)
        }

        override fun safeAddToActionQueue(action: RunnableAction, queue: MutableList<RunnableAction>, mutex: Mutex) {
            runBlocking {
                try {
                    mutex.lock()
                    queue.add(action)
                } finally {
                    mutex.unlock()
                }
            }
        }

        override fun nameResources(focus: Any) {
        }

        override fun setCurrentThread(state: EventLoopState) {
            // NO Op
        }

        override fun runningOnCurrentThread(state: EventLoopState?): Boolean {
            return state != null
        }

        override fun defaultNameForExtent(extent: Extent<*>): String {
            return "Extent"
        }
    }
}