package core.repository

import core.database.ContentDao
import core.database.ContentEntity
import core.model.ContentItem
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ContentRepository {
    fun getContentItems(): Flow<List<ContentItem>>
    suspend fun refreshContent()
}

class ContentRepositoryImpl(
    private val httpClient: HttpClient,
    private val contentDao: ContentDao
) : ContentRepository {

    override fun getContentItems(): Flow<List<ContentItem>> {
        return contentDao.getAllContent().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshContent() {
        // Mocking the network call as requested
        val response = mockFetchFromNetwork()

        if (response.isNotEmpty()) {
            contentDao.clearAll()
            contentDao.insertContent(response.map { it.toEntity() })
        }
    }

    private fun mockFetchFromNetwork(): List<ContentItem> {
        return listOf(
            ContentItem(1, "Modern Android with KMP", "Explore the world of Kotlin Multiplatform.", "https://example.com/image1.jpg", listOf("KMP", "Android")),
            ContentItem(2, "Compose Multiplatform UI", "Build beautiful UIs for all platforms.", "https://example.com/image2.jpg", listOf("Compose", "UI")),
            ContentItem(3, "Room for Shared State", "Persist your data across platforms with Room.", "https://example.com/image3.jpg", listOf("Room", "Database"))
        )
    }
}

private fun ContentEntity.toDomain() = ContentItem(
    id = id,
    title = title,
    description = description,
    imageUrl = imageUrl,
    tags = tags.split(",").filter { it.isNotBlank() }
)

private fun ContentItem.toEntity() = ContentEntity(
    id = id,
    title = title,
    description = description,
    imageUrl = imageUrl,
    tags = tags.joinToString(",")
)
