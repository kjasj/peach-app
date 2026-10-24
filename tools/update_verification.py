"""只合并证据，不把静态接口接入升级为业务验收通过。"""
import argparse
import json
import re
from pathlib import Path
from urllib.parse import urlsplit

parser = argparse.ArgumentParser()
parser.add_argument("--results", type=Path, required=True)
parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
args = parser.parse_args()
inventory = json.loads((args.root / "docs/protocol-inventory.json").read_text())
results = json.loads(args.results.read_text())
lines = ["# 功能与协议对照表", "", f"源版本：`{inventory['source_commit']}`。", "",
         "此表是路由登记和有限证据索引，不是完整的页面/操作验收表。协议通过仅证明列出的合成用例；未登记的隐含前端状态、字段条件及外部来源仍需人工核对。", "",
         "| 方法 | 接口 | 源处理器 | 安卓入口/权限 | 证据状态 |", "|---|---|---|---|---|"]
player_paths = {"/api/stream-plan", "/api/stream-cancel", "/api/activity", "/api/play", "/api/follow/activity", "/api/follow/play", "/follow-stream", "/follow-qualities", "/api/subtitles/search", "/api/subtitles/convert", "/api/timeline"}
for row in inventory["routes"]:
    matched = [case for case in results["results"] if case["method"] == row["method"] and re.fullmatch(re.sub(r"\{[^}]+\}", "[^/]+", row["path"]), urlsplit(case["path"]).path)]
    if row.get("local_only"):
        entry = "电脑端限制（不计安卓实现）"
    elif row["path"] in player_paths:
        entry = "原生播放器"
    elif row.get("dynamic"):
        entry = "对应领域 → 全部功能 / 记录操作表单"
    elif row["path"] in {"/login", "/healthz", "/api/libraries"}:
        entry = "连接与认证"
    else:
        entry = "专用路由：需人工核对入口和载荷"
    if matched:
        evidence = "；".join(("协议用例通过：" if case["passed"] else "失败：") + case["name"] for case in matched)
    else:
        evidence = "未验收"
    lines.append(f"| {row['method']} | `{row['path']}` | `{row['handler']}` | {entry} | {evidence} |")
(args.root / "docs/FEATURE_MATRIX.md").write_text("\n".join(lines) + "\n")
target = args.root / "verification/contract-results.json"
target.parent.mkdir(parents=True, exist_ok=True)
target.write_text(json.dumps(results, ensure_ascii=False, indent=2))
