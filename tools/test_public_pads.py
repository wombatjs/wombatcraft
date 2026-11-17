"""Two-boot integration test in the existing loopback-only disposable Forge server."""
import os
import argparse
from pathlib import Path
import shutil
import socket
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
SERVER = ROOT / 'dist/server-smoke'
JAR = ROOT / 'mods/wombatcraft-smp/build/libs/wombatcraft-smp-0.1.0-staging.jar'

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--boots',type=int,choices=[1,2],default=2);args=parser.parse_args()
    properties = (SERVER / 'server.properties').read_text()
    assert 'server-ip=127.0.0.1' in properties and 'server-port=25577' in properties, 'Expected disposable loopback server'
    with socket.socket() as check:
        assert check.connect_ex(('127.0.0.1',25577)) != 0, 'Disposable server is already running'
    shutil.copy2(JAR, SERVER / 'mods' / JAR.name)
    java = str(Path(os.environ.get('JAVA_HOME','C:/Users/yeete/.jdks/ms-17.0.18')) / 'bin' / ('java.exe' if os.name=='nt' else 'java'))
    argfile = 'win_args.txt' if os.name=='nt' else 'unix_args.txt'
    for boot in range(args.boots):
        shutil.copy2(JAR, SERVER / 'mods' / JAR.name)
        logfile = ROOT / f'dist/public-pads-boot-{boot+1}.log'
        with logfile.open('w') as log:
            process = subprocess.Popen([java,'-Dwombatcraft.padsQa=true','-Xms1G','-Xmx6G',f'@libraries/net/minecraftforge/forge/1.20.1-47.4.28/{argfile}','nogui'],cwd=SERVER,stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT,text=True)
            try:
                deadline=time.monotonic()+900
                while time.monotonic()<deadline:
                    text=logfile.read_text(errors='replace')
                    if process.poll() is not None: raise RuntimeError(f'Server exited; see {logfile}')
                    if 'PUBLIC_PADS_AUDIT_FAILED' in text or 'Failed to start the minecraft server' in text or 'Mod Loading has failed' in text: raise RuntimeError(f'Pad audit failed; see {logfile}')
                    if 'PUBLIC_PADS_AUDIT_COMPLETE' in text:
                        for hook in ['flight guards','placement guard','removal hook']:
                            assert f'Wombatcraft public pad {hook} applied' in text, 'Missing transformed hook: '+hook
                        if boot==1: assert 'PUBLIC_PADS_AUDIT_COMPLETE restored=true' in text, 'Restart persistence failed'
                        print(f'Public landing pads: boot {boot+1} passed',flush=True)
                        break
                    time.sleep(2)
                else: raise TimeoutError('Pad audit startup timed out')
            finally:
                if process.poll() is None:
                    process.stdin.write('stop\n');process.stdin.flush()
                    try: process.wait(timeout=120)
                    except subprocess.TimeoutExpired: process.kill();process.wait()
            assert process.returncode==0, 'Server did not stop cleanly'
    print('PUBLIC_PADS_TWO_BOOT_PASS' if args.boots==2 else 'PUBLIC_PADS_SINGLE_BOOT_PASS',flush=True)

if __name__=='__main__': main()
