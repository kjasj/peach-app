"""从指定 Peach 源码提取路由和参数，生成可重复核对的协议基线。"""
import argparse
import ast
import json
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument("source", type=Path)
parser.add_argument("--output", type=Path, default=Path(__file__).resolve().parents[1])
options = parser.parse_args()
source = options.source / "src/peach"
output = options.output
trees = {path.stem: (path, ast.parse(path.read_text())) for path in source.glob("*.py")}
functions = {}
for module, (path, tree) in trees.items():
    for node in tree.body:
        if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
            functions[(module, node.name)] = (path, node)

def literal(node, fallback=None):
    try:
        return ast.literal_eval(node)
    except (ValueError, TypeError):
        return fallback

def describe(module, name, parameter, seen=None):
    seen = set(seen or ())
    identity = (module, name, parameter)
    if identity in seen or len(seen) > 20:
        return {}, set()
    seen.add(identity)
    record = functions.get((module, name))
    if not record:
        return {}, set()
    path, function = record
    fields = {}
    choices = set()
    action_names = {"action"}
    field_choices = {}
    def is_parameter(node):
        return isinstance(node, ast.Name) and node.id == parameter or isinstance(node, ast.BoolOp) and any(is_parameter(value) for value in node.values)
    for node in ast.walk(function):
        if isinstance(node, ast.Call) and isinstance(node.func, ast.Attribute) and is_parameter(node.func.value) and node.func.attr == "get" and node.args and isinstance(literal(node.args[0]), str):
            key = literal(node.args[0])
            default = literal(node.args[1]) if len(node.args) > 1 else None
            kind = "boolean" if isinstance(default, bool) else "number" if isinstance(default, (int, float)) else "array" if isinstance(default, (list, tuple)) else "object" if isinstance(default, dict) else "string"
            if key in {"id", "asset_id", "entity_id", "source_id", "position", "duration", "delta", "limit", "offset", "rating", "index", "media", "seeks", "revision"}:
                kind = "number"
            if key in {"ids", "asset_ids", "source_ids", "tags"}: kind = "array"
            if key in {"enabled", "liked", "wanted", "ended", "watch_later", "approved", "hidden"}: kind = "boolean"
            fields[key] = {"key": key, "type": kind, "default": default, "required": False}
        if isinstance(node, ast.Subscript) and isinstance(node.value, ast.Name) and node.value.id == parameter and isinstance(literal(node.slice), str):
            key = literal(node.slice)
            fields.setdefault(key, {"key": key, "type": "number" if key in {"id", "asset_id", "entity_id", "revision"} else "string", "default": None, "required": False})
        if isinstance(node, ast.Compare) and isinstance(node.left, ast.Name) and node.left.id in action_names:
            for comparator in node.comparators:
                value = literal(comparator)
                if isinstance(value, str): choices.add(value)
                elif isinstance(value, (tuple, list)): choices.update(x for x in value if isinstance(x, str))
        if isinstance(node, ast.Compare) and isinstance(node.left, ast.Name):
            for comparator in node.comparators:
                value = literal(comparator)
                values = [value] if isinstance(value, str) else list(value) if isinstance(value, (tuple, list, set)) else []
                field_choices.setdefault(node.left.id, set()).update(x for x in values if isinstance(x, str))
        if isinstance(node, ast.Call):
            target_module, target_name = module, None
            if isinstance(node.func, ast.Name):
                target_name = node.func.id
                if (module, target_name) not in functions:
                    matches = [m for (m, n) in functions if n == target_name]
                    if len(matches) == 1: target_module = matches[0]
            elif isinstance(node.func, ast.Attribute) and isinstance(node.func.value, ast.Name):
                target_module, target_name = node.func.value.id, node.func.attr
            target = functions.get((target_module, target_name))
            if target:
                for index, argument in enumerate(node.args):
                    if isinstance(argument, ast.Name) and argument.id == parameter and index < len(target[1].args.args):
                        extra, nested_choices = describe(target_module, target_name, target[1].args.args[index].arg, seen)
                        for key, field in extra.items(): fields.setdefault(key, field)
                        choices.update(nested_choices)
    for key, field in fields.items():
        if field_choices.get(key): field["choices"] = sorted(field_choices[key])
    return fields, choices

routes = []
router = trees["web_router"][1]
imports = {}
for node in router.body:
    if isinstance(node, ast.ImportFrom):
        imports.update({item.asname or item.name: node.module for item in node.names})
for node in router.body:
    if not isinstance(node, ast.Assign) or not isinstance(node.targets[0], ast.Name) or node.targets[0].id not in {"GET_HANDLERS", "POST_HANDLERS"}: continue
    method = "GET" if node.targets[0].id == "GET_HANDLERS" else "POST"
    for key, handler in zip(node.value.keys, node.value.values):
        name = handler.id if isinstance(handler, ast.Name) else "lambda"
        module = imports.get(name, "web_router")
        record = functions.get((module, name))
        parameter = "args" if method == "GET" else "body"
        fields, choices = describe(module, name, parameter)
        if choices and "action" in fields: fields["action"]["choices"] = sorted(choices)
        routes.append({"method": method, "path": literal(key), "handler": f"{module}.{name}", "source": f"src/peach/{module}.py", "line": record[1].lineno if record else node.lineno, "fields": list(fields.values()), "dynamic": True, "verification": "pending"})

