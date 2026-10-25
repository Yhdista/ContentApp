package core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MutationDao {
    @Query("SELECT * FROM sync_mutations ORDER BY createdAt ASC")
    fun getPendingMutations(): Flow<List<SyncMutationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMutation(mutation: SyncMutationEntity)

    @Query("DELETE FROM sync_mutations WHERE id = :id")
    suspend fun deleteMutation(id: String)

    @Query("UPDATE sync_mutations SET retryCount = :retryCount, lastError = :error WHERE id = :id")
    suspend fun markError(id: String, error: String, retryCount: Int)
}
