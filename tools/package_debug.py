"""打包已经成功构建的调试 APK，不包含本机配置或签名密钥。"""
import hashlib
import json
from pathlib import Path
import shutil
import zipfile

root = Path(__file__).resolve().parents[1]
output = root / "app/build/outputs/apk/debug"
metadata = json.loads((output / "output-metadata.json").read_text())
element = metadata["elements"][0]
apk = output / element["outputFile"]
with zipfile.ZipFile(apk) as archive:
    assert {"AndroidManifest.xml", "classes.dex"}.issubset(archive.namelist()), "不是可用的 Android APK"
dist = root / "dist"
dist.mkdir(exist_ok=True)
destination = dist / f"Peach-Android-{element['versionName']}-debug.apk"
shutil.copyfile(apk, destination)
digest = hashlib.sha256(destination.read_bytes()).hexdigest()
(dist / "SHA256SUMS").write_text(f"{digest}  {destination.name}\n")
print(json.dumps({"apk": destination.name, "bytes": destination.stat().st_size, "sha256": digest}))