for module in ["routes_api", "routes_auth", "routes_configuration", "routes_media", "routes_subtitles", "routes_jav_browser"]:
    for function in trees[module][1].body:
        if not isinstance(function, (ast.FunctionDef, ast.AsyncFunctionDef)): continue
        for decorator in function.decorator_list:
            if not isinstance(decorator, ast.Call) or not isinstance(decorator.func, ast.Attribute) or not decorator.args: continue
            path = literal(decorator.args[0])
            if not isinstance(path, str): continue
            methods = [decorator.func.attr.upper()]
            if methods[0] == "API_ROUTE":
                methods = next((literal(kw.value, []) for kw in decorator.keywords if kw.arg == "methods"), [])
            local = any(isinstance(n, ast.Call) and isinstance(n.func, ast.Name) and n.func.id in {"local_only", "local_client"} for n in ast.walk(function))
            parameters = [arg.arg for arg in function.args.args if arg.arg not in {"request", "args", "body", "response"}]
            for method in methods:
                fields, _ = describe(module, function.name, "body")
                for parameter in ("payload", "sent"):
                    extra, _ = describe(module, function.name, parameter)
                    fields.update(extra)
                if method == "GET":
                    fields.update({name: {"key": name, "type": "string", "default": None, "required": False} for name in parameters})
                routes.append({"method": method, "path": path, "handler": f"{module}.{function.name}", "source": f"src/peach/{module}.py", "line": function.lineno, "local_only": local, "parameters": parameters, "fields": list(fields.values()), "dynamic": False, "verification": "pending"})

sha = subprocess.check_output(["git", "-C", str(options.source), "rev-parse", "HEAD"], text=True).strip()
overrides = {
    "/api/media-processing": [{"key": "metadata_only", "type": "boolean"}],
    "/api/thumbnail-jobs": [{"key": "mode", "choices": ["off", "coarse", "precise"]}],
    "/api/scraping/settings": [{"key": "network", "choices": ["peach", "direct"]}, {"key": "cookie"}, {"key": "cookies_text"}, {"key": "revoke", "type": "boolean"}],
    "/api/custom-metadata": [{"key": "fields", "type": "object", "default": {"code": "code", "title": "title"}}, {"key": "content_types", "type": "array", "default": ["censored"]}, {"key": "network", "choices": ["direct", "peach_proxy"]}, {"key": "code_format", "choices": ["original", "lowercase", "compact", "underscore"]}],
    "/api/follow/credential": [{"key": "values", "type": "object"}],
    "/api/follow/status": [{"key": "to", "choices": ["new", "seen", "saved", "ignored"]}],
    "/api/batch": [{"key": "operation", "choices": ["like", "seen", "later", "dispose", "restore", "delete", "dismiss-junk", "reconsider-junk", "region"]}],
    "/api/settings": [{"key": "postSetupTutorialDone", "type": "boolean"}, {"key": "searchHistoryLimit", "type": "number"}, {"key": "followInitialDays", "type": "number"}, {"key": "sidebarOrder", "type": "array"}, {"key": "organizeTemplates", "type": "array"}],
}
for route in routes:
    if route["method"] != "POST" or not route.get("dynamic"): continue
    fields = {field["key"]: field for field in route["fields"]}
    for field in overrides.get(route["path"], []):
        fields[field["key"]] = dict(fields.get(field["key"], {"type": "string", "required": False, "default": None}), **field)
    for key, field in fields.items():
        if key in {"items", "sources", "entries", "aliases", "selected_ids", "tags", "content_types", "sidebarOrder", "organizeTemplates"}: field["type"] = "array"
        if key in {"confirm", "background", "automatic", "backfill_all", "rewind", "older", "future", "recursive", "hidden", "enabled", "restart", "stop", "retry", "clean_cache", "on", "remove", "status_only", "revoke", "metadata_only", "postSetupTutorialDone"}: field["type"] = "boolean"
        if key in {"item", "media", "interval_minutes", "expected_revision", "searchHistoryLimit", "followInitialDays", "source_seed_asset_id", "job_id"}: field["type"] = "number" if key != "job_id" else "string"
    route["fields"] = list(fields.values())
guide_tree = trees["follow_secrets"][1]
guides = {}
for node in guide_tree.body:
    if isinstance(node, ast.Assign) and any(isinstance(target, ast.Name) and target.id == "CREDENTIAL_GUIDE" for target in node.targets): guides = literal(node.value, {})
    if isinstance(node, ast.AnnAssign) and isinstance(node.target, ast.Name) and node.target.id == "CREDENTIAL_GUIDE": guides = literal(node.value, {})
payload = {"source_commit": sha, "routes": routes, "credential_guides": guides, "note": "字段静态提取用于协议清点，不代表行为测试通过；action 分支和嵌套对象必须人工核对。"}
(output / "docs").mkdir(parents=True, exist_ok=True)
(output / "app/src/main/assets").mkdir(parents=True, exist_ok=True)
(output / "docs/protocol-inventory.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2))
(output / "app/src/main/assets/protocol.json").write_text(json.dumps(payload, ensure_ascii=False))
lines = ["# 功能与协议对照表", "", f"源版本：`{sha}`。静态登记不等于功能验收通过。", "", "| 方法 | 接口 | 源处理器 | 权限 | 验收 |", "|---|---|---|---|---|"]
for row in routes:
    lines.append(f"| {row['method']} | `{row['path']}` | `{row['handler']}` | {'服务器本机' if row.get('local_only') else '按原服务认证与业务权限'} | 待验收 |")
(output / "docs/FEATURE_MATRIX.md").write_text("\n".join(lines) + "\n")
print(f"已登记 {len(routes)} 个方法/路由，源版本 {sha[:8]}")
