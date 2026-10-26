package features.workout.active

import androidx.lifecycle.viewModelScope
import core.common.AppClock
import core.common.ApplicationScope
import core.viewmodel.BaseViewModel
import features.workout.domain.Exercise
import features.workout.domain.WorkoutRepository
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ActiveWorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val appScope: ApplicationScope,
    private val clock: AppClock
) : BaseViewModel<ActiveWorkoutContract.State, ActiveWorkoutContract.Intent, ActiveWorkoutContract.Effect>(
    ActiveWorkoutContract.State()
) {

    private var restTimerJob: Job? = null
    private var workoutTimerJob: Job? = null
    private val selectedExercises = mutableListOf<Exercise>()

    init {
        onIntent(ActiveWorkoutContract.Intent.LoadWorkout)
    }

    override fun handleIntent(intent: ActiveWorkoutContract.Intent) {
        when (intent) {
            is ActiveWorkoutContract.Intent.LoadWorkout -> loadWorkout()
            is ActiveWorkoutContract.Intent.OpenExercisePicker -> updateState { copy(isPickerVisible = intent.isOpen) }
            is ActiveWorkoutContract.Intent.AddExerciseToWorkout -> addExercise(intent.exercise)
            is ActiveWorkoutContract.Intent.RemoveExerciseFromWorkout -> removeExercise(intent.exerciseId)
            is ActiveWorkoutContract.Intent.AddSet -> addSet(intent.exerciseId)
            is ActiveWorkoutContract.Intent.ToggleSetComplete -> toggleSetComplete(intent.set)
            is ActiveWorkoutContract.Intent.UpdateWeight -> updateWeight(intent.set, intent.weightKg)
            is ActiveWorkoutContract.Intent.UpdateReps -> updateReps(intent.set, intent.reps)
            is ActiveWorkoutContract.Intent.UpdateRpe -> updateRpe(intent.set, intent.rpe)
            is ActiveWorkoutContract.Intent.DeleteSet -> deleteSet(intent.set)
            is ActiveWorkoutContract.Intent.AdjustRestTimer -> adjustRestTimer(intent.deltaSeconds)
            is ActiveWorkoutContract.Intent.SkipRestTimer -> skipRestTimer()
            is ActiveWorkoutContract.Intent.FinishWorkout -> finishWorkout()
            is ActiveWorkoutContract.Intent.CancelWorkout -> cancelWorkout()
        }
    }

    private fun loadWorkout() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            workoutRepository.ensureDefaultExercises()

            // Načtení katalogu cviků
            launch {
                workoutRepository.getExercises().collect { exercises ->
                    updateState { copy(availableExercises = exercises) }
                }
            }

            // Načtení nebo založení aktivního tréninku
            val active = workoutRepository.getActiveSession().firstOrNull() ?: workoutRepository.startWorkout()
            updateState { copy(session = active, isLoading = false) }

            // Spuštění stopek tréninku
            startWorkoutTimer(active.startTimeEpochMs)

            // Sledování sérií aktivního tréninku
            workoutRepository.getSetsForSession(active.id).collect { sessionSets ->
                val allEx = uiState.value.availableExercises
                // Aktualizace seznamu cviků v tréninku
                val exerciseIds = sessionSets.map { it.exerciseId }.distinct()
                val exercisesInWorkout = exerciseIds.mapNotNull { id -> allEx.find { it.id == id } }
                
                selectedExercises.clear()
                selectedExercises.addAll(exercisesInWorkout)

                updateState {
                    copy(
                        sets = sessionSets,
                        workoutExercises = selectedExercises.toList()
                    )
                }
            }
        }
    }

    private fun startWorkoutTimer(startTime: Long) {
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = ((clock.nowMillis() - startTime) / 1000).coerceAtLeast(0L)
                updateState { copy(workoutElapsedSeconds = elapsed) }
                delay(1000)
            }
        }
    }

    private fun addExercise(exercise: Exercise) {
        if (selectedExercises.none { it.id == exercise.id }) {
            selectedExercises.add(exercise)
            updateState {
                copy(
                    workoutExercises = selectedExercises.toList(),
                    isPickerVisible = false
                )
            }
        } else {
            updateState { copy(isPickerVisible = false) }
        }
        addSet(exercise.id)
    }

    private fun removeExercise(exerciseId: String) {
        selectedExercises.removeAll { it.id == exerciseId }
        updateState { copy(workoutExercises = selectedExercises.toList()) }

        val sessionId = uiState.value.session?.id ?: return
        viewModelScope.launch {
            val setsToDelete = uiState.value.sets.filter { it.sessionId == sessionId && it.exerciseId == exerciseId }
            setsToDelete.forEach { set ->
                workoutRepository.deleteSet(set.id)
            }
        }
    }

    private fun addSet(exerciseId: String) {
        val sessionId = uiState.value.session?.id ?: return
        viewModelScope.launch {
            val existingSets = uiState.value.sets.filter { it.exerciseId == exerciseId }
            val lastSet = existingSets.lastOrNull()
            val weight = lastSet?.weightKg ?: 60.0
            val reps = lastSet?.reps ?: 10

            workoutRepository.addSet(
                sessionId = sessionId,
                exerciseId = exerciseId,
                weightKg = weight,
                reps = reps
            )
        }
    }

    private fun toggleSetComplete(set: WorkoutSet) {
        val targetComplete = !set.isCompleted
        viewModelScope.launch {
            workoutRepository.updateSet(set.copy(isCompleted = targetComplete))
        }

        // Auto-trigger Rest Timer při odškrtnutí
        if (targetComplete) {
            val exercise = uiState.value.workoutExercises.find { it.id == set.exerciseId }
                ?: uiState.value.availableExercises.find { it.id == set.exerciseId }
            val restSeconds = exercise?.defaultRestSeconds ?: 90

            startRestTimer(restSeconds)
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

        restTimerJob = viewModelScope.launch {
            while (isActive && uiState.value.restTimerRemainingSeconds > 0) {
                delay(1000)
                updateState {
                    val remaining = restTimerRemainingSeconds - 1
                    copy(
                        restTimerRemainingSeconds = remaining,
                        isRestTimerActive = remaining > 0
                    )
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
        viewModelScope.launch {
            workoutRepository.updateSet(set.copy(weightKg = weightKg))
        }
    }

    private fun updateReps(set: WorkoutSet, reps: Int) {
        viewModelScope.launch {
            workoutRepository.updateSet(set.copy(reps = reps))
        }
    }

    private fun updateRpe(set: WorkoutSet, rpe: Double?) {
        viewModelScope.launch {
            workoutRepository.updateSet(set.copy(rpe = rpe))
        }
    }

    private fun deleteSet(set: WorkoutSet) {
        viewModelScope.launch {
            workoutRepository.deleteSet(set.id)
        }
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
        cancelTimers()

        // Uložení chráněno v ApplicationScope pro přežití navigace a zničení ViewModelu
        appScope.launch {
            workoutRepository.completeWorkout(session.id)
            sendEffect(ActiveWorkoutContract.Effect.WorkoutFinished)
        }
    }

    private fun cancelWorkout() {
        val session = uiState.value.session ?: return
        cancelTimers()

        appScope.launch {
            workoutRepository.cancelWorkout(session.id)
            sendEffect(ActiveWorkoutContract.Effect.WorkoutCancelled)
        }
    }
}
