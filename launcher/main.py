import json,os,subprocess,sys,threading,webbrowser,urllib.error
from pathlib import Path
from PySide6.QtCore import Qt,QObject,Signal,QRunnable,QThreadPool,QTimer,QLockFile,QRect
from PySide6.QtGui import QPainter,QPixmap,QColor,QLinearGradient,QFontDatabase,QFont
from PySide6.QtWidgets import QApplication,QWidget,QVBoxLayout,QHBoxLayout,QLabel,QPushButton,QProgressBar,QSpinBox,QMessageBox
import minecraft_launcher_lib as mc
import auth,updater,server_status,interface

BASE=Path(getattr(sys,'_MEIPASS',Path(__file__).resolve().parent))
BRAND=BASE/'branding' if (BASE/'branding').exists() else BASE.parent/'branding'
CONFIG=json.loads((BASE/'config.json').read_text())
DATA=Path(os.environ.get('LOCALAPPDATA',str(Path.home()/'.local/share')))/'Wombatcraft'
DATA.mkdir(parents=True,exist_ok=True)
INSTANCE=DATA/'instance';RUNTIME=DATA/'minecraft'

class Signals(QObject):
 status=Signal(str);success=Signal(object);error=Signal(str)
class Work(QRunnable):
 def __init__(self,fn):super().__init__();self.fn=fn;self.signals=Signals()
 def run(self):
  try:self.signals.success.emit(self.fn(self.signals.status.emit))
  except Exception as e:
   # Authentication tokens and full HTTP bodies must never be shown or logged.
   message=str(e) if isinstance(e,(RuntimeError,ValueError)) else 'The operation could not finish. Check your connection and retry.'
   if isinstance(e,urllib.error.HTTPError) and e.code==404:message='The first public release is being prepared. Check Releases for availability.'
   self.signals.error.emit(message[:500])

