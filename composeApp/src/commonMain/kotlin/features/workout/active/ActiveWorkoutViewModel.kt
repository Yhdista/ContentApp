package features.workout.active

import androidx.lifecycle.viewModelScope
import core.common.AppClock
import core.common.ApplicationScope
import core.feedback.NoOpPlatformFeedback
import core.feedback.PlatformFeedback
import core.viewmodel.BaseViewModel
import features.workout.domain.Exercise
import features.workout.domain.MuscleGroup
import features.workout.domain.WorkoutRepository
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet
import features.workout.domain.WorkoutSetType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ActiveWorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val appScope: ApplicationScope,
    private val clock: AppClock,
    private val platformFeedback: PlatformFeedback = NoOpPlatformFeedback()
) : BaseViewModel<ActiveWorkoutContract.State, ActiveWorkoutContract.Intent, ActiveWorkoutContract.Effect>(
    ActiveWorkoutContract.State()
) {

    private var restTimerJob: Job? = null
    private var workoutTimerJob: Job? = null
    private var statsJob: Job? = null
    private val selectedExercises = mutableListOf<Exercise>()

    init {
        onIntent(ActiveWorkoutContract.Intent.LoadWorkout)
    }

    override fun handleIntent(intent: ActiveWorkoutContract.Intent) {
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
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            workoutRepository.ensureDefaultExercises()

            // Načtení katalogu cviků
            launch {
                workoutRepository.getExercises().collect { exercises ->
                    updateState { copy(availableExercises = exercises) }
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
                        workoutRepository.getSetsForSession(active.id).collect { sessionSets ->
                            val allEx = uiState.value.availableExercises
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
                } else {
                    setsCollectorJob?.cancel()
                    selectedExercises.clear()
                    updateState { copy(sets = emptyList(), workoutExercises = emptyList()) }
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

    private fun createCustomExercise(name: String, muscleGroup: MuscleGroup, defaultRestSeconds: Int) {
        viewModelScope.launch {
            val exercise = workoutRepository.createCustomExercise(name, muscleGroup, defaultRestSeconds)
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
        statsJob = viewModelScope.launch {
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

    private fun changeSetType(set: WorkoutSet, newType: WorkoutSetType) {
        viewModelScope.launch {
            workoutRepository.updateSet(set.copy(setType = newType))
        }
    }

    private fun deleteSet(set: WorkoutSet) {
        viewModelScope.launch {
            workoutRepository.deleteSet(set.id)
            val remaining = uiState.value.sets
                .filter { it.sessionId == set.sessionId && it.exerciseId == set.exerciseId && it.id != set.id }
                .sortedBy { it.setNumber }
            remaining.forEachIndexed { index, item ->
                val expectedNumber = index + 1
                if (item.setNumber != expectedNumber) {
                    workoutRepository.updateSet(item.copy(setNumber = expectedNumber))
                }
            }
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
        updateState { copy(session = null) }

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
