package features.routines.api

import kotlinx.coroutines.flow.Flow

/** App-composed read model avoids a dependency from routines back to plans. */
fun interface RoutineUsage { fun observeDependentPlanCount(routineId: String): Flow<Int> }
fun interface RoutineDraftFactory { suspend fun fromWorkout(sessionId: String): String }
