"""对用户授权服务执行只读联调；口令只从终端读取，输出不含馆藏详情。"""
import argparse
import getpass
import http.cookiejar
import json
from pathlib import Path
import urllib.parse
import urllib.request
import urllib.error
import uuid
import re

parser = argparse.ArgumentParser()
parser.add_argument("--base", required=True)
parser.add_argument("--output", type=Path, required=True)
args = parser.parse_args()
base = args.base.rstrip("/")
password = getpass.getpass("Peach 访问口令: ")
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
results = []

def read(path):
    operation = re.sub(r"/assets/\d+/", "/assets/{id}/", path.split("?")[0])
    try:
        with opener.open(base + path, timeout=25) as response:
            payload = json.loads(response.read())
            if payload.get("error"):
                raise ValueError("服务端返回业务错误")
            results.append({"operation": operation, "passed": True, "status": response.status})
            return payload
    except urllib.error.HTTPError as error:
        results.append({"operation": operation, "passed": False, "status": error.code})
    except Exception as error:
        results.append({"operation": operation, "passed": False, "error_type": type(error).__name__})
    return {}

read("/healthz")
request = urllib.request.Request(base + "/login", data=urllib.parse.urlencode({"token": password, "days": 0, "next": "/"}).encode(), headers={"Content-Type": "application/x-www-form-urlencoded", "Origin": base})
with opener.open(request, timeout=25) as response:
    response.read(1024)
del password
for route in ["libraries", "providers", "sources", "tasks", "downloads", "follow", "feeds", "playlists", "stats"]:
    read("/api/" + route)
items = read("/api/items?limit=2")
rows = items.get("items", [])
if rows:
    aid = rows[0]["id"]
    detail = read(f"/api/item?id={aid}")
    read(f"/api/assets/{aid}/subtitles")
    read(f"/api/timeline?id={aid}")
# 真实播放交互在用户设备另行验证，不对真实馆藏写入观看累计。
report = {"scope": "用户授权的真实服务，只读；仅登录创建会话", "results": results,
          "passed": sum(row["passed"] for row in results), "failed": sum(not row["passed"] for row in results),
          "privacy": "不记录服务器地址、口令、Cookie、作品编号、标题或返回正文"}
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2))
print(json.dumps({"passed": report["passed"], "failed": report["failed"]}))
