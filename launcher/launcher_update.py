"""Install verified Windows launcher bundles alongside the running launcher."""

from dataclasses import dataclass
import hashlib
import json
import os
from pathlib import Path, PureWindowsPath
import re
import shutil
import stat
import subprocess
import tempfile
import urllib.parse
import urllib.request
import zipfile


REPO = 'wombatjs/wombatcraft'
RELEASE_API = f'https://api.github.com/repos/{REPO}/releases/latest'
BUILD_FILE = 'launcher-build.json'
ASSET_NAME = 'Wombatcraft-Launcher-Windows.zip'
EXE_NAME = 'Wombatcraft.exe'
MAX_DOWNLOAD = 200 * 1024**2
MAX_UNPACKED = 500 * 1024**2


@dataclass(frozen=True)
class Build:
    commit: str
    revision: int

    @classmethod
    def parse(cls, value):
        if (
            not isinstance(value, dict)
            or not isinstance(value.get('commit'), str)
            or not re.fullmatch(r'[0-9a-f]{40}', value['commit'])
            or type(value.get('revision')) is not int
            or value['revision'] < 0
        ):
            raise ValueError('Invalid launcher build information.')
        return cls(value['commit'], value['revision'])

    def metadata(self):
        return {'commit': self.commit, 'revision': self.revision}


def read_build(path):
    return Build.parse(json.loads(path.read_text(encoding='utf-8')))


def _request(url, timeout):
    request = urllib.request.Request(url, headers={'User-Agent': 'Wombatcraft-Launcher'})
    return urllib.request.urlopen(request, timeout=timeout)


def _asset(release, name, maximum):
    asset = next((item for item in release['assets'] if item.get('name') == name), None)
    if asset is None:
        return None
    expected_url = f"https://github.com/{REPO}/releases/download/{release['tag_name']}/{name}"
    if (
        asset.get('browser_download_url') != expected_url
        or asset.get('state') != 'uploaded'
        or not re.fullmatch(r'sha256:[0-9a-f]{64}', str(asset.get('digest', '')))
        or type(asset.get('size')) is not int
        or not 0 < asset['size'] <= maximum
    ):
        raise ValueError('Invalid launcher release asset.')
    return asset


def _download(asset, output):
    size = 0
    digest = hashlib.sha256()
    with _request(asset['browser_download_url'], 25) as response:
        destination = urllib.parse.urlsplit(response.geturl())
        if destination.scheme != 'https' or destination.hostname not in {
            'github.com', 'release-assets.githubusercontent.com', 'objects.githubusercontent.com',
        }:
            raise ValueError('Unexpected launcher download destination.')
        while chunk := response.read(1024 * 1024):
            size += len(chunk)
            if size > asset['size']:
                raise ValueError('Launcher download exceeded its published size.')
            digest.update(chunk)
            output.write(chunk)
    if size != asset['size'] or 'sha256:' + digest.hexdigest() != asset['digest']:
        raise ValueError('Launcher download failed its checksum check.')


def latest_build(current):
    from io import BytesIO

    with _request(RELEASE_API, 8) as response:
        release = json.loads(response.read(1024 * 1024 + 1))
    tag = release.get('tag_name', '')
    if release.get('draft') or release.get('prerelease') or not re.fullmatch(r'build-[0-9a-f]{40}', tag):
        raise ValueError('Invalid Wombatcraft release.')
    if tag == 'build-' + current.commit and current.revision > 0:
        return None
    descriptor = _asset(release, BUILD_FILE, 4096)
    if descriptor is None:
        # Releases before the self-updater do not contain launcher build information.
        return None
    info = BytesIO()
    _download(descriptor, info)
    build = Build.parse(json.loads(info.getvalue()))
    if tag != 'build-' + build.commit:
        raise ValueError('Launcher build information does not match the release.')
    if build.revision <= current.revision:
        return None
    asset = _asset(release, ASSET_NAME, MAX_DOWNLOAD)
    if asset is None:
        raise ValueError('The Windows launcher is missing from the release.')
    return build, asset


