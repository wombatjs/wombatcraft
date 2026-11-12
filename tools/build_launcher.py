"""Run with the launcher build venv on Windows."""
import json
import os
import shutil
import subprocess
import sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
DIST = ROOT / 'dist'
DIST.mkdir(exist_ok=True)
build_file = DIST / 'launcher-build.json'
revision = int(os.environ.get('GITHUB_RUN_NUMBER', '0'))
commit = os.environ['GITHUB_SHA'] if revision else (
    subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    if (ROOT / '.git').exists() else '0' * 40
)
build = {'commit': commit, 'revision': revision}
build_file.write_text(json.dumps(build) + '\n', encoding='utf-8')
discord_config = ROOT / 'pack/config/simple-rpc/simple-rpc.toml'
args=[sys.executable,'-m','PyInstaller','--noconfirm','--clean','--windowed','--onedir','--name','Wombatcraft','--distpath',str(ROOT/'dist/launcher'),'--workpath',str(ROOT/'dist/launcher-build'),'--specpath',str(ROOT/'dist'),'--add-data',str(ROOT/'launcher/config.json')+';.', '--add-data',str(build_file)+';.', '--add-data',str(ROOT/'branding')+';branding', '--add-data',str(discord_config)+';discord',str(ROOT/'launcher/main.py')]
subprocess.run(args,check=True)
# Qt 6.12 uses the Windows ICU API. PyInstaller can pick an unrelated ICU
# from PATH whose version-suffixed exports break QtCore at startup.
bundle=ROOT/'dist/launcher/Wombatcraft'
icu=(bundle/'_internal/icuuc.dll').resolve()
assert icu.is_relative_to(bundle.resolve())
icu.unlink(missing_ok=True)
report=ROOT/'dist/launcher-smoke.json'
report.unlink(missing_ok=True)
subprocess.run([str(bundle/'Wombatcraft.exe'),'--smoke-test',str(report)],check=True,timeout=30)
result = json.loads(report.read_text(encoding='utf-8'))
assert result['window_created'] and result['build'] == build, 'Packaged launcher did not start with the expected build metadata'
shutil.make_archive(str(ROOT/'dist/Wombatcraft-Launcher-Windows'),'zip',ROOT/'dist/launcher')
print(ROOT/'dist/launcher/Wombatcraft/Wombatcraft.exe')
