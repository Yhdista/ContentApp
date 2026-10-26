package features.workout.detail

import androidx.lifecycle.viewModelScope
import core.viewmodel.BaseViewModel
import features.workout.domain.WorkoutRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WorkoutDetailViewModel(
    private val workoutRepository: WorkoutRepository
) : BaseViewModel<WorkoutDetailContract.State, WorkoutDetailContract.Intent, WorkoutDetailContract.Effect>(
    WorkoutDetailContract.State()
) {

    override fun handleIntent(intent: WorkoutDetailContract.Intent) {
        when (intent) {
            is WorkoutDetailContract.Intent.LoadDetail -> loadDetail(intent.sessionId)
            is WorkoutDetailContract.Intent.BackClicked -> sendEffect(WorkoutDetailContract.Effect.NavigateBack)
            is WorkoutDetailContract.Intent.SaveAsTemplate -> saveAsTemplate(intent.templateName)
        }
    }

    fun load(sessionId: String) {
        onIntent(WorkoutDetailContract.Intent.LoadDetail(sessionId))
    }

    private fun saveAsTemplate(templateName: String) {
        val currentSession = uiState.value.session ?: return
        viewModelScope.launch {
            workoutRepository.saveSessionAsTemplate(currentSession.id, templateName)
            sendEffect(WorkoutDetailContract.Effect.ShowSnackbar("Šablona '$templateName' byla úspěšně uložena."))
        }
    }

    private fun loadDetail(sessionId: String) {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            val session = workoutRepository.getSessionById(sessionId)
            val allExercises = workoutRepository.getExercises().first()
            val sets = workoutRepository.getSetsForSession(sessionId).first()

            val durationStr = if (session?.endTimeEpochMs != null) {
                val totalSec = (session.endTimeEpochMs - session.startTimeEpochMs) / 1000
                val min = totalSec / 60
                val sec = totalSec % 60
                "${min}m ${sec}s"
            } else {
                "—"
            }

            val grouped = sets.groupBy { it.exerciseId }
            val exercisesWithSets = grouped.mapNotNull { (exerciseId, exerciseSets) ->
                val exercise = allExercises.find { it.id == exerciseId } ?: return@mapNotNull null
                val volume = exerciseSets.filter { it.isCompleted }.sumOf { it.weightKg * it.reps }
                WorkoutDetailContract.ExerciseWithSets(
                    exercise = exercise,
                    sets = exerciseSets,
                    exerciseVolumeKg = volume
                )
            }

            updateState {
                copy(
                    session = session,
                    exercisesWithSets = exercisesWithSets,
                    durationFormatted = durationStr,
                    isLoading = false
                )
            }
        }
    }
}
