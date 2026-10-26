package features.workout

import features.workout.domain.PersonalRecordCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersonalRecordCalculatorTest {

    @Test
    fun testOneRepMaxForSingleRepEqualsWeight() {
        val result = PersonalRecordCalculator.calculateOneRepMax(120.0, 1)
        assertEquals(120.0, result)
    }

    @Test
    fun testOneRepMaxCalculatesEpleyFormulaCorrectly() {
        // 100 kg × 10 reps -> 100 * (1 + 10/30) = 133.333... kg
        val result = PersonalRecordCalculator.calculateOneRepMax(100.0, 10)
        assertEquals(133.33, (result * 100).toInt() / 100.0)
    }

    @Test
    fun testOneRepMaxHandlesZeroOrNegativeInputs() {
        assertEquals(0.0, PersonalRecordCalculator.calculateOneRepMax(0.0, 10))
        assertEquals(0.0, PersonalRecordCalculator.calculateOneRepMax(100.0, 0))
        assertEquals(0.0, PersonalRecordCalculator.calculateOneRepMax(-50.0, 5))
    }

    @Test
    fun testIsPersonalRecordReturnsTrueWhenSurpassingHistoricalBest() {
        // Historické maximum je 120 kg 1RM
        val historicalMax = 120.0

        // 100 kg × 10 reps = 133.33 kg 1RM > 120 kg -> PR!
        assertTrue(PersonalRecordCalculator.isPersonalRecord(100.0, 10, historicalMax))

        // 110 kg × 1 rep = 110 kg 1RM < 120 kg -> Not PR
        assertFalse(PersonalRecordCalculator.isPersonalRecord(110.0, 1, historicalMax))

        // 120 kg × 1 rep = 120 kg 1RM == historicalMax -> Not PR
        assertFalse(PersonalRecordCalculator.isPersonalRecord(120.0, 1, historicalMax))
    }

    @Test
    fun testFormatWeight() {
        assertEquals("100 kg", PersonalRecordCalculator.formatWeight(100.0))
        assertEquals("102.5 kg", PersonalRecordCalculator.formatWeight(102.5))
    }
}
