"""Offline client fixture using installed Forge libraries and an isolated minimal mod set."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import tomllib
import zipfile
import minecraft_launcher_lib

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'dist/public-pads-client'
RUNTIME=Path(os.environ['LOCALAPPDATA'])/'Wombatcraft/minecraft'

def main():
    mods=DEST/'mods';mods.mkdir(parents=True,exist_ok=True)
    jars={};metadata={}
    for path in (ROOT/'dist/server-smoke/mods').glob('*.jar'):
        with zipfile.ZipFile(path) as jar:
            if 'META-INF/mods.toml' not in jar.namelist():continue
            data=tomllib.loads(jar.read('META-INF/mods.toml').decode())
            for entry in data.get('mods',[]):jars[entry['modId']]=path;metadata[entry['modId']]=data
    queue=['wombatcraft_smp','tardis_refined_rwf'];selected=set()
    while queue:
        mod=queue.pop()
        if mod in selected or mod in {'forge','minecraft'}:continue
        selected.add(mod)
        for deps in metadata[mod].get('dependencies',{}).values():
            queue.extend(d['modId'] for d in deps if d.get('mandatory') and d.get('side')!='SERVER')
    for mod in selected:shutil.copy2(jars[mod],mods/jars[mod].name)
    jar=ROOT/'mods/wombatcraft-smp/build/libs/wombatcraft-smp-0.1.0-staging.jar';shutil.copy2(jar,mods/jar.name)
    (DEST/'options.txt').write_text('onboardAccessibility:false\npauseOnLostFocus:false\nmaxFps:60\n')
    # No account files or tokens are read. This client only renders fixtures and exits.
    options={'username':'PadPreview','uuid':'00000000000000000000000000000001','token':'0','gameDirectory':str(DEST),
        'executablePath':str(Path(os.environ.get('JAVA_HOME','C:/Users/yeete/.jdks/ms-17.0.18'))/'bin/javaw.exe'),
        'jvmArguments':['-Xmx3G','-Dwombatcraft.padsPreview=true'], 'customResolution':True,'resolutionWidth':'960','resolutionHeight':'720'}
    command=minecraft_launcher_lib.command.get_minecraft_command('1.20.1-forge-47.4.28',str(RUNTIME),options)
    with (ROOT/'dist/public-pads-client.log').open('w') as log:
        startup=subprocess.STARTUPINFO();startup.dwFlags|=subprocess.STARTF_USESHOWWINDOW;startup.wShowWindow=0
        process=subprocess.Popen(command,cwd=DEST,stdout=log,stderr=subprocess.STDOUT,startupinfo=startup)
        try:code=process.wait(timeout=360)
        except subprocess.TimeoutExpired:process.terminate();process.wait();raise
    text=(ROOT/'dist/public-pads-client.log').read_text(errors='replace')
    assert code==0 and 'PUBLIC_PADS_PREVIEW_COMPLETE' in text and 'PUBLIC_PADS_MONITOR_CLIENT_PASS' in text, 'Client preview failed; see dist/public-pads-client.log'
    assert len(list((DEST/'screenshots').glob('public-pads-*.png')))==6,'Missing preview screenshots'
    print('PUBLIC_PADS_CLIENT_PREVIEW_PASS',flush=True)

if __name__=='__main__':main()
