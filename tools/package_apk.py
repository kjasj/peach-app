"""打包已经成功构建的 APK (Release 与 Debug)，计算 SHA256 校验和。"""
import hashlib
import json
from pathlib import Path
import shutil
import zipfile

root = Path(__file__).resolve().parents[1]
dist = root / "dist"
dist.mkdir(exist_ok=True)

packaged = []
new_sums = {}

variants = [
    ("release", ""),
    ("debug", "-debug"),
]

for variant, suffix in variants:
    output_dir = root / f"app/build/outputs/apk/{variant}"
    metadata_file = output_dir / "output-metadata.json"
    if not metadata_file.exists():
        continue

    metadata = json.loads(metadata_file.read_text())
    element = metadata["elements"][0]
    apk = output_dir / element["outputFile"]
    if not apk.exists():
        continue

    with zipfile.ZipFile(apk) as archive:
        assert {"AndroidManifest.xml", "classes.dex"}.issubset(archive.namelist()), f"{apk} 不是有效的 Android APK"

    version_name = element.get("versionName", "unknown")
    dest_name = f"Peach-Android-{version_name}{suffix}.apk"
    destination = dist / dest_name
    shutil.copyfile(apk, destination)

    digest = hashlib.sha256(destination.read_bytes()).hexdigest()
    new_sums[dest_name] = digest
    packaged.append({
        "variant": variant,
        "name": dest_name,
        "size": destination.stat().st_size,
        "sha256": digest
    })

if new_sums:
    sums_file = dist / "SHA256SUMS"
    existing_sums = {}
    if sums_file.exists():
        for line in sums_file.read_text().splitlines():
            parts = line.strip().split(None, 1)
            if len(parts) == 2:
                existing_sums[parts[1]] = parts[0]
    existing_sums.update(new_sums)
    sums_content = "".join(f"{h}  {f}\n" for f, h in sorted(existing_sums.items()))
    sums_file.write_text(sums_content)

print(json.dumps(packaged, indent=2))
