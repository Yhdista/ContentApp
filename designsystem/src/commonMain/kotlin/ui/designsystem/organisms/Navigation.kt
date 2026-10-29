package ui.designsystem.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
/** Natural-height navigation keeps labels and icons visible when text wraps. */
@Composable fun AppTabs(items: List<AppTabItem>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    Row(modifier.fillMaxWidth().background(AppStyle.colors.surface).navigationBarsPadding().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEachIndexed { index, item ->
            AppSelection(selected == index, {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { item.icon(); item.content() }
            }, { onSelect(index) }, Modifier.weight(1f))
        }
    }
}
