import truststore
truststore.inject_into_ssl()

import json,os,sys,webbrowser,urllib.error
from pathlib import Path
from PySide6.QtCore import Qt,QObject,Signal,QRunnable,QThreadPool,QTimer,QLockFile,QRect
from PySide6.QtGui import QPainter,QPixmap,QColor,QLinearGradient,QFontDatabase,QFont
from PySide6.QtWidgets import QApplication,QWidget,QVBoxLayout,QHBoxLayout,QLabel,QPushButton,QProgressBar,QSpinBox,QMessageBox
import minecraft_launcher_lib as mc
from requests.exceptions import ConnectionError as RequestsConnectionError, Timeout as RequestsTimeout
from urllib3.exceptions import ProtocolError, ReadTimeoutError
import updater,server_status,interface,launcher_update,official_launcher
import discord_presence

BASE=Path(getattr(sys,'_MEIPASS',Path(__file__).resolve().parent))
BRAND=BASE/'branding' if (BASE/'branding').exists() else BASE.parent/'branding'
CONFIG=json.loads((BASE/'config.json').read_text())
PACKAGED = getattr(sys, 'frozen', False)
DISCORD_CONFIG = BASE/'discord/simple-rpc.toml' if PACKAGED else BASE.parent/'pack/config/simple-rpc/simple-rpc.toml'
DISCORD_APP_ID = discord_presence.application_id(DISCORD_CONFIG)
BUILD = launcher_update.read_build(BASE / launcher_update.BUILD_FILE) if PACKAGED else launcher_update.Build('0' * 40, 0)
DATA=Path(os.environ.get('LOCALAPPDATA',str(Path.home()/'.local/share')))/'Wombatcraft'
DATA.mkdir(parents=True,exist_ok=True)
INSTANCE=DATA/'instance';RUNTIME=DATA/'minecraft'
LAUNCHER_UPDATES = DATA / 'launcher-updates'
SETUP_NETWORK_ERRORS = (RequestsConnectionError, RequestsTimeout, ProtocolError, ReadTimeoutError)

class Signals(QObject):
 status=Signal(str);success=Signal(object);error=Signal(str)
class Work(QRunnable):
 def __init__(self,fn):super().__init__();self.fn=fn;self.signals=Signals()
 def run(self):
  try:self.signals.success.emit(self.fn(self.signals.status.emit))
  except Exception as e:
   # Authentication tokens and full HTTP bodies must never be shown or logged.
   message=str(e) if isinstance(e,(RuntimeError,ValueError,updater.DownloadInterrupted)) else 'The operation could not finish. Check your connection and retry.'
   if updater.disk_full(e):message='The Wombatcraft drive is full. Free up disk space, then retry.'
   if isinstance(e,urllib.error.HTTPError) and e.code==404:message='The first public release is being prepared. Check Releases for availability.'
   self.signals.error.emit(message[:500])

