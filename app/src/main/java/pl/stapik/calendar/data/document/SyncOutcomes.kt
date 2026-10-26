package pl.stapik.calendar.data.document

import pl.stapik.common.sync.SyncOutcome

object SyncOutcomes {
    fun combine(outcomes: Collection<SyncOutcome>): SyncOutcome {
        val needingChoice = outcomes.filterIsInstance<SyncOutcome.NeedsFirstConnectionChoice>()
        val worst = outcomes.maxByOrNull(::severity) ?: return SyncOutcome.LocalOnly
        if (worst !is SyncOutcome.NeedsFirstConnectionChoice) return worst
        return SyncOutcome.NeedsFirstConnectionChoice(canKeepLocal = needingChoice.all { it.canKeepLocal })
    }

    private fun severity(outcome: SyncOutcome): Int = when (outcome) {
        is SyncOutcome.AuthProblem -> 10
        is SyncOutcome.RemoteUnreadable -> 9
        is SyncOutcome.NewerSchema -> 8
        is SyncOutcome.Offline -> 7
        is SyncOutcome.NeedsFirstConnectionChoice -> 6
        SyncOutcome.KeptLocalUnsynced -> 5
        SyncOutcome.ServerWon, SyncOutcome.LocalWon -> 4
        SyncOutcome.Synchronized -> 3
        SyncOutcome.LocalOnly -> 2
    }
}
