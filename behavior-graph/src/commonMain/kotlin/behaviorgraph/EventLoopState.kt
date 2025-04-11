package behaviorgraph

internal data class EventLoopState(val action: RunnableAction, val actionUpdates: MutableList<Resource> = mutableListOf(), var currentSideEffect: SideEffect? = null, var phase: EventLoopPhase = EventLoopPhase.Queued) {
    internal var thread: Any? = null
    override fun toString(): String {
        var rows = mutableListOf<String>("Action")
        actionUpdates?.forEach { resource ->
            rows.add("  " + resource.toString())
        }
        return rows.joinToString("\n")
    }
}
