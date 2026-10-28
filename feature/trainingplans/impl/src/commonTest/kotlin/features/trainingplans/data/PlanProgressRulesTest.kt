package features.trainingplans.data

import features.trainingplans.api.*
import features.workout.domain.PlanWorkoutSource
import kotlin.test.*

class PlanProgressRulesTest {
    private fun plan(count: Int = 3) = TrainingPlan("plan", "Cycle", revision = 1, slots = List(count) { PlanSlot("ABC"[it].toString(), "Day $it", "routine") })
    private fun source() = PlanWorkoutSource("plan", 1, "A", 1)
    @Test fun initializesAtFirstAndCyclesWithIncreasingRevision() {
        val p = plan()
        var progress = PlanProgressRules.afterSave(p, null)
        assertEquals(PlanProgress("plan", "A", 1, false), progress)
        for ((slot, rev) in listOf("B" to 2L, "C" to 3L, "A" to 4L)) {
            progress = PlanProgressRules.next(p, progress)
            assertEquals(PlanProgress("plan", slot, rev, false), progress)
        }
    }
    @Test fun oneSlotAdvanceStillIncrementsRevision() {
        assertEquals(PlanProgress("plan", "A", 2, false), PlanProgressRules.next(plan(1), PlanProgress("plan", "A", 1, false)))
    }
    @Test fun removedCursorBecomesNullAndRequiresReview() {
        assertEquals(PlanProgress("plan", null, 2, true), PlanProgressRules.afterSave(plan().copy(slots = plan().slots.drop(1)), PlanProgress("plan", "A", 1, false)))
        val current = PlanProgress("plan", "A", 5, true)
        assertEquals(current, PlanProgressRules.afterSave(plan(), current))
    }
    @Test fun completionRequiresSlotPlanAndProgressRevisionMatch() {
        val current = PlanProgress("plan", "A", 1, false)
        assertEquals(PlanProgress("plan", "B", 2, false), assertIs<ProgressOutcome.Advanced>(PlanProgressRules.completed(plan(), current, source())).progress)
        for ((p, progress, src) in listOf(
            Triple(plan().copy(revision = 2), current, source()),
            Triple(plan(), current.copy(nextSlotId = "B"), source()),
            Triple(plan(), current.copy(progressRevision = 4), source()),
            Triple(plan(1), current.copy(progressRevision = 2), source()),
            Triple(plan(), current.copy(needsReview = true), source()))) {
            assertTrue(assertIs<ProgressOutcome.ReviewRequired>(PlanProgressRules.completed(p, progress, src)).progress.needsReview)
        }
    }
    @Test fun archiveHasPrecedenceOverConflictingSource() {
        val current = PlanProgress("plan", "B", 5, true)
        assertEquals(current, assertIs<ProgressOutcome.ArchivedUnchanged>(PlanProgressRules.completed(plan().copy(archived = true, revision = 9), current, source())).progress)
    }
}
