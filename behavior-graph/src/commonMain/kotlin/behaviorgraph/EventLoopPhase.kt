package behaviorgraph

internal enum class EventLoopPhase {
    Queued,
    Action,
    Updates,
    SideEffects;

    val processingChanges: Boolean get() = (this == Action || this == Updates)
}