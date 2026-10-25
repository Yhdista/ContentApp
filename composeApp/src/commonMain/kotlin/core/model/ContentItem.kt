package core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ItemSyncStatus {
    PENDING,
    SYNCED,
    ERROR
}

@Serializable
data class ContentItem(
    val id: Int = 0,
    val title: String,
    val description: String,
    val value: Int = 1,
    val category: String = "General",
    val syncStatus: ItemSyncStatus = ItemSyncStatus.PENDING,
    val updatedAt: Long = 0L,
    val clientUuid: String = "",
    val imageUrl: String = "",
    val tags: List<String> = emptyList()
)
