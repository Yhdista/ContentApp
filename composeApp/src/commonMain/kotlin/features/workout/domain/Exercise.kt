package features.workout.domain

import kotlinx.serialization.Serializable

@Serializable
enum class MuscleGroup(val displayName: String) {
    CHEST("Hrudník"),
    BACK("Záda"),
    LEGS("Nohy"),
    SHOULDERS("Ramena"),
    ARMS("Paže"),
    CORE("Břicho"),
    CARDIO("Kardio")
}

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val muscleGroup: MuscleGroup,
    val defaultRestSeconds: Int = 90
)
