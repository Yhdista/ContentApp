package features.trainingplans.api

import kotlinx.serialization.Serializable

@Serializable data object PlanListRoute
@Serializable data class PlanDetailRoute(val id: String)
@Serializable data class PlanEditorRoute(val draftId: String)