class Window(QWidget):
 def __init__(self):
  super().__init__();self.account=None;self.busy=False;self.game=None;self.log=None;self.release_available=False;self.checking_server=False
  interface.build(self,BRAND,CONFIG,DATA)
  self.timer=QTimer(self);self.timer.timeout.connect(self.poll_game);self.timer.start(1500)
  self.server_timer=QTimer(self);self.server_timer.timeout.connect(self.check_server);self.server_timer.start(30000)
  self.set_busy(False);QTimer.singleShot(200,self.check_release);QTimer.singleShot(300,self.check_server)
 def set_busy(self,busy):
  self.busy=busy
  idle=not busy and self.game is None
  self.sign.setEnabled(idle and bool(CONFIG['microsoft_client_id']))
  self.play.setEnabled(idle and self.release_available and bool(CONFIG['microsoft_client_id']))
  self.install.setEnabled(idle);self.memory.setEnabled(idle)
  self.install.setText('Install / check for updates' if self.release_available else 'Check for a release')
  self.logout_button.setEnabled(idle and self.account is not None)
  self.progress.setRange(0,0 if busy else 1);self.progress.setValue(0)
 def run(self,fn,success):
  if self.busy or self.game:return
  self.set_busy(True);self.worker=Work(fn);self.worker.signals.status.connect(self.status.setText);self.worker.signals.error.connect(self.failed)
  self.worker.signals.success.connect(lambda value:(self.set_busy(False),success(value)))
  QThreadPool.globalInstance().start(self.worker)
 def failed(self,message):self.set_busy(False);self.status.setText(message)
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
 def sign_in(self):
  self.status.setText('Complete sign-in in your browser.');self.run(lambda progress:auth.login(CONFIG),self.signed_in)
 def signed_in(self,account):self.account=account;self.account_label.setText('Welcome, '+account['name']);self.sign.setText('Switch Microsoft account');self.set_busy(False);self.status.setText('Signed in. Play will check and install the latest release.' if self.release_available else 'Signed in. Check Releases for pack availability.')
 def sign_out(self):
  auth.logout();self.account=None;self.account_label.setText('Your next expedition starts here.');self.sign.setText('Sign in with Microsoft');self.set_busy(False);self.status.setText('Signed out on this computer.')
 def prepare(self,progress):
  progress('Checking the latest published release…');m=updater.fetch_json(CONFIG['manifest_url']);updater.validate_manifest(m)
  stage=DATA/'staging'/m['commit'];records=updater.stage(m,stage,DATA/'cache',progress=progress)
  progress('Installing Minecraft and Java…');callback={'setStatus':progress}
  mc.install.install_minecraft_version(m['minecraft'],str(RUNTIME),callback)
  java=mc.runtime.get_executable_path('java-runtime-gamma',str(RUNTIME))
  if not java:
   mc.runtime.install_jvm_runtime('java-runtime-gamma',str(RUNTIME),callback);java=mc.runtime.get_executable_path('java-runtime-gamma',str(RUNTIME))
  if not java:raise RuntimeError('Java 17 installation did not finish. Retry installation.')
  version=m['minecraft']+'-'+m['forge'];installed=mc.forge.forge_to_installed_version(version)
  if not (RUNTIME/'versions'/installed/(installed+'.json')).exists():mc.forge.install_forge_version(version,str(RUNTIME),callback,java=java)
  progress('Applying verified pack files…');updater.activate(m,records,stage,INSTANCE,DATA/'transaction')
  # Tell Windows to prefer the high-performance GPU for this Java runtime.
  if sys.platform=='win32':
   import winreg
   with winreg.CreateKey(winreg.HKEY_CURRENT_USER,r'Software\Microsoft\DirectX\UserGpuPreferences') as key:winreg.SetValueEx(key,str(Path(java).resolve()),0,winreg.REG_SZ,'GpuPreference=2;')
  return dict(manifest=m,java=java,version=installed)
 def installed(self,result):
  self.release_available=True;self.set_busy(False);suffix='Choose Play to join.' if self.account else 'Sign in to play.' if CONFIG['microsoft_client_id'] else 'Use Modrinth to play while Microsoft sign-in is unavailable.';self.status.setText('Release '+result['manifest']['commit'][:8]+' is installed. '+suffix);self.progress.setValue(1)
 def play_game(self):
  memory=self.memory.value()
  def launch(progress):
   progress('Refreshing your Microsoft session…');account=auth.refresh(CONFIG);result=self.prepare(progress)
   options={'username':account['name'],'uuid':account['id'],'token':account['access_token'],'executablePath':result['java'],'gameDirectory':str(INSTANCE),'jvmArguments':['-Xms2G',f'-Xmx{memory}G'],'launcherName':'Wombatcraft','launcherVersion':'0.1.0','server':CONFIG['server_address'].split(':')[0],'port':CONFIG['server_address'].split(':')[1]}
   command=mc.command.get_minecraft_command(result['version'],str(RUNTIME),options)
   log=(DATA/'game.log').open('w',encoding='utf-8');process=subprocess.Popen(command,cwd=INSTANCE,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
   return process,log,account
  self.run(launch,self.started)
 def started(self,result):
  self.game,self.log,self.account=result;self.set_busy(False);self.status.setText('Minecraft is starting. The first launch can take several minutes.');self.play.setText('Minecraft is running')
 def poll_game(self):
  if self.game and self.game.poll() is not None:
   code=self.game.returncode;self.game=None;self.log.close();self.log=None;self.set_busy(False);self.play.setText('Play Wombatcraft');self.status.setText('Minecraft closed.' if code==0 else 'Minecraft stopped unexpectedly. Open Files and check game.log before retrying.')
 def closeEvent(self,event):
  if self.busy or self.game:
   QMessageBox.information(self,'Wombatcraft','Close Minecraft or let the current operation finish before closing the launcher.');event.ignore()
  else:event.accept()

if __name__=='__main__':
 app=QApplication(sys.argv);app.setApplicationName('Wombatcraft')
 lock=QLockFile(str(DATA/'launcher.lock'));lock.setStaleLockTime(0)
 if not lock.tryLock(100):QMessageBox.information(None,'Wombatcraft','Wombatcraft is already open.');sys.exit(1)
 updater.recover(INSTANCE,DATA/'transaction')
 window=Window()
 if '--compact' in sys.argv:window.resize(1000,680)
 window.show();sys.exit(app.exec())
