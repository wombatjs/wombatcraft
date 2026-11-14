"""Apply pack-owned presentation settings while the server is stopped."""
import json
from pathlib import Path
import shutil
import re


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
    # FTB copies defaults only when creating a world. Update just the TPA section
    # in existing worlds, retaining every other administrative setting.
    essentials = instance / 'world/serverconfig/ftbessentials.snbt'
    if essentials.exists():
        text = essentials.read_text()
        def tpa(match):
            body = match[2]
            for key, value in {'enabled': 'true', 'cooldown': '10', 'warmup': '3'}.items():
                body = re.sub(r'(?m)^(\s*' + key + r':\s*)\S+', lambda m: m[1] + value, body)
            return match[1] + body + match[3]
        updated, count = re.subn(r'(\btpa:\s*\{)([^{}]*)(\})', tpa, text)
        if count != 1:
            raise RuntimeError('Expected one FTB Essentials TPA section; left existing configuration unchanged.')
        essentials.write_text(updated)
