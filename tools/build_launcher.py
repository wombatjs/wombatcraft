"""Run with the launcher build venv on Windows."""
import subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
args=[sys.executable,'-m','PyInstaller','--noconfirm','--clean','--windowed','--onedir','--name','Wombatcraft','--distpath',str(ROOT/'dist/launcher'),'--workpath',str(ROOT/'dist/launcher-build'),'--specpath',str(ROOT/'dist'),'--add-data',str(ROOT/'launcher/config.json')+';.', '--add-data',str(ROOT/'branding')+';branding',str(ROOT/'launcher/main.py')]
subprocess.run(args,check=True)
print(ROOT/'dist/launcher/Wombatcraft/Wombatcraft.exe')
