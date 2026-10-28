package features.workout.domain

import core.result.Result
fun interface CreateRoutineDraft { suspend fun create(sessionId: String): Result<String, WorkoutError> }
