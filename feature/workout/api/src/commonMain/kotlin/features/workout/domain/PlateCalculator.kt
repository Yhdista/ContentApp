package features.workout.domain

import kotlinx.serialization.Serializable

@Serializable
data class BarbellPlate(
    val weightKg: Double
)

object PlateCalculator {

    val STANDARD_AVAILABLE_PLATES = listOf(
        BarbellPlate(25.0),
        BarbellPlate(20.0),
        BarbellPlate(15.0),
        BarbellPlate(10.0),
        BarbellPlate(5.0),
        BarbellPlate(2.5),
        BarbellPlate(1.25)
    )

    /**
     * Spočítá rozpis kotoučů na jednu stranu činky.
     * @param totalWeightKg Celková požadovaná váha osy včetně kotoučů
     * @param barWeightKg Váha samotné osy činky (typicky 20 kg pro olympijskou osu)
     * @return Seznam kotoučů seřazený od nejtěžšího po nejlehčí pro jednu stranu
     */
    fun calculatePlatesPerSide(
        totalWeightKg: Double,
        barWeightKg: Double = 20.0,
        availablePlates: List<BarbellPlate> = STANDARD_AVAILABLE_PLATES
    ): List<BarbellPlate> {
        var remainingPerSide = (totalWeightKg - barWeightKg) / 2.0
        if (remainingPerSide <= 0.0) return emptyList()

        val result = mutableListOf<BarbellPlate>()
        for (plate in availablePlates) {
            val count = (remainingPerSide / plate.weightKg).toInt()
            repeat(count) {
                result.add(plate)
            }
            remainingPerSide -= count * plate.weightKg
        }
        return result
    }
}
