package core.repository

import core.common.AppClock
import core.database.ContentDao
import core.database.ContentEntity
import core.database.MutationDao
import core.database.SyncMutationEntity
import core.model.ContentItem
import core.model.ItemSyncStatus
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

interface ContentRepository {
    fun getContentItems(): Flow<List<ContentItem>>
    suspend fun addRecord(title: String, description: String, value: Int, category: String): ContentItem
    suspend fun refreshContent()
}

class ContentRepositoryImpl(
    private val httpClient: HttpClient,
    private val contentDao: ContentDao,
    private val mutationDao: MutationDao,
    private val clock: AppClock
) : ContentRepository {

    override fun getContentItems(): Flow<List<ContentItem>> {
        return contentDao.getAllContent().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addRecord(
        title: String,
        description: String,
        value: Int,
        category: String
    ): ContentItem {
        val now = clock.nowMillis()
        val clientUuid = "uuid-${now}-${(1000..9999).random()}"

        val entity = ContentEntity(
            title = title,
            description = description,
            value = value,
            category = category,
            syncStatus = "PENDING",
            updatedAt = now,
            clientUuid = clientUuid
        )

        // 1. Lokální optimistický zápis do Roomu
        val generatedId = contentDao.insertItem(entity).toInt()

        // 2. Vložení do outbox tabulky sync_mutations v témže okamžiku
        val mutation = SyncMutationEntity(
            id = clientUuid,
            entityName = "content_item",
            recordId = generatedId.toString(),
            operation = "INSERT",
            payloadJson = "{\"title\":\"$title\",\"value\":$value,\"category\":\"$category\"}",
            createdAt = now,
            retryCount = 0
        )
        mutationDao.insertMutation(mutation)

        return entity.copy(id = generatedId).toDomain()
    }

    override suspend fun refreshContent() {
        val initialItems = listOf(
            ContentEntity(title = "Benchmark KMP Sestavení", description = "Ověření rychlosti kompilace Kotlin 2.2 na Mac ARM64.", value = 10, category = "Vývoj", syncStatus = "SYNCED", updatedAt = clock.nowMillis() - 5000),
            ContentEntity(title = "Běh v lesním terénu", description = "Záznam tratě a kontrolních bodů (ORISapp styl).", value = 42, category = "Trénink", syncStatus = "SYNCED", updatedAt = clock.nowMillis() - 15000),
            ContentEntity(title = "Série: Mrtvý tah (3x5)", description = "Pracovní série s pauzou 180s (Hevy styl).", value = 140, category = "Workout", syncStatus = "SYNCED", updatedAt = clock.nowMillis() - 30000)
        )
        contentDao.insertContent(initialItems)
    }
}

private fun ContentEntity.toDomain() = ContentItem(
    id = id,
    title = title,
    description = description,
    value = value,
    category = category,
    syncStatus = when (syncStatus) {
        "SYNCED" -> ItemSyncStatus.SYNCED
        "ERROR" -> ItemSyncStatus.ERROR
        else -> ItemSyncStatus.PENDING
    },
    updatedAt = updatedAt,
    clientUuid = clientUuid
)
