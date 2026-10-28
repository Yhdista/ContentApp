package features.routines.api

import kotlinx.serialization.Serializable

@Serializable data object RoutineListRoute
@Serializable data class RoutineDetailRoute(val id: String)
@Serializable data class RoutineEditorRoute(val draftId: String)
