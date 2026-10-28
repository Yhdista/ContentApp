package features.workout.data

import features.exercises.api.Exercise
import features.exercises.api.MuscleGroup

/** Example product content, deliberately kept out of the reusable foundation. */
internal object WorkoutDefaults {
    val exercises: List<Exercise> = listOf(
        Exercise("ex_bench", "Bench press", MuscleGroup.CHEST, defaultRestSeconds = 120),
        Exercise("ex_squat", "Dřep s činkou", MuscleGroup.LEGS, defaultRestSeconds = 180),
        Exercise("ex_deadlift", "Mrtvý tah", MuscleGroup.BACK, defaultRestSeconds = 180),
        Exercise("ex_ohp", "Tlaky na ramena (OHP)", MuscleGroup.SHOULDERS, defaultRestSeconds = 120),
        Exercise("ex_pullup", "Shyby na hrazdě", MuscleGroup.BACK, defaultRestSeconds = 90),
        Exercise("ex_biceps", "Bicepsový zdvih", MuscleGroup.ARMS, defaultRestSeconds = 60),
        Exercise("ex_plank", "Plank", MuscleGroup.CORE, defaultRestSeconds = 60)
    )

}
