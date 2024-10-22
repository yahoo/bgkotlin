package behaviorgraph

internal data class EventLoopState(val action: RunnableAction, val actionUpdates: MutableList<Resource> = mutableListOf(), var currentSideEffect: SideEffect? = null, var phase: EventLoopPhase = EventLoopPhase.Queued, var thread: Thread = Thread.currentThread()) {
    override fun toString(): String {
        var rows = mutableListOf<String>("Action")
        actionUpdates?.forEach { resource ->
            rows.add("  " + resource.toString())
        }
        return rows.joinToString("\n")
    }

    val runningOnCurrentThread: Boolean get() = this.thread == Thread.currentThread()
}
