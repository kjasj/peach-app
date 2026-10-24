package video.peach.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import video.peach.android.data.*

@OptIn(ExperimentalLayoutApi::class)
@Composable fun ActionDialog(endpoint: Endpoint, seed: JSONObject?, busy: Boolean, error: String?, onClose: () -> Unit, credentialGuides: Map<String, List<String>> = emptyMap(), onSubmit: (JSONObject) -> Unit) {
    val body = remember(endpoint, seed) { mutableStateMapOf<String, Any>() }
    var confirm by remember(endpoint) { mutableStateOf(false) }
    var invalid by remember { mutableStateOf<String?>(null) }
    val fields = remember(endpoint) { endpoint.fields.distinctBy { it.key }.sortedBy { when(it.key) { "action" -> 0; "id", "ids" -> 1; else -> 2 } } }
    LaunchedEffect(endpoint, seed) {
        for (field in fields) {
            val value = when {
                field.key in listOf("id", "item", "asset_id", "entity_id") && seed?.has("id") == true -> seed.opt("id")
                field.key in listOf("ids", "items", "asset_ids") && seed?.has("id") == true -> JSONArray().put(seed.opt("id"))
                field.key == "revision" && seed?.has("mutation_revision") == true -> seed.opt("mutation_revision")
                field.key == "action" && field.choices.isNotEmpty() -> field.choices.first()
                seed?.has(field.key) == true && field.key !in listOf("action", "kind") -> seed.opt(field.key)
                else -> field.default
            }
            if (value != null && value != JSONObject.NULL) body[field.key] = if (field.type == "boolean" && value is Number) value.toInt() != 0 else value
        }
    }
    fun submit() {
        invalid = null
        val result = JSONObject()
        body.forEach { (key, value) -> result.put(key, value) }
        onSubmit(result)
    }
    AlertDialog(onDismissRequest = { if (!busy) onClose() }, title = { Text(endpoint.title) }, text = {
        Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (seed != null) Text(titleOf(seed), style = MaterialTheme.typography.labelLarge)
            if (endpoint.destructive) Text("此操作会修改服务器数据或文件，请先确认预览结果和操作范围。", color = MaterialTheme.colorScheme.error)
            if (fields.isEmpty()) Text("确认后将在当前服务器执行此操作。")
            for (field in fields) {
                if (field.key == "provider" && endpoint.path == "/api/follow/credential") {
                    Text("凭据来源", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        credentialGuides.filterValues { it.isNotEmpty() }.forEach { (provider, keys) ->
                            FilterChip(selected = body["provider"] == provider, onClick = {
                                body["provider"] = provider
                                body["values"] = JSONObject().apply { keys.forEach { put(it, "") } }
                            }, label = { Text(provider) })
                        }
                    }
                } else if (field.choices.isNotEmpty()) {
                    Text(Labels.of(field.key), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        field.choices.forEach { option -> FilterChip(selected = body[field.key] == option, onClick = { body[field.key] = option }, label = { Text(Labels.of(option)) }) }
                    }
                } else NativeField(field.key, body[field.key] ?: when(field.type) { "boolean" -> false; "array" -> JSONArray(); "object" -> JSONObject(); else -> "" }, field.type) { value -> body[field.key] = value }
            }
            if (invalid != null) Text(invalid!!, color = MaterialTheme.colorScheme.error)
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = { Button(onClick = { if (endpoint.destructive || body["action"] in listOf("delete", "remove", "dispose", "apply") || body["operation"] in listOf("delete", "dispose", "clear")) confirm = true else submit() }, enabled = !busy) { Text(if (busy) "正在提交…" else "确认提交") } }, dismissButton = { TextButton(onClick = onClose, enabled = !busy) { Text("取消") } })
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("确认服务器操作") }, text = { Text("即将在当前连接的服务器执行“${endpoint.title}”。请确认选择范围，执行结果以服务器返回为准。") }, confirmButton = { Button(onClick = { confirm = false; submit() }) { Text("确认执行") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("返回检查") } })
}

