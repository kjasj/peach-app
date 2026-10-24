"""准备可重复的隔离 Peach 联调数据；拒绝使用已有真实数据库。"""
import argparse
import json
import os
from pathlib import Path
import sqlite3
import subprocess
import shutil

parser = argparse.ArgumentParser()
parser.add_argument("root", type=Path)
parser.add_argument("--peach", required=True)
parser.add_argument("--ffmpeg", required=True)
parser.add_argument("--existing-test-fixture", action="store_true")
args = parser.parse_args()
root = args.root.resolve()
marker = root / ".peach-android-test-fixture"
db = root / "data/database/ledger.db"
if db.exists() and not marker.exists() and not args.existing_test_fixture:
    raise SystemExit("拒绝写入未标记的已有数据库。请使用全新的测试目录。")
root.mkdir(parents=True, exist_ok=True)
marker.write_text("仅合成测试数据\n")
media = root / "media"
media.mkdir(exist_ok=True)
environment = dict(os.environ, PEACH_DATA_ROOT=str(root / "data"))
for index in [1, 2]:
    video = media / f"TEST-{index:03}.mp4"
    if not video.exists():
        subprocess.run([args.ffmpeg, "-hide_banner", "-loglevel", "error", "-f", "lavfi", "-i", f"testsrc2=size=640x360:rate=24", "-f", "lavfi", "-i", f"sine=frequency={440*index}:sample_rate=44100", "-t", "60", "-c:v", "libx264", "-preset", "ultrafast", "-pix_fmt", "yuv420p", "-c:a", "aac", "-movflags", "+faststart", str(video)], check=True)
(media / "TEST-001.zh.vtt").write_text("WEBVTT\n\n00:00:00.000 --> 00:00:06.000\nPeach 安卓原生播放器字幕测试\n\n00:00:06.000 --> 00:00:15.000\n长按画面临时加速，松开恢复\n")
if not (media / "TEST-003.mkv").exists():
    subprocess.run([args.ffmpeg, "-hide_banner", "-loglevel", "error", "-i", str(media / "TEST-001.mp4"), "-c", "copy", str(media / "TEST-003.mkv")], check=True)
for index in (1, 2):
    part = media / f"TEST-010-CD{index}.mp4"
    if not part.exists(): shutil.copyfile(media / f"TEST-{index:03}.mp4", part)
if not db.exists():
    subprocess.run([args.peach, "init", "--no-input", "--data-root", str(root / "data"), "--host", "127.0.0.1", "--port", "18901", "--mount", f"local={media}"], check=True, env=environment)
state = root / "data/state"
state.mkdir(exist_ok=True)
(state / "remote-reporting.json").write_text(json.dumps({"usage": False, "errors": False, "revision": "android-test-fixture"}))
subprocess.run([args.peach, "scan", "local"], check=True, env=environment)
snapshots = root / "data/generated/snapshots"
snapshots.mkdir(parents=True, exist_ok=True)
with sqlite3.connect(db) as connection:
    rows = connection.execute("SELECT id,name FROM asset WHERE lower(name) LIKE '%.mp4'").fetchall()
    for aid, name in rows:
        image = snapshots / f"{aid}.jpg"
        if not image.exists(): subprocess.run([args.ffmpeg, "-hide_banner", "-loglevel", "error", "-i", str(media/name), "-frames:v", "1", str(image)], check=True)
        duration = 18 if name == "TEST-001.mp4" else 15
        connection.execute("UPDATE asset SET snapshot_path=?,duration=?,width=640,height=360 WHERE id=?", (str(image), duration, aid))
        if name.startswith("TEST-010-"): connection.execute("UPDATE asset SET code='TEST-010' WHERE id=?", (aid,))
    connection.execute("UPDATE asset SET duration=18,width=640,height=360 WHERE name='TEST-003.mkv'")
print(json.dumps({"ok": True, "assets": len(rows), "fixture": str(root)}, ensure_ascii=False))
