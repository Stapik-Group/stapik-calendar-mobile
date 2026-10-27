package pl.stapik.calendar.data.document

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.stapik.common.sync.CloudError
import pl.stapik.common.sync.SyncOutcome

class SyncOutcomesTest {
    @Test
    fun noOutcomesMeanLocalOnly() {
        assertEquals(SyncOutcome.LocalOnly, SyncOutcomes.combine(emptyList()))
    }

    @Test
    fun theMostSevereOutcomeWins() {
        val auth = SyncOutcome.AuthProblem(CloudError.Unauthorized)

        val combined = SyncOutcomes.combine(
            listOf(SyncOutcome.Synchronized, SyncOutcome.Offline(CloudError.Network()), auth, SyncOutcome.ServerWon)
        )

        assertEquals(auth, combined)
    }

    @Test
    fun aProblemOutranksAChoice() {
        val offline = SyncOutcome.Offline(CloudError.Network())

        assertEquals(offline, SyncOutcomes.combine(listOf(SyncOutcome.NeedsFirstConnectionChoice(true), offline)))
    }

    @Test
    fun aResolvedConflictOutranksAPlainSuccess() {
        assertEquals(SyncOutcome.LocalWon, SyncOutcomes.combine(listOf(SyncOutcome.Synchronized, SyncOutcome.LocalWon)))
    }

    @Test
    fun keepingLocalDataIsOfferedOnlyWhenEveryYearAllowsIt() {
        val combined = SyncOutcomes.combine(
            listOf(
                SyncOutcome.NeedsFirstConnectionChoice(canKeepLocal = true),
                SyncOutcome.NeedsFirstConnectionChoice(canKeepLocal = false),
                SyncOutcome.Synchronized
            )
        )

        assertEquals(SyncOutcome.NeedsFirstConnectionChoice(canKeepLocal = false), combined)
    }
}
