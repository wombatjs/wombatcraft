"""Keep the official server first while retaining every unrelated saved server."""
import os
from pathlib import Path
import shutil
import tempfile


def ensure_official(instance):
    import nbtlib
    path = Path(instance) / 'servers.dat'
    try:
        data = nbtlib.load(path, gzipped=False) if path.exists() else nbtlib.File({})
    except Exception as error:
        raise RuntimeError('Minecraft server list could not be read; it was left unchanged.') from error
    entries = list(data.get('servers', []))
    aliases = {'74.208.117.88', '74.208.117.88:25565'}
    official = next((entry for entry in entries if str(entry.get('ip', '')).lower() in aliases), None)
    if official is None:
        official = nbtlib.Compound()
    official['name'] = nbtlib.String('Wombatcraft | Official')
    official['ip'] = nbtlib.String('74.208.117.88:25565')
    data['servers'] = nbtlib.List[nbtlib.Compound]([official] + [entry for entry in entries if str(entry.get('ip', '')).lower() not in aliases])
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists():
        shutil.copy2(path, path.with_suffix('.dat.wombatcraft-backup'))
    fd, temporary = tempfile.mkstemp(dir=path.parent, suffix='.dat')
    os.close(fd)
    try:
        data.save(temporary, gzipped=False)
        os.replace(temporary, path)
    finally:
        Path(temporary).unlink(missing_ok=True)
