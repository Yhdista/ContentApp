package features.workout.active

import androidx.lifecycle.viewModelScope
import core.result.DataError
import core.ui.toUiText
import core.common.AppClock
import core.feedback.NoOpPlatformFeedback
import core.feedback.PlatformFeedback
import core.viewmodel.BaseViewModel
import features.exercises.api.Exercise
import features.exercises.api.MuscleGroup
import features.workout.domain.WorkoutRepository
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet
import features.workout.domain.WorkoutSetType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class ActiveWorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val clock: AppClock,
    private val platformFeedback: PlatformFeedback = NoOpPlatformFeedback(),
    private val completeWorkout: features.workout.domain.CompleteWorkout,
    private val catalogue: features.exercises.api.ExerciseCatalog
) : BaseViewModel<ActiveWorkoutContract.State, ActiveWorkoutContract.Intent, ActiveWorkoutContract.Effect>(
    ActiveWorkoutContract.State()
) {

    private var restTimerJob: Job? = null
    private var workoutTimerJob: Job? = null
    private var statsJob: Job? = null
    private var loadJob: Job? = null

    init {
        onIntent(ActiveWorkoutContract.Intent.LoadWorkout)
    }

    override fun onOperationFailure(error: DataError.Local) {
        updateState { copy(error = error.toUiText(), isLoading = false) }
        sendEffect(ActiveWorkoutContract.Effect.ShowSnackbar(error.toUiText().asString()))
    }

    override fun handleIntent(intent: ActiveWorkoutContract.Intent) {
        if (uiState.value.isSaving) return
        when (intent) {
            is ActiveWorkoutContract.Intent.LoadWorkout -> loadWorkout()
            is ActiveWorkoutContract.Intent.OpenExercisePicker -> updateState { copy(isPickerVisible = intent.isOpen) }
            is ActiveWorkoutContract.Intent.OpenExerciseStats -> openExerciseStats(intent.exercise)
            is ActiveWorkoutContract.Intent.AddExerciseToWorkout -> addExercise(intent.exercise)
            is ActiveWorkoutContract.Intent.CreateCustomExercise -> createCustomExercise(
                intent.name,
                intent.muscleGroup,
                intent.defaultRestSeconds
            )
            is ActiveWorkoutContract.Intent.RemoveExerciseFromWorkout -> removeExercise(intent.exerciseId)
            is ActiveWorkoutContract.Intent.AddSet -> addSet(intent.exerciseId)
            is ActiveWorkoutContract.Intent.ToggleSetComplete -> toggleSetComplete(intent.set)
            is ActiveWorkoutContract.Intent.UpdateWeight -> updateWeight(intent.set, intent.weightKg)
            is ActiveWorkoutContract.Intent.UpdateReps -> updateReps(intent.set, intent.reps)
            is ActiveWorkoutContract.Intent.UpdateRpe -> updateRpe(intent.set, intent.rpe)
            is ActiveWorkoutContract.Intent.ChangeSetType -> changeSetType(intent.set, intent.newType)
            is ActiveWorkoutContract.Intent.DeleteSet -> deleteSet(intent.set)
            is ActiveWorkoutContract.Intent.StartRestTimer -> startRestTimer(intent.seconds)
            is ActiveWorkoutContract.Intent.AdjustRestTimer -> adjustRestTimer(intent.deltaSeconds)
            is ActiveWorkoutContract.Intent.SkipRestTimer -> skipRestTimer()
            is ActiveWorkoutContract.Intent.FinishWorkout -> finishWorkout()
            is ActiveWorkoutContract.Intent.CancelWorkout -> cancelWorkout()
        }
    }

    private fun loadWorkout() {
        loadJob?.cancel()
        loadJob = launchOperation {
            updateState { copy(isLoading = true, error = null) }

            // Načtení katalogu cviků
            launch {
                workoutRepository.getExercises().collect { exercises ->
                    updateState { copy(availableExercises = exercises.filterNot { it.archived }) }
                }
            }

            // Sledování aktivního sezení a jeho sérií
            var setsCollectorJob: Job? = null
            workoutRepository.getActiveSession().collect { active ->
                updateState { copy(session = active, isLoading = false) }
                if (active != null) {
                    startWorkoutTimer(active.startTimeEpochMs)
                    setsCollectorJob?.cancel()
                    setsCollectorJob = launch {
                        combine(workoutRepository.getSetsForSession(active.id), workoutRepository.getWorkoutExercises(active.id)) { sets, entries ->
                            sets to entries
                        }.collect { (sets,entries) -> updateState { copy(sets=sets,workoutExercises=entries) } }
                    }
                } else {
                    setsCollectorJob?.cancel()
                    workoutTimerJob?.cancel()
                    updateState { copy(sets = emptyList(), workoutExercises = emptyList()) }
                }
            }
        }
    }

    private fun startWorkoutTimer(startTime: Long) {
        workoutTimerJob?.cancel()
        workoutTimerJob = launchOperation {
            while (isActive) {
                val elapsed = ((clock.nowMillis() - startTime) / 1000).coerceAtLeast(0L)
                updateState { copy(workoutElapsedSeconds = elapsed) }
                delay(1000)
            }
        }
    }

    private fun addExercise(exercise: Exercise) {
        val sessionId=uiState.value.session?.id ?: return
        launchOperation {
            workoutRepository.addExerciseWithInitialSet(sessionId, exercise.id, 60.0, 10)
            updateState { copy(isPickerVisible=false) }
        }
    }

    private fun createCustomExercise(name: String, muscleGroup: MuscleGroup, defaultRestSeconds: Int) {
        launchOperation {
            val exercise = catalogue.create(name, muscleGroup, defaultRestSeconds)
            addExercise(exercise)
        }
    }

    private fun openExerciseStats(exercise: Exercise?) {
        statsJob?.cancel()
        if (exercise == null) {
            updateState { copy(selectedStatsExercise = null, exerciseStats = null) }
            return
        }
        updateState { copy(selectedStatsExercise = exercise, exerciseStats = null) }
        statsJob = launchOperation {
            workoutRepository.getExerciseStats(exercise.id).collect { stats ->
                updateState {
                    if (selectedStatsExercise?.id == exercise.id) {
                        copy(exerciseStats = stats)
                    } else this
                }
            }
        }
    }

    private fun removeExercise(exerciseId: String) {
        val sessionId = uiState.value.session?.id ?: return
        launchOperation {
            workoutRepository.removeWorkoutExercise(sessionId, exerciseId)
            updateState { copy(workoutExercises = workoutExercises.filterNot { it.id == exerciseId }) }
        }
    }

    private fun addSet(exerciseId: String) {
        val sessionId = uiState.value.session?.id ?: return
        launchOperation {
            val existingSets = uiState.value.sets.filter { (it.workoutExerciseId ?: it.exerciseId) == exerciseId }
            val lastSet = existingSets.lastOrNull()
            val weight = lastSet?.weightKg ?: 60.0
            val reps = lastSet?.reps ?: 10

            workoutRepository.addSetToEntry(
                sessionId = sessionId,
                workoutExerciseId = exerciseId,
                weightKg = weight,
                reps = reps
            )
        }
    }

    private fun toggleSetComplete(set: WorkoutSet) {
        launchOperation {
            val updated = workoutRepository.updateSet(set.id) { it.copy(isCompleted = !it.isCompleted) }
            // Start feedback only after the transaction succeeds.
            if (updated.isCompleted) {
                val restSeconds = set.restSecondsSnapshot
                    ?: uiState.value.workoutExercises.find { it.id == (set.workoutExerciseId ?: set.exerciseId) }?.restSeconds
                    ?: uiState.value.availableExercises.find { it.id == set.exerciseId }?.defaultRestSeconds ?: 90

                startRestTimer(restSeconds)
            }
        }
    }

    private fun startRestTimer(seconds: Int) {
        restTimerJob?.cancel()
        updateState {
            copy(
                isRestTimerActive = true,
                restTimerTotalSeconds = seconds,
                restTimerRemainingSeconds = seconds
            )
        }

        restTimerJob = launchOperation {
            while (isActive && uiState.value.restTimerRemainingSeconds > 0) {
                delay(1000)
                val remaining = uiState.value.restTimerRemainingSeconds - 1
                updateState {
                    copy(
                        restTimerRemainingSeconds = remaining,
                        isRestTimerActive = remaining > 0
                    )
                }
                if (remaining == 0) {
                    platformFeedback.vibrateTimerComplete()
                    platformFeedback.playTimerSound()
                    sendEffect(ActiveWorkoutContract.Effect.ShowSnackbar("Odpočet dokončen! Jdeme na další sérii! 💪"))
                }
            }
        }
    }

    private fun adjustRestTimer(deltaSeconds: Int) {
        val current = uiState.value.restTimerRemainingSeconds
        val updated = (current + deltaSeconds).coerceAtLeast(0)
        updateState {
            copy(
                restTimerRemainingSeconds = updated,
                isRestTimerActive = updated > 0
            )
        }
        if (updated == 0) {
            restTimerJob?.cancel()
        }
    }

    private fun skipRestTimer() {
        restTimerJob?.cancel()
        updateState {
            copy(
                isRestTimerActive = false,
                restTimerRemainingSeconds = 0
            )
        }
    }

    private fun updateWeight(set: WorkoutSet, weightKg: Double) {
        launchOperation {
            workoutRepository.updateSet(set.id) { it.copy(weightKg = weightKg) }
        }
    }

    private fun updateReps(set: WorkoutSet, reps: Int) {
        launchOperation {
            workoutRepository.updateSet(set.id) { it.copy(reps = reps) }
        }
    }

    private fun updateRpe(set: WorkoutSet, rpe: Double?) {
        launchOperation {
            workoutRepository.updateSet(set.id) { it.copy(rpe = rpe) }
        }
    }

    private fun changeSetType(set: WorkoutSet, newType: WorkoutSetType) {
        launchOperation {
            workoutRepository.updateSet(set.id) { it.copy(setType = newType) }
        }
    }

    private fun deleteSet(set: WorkoutSet) {
        launchOperation { workoutRepository.deleteSet(set.id) }
    }

    fun cancelTimers() {
        restTimerJob?.cancel()
        workoutTimerJob?.cancel()
        restTimerJob = null
        workoutTimerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        cancelTimers()
    }

    private fun finishWorkout() {
        val session = uiState.value.session ?: return
        terminalCommand {
            when (completeWorkout.complete(session.id)) {
                is core.result.Result.Success -> {
                    cancelTimers()
                    sendEffect(ActiveWorkoutContract.Effect.WorkoutFinished)
                }
                is core.result.Result.Error -> onOperationFailure(DataError.Local.UNKNOWN)
            }
        }
    }

    private fun cancelWorkout() {
        val session = uiState.value.session ?: return
        terminalCommand {
            workoutRepository.cancelWorkout(session.id)
            cancelTimers()
            sendEffect(ActiveWorkoutContract.Effect.WorkoutCancelled)
        }
    }

    private fun terminalCommand(block: suspend () -> Unit) {
        if (uiState.value.isSaving) return
        updateState { copy(isSaving = true, error = null) }
        launchOperation {
            // Only this accepted command owns the lock; observation failures cannot release it.
            try { block() } finally { updateState { copy(isSaving = false) } }
        }
    }
}
