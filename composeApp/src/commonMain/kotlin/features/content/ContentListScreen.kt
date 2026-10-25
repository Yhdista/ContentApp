package features.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.components.AppScaffold
import ui.components.ContentCard
import ui.components.EmptyStateView
import ui.components.LoadingScreen
import ui.components.ScreenErrorView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentListScreen(
    viewModel: ContentListViewModel,
    onNavigateToDetail: (Int) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    AppScaffold(
        screenName = "ContentList",
        topBar = {
            TopAppBar(
                title = { Text("Content Explore") }
            )
        }
    ) { padding ->
        when {
            state.isLoading && state.items.isEmpty() -> {
                LoadingScreen(modifier = Modifier.padding(padding))
            }
            state.error != null && state.items.isEmpty() -> {
                ScreenErrorView(
                    message = state.error ?: "Unknown error",
                    onRetry = { viewModel.onIntent(ContentListContract.Intent.LoadContent) },
                    modifier = Modifier.padding(padding)
                )
            }
            state.items.isEmpty() && !state.isLoading -> {
                EmptyStateView(
                    message = "Please check back later.",
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        ContentCard(
                            title = item.title,
                            description = item.description,
                            imageUrl = item.imageUrl,
                            onClick = {
                                viewModel.onIntent(ContentListContract.Intent.OnItemClick(item))
                                onNavigateToDetail(item.id)
                            }
                        )
                    }
                }
            }
        }
    }
}
