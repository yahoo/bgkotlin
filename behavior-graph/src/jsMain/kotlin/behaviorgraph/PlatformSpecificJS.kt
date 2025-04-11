package behaviorgraph

import kotlinx.coroutines.sync.Mutex

internal actual fun makePlatformSpecific(): PlatformSpecific {
    return object : PlatformSpecific {
        override fun assert(condition: Boolean, lazyMessage: () -> String) {
            if (!condition) {
                throw AssertionError(lazyMessage())
            }
        }

        override fun safeAddToActionQueue(action: RunnableAction, queue: MutableList<RunnableAction>, mutex: Mutex) {
            // JS is single threaded, so we don't need to lock the queue
            queue.add(action)
        }

        override fun nameResources(focus: Any) {
            val dynamicFocus = focus.asDynamic()
            val keys = js("Object").keys(dynamicFocus) as Array<String>
            for (key in keys) {
                // iterate through each field and see if it is a resource subclass
                val field = dynamicFocus[key]
                if (field != null && field["__bg_isResource"] != null) {
                    // sometimes fields aren't accessible to reflection, try enabling that
                    if (field["debugName"] == null) {
                        field["debugName"] = key
                    }
                }
            }
        }

        override fun setCurrentThread(state: EventLoopState) {
            // NO Op
        }

        override fun runningOnCurrentThread(state: EventLoopState?): Boolean {
            return state != null
        }

        override fun defaultNameForExtent(extent: Extent<*>): String {
            return extent.asDynamic().constructor.name as? String ?: "Extent"
        }
    }
}