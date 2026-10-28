package features.trainingplans.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingPlanDao {
    @Transaction
    @Query("SELECT * FROM training_plans WHERE :includeArchived OR archived = 0 ORDER BY name, id")
    fun observeAll(includeArchived: Boolean): Flow<List<TrainingPlanSnapshot>>
    @Transaction
    @Query("SELECT * FROM training_plans WHERE id = :id")
    fun observeById(id: String): Flow<TrainingPlanSnapshot?>
    @Transaction
    @Query("SELECT * FROM training_plans WHERE id = :id")
    suspend fun getById(id: String): TrainingPlanSnapshot?
    @Transaction
    @Query("SELECT training_plans.* FROM training_plans INNER JOIN training_plan_selection ON training_plan_selection.planId = training_plans.id WHERE training_plan_selection.id = 0 AND training_plans.archived = 0")
    fun observeActive(): Flow<TrainingPlanSnapshot?>
    @Query("SELECT * FROM training_plan_progress WHERE planId = :planId")
    fun observeProgress(planId: String): Flow<PlanProgressEntity?>
    @Query("SELECT * FROM training_plan_progress WHERE planId = :planId")
    suspend fun getProgress(planId: String): PlanProgressEntity?
    @Insert
    suspend fun insertPlan(plan: TrainingPlanEntity)
    @Query("UPDATE training_plans SET name = :name, notes = :notes, revision = revision + 1 WHERE id = :id AND revision = :expectedRevision AND archived = 0")
    suspend fun updatePlan(id: String, name: String, notes: String, expectedRevision: Long): Int
    @Query("UPDATE training_plans SET archived = 1, revision = revision + 1 WHERE id = :id AND revision = :expectedRevision AND archived = 0")
    suspend fun archive(id: String, expectedRevision: Long): Int
    @Query("DELETE FROM training_plan_slots WHERE planId = :planId")
    suspend fun deleteSlots(planId: String)
    @Insert
    suspend fun insertSlots(slots: List<PlanSlotEntity>)
    @Insert
    suspend fun insertProgress(progress: PlanProgressEntity)
    @Query("UPDATE training_plan_progress SET nextSlotId = :nextSlotId, progressRevision = :revision, needsReview = :needsReview WHERE planId = :planId AND progressRevision = :expectedRevision")
    suspend fun updateProgress(planId: String, nextSlotId: String?, revision: Long, needsReview: Boolean, expectedRevision: Long): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun selectActive(selection: PlanSelectionEntity)
    @Query("UPDATE training_plan_selection SET planId = NULL WHERE id = 0 AND planId = :planId")
    suspend fun clearSelection(planId: String)
}