class Window(QWidget):
 def __init__(self):
  super().__init__();self.busy=False;self.game=None;self.release_available=False;self.checking_server=False
  interface.build(self,BRAND,CONFIG,DATA,discord_configured=bool(DISCORD_APP_ID))
  self.discord = None
  if DISCORD_APP_ID and '--smoke-test' not in sys.argv:
   self.discord = discord_presence.LauncherPresence(DISCORD_APP_ID, RUNTIME, self.discord_activity.isChecked())
   self.discord_activity.toggled.connect(self.discord.set_enabled)
  self.timer=QTimer(self);self.timer.timeout.connect(self.poll_game);self.timer.start(1500)
  self.server_timer=QTimer(self);self.server_timer.timeout.connect(self.check_server);self.server_timer.start(30000)
  self.set_busy(False)
  self.launcher_update_status.setText('Launcher build ' + BUILD.commit[:8] if BUILD.revision else 'Local launcher build')
  QTimer.singleShot(200, self.check_launcher_release if PACKAGED else self.check_release)
  QTimer.singleShot(300, self.check_server)
 def set_busy(self,busy):
  self.busy=busy
  idle=not busy and self.game is None
  self.play.setEnabled(idle)
  self.official_play.setEnabled(idle)
  self.install.setEnabled(idle);self.memory.setEnabled(idle)
  self.install.setText('Install / check for updates' if self.release_available else 'Check for a release')
  self.launcher_update_button.setEnabled(idle and PACKAGED)
  self.progress.setRange(0,0 if busy else 1);self.progress.setValue(0)
 def run(self,fn,success):
  if self.busy or self.game:return
  self.set_busy(True);self.worker=Work(fn);self.worker.signals.status.connect(self.status.setText);self.worker.signals.error.connect(self.failed)
  self.worker.signals.success.connect(lambda value:(self.set_busy(False),success(value)))
  QThreadPool.globalInstance().start(self.worker)
 def failed(self,message):self.set_busy(False);self.status.setText(message)
 def check_launcher_release(self):
  if not PACKAGED or self.busy or self.game:
   return

  def check(progress):
   progress('Checking for a launcher update…')
   release = launcher_update.latest_build(BUILD)
   if release is None:
    return None
   build, asset = release
   executable = launcher_update.stage(LAUNCHER_UPDATES, build, asset, progress)
   return build, executable

  self.set_busy(True)
  self.worker = Work(check)
  self.worker.signals.status.connect(self.status.setText)
  self.worker.signals.success.connect(self.launcher_checked)
  self.worker.signals.error.connect(self.launcher_update_failed)
  QThreadPool.globalInstance().start(self.worker)

 def launcher_checked(self, result):
  self.set_busy(False)
  if result is None:
   message = f'Launcher build {BUILD.commit[:8]} is up to date.' if BUILD.revision else 'No newer published launcher found.'
   self.launcher_update_status.setText(message)
   self.check_release()
   return
  build, executable = result
  try:
   launcher_update.activate(LAUNCHER_UPDATES, build, executable, '--compact' in sys.argv)
  except OSError:
   self.launcher_update_failed('The new launcher could not restart.')
   return
  try:
   launcher_update.prune(LAUNCHER_UPDATES, {build.commit, BUILD.commit})
  except OSError:
   pass
  self.close()

 def launcher_update_failed(self, message):
  self.set_busy(False)
  self.launcher_update_status.setText('Launcher update could not finish. ' + message)
  self.check_release()

 def check_release(self):
  def check(progress):
   m=updater.fetch_json(CONFIG['manifest_url']);updater.validate_manifest(m);return m
  self.run(check,self.release_checked)
 def release_checked(self,m):
  self.release_available=True;self.set_busy(False);self.status.setText('Release '+m['commit'][:8]+' is available. Install the pack to get ready.')
 def check_server(self):
  if self.checking_server:return
  self.checking_server=True;host,port=CONFIG['server_address'].rsplit(':',1)
  self.status_worker=Work(lambda progress:server_status.status(host,int(port)))
  self.status_worker.signals.success.connect(self.server_checked);self.status_worker.signals.error.connect(lambda message:self.server_unavailable())
  QThreadPool.globalInstance().start(self.status_worker)
 def server_checked(self,result):
  self.checking_server=False;players=result.get('players',{});self.server_label.setText(f'Online · {players.get("online",0)} / {players.get("max",0)} players')
 def server_unavailable(self):self.checking_server=False;self.server_label.setText('Status unavailable')
 def pack_manifest(self, progress):
  progress('Checking the latest published release…')
  manifest = updater.fetch_json(CONFIG['manifest_url'])
  updater.validate_manifest(manifest)
  return manifest

 def pack_ready(self, manifest):
  try:
   installed = json.loads((INSTANCE / '.wombatcraft.json').read_text(encoding='utf-8'))
   if installed.get('commit') != manifest['commit'] or not installed.get('controls_migrated'):
    return False
   for file in manifest['files']:
    if file['client']:
     path = updater.safe_path(INSTANCE, file['path'])
     if not updater.matches(path, file):
      return False
   return all(
    (updater.safe_path(INSTANCE, file['path']).is_file() if file.get('seed_only')
     else updater.matches(updater.safe_path(INSTANCE, file['path']),file))
    for file in manifest['overrides']['files'] if file['client']
   )
  except (OSError, ValueError, TypeError):
   return False

 def require_game_closed(self):
  if official_launcher.game_running(RUNTIME):
   raise RuntimeError('Wombatcraft is already running. Close Minecraft before updating or opening it again.')

 def require_pack_idle(self):
  self.require_game_closed()
  if official_launcher.launcher_running() and official_launcher.has_managed_profile(official_launcher.minecraft_directory()):
   raise RuntimeError('Close Minecraft Launcher before updating the Wombatcraft pack.')

 def repair_game_discord(self):
  if DISCORD_APP_ID and not official_launcher.game_running(RUNTIME):
   discord_presence.repair_game_config(INSTANCE/'config/simple-rpc/simple-rpc.toml')

 def sync_pack(self, manifest, progress):
  progress('Checking installed Wombatcraft files…')
  if not self.pack_ready(manifest):
   self.require_pack_idle()
   stage = DATA / 'staging' / manifest['commit']
   records = updater.stage(manifest, stage, DATA / 'cache', progress=progress, installed=INSTANCE)
   self.require_pack_idle()
   progress('Applying verified pack files…')
   updater.activate(manifest, records, stage, INSTANCE, DATA / 'transaction')
  launcher_update.prune(DATA / 'staging', keep=set())
  self.repair_game_discord()

 def install_pack(self, progress):
  manifest = self.pack_manifest(progress)
  self.sync_pack(manifest, progress)
  return {'manifest': manifest}

 def prepare(self, progress, manifest=None, runtime=None):
  m = manifest if manifest is not None else self.pack_manifest(progress)
  runtime = RUNTIME if runtime is None else runtime
  self.require_game_closed()
  self.sync_pack(m, progress)

  progress('Checking Java and Forge…')
  callback = {'setStatus': progress}
  java=mc.runtime.get_executable_path('java-runtime-gamma',str(RUNTIME))
  if not java:
   updater.retry_download(
    lambda: mc.runtime.install_jvm_runtime('java-runtime-gamma',str(RUNTIME),callback),
    'Java 17', network_errors=SETUP_NETWORK_ERRORS,
   )
   java=mc.runtime.get_executable_path('java-runtime-gamma',str(RUNTIME))
  if not java:raise RuntimeError('Java 17 installation did not finish. Retry installation.')
  version=m['minecraft']+'-'+m['forge'];installed=mc.forge.forge_to_installed_version(version)
  if not official_launcher.forge_ready(runtime, m['minecraft'], m['forge'], installed):
   updater.retry_download(
    lambda: mc.forge.install_forge_version(version,str(runtime),callback,java=java),
    'Minecraft and Forge', network_errors=SETUP_NETWORK_ERRORS,
   )
   if not official_launcher.forge_ready(runtime, m['minecraft'], m['forge'], installed):
    raise RuntimeError('Forge installation did not finish. Retry Play to repair it.')
  # Tell Windows to prefer the high-performance GPU for this Java runtime.
  if sys.platform=='win32':
   import winreg
   try:
    with winreg.CreateKey(winreg.HKEY_CURRENT_USER,r'Software\Microsoft\DirectX\UserGpuPreferences') as key:
     for executable in (Path(java), Path(java).with_name('javaw.exe')):
      if executable.is_file():
       winreg.SetValueEx(key, str(executable.resolve()), 0, winreg.REG_SZ, 'GpuPreference=2;')
   except PermissionError:
    pass
  return dict(manifest=m,java=java,version=installed)

 def prepare_official(self, progress, memory):
  target = official_launcher.find_launcher()
  if target is None:
   webbrowser.open(official_launcher.DOWNLOAD_URL)
   return 'Install Minecraft Launcher, open it once, then return to Wombatcraft and click Play.'

  root = official_launcher.minecraft_directory()
  profiles = official_launcher.profile_files(root)
  if not profiles:
   official_launcher.open_launcher(target)
   return 'Sign in, open Java Edition in Minecraft Launcher once, then close it and click Play here again.'

  self.require_game_closed()
  manifest = self.pack_manifest(progress)
  version = mc.forge.forge_to_installed_version(manifest['minecraft'] + '-' + manifest['forge'])
  if official_launcher.launcher_running():
   java = mc.runtime.get_executable_path('java-runtime-gamma', str(RUNTIME))
   if (
    not java
    or not self.pack_ready(manifest)
    or not official_launcher.forge_ready(root, manifest['minecraft'], manifest['forge'], version)
    or not official_launcher.profile_matches(profiles, official_launcher.profile_values(INSTANCE, version, java, memory))
   ):
    raise RuntimeError('Close Minecraft Launcher, then click Play here so Wombatcraft can prepare its installation.')
   self.repair_game_discord()
  else:
   result = self.prepare(progress, manifest, runtime=root)
   progress('Adding Wombatcraft to Minecraft Launcher…')
   official_launcher.register_profiles(
    profiles, official_launcher.profile_values(INSTANCE, version, result['java'], memory),
   )
  official_launcher.open_launcher(target)
  return 'In Minecraft Launcher, sign in and choose Wombatcraft under Java Edition, then click Play.'

 def play_with_official(self):
  memory = self.memory.value()
  self.run(lambda progress: self.prepare_official(progress, memory), self.status.setText)
 def installed(self,result):
  self.release_available=True;self.set_busy(False);self.status.setText('Release '+result['manifest']['commit'][:8]+' is installed. Choose Play to continue.');self.progress.setValue(1)
 def play_game(self):
  self.play_with_official()
 def poll_game(self):
  running = official_launcher.game_running(RUNTIME)
  if running != bool(self.game):
   self.game = True if running else None
   self.set_busy(self.busy)
   self.play.setText('Minecraft is running' if running else 'Play Wombatcraft')
 def closeEvent(self,event):
  if self.busy or self.game:
   QMessageBox.information(self,'Wombatcraft','Close Minecraft or let the current operation finish before closing the launcher.');event.ignore()
  else:
   if self.discord is not None:
    self.discord.close()
   event.accept()

