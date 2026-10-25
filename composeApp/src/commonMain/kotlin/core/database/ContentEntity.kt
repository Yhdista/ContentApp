package core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "content_items")
data class ContentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val value: Int = 1,
    val category: String = "General",
    val syncStatus: String = "PENDING", // PENDING, SYNCED, ERROR
    val updatedAt: Long = 0L,
    val clientUuid: String = ""
)