def _extract(archive, directory):
    seen = set()
    unpacked_size = 0
    with zipfile.ZipFile(archive) as bundle:
        for entry in bundle.infolist():
            parts = entry.filename.removesuffix('/').split('/')
            if (
                parts[0] != 'Wombatcraft'
                or (len(parts) == 1 and not entry.is_dir())
                or any(
                    not part or part in {'.', '..'} or part.endswith((' ', '.'))
                    or PureWindowsPath(part).is_reserved() or re.search(r'[\\:*?"<>|\x00-\x1f]', part)
                    for part in parts
                )
                or stat.S_ISLNK(entry.external_attr >> 16)
            ):
                raise ValueError('The launcher archive contains an unsafe path.')
            name = '/'.join(parts).casefold()
            if name in seen:
                raise ValueError('The launcher archive contains duplicate paths.')
            seen.add(name)
            unpacked_size += entry.file_size
            if unpacked_size > MAX_UNPACKED or len(seen) > 20_000:
                raise ValueError('The launcher archive exceeds its installation limits.')
        bundle.extractall(directory)


def _slot(root, build):
    slot = root / build.commit
    if slot.resolve().parent != root.resolve():
        raise ValueError('Launcher version folder is outside the update directory.')
    return slot


def active_executable(root, current):
    try:
        build = read_build(root / 'active.json')
        if build.revision <= current.revision:
            return None
        slot = _slot(root, build)
        if read_build(slot / '_internal' / BUILD_FILE) == build:
            executable = slot / EXE_NAME
            if executable.is_file() and executable.resolve().parent == slot.resolve():
                return executable
    except (OSError, ValueError):
        pass
    return None


def _launch_options():
    return {
        'env': {**os.environ, 'PYINSTALLER_RESET_ENVIRONMENT': '1'},
        'creationflags': getattr(subprocess, 'CREATE_NO_WINDOW', 0),
    }


def stage(root, build, asset, progress):
    root.mkdir(parents=True, exist_ok=True)
    slot = _slot(root, build)
    with tempfile.TemporaryDirectory(prefix='staging-', dir=root.resolve()) as temporary:
        scratch = Path(temporary)
        if not slot.exists():
            progress('Downloading the launcher update…')
            archive = scratch / 'launcher.zip'
            with archive.open('wb') as output:
                _download(asset, output)
            progress('Verifying and extracting the launcher update…')
            _extract(archive, scratch)
            candidate = scratch / 'Wombatcraft'
        else:
            candidate = slot
        if read_build(candidate / '_internal' / BUILD_FILE) != build:
            raise ValueError('The downloaded launcher does not match the release.')

        progress('Testing the updated launcher…')
        report = scratch / 'smoke.json'
        options = _launch_options()
        options['env']['QT_QPA_PLATFORM'] = 'offscreen'
        subprocess.run(
            [str(candidate / EXE_NAME), '--smoke-test', str(report)],
            cwd=candidate, timeout=30, check=True, **options,
        )
        result = json.loads(report.read_text(encoding='utf-8'))
        if result.get('window_created') is not True or Build.parse(result.get('build')) != build:
            raise ValueError('The updated launcher did not pass its startup test.')
        if candidate != slot:
            candidate.rename(slot)
    return slot / EXE_NAME


def start(executable, compact=False):
    args = [str(executable), '--after-update']
    if compact:
        args.append('--compact')
    return subprocess.Popen(args, cwd=executable.parent, **_launch_options())


def activate(root, build, executable, compact=False):
    active = root / 'active.json'
    previous = active.read_bytes() if active.exists() else None
    pending = root / 'active.tmp'
    pending.write_text(json.dumps(build.metadata()), encoding='utf-8')
    pending.replace(active)
    try:
        start(executable, compact)
    except OSError:
        if previous is None:
            active.unlink(missing_ok=True)
        else:
            pending.write_bytes(previous)
            pending.replace(active)
        raise


def prune(root, keep):
    if not root.is_dir():
        return
    for child in root.iterdir():
        if re.fullmatch(r'[0-9a-f]{40}', child.name) and child.name not in keep and child.is_dir():
            if child.resolve().parent == root.resolve() and not child.is_symlink():
                shutil.rmtree(child, ignore_errors=True)
