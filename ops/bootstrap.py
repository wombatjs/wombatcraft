"""First installation into the newly created, stopped Pterodactyl instance."""
import json,subprocess,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'launcher'))
import updater
import server_branding
cfg=json.loads(Path('/etc/wombatcraft/production.json').read_text());instance=Path(cfg['instance']);state=Path('/var/lib/wombatcraft')
if (instance/'world').exists() or (instance/'.wombatcraft.json').exists():raise SystemExit('Initial installation only; use the maintenance controller for an existing world.')
manifest=json.loads(Path(sys.argv[1]).read_text());records=updater.stage(manifest,state/'initial-stage',state/'cache','server',print);updater.activate(manifest,records,state/'initial-stage',instance,state/'transaction')
(instance/'eula.txt').write_text('eula=true\n')
(instance/'server.properties').write_text('''server-ip=0.0.0.0
server-port=25565
online-mode=true
enforce-secure-profile=false
motd=Wombatcraft | Industry, spellcraft and a shared frontier
max-players=100
view-distance=8
simulation-distance=5
level-name=world
gamemode=survival
difficulty=normal
enable-command-block=false
enable-rcon=false
enable-query=false
white-list=false
spawn-protection=16
allow-flight=true
max-tick-time=180000
sync-chunk-writes=true
''')
voice=instance/'config/voicechat/voicechat-server.properties';voice.parent.mkdir(parents=True,exist_ok=True);voice.write_text('port=24454\nbind_address=0.0.0.0\nvoice_host=74.208.117.88:24454\n')
server_branding.apply(instance)
subprocess.run(['chown','-R',cfg['owner'],str(instance)],check=True)
print('Initial pack staged. Start through Pterodactyl after reviewing the manifest.')
