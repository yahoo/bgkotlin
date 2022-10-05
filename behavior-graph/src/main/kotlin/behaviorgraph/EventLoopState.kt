package behaviorgraph

internal data class EventLoopState(val action: RunnableAction, val actionUpdates: MutableList<Resource> = mutableListOf(), var currentSideEffect: SideEffect? = null, var phase: EventLoopPhase = EventLoopPhase.Queued)
