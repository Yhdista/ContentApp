package ui.designsystem.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import ui.designsystem.atoms.*
import ui.designsystem.nucleus.AppStyle

/** Title and actions occupy separate natural-height rows, including at large font scales. */
@Composable fun AppTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(modifier.fillMaxWidth().background(AppStyle.colors.background).statusBarsPadding().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            navigationIcon()
            Box(Modifier.weight(1f)) { title() }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, content = actions)
    }
}

data class AppTabItem(val content: @Composable () -> Unit, val icon: @Composable () -> Unit)
/** Narrow navigation and large text use full-width choices, preserving whole labels. */
@Composable fun AppTabs(items: List<AppTabItem>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    BoxWithConstraints(modifier.fillMaxWidth().background(AppStyle.colors.surface).navigationBarsPadding().padding(4.dp)) {
        val stacked = LocalDensity.current.fontScale > 1.3f || maxWidth / maxOf(items.size, 1) < 96.dp
        if(stacked) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items.forEachIndexed { index, item ->
                    AppSelection(selected == index, {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.clearAndSetSemantics {}) { item.icon() }
                            Box(Modifier.weight(1f)) { item.content() }
                        }
                    }, { onSelect(index) }, Modifier.fillMaxWidth())
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items.forEachIndexed { index, item ->
                    AppSelection(selected == index, {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.clearAndSetSemantics {}) { item.icon() }
                            item.content()
                        }
                    }, { onSelect(index) }, Modifier.weight(1f))
                }
            }
        }
    }
}
