package com.yahoo.behaviorgraph

data class EventLoopState(val action: RunnableAction, val actionUpdates: MutableList<Resource> = mutableListOf(), var currentSideEffect: RunnableSideEffect? = null, var phase: EventLoopPhase = EventLoopPhase.queued)
