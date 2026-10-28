package core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "sync_mutations")
data class SyncMutationEntity(
    @PrimaryKey val id: String, // Klientské UUID pro idempotenci
    val entityName: String,
    val recordId: String,
    val operation: String,      // INSERT, UPDATE, DELETE
    val payloadJson: String,
    val createdAt: Long,
    val retryCount: Int = 0,
    val lastError: String? = null
)
