package features.workout

import features.workout.domain.PlateCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlateCalculatorTest {

    @Test
    fun testEmptyBarReturnsNoPlates() {
        val plates = PlateCalculator.calculatePlatesPerSide(totalWeightKg = 20.0, barWeightKg = 20.0)
        assertTrue(plates.isEmpty())
    }

    @Test
    fun testSixtyKgReturnsOneTwentyKgPlatePerSide() {
        val plates = PlateCalculator.calculatePlatesPerSide(totalWeightKg = 60.0, barWeightKg = 20.0)
        assertEquals(1, plates.size)
        assertEquals(20.0, plates.first().weightKg)
    }

    @Test
    fun testOneHundredKgReturnsTwentyFiveAndFifteenKgPlatesPerSide() {
        val plates = PlateCalculator.calculatePlatesPerSide(totalWeightKg = 100.0, barWeightKg = 20.0)
        assertEquals(listOf(25.0, 15.0), plates.map { it.weightKg })
    }

    @Test
    fun testFractionalWeightBreakdown() {
        // (142.5 - 20) / 2 = 61.25 kg per side -> 2x25kg (50kg) + 1x10kg (10kg) + 1x1.25kg (1.25kg)
        val plates = PlateCalculator.calculatePlatesPerSide(totalWeightKg = 142.5, barWeightKg = 20.0)
        val weights = plates.map { it.weightKg }
        assertEquals(listOf(25.0, 25.0, 10.0, 1.25), weights)
    }
}
