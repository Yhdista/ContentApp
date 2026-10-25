package core.model

import kotlinx.serialization.Serializable

@Serializable
data class ContentItem(
    val id: Int,
    val title: String,
    val description: String,
    val imageUrl: String,
    val tags: List<String> = emptyList()
)
