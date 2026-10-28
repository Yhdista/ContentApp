package core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ItemSyncStatus {
    LOCAL,
    PENDING,
    SYNCED,
    ERROR
}
