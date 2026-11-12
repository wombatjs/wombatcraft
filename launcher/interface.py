"""Native launcher presentation; installation and authentication live in main.py."""
import json, os, webbrowser
from PySide6.QtCore import Qt, QSize
from PySide6.QtGui import QColor, QFont, QFontDatabase, QIcon, QPainter, QPainterPath, QPixmap, QLinearGradient
from PySide6.QtWidgets import QWidget, QFrame, QLabel, QPushButton, QVBoxLayout, QHBoxLayout, QStackedWidget, QSpinBox, QProgressBar

STYLE='''
QWidget { color:#f5efff; font-family:"Segoe UI"; font-size:15px; }
QWidget#shell { background:#171220; }
QFrame#sidebar { background:#21192d; border-right:1px solid #392c49; }
QFrame#surface { background:#261e32; border:1px solid #392e47; border-radius:12px; }
QLabel { background:transparent; }
QLabel#muted { color:#c4b6d8; font-size:13px; }
QLabel#heading { font-size:22px; font-weight:600; }
QPushButton { background:#30243f; border:1px solid #59436e; border-radius:8px; padding:11px 16px; text-align:left; }
QPushButton:hover { background:#45305e; border-color:#b58aed; }
QPushButton:focus { border:2px solid #d9baff; }
QPushButton:disabled { color:#b7a5c8; background:#30243f; border-color:#4c3c5d; }
QPushButton#nav { background:transparent; border:1px solid transparent; padding:14px 16px; }
QPushButton#nav:checked { background:#4e2d78; border:1px solid #8051b0; font-weight:600; }
QPushButton#nav:hover { background:#352545; }
QPushButton#play { background:#783ee3; border:1px solid #af7cff; font-size:25px; font-weight:700; padding:17px 24px; }
QPushButton#play:hover { background:#8c51ee; }
QPushButton#play:disabled { background:#49325f; color:#baa5cf; border-color:#674d7e; }
QProgressBar { background:#362947; border:0; border-radius:2px; }
QProgressBar::chunk { background:#b27bff; border-radius:2px; }
QSpinBox { background:#21182d; border:1px solid #79579b; border-radius:6px; padding:9px; }
'''

def label(text, name=None, wrap=False):
 w=QLabel(text);w.setWordWrap(wrap)
 if name:w.setObjectName(name)
 return w

def surface():
 w=QFrame();w.setObjectName('surface');layout=QVBoxLayout(w);layout.setContentsMargins(22,20,22,20);layout.setSpacing(12);return w,layout

def button(text, action):
 w=QPushButton(text);w.clicked.connect(action);return w

