package features.workout.detail

import androidx.lifecycle.viewModelScope
import core.result.DataError
import core.ui.toUiText
import core.result.onFailure
import core.result.onSuccess
import core.result.safeLocalCall
import core.viewmodel.BaseViewModel
import features.workout.domain.WorkoutRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class WorkoutDetailViewModel(
    private val workoutRepository: WorkoutRepository,
    private val createRoutineDraft: features.workout.domain.CreateRoutineDraft
) : BaseViewModel<WorkoutDetailContract.State, WorkoutDetailContract.Intent, WorkoutDetailContract.Effect>(
    WorkoutDetailContract.State()
) {

    private var loadJob: Job? = null

    override fun onOperationFailure(error: DataError.Local) {
        updateState { copy(error = error.toUiText(), isLoading = false) }
        sendEffect(WorkoutDetailContract.Effect.ShowSnackbar(error.toUiText().asString()))
    }

    override fun handleIntent(intent: WorkoutDetailContract.Intent) {
        when (intent) {
            is WorkoutDetailContract.Intent.LoadDetail -> loadDetail(intent.sessionId)
            is WorkoutDetailContract.Intent.BackClicked -> sendEffect(WorkoutDetailContract.Effect.NavigateBack)
            is WorkoutDetailContract.Intent.CreateRoutine -> createRoutine()
        }
    }

    fun load(sessionId: String) {
        onIntent(WorkoutDetailContract.Intent.LoadDetail(sessionId))
    }

    private fun createRoutine() {
        val current = uiState.value.session ?: return
        if(uiState.value.isCreatingRoutine) return
        updateState { copy(isCreatingRoutine = true) }
        launchOperation {
            when(val result = createRoutineDraft.create(current.id)) {
                is core.result.Result.Success -> sendEffect(WorkoutDetailContract.Effect.RoutineDraft(result.data))
                is core.result.Result.Error -> onOperationFailure(DataError.Local.UNKNOWN)
            }
            updateState { copy(isCreatingRoutine = false) }
        }
    }

    private fun loadDetail(sessionId: String) {
        loadJob?.cancel()
        loadJob = launchOperation {
            updateState { copy(isLoading = true, requestedSessionId = sessionId, error = null) }
            val session = workoutRepository.getSessionById(sessionId) ?: throw NoSuchElementException("Session not found")
            val allExercises = workoutRepository.getExercises().first()
            val sets = workoutRepository.getSetsForSession(sessionId).first()

            val endTimeEpochMs = session.endTimeEpochMs
            val durationStr = if (endTimeEpochMs != null) {
                val totalSec = (endTimeEpochMs - session.startTimeEpochMs) / 1000
                val min = totalSec / 60
                val sec = totalSec % 60
                "${min}m ${sec}s"
            } else {
                "—"
            }

            val grouped = sets.groupBy { it.workoutExerciseId ?: it.exerciseId }
            val exercisesWithSets = grouped.mapNotNull { (exerciseId, exerciseSets) ->
                val first=exerciseSets.first()
                val catalogue=allExercises.find { it.id == first.exerciseId }
                val exercise=features.exercises.api.Exercise(first.exerciseId,first.exerciseNameSnapshot ?: catalogue?.name ?: "Neznámý cvik",
                    first.muscleGroupSnapshot ?: catalogue?.muscleGroup ?: features.exercises.api.MuscleGroup.CHEST,
                    first.restSecondsSnapshot ?: catalogue?.defaultRestSeconds ?: 90)
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
