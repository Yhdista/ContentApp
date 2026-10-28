package features.routines.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import features.routines.api.Routine
import features.routines.api.RoutineExercise
import features.routines.api.RoutineSet
import features.workout.domain.WorkoutSetType

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val notes: String,
    val revision: Long,
    val archived: Boolean
)

@Entity(tableName = "routine_exercises", foreignKeys = [ForeignKey(
    entity = RoutineEntity::class, parentColumns = ["id"], childColumns = ["routineId"],
    onDelete = ForeignKey.CASCADE
)], indices = [Index("routineId")])
data class RoutineExerciseEntity(
    @PrimaryKey val id: String,
    val routineId: String,
    val exerciseId: String,
    val restSeconds: Int,
    val orderIndex: Int,
    val legacyExerciseName: String? = null,
    val legacyTargetSets: Int? = null,
    val legacyDefaultWeightKg: Double? = null,
    val legacyDefaultReps: Int? = null
)

@Entity(tableName = "routine_sets", foreignKeys = [ForeignKey(
    entity = RoutineExerciseEntity::class, parentColumns = ["id"], childColumns = ["routineExerciseId"],
    onDelete = ForeignKey.CASCADE
)], indices = [Index("routineExerciseId")])
data class RoutineSetEntity(
    @PrimaryKey val id: String,
    val routineExerciseId: String,
    val weightKg: Double,
    val reps: Int,
    val setType: String,
    val orderIndex: Int
)

// Room batches relation queries under the enclosing @Transaction snapshot.
data class RoutineExerciseSnapshot(
    @Embedded val entry: RoutineExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "routineExerciseId") val sets: List<RoutineSetEntity>
)

data class RoutineSnapshot(
    @Embedded val routine: RoutineEntity,
    @Relation(parentColumn = "id", entityColumn = "routineId", entity = RoutineExerciseEntity::class)
    val exercises: List<RoutineExerciseSnapshot>
)

internal fun RoutineSnapshot.toDomain() = Routine(
    routine.id, routine.name, routine.notes, routine.revision, routine.archived,
    exercises.sortedBy { it.entry.orderIndex }.map { snapshot ->
        val entry = snapshot.entry
        RoutineExercise(entry.id, entry.exerciseId, entry.restSeconds,
            snapshot.sets.sortedBy { it.orderIndex }.map {
                RoutineSet(it.id, it.weightKg, it.reps, WorkoutSetType.valueOf(it.setType))
            }, entry.legacyExerciseName?.let { features.routines.api.LegacyRoutineValues(it,
                entry.legacyTargetSets!!,entry.legacyDefaultWeightKg!!,entry.legacyDefaultReps!!) })
    }
)
