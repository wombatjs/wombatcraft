"""Apply pack-owned presentation settings while the server is stopped."""
import json
from pathlib import Path
import shutil


def apply(instance):
    instance = Path(instance)
    source = instance / 'config/wombatcraft'
    if not (source / 'server.json').exists():
        return
    settings = json.loads((source / 'server.json').read_text())
    updates = {key: str(settings[key]) for key in ('motd', 'max-players')}
    def escape(value):
        return value.replace('\\', '\\\\').replace('\n', '\\n').encode('ascii', 'backslashreplace').decode().replace('\\x', '\\u00')
    path = instance / 'server.properties'
    lines = path.read_text().splitlines()
    output = [line for line in lines if line.partition('=')[0] not in updates]
    output.extend(key + '=' + escape(value) for key, value in updates.items())
    path.write_text('\n'.join(output) + '\n')
    shutil.copy2(source / 'server-icon.png', instance / 'server-icon.png')
