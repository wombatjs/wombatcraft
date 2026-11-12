"""Run by systemd. Configuration and state live outside Git and player storage."""
import datetime,fcntl,json,os,shutil,subprocess,sys,tarfile,time,urllib.error
from pathlib import Path
from zoneinfo import ZoneInfo
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'launcher'))
import updater
from mc_status import status

STATE=Path('/var/lib/wombatcraft');STATE.mkdir(parents=True,exist_ok=True)
CONFIG=Path('/etc/wombatcraft/production.json')
def panel(action,*args):
 subprocess.run(['docker','cp',str(Path(__file__).with_name('panel_bridge.php')),'pterodactyl-panel-1:/tmp/wombatcraft-panel-bridge.php'],check=True,stdout=subprocess.DEVNULL,timeout=30)
 return subprocess.check_output(['docker','exec','pterodactyl-panel-1','php','/tmp/wombatcraft-panel-bridge.php',action,*args],text=True,timeout=45).strip()
def running(uuid):
 p=subprocess.run(['docker','inspect','--format','{{.State.Running}}',uuid],capture_output=True,text=True,timeout=20)
 return p.returncode==0 and p.stdout.strip()=='true'
def write(path,data):
 tmp=path.with_suffix('.new');tmp.write_text(json.dumps(data,indent=2));os.replace(tmp,path)
def wait_stopped(uuid):
 deadline=time.monotonic()+180
 while time.monotonic()<deadline:
  if not running(uuid):return
  time.sleep(3)
 raise RuntimeError('Graceful stop timed out; deployment aborted without killing the server')
def wait_healthy(host,instance):
 deadline=time.monotonic()+900
 while time.monotonic()<deadline:
  try:
   response=status(host)
   log=instance/'logs/latest.log'
   text=log.read_text(errors='replace') if log.exists() else ''
   if 'players' in response and ')! For help, type ' in text:
    time.sleep(20)
    status(host)
    return
  except (OSError,ValueError):pass
  time.sleep(10)
 raise RuntimeError('Minecraft did not become healthy within 15 minutes')
def snapshot(instance,backup):
 roots=[p for p in instance.iterdir() if p.name not in {'libraries','mods','logs','crash-reports','cache','packmenu','resourcepacks'} and (p.is_dir() or p.name in {'.wombatcraft.json','server.properties','ops.json','whitelist.json','banned-players.json','banned-ips.json','usercache.json'})]
 size=sum(p.stat().st_size for r in roots for p in ([r] if r.is_file() else r.rglob('*')) if p.is_file())
 if shutil.disk_usage(backup.parent).free<size+5*1024**3:raise RuntimeError('Insufficient free space for a rollback snapshot')
 with tarfile.open(backup,'w:gz',compresslevel=1) as tar:
  for p in roots:tar.add(p,arcname=p.name)
 return [p.name for p in roots]
def restore_snapshot(instance,backup,roots):
 # Retain the failed world/configuration for inspection instead of deleting it.
 quarantine=STATE/'failed-runtime'/str(int(time.time()));quarantine.mkdir(parents=True)
 for name in roots:
  p=instance/name
  if p.exists():shutil.move(str(p),quarantine/name)
 with tarfile.open(backup) as tar:tar.extractall(instance,filter='data')

def main():
 cfg=json.loads(CONFIG.read_text());instance=Path(cfg['instance']);uuid=cfg['uuid'];host=cfg.get('status_host','10.77.5.2')
 state_file=STATE/'state.json';state=json.loads(state_file.read_text()) if state_file.exists() else {}
 if cfg.get('paused',False):return
 now=datetime.datetime.now(ZoneInfo('America/Chicago'));today=now.date().isoformat()
 # First installation never treats the previous midnight as a missed restart.
 if 'last_daily' not in state:state['last_daily']=today;write(state_file,state)
 due=now.hour>=5 and state['last_daily']!=today
 current=json.loads((instance/'.wombatcraft.json').read_text()) if (instance/'.wombatcraft.json').exists() else {'commit':None,'managed':[]}
 try:m=updater.fetch_json(cfg.get('manifest_url',updater.RELEASE));updater.validate_manifest(m)
 except (OSError,ValueError):
  if not due:raise
  m=None
 pending=bool(m and m['commit']!=current['commit'] and m['commit']!=state.get('failed_commit'))
 if not due and not pending:return
 if not running(uuid):
  # Respect a deliberate panel stop. Initial deployment uses the bootstrap tool.
  print('Server is stopped; maintenance will not start it.');return
 players=status(host)['players']['online']
 if players:
  state.pop('empty_since',None)
  if not due:write(state_file,state);print('Update waits for players to leave.');return
  if not state.get('warned_at'):
   panel('command','say [Wombatcraft] Daily maintenance in 10 minutes. Please return somewhere safe.');state['warned_at']=time.time();write(state_file,state);return
  elapsed=time.time()-state['warned_at']
  if elapsed<540:return
  if elapsed<600:
   if not state.get('final_warning'):panel('command','say [Wombatcraft] Maintenance in one minute. The world will be saved.');state['final_warning']=True;write(state_file,state)
   return
 else:
  if not state.get('empty_since'):state['empty_since']=time.time();write(state_file,state);return
  if time.time()-state['empty_since']<300:return
 records=None
 if pending:
  stage=STATE/'staging'/m['commit'];records=updater.stage(m,stage,STATE/'cache','server',print)
  # Downloads may take minutes; players could have joined while staging.
  if status(host)['players']['online'] and not due:print('Players joined while staging; postponing activation.');return
 panel('command','save-all flush');panel('stop');wait_stopped(uuid)
 backups=STATE/'backups';backups.mkdir(exist_ok=True);backup=backups/(now.strftime('%Y%m%d-%H%M%S')+'.tar.gz')
 try:roots=snapshot(instance,backup)
 except Exception:
  panel('start')
  raise
 rollback=STATE/'rollback-pack';rollback.mkdir(exist_ok=True)
 for name in current.get('managed',[]):
  p=updater.safe_path(instance,name)
  if p.is_file():
   dest=updater.safe_path(rollback,name);dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,dest)
 try:
  if pending:updater.activate(m,records,stage,instance,STATE/'transaction')
  subprocess.run(['chown','-R',cfg['owner'],str(instance)],check=True)
  panel('start');wait_healthy(host,instance)
 except Exception:
  if running(uuid):panel('stop');wait_stopped(uuid)
  if pending:
   for f in records:
    if f['path'] not in current['managed']:
     p=updater.safe_path(instance,f['path'])
     if p.is_file():p.unlink()
   for name in current['managed']:
    source=updater.safe_path(rollback,name)
    if source.exists():
     target=updater.safe_path(instance,name);target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,target)
   restore_snapshot(instance,backup,roots);state['failed_commit']=m['commit'];write(state_file,state)
   subprocess.run(['chown','-R',cfg['owner'],str(instance)],check=True)
   panel('start');wait_healthy(host,instance)
  raise
 state.update(last_daily=today,active_commit=m['commit'] if pending else current['commit']);state.pop('warned_at',None);state.pop('final_warning',None);state.pop('empty_since',None);write(state_file,state)
 print('Wombatcraft maintenance complete:',state['active_commit'])
 # Keep seven successful maintenance snapshots; never erase failed-runtime data.
 for old in sorted(backups.glob('*.tar.gz'))[:-7]:old.unlink()

if __name__=='__main__':
 with (STATE/'maintenance.lock').open('w') as lock:
  try:fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
  except BlockingIOError:sys.exit(0)
  main()
