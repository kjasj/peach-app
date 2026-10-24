package video.peach.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import video.peach.android.data.AppNavigation

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun NavigationMenu(selected: String, close: () -> Unit, select: (String) -> Unit, advanced: () -> Unit) {
    ModalBottomSheet(onDismissRequest = close, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
            item { Text("导航菜单", style = MaterialTheme.typography.headlineSmall); Spacer(Modifier.height(12.dp)) }
            items(AppNavigation.primary + AppNavigation.extras) { entry ->
                NavigationDrawerItem(label = { Text(entry.title) }, selected = selected == entry.key,
                    onClick = { close(); select(entry.key) }, modifier = Modifier.padding(vertical = 2.dp).testTag("nav-${entry.key}"))
            }
            item {
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                ListItem(headlineContent = { Text("其他功能与管理工具") }, modifier = Modifier.clickable { close(); advanced() })
            }
        }
    }
}
