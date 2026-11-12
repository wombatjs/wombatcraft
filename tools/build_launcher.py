"""Run with the launcher build venv on Windows."""
import subprocess,sys,shutil,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
args=[sys.executable,'-m','PyInstaller','--noconfirm','--clean','--windowed','--onedir','--name','Wombatcraft','--distpath',str(ROOT/'dist/launcher'),'--workpath',str(ROOT/'dist/launcher-build'),'--specpath',str(ROOT/'dist'),'--add-data',str(ROOT/'launcher/config.json')+';.', '--add-data',str(ROOT/'branding')+';branding',str(ROOT/'launcher/main.py')]
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
assert json.loads(report.read_text())['window_created'], 'Packaged launcher did not create its window'
shutil.make_archive(str(ROOT/'dist/Wombatcraft-Launcher-Windows'),'zip',ROOT/'dist/launcher')
print(ROOT/'dist/launcher/Wombatcraft/Wombatcraft.exe')
