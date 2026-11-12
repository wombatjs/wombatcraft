"""Build the Forge 1.20.1 Immersive Portals compatibility patch."""

import io
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo


ROOT = Path(__file__).resolve().parents[1]
PATCH_NAME = "wombatcraft-portal-fix.jar"


def build_portal_patch():
    source = ROOT / "patches" / "portal-chunk-tracking"
    buffer = io.BytesIO()
    with ZipFile(buffer, "w", ZIP_DEFLATED) as jar:
        for path in sorted(source.rglob("*")):
            if path.is_file():
                entry = ZipInfo(path.relative_to(source).as_posix())
                content = path.read_text(encoding="utf-8").encode("utf-8")
                jar.writestr(entry, content, compress_type=ZIP_DEFLATED)
    return buffer.getvalue()
