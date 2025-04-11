package behaviorgraph

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex

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
            try {
                focus.javaClass.declaredFields.forEach { field ->
                    // iterate through each field and see if its a resource subclass
                    if ((Resource::class.java as Class).isAssignableFrom(field.type)) {
                        // sometimes fields aren't accessible to reflection, try enabling that
                        field.isAccessible = true // throws error if not possible
                        val resource = field.get(focus) as Resource
                        if (resource.debugName == null) {
                            resource.debugName = field.name
                        }
                    }
                }
            } catch (ex: Exception) {
                // throws error if we cannot make fields accessible for security reasons
                // catching the error is fine here, it just means we won't get debug names
            }
        }

        override fun setCurrentThread(state: EventLoopState) {
            state.thread = Thread.currentThread()
        }

        override fun runningOnCurrentThread(state: EventLoopState?): Boolean {
            return state?.thread == Thread.currentThread()
        }

        override fun defaultNameForExtent(extent: Extent<*>): String {
            return extent.javaClass.simpleName
        }
    }
}