@Composable fun NativeField(key: String, value: Any, type: String = "string", depth: Int = 0, onChange: (Any) -> Unit) {
    when {
        type == "boolean" || value is Boolean -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(Labels.of(key), Modifier.weight(1f).padding(top = 12.dp))
            Switch(checked = value as? Boolean ?: false, onCheckedChange = onChange)
        }
        value is JSONObject -> {
            var adding by remember(key) { mutableStateOf(false) }
            var newKey by remember(key) { mutableStateOf("") }
            Text(Labels.of(key), style = MaterialTheme.typography.titleSmall)
            Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((childKey, child) in value.values()) NativeField(childKey, child, depth = depth + 1) { updated -> onChange(JSONObject(value.toString()).put(childKey, updated)) }
                if (key != "values") TextButton(onClick = { adding = true }) { Text("添加映射字段") }
                else if (value.length() == 0) Text("请先选择凭据来源。", style = MaterialTheme.typography.bodySmall)
            }
            if (adding) AlertDialog(onDismissRequest = { adding = false }, title = { Text("添加映射字段") }, text = { OutlinedTextField(newKey, { newKey = it }, label = { Text("字段名称") }) }, confirmButton = { TextButton(onClick = { if (newKey.isNotBlank()) { onChange(JSONObject(value.toString()).put(newKey.trim(), "")); adding = false; newKey = "" } }) { Text("添加") } }, dismissButton = { TextButton(onClick = { adding = false }) { Text("取消") } })
        }
        value is JSONArray && (value.length() == 0 || value.opt(0) !is JSONObject) -> {
            var input by remember(key, value.toString()) { mutableStateOf((0 until value.length()).joinToString(", ") { value.get(it).toString() }) }
            OutlinedTextField(input, { input = it; onChange(JSONArray(it.split(',', '，').map(String::trim).filter(String::isNotBlank).map { entry -> if (key.endsWith("ids") || key == "ids") entry.toLongOrNull() ?: entry else entry })) }, label = { Text(Labels.of(key)) }, supportingText = { Text("多项用逗号分隔") }, modifier = Modifier.fillMaxWidth())
        }
        value is JSONArray -> {
            Text(Labels.of(key), style = MaterialTheme.typography.titleSmall)
            for (index in 0 until value.length()) NativeField("${Labels.of(key)} ${index + 1}", value.get(index), depth = depth + 1) { updated -> onChange(JSONArray(value.toString()).put(index, updated)) }
        }
        else -> {
            var input by remember(key, value.toString()) { mutableStateOf(value.takeUnless { it == JSONObject.NULL }?.toString().orEmpty()) }
            val secret = key.lowercase().let { it.contains("token") || it.contains("password") || it.contains("cookie") || it.contains("secret") }
            OutlinedTextField(input, { raw -> input = raw; onChange(if (type == "number" || value is Number) raw.toLongOrNull() ?: raw.toDoubleOrNull() ?: raw else raw) }, label = { Text(Labels.of(key)) }, modifier = Modifier.fillMaxWidth(), minLines = if (key in listOf("reason", "description", "template")) 2 else 1,
                keyboardOptions = KeyboardOptions(keyboardType = if (type == "number" || value is Number) KeyboardType.Decimal else KeyboardType.Text), visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None)
        }
    }
}

@Composable fun FilterDialog(endpoint: Endpoint, query: Map<String, String>, onClose: () -> Unit, onApply: (Map<String, String>) -> Unit) {
    val values = remember(endpoint, query) { mutableStateMapOf<String, String>().apply { putAll(query) } }
    AlertDialog(onDismissRequest = onClose, title = { Text("${endpoint.title} · 筛选") }, text = {
        Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val fields = endpoint.fields.map { it.key }.filterNot { it in listOf("offset", "seed") }.ifEmpty { listOf("q") }.distinct()
            for (key in fields) OutlinedTextField(values[key].orEmpty(), { values[key] = it }, label = { Text(Labels.of(key)) }, modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = { Button(onClick = { onApply(values.toMap().filterValues { it.isNotBlank() } + ("offset" to "0")) }) { Text("应用筛选") } }, dismissButton = { TextButton(onClick = { onApply(emptyMap()) }) { Text("清除筛选") } })
}
