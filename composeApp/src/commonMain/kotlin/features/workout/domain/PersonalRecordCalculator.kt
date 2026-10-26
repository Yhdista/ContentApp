package features.workout.domain

object PersonalRecordCalculator {

    /**
     * Epley vzorec pro odhad maximální váhy na jedno opakování (1RM):
     * 1RM = váha * (1 + počet opakování / 30.0)
     * Pro 1 opakování je 1RM rovno zvednuté váze.
     */
    fun calculateOneRepMax(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weightKg
        return weightKg * (1.0 + reps / 30.0)
    }

    /**
     * Určí, zda aktuální série překonává historické maximum pro daný cvik.
     * Vrátí true pouze pokud je odhadované 1RM ostře vyšší než dosavadní maximum.
     */
    fun isPersonalRecord(
        weightKg: Double,
        reps: Int,
        historicalMax1RM: Double
    ): Boolean {
        if (weightKg <= 0.0 || reps <= 0) return false
        val current1RM = calculateOneRepMax(weightKg, reps)
        // Epsilon 0.05 pro eliminaci zaokrouhlovacích chyb plovoucí čárky
        return current1RM > historicalMax1RM + 0.05
    }

    /**
     * Naformátuje 1RM na přehledný text (např. "100 kg" nebo "102.5 kg").
     */
    fun formatWeight(weight: Double): String {
        return if (weight % 1.0 == 0.0) {
            "${weight.toInt()} kg"
        } else {
            val rounded = (weight * 10).toInt() / 10.0
            "$rounded kg"
        }
    }
}
