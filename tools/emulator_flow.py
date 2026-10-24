"""从 UI 树定位控件，在隔离测试服务上验证原生流程。"""
import argparse
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("--adb", default="adb")
parser.add_argument("--serial", default="emulator-5554")
parser.add_argument("--url", default="http://127.0.0.1:18901")
parser.add_argument("--password", default="android-fixture")
parser.add_argument("--output", type=Path, default=Path("verification/screenshots"))
parser.add_argument("--step", choices=["connect", "capture", "play"], default="connect")
args = parser.parse_args()
args.output.mkdir(parents=True, exist_ok=True)

def adb(*commands):
    return subprocess.check_output([args.adb, "-s", args.serial, *commands])

def tree():
    for _ in range(6):
        data = adb("exec-out", "uiautomator", "dump", "/dev/tty").decode(errors="replace")
        if "<?xml" in data and "</hierarchy>" in data:
            start, end = data.index("<?xml"), data.index("</hierarchy>") + len("</hierarchy>")
            return ET.fromstring(data[start:end])
        time.sleep(.5)
    raise AssertionError("模拟器未提供可读取的 UI 树")

def tap(node):
    x0, y0, x1, y1 = map(int, re.findall(r"\d+", node.get("bounds")))
    adb("shell", "input", "tap", str((x0+x1)//2), str((y0+y1)//2))

def find(text=None, description=None):
    for _ in range(8):
        for node in tree().iter("node"):
            if text and node.get("text") == text or description and node.get("content-desc") == description:
                return node
        time.sleep(.5)
    raise AssertionError(f"未找到控件：{text or description}")

def fill(label, value):
    root = tree()
    for node in root.iter("node"):
        if node.get("class") == "android.widget.EditText" and any(child.get("text") == label for child in node.iter("node")):
            tap(node)
            time.sleep(.5)
            adb("shell", "input", "keyevent", "123")
            existing = node.get("text", "")
            if existing: adb("shell", "input", "keyevent", *(["67"] * (len(existing) + 2)))
            # 模拟器软键盘初始化期间，整串注入可能交换开头字符。
            for character in value:
                adb("shell", "input", "text", character)
                time.sleep(.04)
            adb("shell", "input", "keyevent", "4")
            return
    raise AssertionError(f"未找到输入框：{label}")

def capture(name):
    ET.ElementTree(tree()).write(args.output / f"{name}.xml", encoding="utf-8")
    (args.output / f"{name}.png").write_bytes(adb("exec-out", "screencap", "-p"))

if args.step == "connect":
    capture("connection")
    fill("服务器名称", "Peach-QA")
    fill("服务器地址", args.url)
    fill("访问口令", args.password)
    tap(find(text="连接 Peach"))
    find(description="切换服务器")
    capture("catalog")
elif args.step == "play":
    tap(find(text="TEST-001.mp4"))
    capture("detail")
    tap(find(text="开始播放"))
    time.sleep(3)
    capture("player")
else:
    capture("current")
print(json.dumps({"ok": True, "step": args.step, "screenshots": str(args.output)}, ensure_ascii=False))
