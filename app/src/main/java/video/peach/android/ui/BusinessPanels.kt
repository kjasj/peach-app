package video.peach.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import video.peach.android.AppViewModel
import video.peach.android.data.*

@Composable fun TasksContent(payload: JSONObject, vm: AppViewModel, modifier: Modifier = Modifier) {
    val sections = listOf("running" to "正在进行", "skipped" to "未执行", "finished" to "最近完成", "runs" to "任务记录")
    LazyColumn(modifier, contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "management_shortcuts", contentType = "shortcuts") {
            Text("常用管理", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { vm.navigate("/api/downloads") }) { Text("云下载") }
                TextButton(onClick = { vm.navigate("/api/library-processing") }) { Text("资料入库") }
                TextButton(onClick = { vm.navigate("/api/stats") }) { Text("馆藏统计") }
            }
        }
        payload.text("message").takeIf(String::isNotBlank)?.let { message -> item(key = "tasks_message", contentType = "notice") { Notice(message) } }
        for ((key, title) in sections) {
            val rows = payload.optJSONArray(key)?.objects().orEmpty()
            if (payload.has(key)) {
                item(key = "section_header_$key", contentType = "section_header") { Text("$title · ${rows.size}", style = MaterialTheme.typography.titleMedium) }
                if (rows.isEmpty()) item(key = "empty_$key", contentType = "empty_hint") { Text(if (key == "running") "当前没有正在运行的任务" else "暂无记录", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(
                    items = rows,
                    key = { row -> "task_${key}_${row.text("id", "run_id", "task_key", "started_at").ifBlank { row.hashCode().toString() }}" },
                    contentType = { "task_card" }
                ) { row -> TaskCard(row, vm) }
            }
        }
    }
}

@Composable private fun TaskCard(row: JSONObject, vm: AppViewModel) {
    var expanded by remember(row.text("id")) { mutableStateOf(false) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(row.text("task_label").ifBlank { Labels.of(row.text("task_key")) }, style = MaterialTheme.typography.titleMedium)
            Text(Presentation.value("status", row.text("status")), color = MaterialTheme.colorScheme.primary)
            val current = row.optInt("progress_current")
            val total = row.optInt("progress_total")
            if (total > 0) {
                LinearProgressIndicator(progress = { (current.toFloat() / total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Text("已处理 $current / $total", style = MaterialTheme.typography.bodySmall)
            }
            row.text("progress_label").takeIf(String::isNotBlank)?.let { Text(it) }
            row.text("error").takeIf(String::isNotBlank)?.let { Notice(it, true) }
            row.text("started_at").takeIf(String::isNotBlank)?.let { Text("开始时间：$it", style = MaterialTheme.typography.bodySmall) }
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "收起任务详情" else "查看任务详情") }
            if (expanded) RecordView(row, vm.client)
        }
    }
}

@Composable fun FollowSources(payload: JSONObject, vm: AppViewModel, modifier: Modifier = Modifier, selectRecord: (JSONObject) -> Unit) {
    val rows = payload.optJSONArray("sources")?.objects().orEmpty()
    LazyColumn(modifier, contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "follow_sources_header", contentType = "header") { Text("关注来源与分组", style = MaterialTheme.typography.titleMedium) }
        if (rows.isEmpty()) item(key = "empty_sources", contentType = "empty_state") { EmptyState("还没有关注来源", "通过上方添加来源录入来源链接后，可在动态页查看更新。") }
        items(
            items = rows,
            key = { row -> "source_${row.text("id", "url").ifBlank { row.hashCode().toString() }}" },
            contentType = { "source_card" }
        ) { row ->
            OutlinedCard(onClick = { selectRecord(row) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(row.text("label", "source_label", "name").ifBlank { "关注来源" }, style = MaterialTheme.typography.titleMedium)
                    Text(row.text("provider_label", "provider"), style = MaterialTheme.typography.bodySmall)
                    Text(if (row.flag("enabled")) "正在关注" else "已暂停关注", color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { vm.navigate("/api/follow", mapOf("source" to row.text("id")), section = "follow") }) { Text("查看更新") }
                }
            }
        }
    }
}
