"""向后兼容脚本：重定向至 tools/package_apk.py"""
import subprocess
import sys
from pathlib import Path

script = Path(__file__).resolve().parent / "package_apk.py"
sys.exit(subprocess.call([sys.executable, str(script)]))