class Banner(QWidget):
 def __init__(self, brand):
  super().__init__();self.art=QPixmap(str(brand/'sunset-banner.png'));self.setMinimumHeight(186);self.setMaximumHeight(240)
  fid=QFontDatabase.addApplicationFont(str(brand/'Silkscreen.ttf'));self.face=QFontDatabase.applicationFontFamilies(fid)[0] if fid>=0 else 'Segoe UI'
 def paintEvent(self,event):
  p=QPainter(self);p.setRenderHint(QPainter.Antialiasing);p.setRenderHint(QPainter.SmoothPixmapTransform)
  clip=QPainterPath();clip.addRoundedRect(self.rect(),12,12);p.setClipPath(clip)
  art=self.art.scaled(self.size(),Qt.KeepAspectRatioByExpanding,Qt.SmoothTransformation);p.drawPixmap(self.width()-art.width(),(self.height()-art.height())//2,art)
  shade=QLinearGradient(0,0,self.width()*.78,0);shade.setColorAt(0,QColor(20,12,32,180));shade.setColorAt(1,QColor(20,12,32,0));p.fillRect(self.rect(),shade)
  font=QFont(self.face);font.setPixelSize(max(26,min(49,int(self.width()*.048))))
  path=QPainterPath();path.addText(26,self.height()*.53,font,'WOMBATCRAFT');p.setPen(QColor('#120c1d'));pen=p.pen();pen.setWidth(6);p.setPen(pen);p.setBrush(QColor('#fff9ff'));p.drawPath(path);p.fillPath(path,QColor('#fff9ff'))
  p.setPen(QColor('#fff3ff'));font=QFont('Segoe UI');font.setPixelSize(17);p.setFont(font);p.drawText(28,int(self.height()*.53+35),'Play. Mod. Wombat.')

def build(window, brand, config, data):
 w=window;w.setObjectName('shell');w.setWindowTitle('Wombatcraft Launcher');w.setWindowIcon(QIcon(str(brand/'mascot.png')));w.resize(1220,760);w.setMinimumSize(1000,680);w.setStyleSheet(STYLE)
 root=QHBoxLayout(w);root.setContentsMargins(0,0,0,0);root.setSpacing(0)
 sidebar=QFrame();sidebar.setObjectName('sidebar');sidebar.setFixedWidth(204);side=QVBoxLayout(sidebar);side.setContentsMargins(16,24,16,20);side.setSpacing(9)
 mascot=QLabel();mascot.setPixmap(QPixmap(str(brand/'mascot.png')).scaled(150,112,Qt.KeepAspectRatio,Qt.SmoothTransformation));mascot.setAlignment(Qt.AlignCenter);side.addWidget(mascot)
 name=label('Wombatcraft');name.setAlignment(Qt.AlignCenter);name.setStyleSheet('font-size:24px;font-weight:700');side.addWidget(name)
 subtitle=label('Launcher','muted');subtitle.setAlignment(Qt.AlignCenter);side.addWidget(subtitle);side.addSpacing(24)
 w.pages=QStackedWidget();w.nav=[]
 for index,title in enumerate(['Home','Your pack','Account','Settings','About']):
  nav=QPushButton(title);nav.setObjectName('nav');nav.setCheckable(True);nav.clicked.connect(lambda checked=False,i=index:show_page(w,i));side.addWidget(nav);w.nav.append(nav)
 side.addStretch();motto=label('Play. Mod. Wombat.','muted');motto.setAlignment(Qt.AlignCenter);side.addWidget(motto);side.addWidget(label('Preview 0.1','muted'),alignment=Qt.AlignCenter);root.addWidget(sidebar)
 main=QVBoxLayout();main.setContentsMargins(24,22,24,18);main.setSpacing(14);root.addLayout(main,1);main.addWidget(w.pages,1)

 home=QWidget();hl=QVBoxLayout(home);hl.setContentsMargins(0,0,0,0);hl.setSpacing(14);hl.addWidget(Banner(brand),1)
 bar,bl=surface();row=QHBoxLayout();version=QVBoxLayout();version.addWidget(label('Minecraft version','muted'));version.addWidget(label('1.20.1 · Forge','heading'));row.addLayout(version,1)
 profile=QVBoxLayout();profile.addWidget(label('Installation','muted'));profile.addWidget(label('Wombatcraft','heading'));row.addLayout(profile,1)
 w.play=button('Play Wombatcraft',w.play_game);w.play.setObjectName('play');row.addWidget(w.play);bl.addLayout(row);hl.addWidget(bar)
 details=QHBoxLayout();details.setSpacing(14)
 intro,il=surface();il.addWidget(label('A world worth making yours','heading'));il.addWidget(label('Raise a workshop. Learn forgotten magic. Build a home together. Follow the quest book wherever curiosity takes you.',wrap=True));il.addStretch();il.addWidget(button('Explore the pack',lambda:show_page(w,1)));details.addWidget(intro,3)
 server,sl=surface();sl.addWidget(label('Official server','heading'));w.server_label=label('Checking server status…',wrap=True);w.server_label.setStyleSheet('color:#d9c0ff');sl.addWidget(w.server_label);sl.addWidget(label(config['server_address'],'muted'));sl.addStretch();sl.addWidget(button('Copy server address',lambda:copy_address(w,config)));details.addWidget(server,2);hl.addLayout(details);w.pages.addWidget(home)

 pack=QWidget();pl=QVBoxLayout(pack);pl.setContentsMargins(0,0,0,0);pl.setSpacing(18);pl.addWidget(label('Your pack','heading'));pl.addWidget(label('Wombatcraft · Minecraft Java 1.20.1 · Forge 47.4.28','muted'))
 card,cl=surface();cl.addWidget(label('Technology, magic & discovery','heading'));cl.addWidget(label('Build moving factories, master spellcraft, explore distant realms and grow a thriving settlement. The quest book connects each branch through the Convergence.',wrap=True));cl.addWidget(label('Updates are checked before Play. Your worlds, screenshots and personal options stay in your installation.',wrap=True));w.install=button('Check for a release',lambda:w.run(w.prepare,w.installed) if w.release_available else w.check_release());cl.addWidget(w.install);cl.addWidget(button('Open installation files',lambda:os.startfile(str(data))));cl.addWidget(button('Release notes & Modrinth download',lambda:webbrowser.open('https://github.com/wombatjs/wombatcraft/releases')));pl.addWidget(card);pl.addStretch();w.pages.addWidget(pack)

 account=QWidget();al=QVBoxLayout(account);al.setContentsMargins(0,0,0,0);al.addWidget(label('Your account','heading'));card,cl=surface();w.account_label=label('Sign in to join the adventure.','heading');cl.addWidget(w.account_label);cl.addWidget(label('Use the Microsoft account that owns Minecraft Java Edition.',wrap=True));w.sign=button('Sign in with Microsoft',w.sign_in);cl.addWidget(w.sign);w.auth_note=label('Microsoft sign-in is not available in this preview. Play using the Modrinth package from Releases.','muted',True);w.auth_note.setVisible(not bool(config['microsoft_client_id']));cl.addWidget(w.auth_note);w.logout_button=button('Sign out',w.sign_out);cl.addWidget(w.logout_button);al.addWidget(card);al.addStretch();w.pages.addWidget(account)

 settings=QWidget();st=QVBoxLayout(settings);st.setContentsMargins(0,0,0,0);st.addWidget(label('Settings','heading'));card,cl=surface();cl.addWidget(label('Minecraft memory','heading'));cl.addWidget(label('10 GB is the default for this pack. Leave memory available for Windows and your other apps.',wrap=True));w.memory=QSpinBox();w.memory.setRange(6,16);w.memory.setSuffix(' GB');w.memory.setValue(10)
 settings_path=data/'settings.json'
 try:w.memory.setValue(int(json.loads(settings_path.read_text()).get('memory_gb',10)))
 except (OSError,ValueError,TypeError):pass
 w.memory.valueChanged.connect(lambda value:settings_path.write_text(json.dumps({'memory_gb':value})));cl.addWidget(w.memory);cl.addWidget(button('Open launcher files & game log',lambda:os.startfile(str(data))));st.addWidget(card);st.addStretch();w.pages.addWidget(settings)

 about=QWidget();ab=QVBoxLayout(about);ab.setContentsMargins(0,0,0,0);ab.addWidget(label('About Wombatcraft','heading'));card,cl=surface();cl.addWidget(label('Made for the journey together.','heading'));cl.addWidget(label('An independent modded Minecraft community. Wombatcraft brings engineering, magic and exploration into one shared world.',wrap=True));cl.addWidget(button('Source code & issue tracker',lambda:webbrowser.open('https://github.com/wombatjs/wombatcraft')));cl.addWidget(label('An independent project. Mojang and Microsoft do not endorse Wombatcraft.','muted',True));ab.addWidget(card)
 updates,ul=surface()
 ul.addWidget(label('Launcher updates','heading'))
 w.launcher_update_status=label('The Windows launcher checks for updates when it opens.','muted',True)
 ul.addWidget(w.launcher_update_status)
 w.launcher_update_button=button('Check for launcher updates',w.check_launcher_release)
 ul.addWidget(w.launcher_update_button)
 ab.addWidget(updates);ab.addStretch();w.pages.addWidget(about)
 w.progress=QProgressBar();w.progress.setFixedHeight(5);w.progress.setTextVisible(False);w.progress.setRange(0,1);main.addWidget(w.progress)
 w.status=label('Checking for a published release…','muted',True);w.status.setMinimumHeight(34);main.addWidget(w.status)
 w.home_auth=label('Microsoft sign-in is not available in this preview. Use the Modrinth download in Your pack.','muted',True);w.home_auth.setVisible(not bool(config['microsoft_client_id']));main.addWidget(w.home_auth)
 show_page(w,0)

def show_page(w,index):
 w.pages.setCurrentIndex(index)
 for i,nav in enumerate(w.nav):nav.setChecked(i==index)

def copy_address(w,config):
 from PySide6.QtWidgets import QApplication
 QApplication.clipboard().setText(config['server_address']);w.status.setText('Server address copied. Add it in Minecraft Multiplayer.')