if __name__=='__main__':
 app=QApplication(sys.argv);app.setApplicationName('Wombatcraft')
 if '--smoke-test' in sys.argv:
  # Exercise the packaged Qt imports, fonts, artwork and widgets without auth
  # or changes to an installed pack. A startup error cannot produce this file.
  from PySide6.QtCore import __version__ as qt_version
  report=Path(sys.argv[sys.argv.index('--smoke-test')+1])
  smoke=Window()
  smoke.show()
  report.write_text(json.dumps({'window_created':smoke.isVisible(),'qt':qt_version,'build':BUILD.metadata()}), encoding='utf-8')
  sys.exit(0)
 lock=QLockFile(str(DATA/'launcher.lock'));lock.setStaleLockTime(0)
 if not lock.tryLock(15_000 if '--after-update' in sys.argv else 100):
  QMessageBox.information(None,'Wombatcraft','Wombatcraft is already open.');sys.exit(1)
 if PACKAGED:
  installed = launcher_update.active_executable(LAUNCHER_UPDATES, BUILD)
  if installed is not None:
   try:
    launcher_update.start(installed, '--compact' in sys.argv)
   except OSError:
    pass
   else:
    lock.unlock()
    sys.exit(0)
 if not official_launcher.game_running(RUNTIME):
  updater.recover(INSTANCE,DATA/'transaction')
 window=Window()
 if '--compact' in sys.argv:window.resize(1000,680)
 window.show()
 result = app.exec()
 lock.unlock()
 sys.exit(result)
