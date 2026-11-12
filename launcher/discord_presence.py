"""Publish launcher activity over local Discord IPC until Minecraft takes over."""

import asyncio
from contextlib import suppress
from pathlib import Path
import re
import tempfile
from threading import Event, Thread
from time import time
import tomllib

import official_launcher


RELEASES = 'https://github.com/wombatjs/wombatcraft/releases/latest'


def application_id(path):
    with path.open('rb') as config:
        general = tomllib.load(config)['general']
    if not general['enabled']:
        return ''
    value = general['applicationID']
    if not isinstance(value, str) or not re.fullmatch(r'[1-9][0-9]{16,19}', value):
        raise ValueError('Discord presence requires a valid Application ID.')
    return value


def repair_game_config(path):
    """Fill fields required by Simple RPC 4.1 without replacing player settings."""
    from tomlkit import parse, dumps
    from tomlkit.exceptions import ParseError

    try:
        config = parse(path.read_text(encoding='utf-8'))
    except (FileNotFoundError, ParseError):
        return False

    changed = False
    for section in config.values():
        if not isinstance(section, dict):
            continue
        cards = section.get('presence', [])
        if not isinstance(cards, list):
            continue
        for card in cards:
            if not isinstance(card, dict):
                continue
            for key, default in (('smallImageKey', []), ('smallImageText', '')):
                if key not in card:
                    card[key] = default
                    changed = True
    if not changed:
        return False

    fixed = dumps(config)
    temporary = None
    try:
        with tempfile.NamedTemporaryFile('w', encoding='utf-8', dir=path.parent, delete=False) as output:
            temporary = Path(output.name)
            output.write(fixed)
        temporary.replace(path)
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)
    return True


class LauncherPresence:
    def __init__(self, app_id, runtime, enabled):
        self.app_id = app_id
        self.runtime = runtime
        self.enabled = enabled
        self.stopped = Event()
        self.started_at = int(time())
        Thread(target=lambda: asyncio.run(self._run()), name='Discord presence', daemon=True).start()

    def set_enabled(self, enabled):
        self.enabled = enabled

    def close(self):
        self.stopped.set()

    def _available(self):
        return self.enabled and not self.stopped.is_set() and not official_launcher.game_running(self.runtime)

    async def _run(self):
        from pypresence import AioPresence
        from pypresence.exceptions import PyPresenceException

        ipc_errors = (PyPresenceException, OSError, ValueError, KeyError)
        while not self.stopped.is_set():
            if not self._available():
                await asyncio.sleep(2)
                continue

            client = AioPresence(self.app_id, connection_timeout=2, response_timeout=2)
            published = False
            retry = False
            try:
                await asyncio.wait_for(client.connect(), timeout=4)
                loop = asyncio.get_running_loop()
                next_update = 0
                while self._available():
                    if loop.time() >= next_update:
                        await client.update(
                            details='In the Wombatcraft Launcher',
                            state='Getting ready to play',
                            start=self.started_at,
                            large_image='wombatcraft',
                            large_text='Wombatcraft',
                            buttons=[{'label': 'Get Wombatcraft', 'url': RELEASES}],
                        )
                        published = True
                        next_update = loop.time() + 30
                    await asyncio.sleep(2)
            except ipc_errors:
                retry = True
            finally:
                if published:
                    with suppress(*ipc_errors):
                        await client.clear()
                if client.sock_writer is not None:
                    client.sock_writer.close()
            if retry:
                await asyncio.sleep(10)
