package behaviorgraph

import kotlinx.coroutines.sync.Mutex

internal interface PlatformSpecific {
    fun assert(condition: Boolean, lazyMessage: () -> String)
    fun safeAddToActionQueue(action: RunnableAction, queue: MutableList<RunnableAction>, mutex: Mutex)
    fun nameResources(focus: Any)
    fun setCurrentThread(state: EventLoopState)
    fun runningOnCurrentThread(state: EventLoopState?): Boolean
    fun defaultNameForExtent(extent: Extent<*>): String
}