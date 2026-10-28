package core.storage

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(tableName = "editor_drafts", primaryKeys = ["kind", "id"])
data class EditorDraftEntity(val kind: String, val id: String, val payload: String)

@Dao
interface EditorDraftDao {
    @Query("SELECT payload FROM editor_drafts WHERE kind = :kind AND id = :id")
    suspend fun get(kind: String, id: String): String?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(draft: EditorDraftEntity)
    @Query("DELETE FROM editor_drafts WHERE kind = :kind AND id = :id")
    suspend fun delete(kind: String, id: String)
}
