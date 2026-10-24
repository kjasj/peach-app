"""只对 prepare_fixture 创建的隔离服务执行契约验收。"""
import argparse
import http.cookiejar
import json
from pathlib import Path
import urllib.error
import urllib.parse
import urllib.request
import uuid
import re

parser = argparse.ArgumentParser()
parser.add_argument("fixture", type=Path)
parser.add_argument("--base", default="http://127.0.0.1:18901")
parser.add_argument("--password", default="android-fixture")
parser.add_argument("--output", type=Path, default=Path("verification/contract-results.json"))
args = parser.parse_args()
if not (args.fixture / ".peach-android-test-fixture").exists() or urllib.parse.urlsplit(args.base).hostname not in {"127.0.0.1", "localhost"}:
    raise SystemExit("仅允许对带标记的回环测试环境执行验收")
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
results = []

def request(path, body=None, *, form=False):
    data = None if body is None else (urllib.parse.urlencode(body).encode() if form else json.dumps(body).encode())
    headers = {"Accept": "application/json"}
    if data is not None: headers.update({"Origin": args.base, "Content-Type": "application/x-www-form-urlencoded" if form else "application/json"})
    request = urllib.request.Request(args.base + path, data=data, headers=headers)
    with opener.open(request, timeout=45) as response:
        raw = response.read()
        if form: return {}
        return json.loads(raw)

def check(name, path, body=None, assertion=lambda value: True):
    try:
        value = request(path, body)
        assert not value.get("error"), value.get("error")
        assert assertion(value), "响应未满足行为断言"
        results.append({"name": name, "path": path, "method": "GET" if body is None else "POST", "passed": True})
        return value
    except Exception as error:
        results.append({"name": name, "path": path, "method": "GET" if body is None else "POST", "passed": False, "error": str(error)})
        return {}

def expected_rejection(name, path, expected):
    try:
        value = request(path)
        passed = expected.lower() in str(value.get("error", "")).lower()
    except urllib.error.HTTPError as error:
        passed = expected.lower() in error.read().decode().lower()
    results.append({"name": name, "path": path, "method": "GET", "passed": passed, "evidence": "预期拒绝，无分组或缺少任务参数"})

request("/login", {"token": args.password, "days": 0}, form=True)
catalog = request("/api/items?limit=200")
if catalog.get("error") or not catalog.get("items") or catalog.get("total", len(catalog["items"])) > 20 or any(not re.fullmatch(r"TEST-\d{3}(?:-CD\d+)?\.(?:mp4|mkv)", item.get("name", "")) for item in catalog["items"]):
    raise SystemExit("服务内容与合成夹具不一致，拒绝执行任何写操作")
items = check("馆藏列表", "/api/items", assertion=lambda value: len(value.get("items", [])) >= 2).get("items", [])
aid = next(item["id"] for item in items if item.get("name") == "TEST-001.mp4")
hls_id = next(item["id"] for item in items if item.get("name") == "TEST-003.mkv")
parts_id = next(item["id"] for item in items if item.get("name") == "TEST-010-CD1.mp4")
check("分页", "/api/items?limit=1&offset=1", assertion=lambda value: len(value["items"]) == 1)
check("搜索", "/api/items?q=TEST-001", assertion=lambda value: any(row.get("id") == aid for row in value["items"]))
check("作品详情", f"/api/item?id={aid}", assertion=lambda value: value.get("id") == aid or value.get("item", {}).get("id") == aid)
for route in ["libraries", "providers", "sources", "facets", "tasks", "downloads", "folders", "playlists", "feeds", "feeds/check", "feeds/discoveries", "follow", "follow/credentials", "follow/tags", "follow/authors", "follow/schedule", "follow/check", "settings", "library-shortcuts", "stats", "taste", "taste/refresh", "library-processing", "media-processing", "thumbnail-jobs", "media-repair", "orphan-records", "organize", "duplicates", "ads", "quality-goals", "wants", "search-history", "custom-metadata", "scraping", "scraping/cover", "links/prune", "resource-sync/apply", "review", "post-setup-tutorial"]:
    check("读取 " + route, "/api/" + route)
for route in ["related", "parts", "timeline"]:
    target = parts_id if route in {"editions", "parts"} else aid
    check("读取 " + route, f"/api/{route}?id={target}")
expected_rejection("没有版本分组时给出原因", f"/api/editions?id={aid}", "edition group not found")
expected_rejection("缺少任务参数时拒绝", "/api/library-processing/issues", "任务已变化")
check("本地字幕", f"/api/assets/{aid}/subtitles", assertion=lambda value: len(value.get("subtitles", [])) >= 1)
check("喜欢", "/api/preference", {"id": aid, "liked": True, "reason": "合成测试"}, lambda value: value.get("liked") is True)
check("评分", "/api/feedback", {"id": aid, "kind": "rate", "value": 4})
check("稍后看", "/api/watch-later", {"id": aid}, lambda value: "watch_later" in value)
check("画质目标", "/api/quality-goal", {"id": aid, "wanted": True, "reason": "合成测试"})
check("播放计数", "/api/play", {"id": aid})
check("观看累计", "/api/activity", {"id": aid, "position": 4, "duration": 18, "delta": 2, "seeks": 1, "ended": False}, lambda value: value.get("play_seconds", 0) >= 2)
playlist = check("创建列表", "/api/playlist", {"action": "create", "name": "安卓协议测试", "asset_ids": [aid]})
pid = playlist.get("id") or playlist.get("playlist", {}).get("id")
if pid:
    check("读取列表", f"/api/playlist?id={pid}")
    check("重命名列表", "/api/playlist", {"action": "rename", "id": pid, "name": "安卓已验证"})
    check("删除测试列表", "/api/playlist", {"action": "delete", "id": pid}, lambda value: value.get("deleted") == pid)
check("保存业务偏好", "/api/settings", {"searchHistoryLimit": 20})
check("来源凭据撤销空白测试", "/api/follow/credential", {"provider": "fanbox", "values": {}})
session = str(uuid.uuid4())
check("播放计划", f"/api/stream-plan?id={aid}&session={session}-range", assertion=lambda value: value.get("protocol") in {"range", "hls"})
check("HLS 播放计划", f"/api/stream-plan?id={hls_id}&session={session}&mode=hls", assertion=lambda value: value.get("protocol") == "hls")
check("取消播放会话", f"/api/stream-cancel?session={session}", {})
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps({"results": results, "passed": sum(result["passed"] for result in results), "failed": sum(not result["passed"] for result in results), "limitations": ["外部来源、网盘账号和真实设备未由此脚本验证", "本次只验证合成数据上的指定行为，不代表全部功能通过"]}, ensure_ascii=False, indent=2))
print(json.dumps({"passed": sum(result["passed"] for result in results), "failed": [result for result in results if not result["passed"]]}, ensure_ascii=False))
