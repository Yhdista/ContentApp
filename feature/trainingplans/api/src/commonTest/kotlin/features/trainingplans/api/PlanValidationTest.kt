package features.trainingplans.api

import kotlin.test.*

class PlanValidationTest {
    private fun plan(count: Int = 1) = TrainingPlan("plan", " Cycle ", slots = List(count) { PlanSlot("slot-$it", "Day $it", "routine") })
    @Test fun slotsAreBoundedAndRepeatedRoutineIsAllowed() {
        for (n in listOf(1, 31)) assertNull(PlanValidation.validate(plan(n)))
        for (n in listOf(0, 32)) assertEquals(PlanError.VALIDATION, PlanValidation.validate(plan(n)))
    }
    @Test fun namesAndNotesHavePublishableBounds() {
        for (name in listOf(" ", "a".repeat(101))) assertEquals(PlanError.VALIDATION, PlanValidation.validate(plan().copy(name = name)))
        assertNull(PlanValidation.validate(plan().copy(name = " a ", notes = "n".repeat(2000))))
        assertNull(PlanValidation.validate(plan().copy(name = "a".repeat(100))))
        assertEquals(PlanError.VALIDATION, PlanValidation.validate(plan().copy(notes = "n".repeat(2001))))
    }
    @Test fun identifiersAreNonblankAndSlotIdsUnique() {
        assertEquals(PlanError.VALIDATION, PlanValidation.validate(plan().copy(id = " ")))
        for (slot in listOf(PlanSlot("", "A", "routine"), PlanSlot("slot", "A", " "))) {
            assertEquals(PlanError.VALIDATION, PlanValidation.validate(plan().copy(slots = listOf(slot))))
        }
        assertEquals(PlanError.VALIDATION, PlanValidation.validate(plan().copy(slots = List(2) { PlanSlot("same", "A", "routine") })))
    }
    @Test fun slotLabelsUseNameBounds() {
        for (label in listOf(" ", "a".repeat(101))) assertEquals(PlanError.VALIDATION, PlanValidation.validate(plan().copy(slots = listOf(PlanSlot("slot", label, "routine")))))
        assertNull(PlanValidation.validate(plan().copy(slots = listOf(PlanSlot("slot", "a".repeat(100), "routine")))))
    }
}
