package features.workout.data

import features.workout.domain.Exercise
import features.workout.domain.MuscleGroup
import features.workout.domain.WorkoutTemplate
import features.workout.domain.WorkoutTemplateExercise

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

    val templates: List<WorkoutTemplate> = listOf(
        WorkoutTemplate(
            id = "tmpl_push",
            name = "Push Day",
            description = "Hrudník, ramena a triceps",
            exercises = listOf(
                WorkoutTemplateExercise("ex_bench", "Bench press", 3, 60.0, 8, 0),
                WorkoutTemplateExercise("ex_ohp", "Tlaky na ramena (OHP)", 3, 40.0, 10, 1)
            )
        ),
        WorkoutTemplate(
            id = "tmpl_pull",
            name = "Pull Day",
            description = "Záda, biceps a zadní ramena",
            exercises = listOf(
                WorkoutTemplateExercise("ex_deadlift", "Mrtvý tah", 4, 100.0, 5, 0),
                WorkoutTemplateExercise("ex_pullup", "Shyby na hrazdě", 3, 0.0, 8, 1),
                WorkoutTemplateExercise("ex_biceps", "Bicepsový zdvih", 3, 15.0, 12, 2)
            )
        ),
        WorkoutTemplate(
            id = "tmpl_legs",
            name = "Leg Day",
            description = "Kvadricepsy, hamstringy a lýtka",
            exercises = listOf(
                WorkoutTemplateExercise("ex_squat", "Dřep s činkou", 4, 80.0, 8, 0),
                WorkoutTemplateExercise("ex_plank", "Plank", 3, 0.0, 60, 1)
            )
        )
    )

}
