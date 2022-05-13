package com.yahoo.behaviorgraph

internal enum class EventLoopPhase {
    Queued,
    Action,
    Updates,
    SideEffects
}