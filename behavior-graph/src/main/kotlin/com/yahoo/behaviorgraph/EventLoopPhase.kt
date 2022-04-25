package com.yahoo.behaviorgraph

enum class EventLoopPhase {
    queued,
    action,
    updates,
    sideEffects
}