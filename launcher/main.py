import json,os,subprocess,sys,threading,webbrowser,urllib.error
from pathlib import Path
from PySide6.QtCore import Qt,QObject,Signal,QRunnable,QThreadPool,QTimer,QLockFile
from PySide6.QtGui import QPainter,QPixmap,QColor,QLinearGradient,QFontDatabase,QFont
from PySide6.QtWidgets import QApplication,QWidget,QVBoxLayout,QHBoxLayout,QLabel,QPushButton,QProgressBar,QSpinBox,QMessageBox
import minecraft_launcher_lib as mc
import auth,updater

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
  super().__init__();self.setWindowTitle('Wombatcraft');self.resize(1160,720);self.setMinimumSize(920,620)
  self.art=QPixmap(str(BRAND/'woodland.png'));self.account=None;self.busy=False;self.game=None;self.log=None
  fontid=QFontDatabase.addApplicationFont(str(BRAND/'Cinzel.ttf'));self.display=QFontDatabase.applicationFontFamilies(fontid)[0] if fontid>=0 else 'Georgia'
  self.setStyleSheet('''QWidget{color:#f5e6ca;font-family:"Segoe UI";font-size:15px} QLabel{background:transparent} QPushButton{background:#23382f;color:#f5e6ca;border:1px solid #55705b;border-radius:5px;padding:11px 18px;text-align:left} QPushButton:hover{background:#35543f} QPushButton:focus{border:2px solid #e3c698} QPushButton:disabled{color:#a9b5aa;background:#26352b} QPushButton#play{background:#ad673d;border:1px solid #e1bd85;font-size:20px;font-weight:600} QPushButton#play:hover{background:#c2814c} QProgressBar{background:#243a2d;border:0;height:5px} QProgressBar::chunk{background:#72c9bc} QSpinBox{background:#23382f;border:1px solid #55705b;padding:5px}''')
  outer=QVBoxLayout(self);outer.setContentsMargins(44,40,44,26)
  title=QLabel('WOMBATCRAFT');title.setStyleSheet(f'font-family:"{self.display}";font-size:46px;font-weight:600;color:#f5e6ca');outer.addWidget(title)
  tagline=QLabel('Build a home. Master the wild.');tagline.setStyleSheet('font-size:19px;color:#dfcfb2');outer.addWidget(tagline)
  outer.addStretch(1)
  self.panel=QWidget();self.panel.setFixedWidth(372);layout=QVBoxLayout(self.panel);layout.setContentsMargins(0,0,0,0);layout.setSpacing(12)
  self.account_label=QLabel('Your next expedition starts here.');layout.addWidget(self.account_label)
  self.sign=QPushButton('Sign in with Microsoft');self.sign.clicked.connect(self.sign_in);layout.addWidget(self.sign)
  self.play=QPushButton('Play Wombatcraft');self.play.setObjectName('play');self.play.clicked.connect(self.play_game);layout.addWidget(self.play)
  self.install=QPushButton('Install / check for updates');self.install.clicked.connect(lambda:self.run(self.prepare,self.installed));layout.addWidget(self.install)
  self.progress=QProgressBar();self.progress.setFixedHeight(5);self.progress.setTextVisible(False);self.progress.setRange(0,1);self.progress.setValue(0);layout.addWidget(self.progress)
  self.status=QLabel('Checking for a published release…');self.status.setWordWrap(True);self.status.setMinimumHeight(54);self.status.setStyleSheet('color:#e4d7bf;font-size:14px');layout.addWidget(self.status)
  settings=QHBoxLayout();settings.addWidget(QLabel('Memory'));self.memory=QSpinBox();self.memory.setRange(6,16);self.memory.setSuffix(' GB');self.memory.setValue(10);settings.addWidget(self.memory);settings.addStretch();layout.addLayout(settings)
  links=QHBoxLayout();folder=QPushButton('Files');folder.clicked.connect(lambda:os.startfile(str(DATA)));links.addWidget(folder);news=QPushButton('Releases');news.clicked.connect(lambda:webbrowser.open('https://github.com/wombatjs/wombatcraft/releases'));links.addWidget(news);self.logout_button=QPushButton('Sign out');self.logout_button.clicked.connect(self.sign_out);links.addWidget(self.logout_button);layout.addLayout(links)
  outer.addWidget(self.panel);outer.addSpacing(22)
  footer=QLabel('Minecraft Java 1.20.1  ·  Forge  ·  Official community server');footer.setStyleSheet('font-size:12px;color:#d1c4ac');outer.addWidget(footer)
  self.timer=QTimer(self);self.timer.timeout.connect(self.poll_game);self.timer.start(1500)
  QTimer.singleShot(200,self.check_release)
 def paintEvent(self,event):
  p=QPainter(self);p.setRenderHint(QPainter.SmoothPixmapTransform)
  p.drawPixmap(self.rect(),self.art)
  gradient=QLinearGradient(0,0,self.width()*.72,0);gradient.setColorAt(0,QColor(9,23,16,235));gradient.setColorAt(.65,QColor(9,23,16,180));gradient.setColorAt(1,QColor(9,23,16,0));p.fillRect(self.rect(),gradient)
 def set_busy(self,busy):
  self.busy=busy
  for w in [self.sign,self.play,self.install,self.memory,self.logout_button]:w.setEnabled(not busy and self.game is None)
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
  self.run(check,lambda m:self.status.setText('Release '+m['commit'][:8]+' is available. Install it or choose Play.'))
 def sign_in(self):
  self.status.setText('Complete sign-in in your browser.');self.run(lambda progress:auth.login(CONFIG),self.signed_in)
 def signed_in(self,account):self.account=account;self.account_label.setText('Welcome, '+account['name']);self.sign.setText('Switch Microsoft account');self.status.setText('Signed in. Ready for your next expedition.')
 def sign_out(self):
  auth.logout();self.account=None;self.account_label.setText('Your next expedition starts here.');self.sign.setText('Sign in with Microsoft');self.status.setText('Signed out on this computer.')
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
 def installed(self,result):self.status.setText('Release '+result['manifest']['commit'][:8]+' is installed. Sign in and choose Play.');self.progress.setValue(1)
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
 window=Window();window.show();sys.exit(app.exec())
