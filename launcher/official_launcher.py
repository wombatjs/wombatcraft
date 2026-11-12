"""Windows handoff to an official Minecraft: Java Edition installation."""

from datetime import datetime, timezone
import json
import os
from pathlib import Path
import tempfile


DOWNLOAD_URL = 'https://www.minecraft.net/en-us/download/?platform=windows'
STORE_APP_ID = 'Microsoft.4297127D64EC6_8wekyb3d8bbwe!Minecraft'
PROFILE_KEY = 'wombatcraft-managed'
PROFILE_FILES = ('launcher_profiles.json', 'launcher_profiles_microsoft_store.json')


def find_launcher():
    import pythoncom
    import win32com.client

    pythoncom.CoInitialize()
    try:
        folder = win32com.client.Dispatch('Shell.Application').NameSpace('shell:AppsFolder')
        if folder is not None and folder.ParseName(STORE_APP_ID) is not None:
            return 'shell:AppsFolder\\' + STORE_APP_ID
    except pythoncom.com_error:
        pass
    finally:
        pythoncom.CoUninitialize()

    for key in ('ProgramFiles(x86)', 'ProgramFiles', 'LOCALAPPDATA'):
        if os.environ.get(key):
            executable = Path(os.environ[key]) / 'Minecraft Launcher' / 'MinecraftLauncher.exe'
            if executable.is_file():
                return str(executable)
    return None


def open_launcher(target):
    try:
        os.startfile(target)
    except OSError:
        raise RuntimeError('Windows could not open Minecraft Launcher. Open it from Start and select Wombatcraft.') from None


def minecraft_directory():
    return Path(os.environ['APPDATA']) / '.minecraft'


def profile_files(root):
    return [root / name for name in PROFILE_FILES if (root / name).is_file()]


def _processes():
    import psutil

    for process in psutil.process_iter(['name', 'exe']):
        yield (process.info.get('name') or '').lower(), process.info.get('exe')


def launcher_running():
    return any(name in {'minecraft.exe', 'minecraftlauncher.exe'} for name, _ in _processes())


def game_running(runtime):
    runtime = runtime.resolve()
    return any(
        executable and name in {'java.exe', 'javaw.exe'} and Path(executable).resolve().is_relative_to(runtime)
        for name, executable in _processes()
    )


def _read_profile_file(path):
    original = path.read_bytes()
    value = json.loads(original.decode('utf-8-sig'))
    if not isinstance(value, dict) or not isinstance(value.get('profiles', {}), dict):
        raise RuntimeError('Minecraft Launcher’s installations file could not be read.')
    return original, value


def profile_values(instance, version, java, memory):
    java = Path(java)
    if java.with_name('javaw.exe').is_file():
        java = java.with_name('javaw.exe')
    return {
        'name': 'Wombatcraft',
        'type': 'custom',
        'lastVersionId': version,
        'gameDir': str(instance.resolve()),
        'javaDir': str(java.resolve()),
        'javaArgs': f'-Xms2G -Xmx{memory}G',
    }


def profile_matches(paths, values):
    if not paths:
        return False
    for path in paths:
        _, data = _read_profile_file(path)
        entry = data.get('profiles', {}).get(PROFILE_KEY, {})
        if not isinstance(entry, dict) or any(entry.get(key) != value for key, value in values.items()):
            return False
    return True


def has_managed_profile(root):
    return any(PROFILE_KEY in _read_profile_file(path)[1].get('profiles', {}) for path in profile_files(root))


def register_profiles(paths, values):
    if not paths:
        raise RuntimeError('Open Minecraft Launcher and complete its first-time setup before installing Wombatcraft.')
    for path in paths:
        original, data = _read_profile_file(path)
        profiles = data.setdefault('profiles', {})
        entry = profiles.get(PROFILE_KEY, {})
        if not isinstance(entry, dict):
            raise RuntimeError('The existing Wombatcraft installation entry could not be read.')
        if all(entry.get(key) == value for key, value in values.items()):
            continue
        now = datetime.now(timezone.utc).isoformat(timespec='milliseconds').replace('+00:00', 'Z')
        profiles[PROFILE_KEY] = {'created': now, 'lastUsed': now, 'icon': 'Grass', **entry, **values}
        with tempfile.NamedTemporaryFile('w', encoding='utf-8', dir=path.parent, delete=False) as temporary:
            temp_path = Path(temporary.name)
            json.dump(data, temporary, indent=2, ensure_ascii=False)
        try:
            if launcher_running() or path.read_bytes() != original:
                raise RuntimeError('Close Minecraft Launcher, then click Play in Wombatcraft again to update its installation.')
            temp_path.replace(path)
        finally:
            temp_path.unlink(missing_ok=True)


def forge_ready(runtime, minecraft, forge, version):
    version_file = runtime / 'versions' / version / (version + '.json')
    vanilla = runtime / 'versions' / minecraft / (minecraft + '.jar')
    try:
        info = json.loads(version_file.read_text(encoding='utf-8'))
        game_args = info['arguments']['game']
        mcp = game_args[game_args.index('--fml.mcpVersion') + 1]
    except (OSError, ValueError, KeyError, IndexError, TypeError):
        return False
    if info.get('inheritsFrom') != minecraft or not isinstance(mcp, str) or not mcp.replace('.', '').isdigit():
        return False
    libraries = runtime / 'libraries'
    generated = libraries / 'net/minecraft/client' / f'{minecraft}-{mcp}'
    forge_version = f'{minecraft}-{forge}'
    return (
        vanilla.is_file()
        and (libraries / 'net/minecraftforge/forge' / forge_version / f'forge-{forge_version}-universal.jar').is_file()
        and all((generated / f'client-{minecraft}-{mcp}-{suffix}.jar').is_file() for suffix in ('extra', 'srg'))
    )
