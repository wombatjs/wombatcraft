"""Generate Wombatcraft's native PackMenu configuration and wordmark."""
from pathlib import Path
import json,shutil
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[1]
pack=ROOT/'pack/packmenu/resources';assets=pack/'assets/packmenu'
(assets/'textures/gui').mkdir(parents=True,exist_ok=True)
(assets/'buttons').mkdir(exist_ok=True)
(pack/'pack.mcmeta').write_text(json.dumps({'pack':{'pack_format':15,'description':'Wombatcraft menu'}}))
shutil.copy2(ROOT/'branding/woodland.png',assets/'textures/gui/background.png')
# A typeset wordmark, generated from the licensed display font.
im=Image.new('RGBA',(1100,170));d=ImageDraw.Draw(im)
font=ImageFont.truetype(str(ROOT/'branding/Cinzel.ttf'),112)
d.text((12,0),'WOMBATCRAFT',font=font,fill='#f5e6ca',stroke_width=0)
im.save(assets/'textures/gui/logo.png')
# Native button textures with distinct hover and keyboard-focus treatment.
for name,bg,hover in [('primary','#ad673d','#cf8b55'),('secondary','#23382f','#3b5845')]:
 im=Image.new('RGBA',(200,48));d=ImageDraw.Draw(im)
 for y,color in [(0,bg),(24,hover)]:
  d.rounded_rectangle((0,y,199,y+23),radius=2,fill=color)
  d.line((2,y,197,y),fill='#e1bd85',width=1)
 im.save(assets/f'textures/gui/{name}.png')
buttons=[('join','Join Wombatcraft','CONNECT_TO_SERVER','74.208.117.88:25565'),('singleplayer','Singleplayer','OPEN_GUI','SINGLEPLAYER'),('multiplayer','Other servers','OPEN_GUI','MULTIPLAYER'),('options','Options','OPEN_GUI','OPTIONS'),('mods','Mods','OPEN_GUI','MODS'),('quit','Quit game','QUIT',None)]
for i,(name,label,action,data) in enumerate(buttons):
 obj=dict(x=24,y=-55+i*26,width=170,height=22,anchor='MIDDLE_LEFT',langKey=label,action=action,fontColor=0xFFF1D8,hoverFontColor=0xFFFFFF,texture=f'packmenu:textures/gui/{"primary" if i==0 else "secondary"}.png',u=0,v=0,hoverU=0,hoverV=24,texWidth=200,texHeight=48,widgets=False,dropShadow=True)
 if data:obj['data']=data
 (assets/f'buttons/{i:02}_{name}.json').write_text(json.dumps(obj,indent=2)+'\n')
(ROOT/'pack/config/packmenu.cfg').write_text('''general {
 B:"Draw Title"=false
 B:"Draw Splash"=false
 B:"Draw Forge Info"=false
 B:"Draw Panorama"=false
 B:"Folder Pack"=true
}
logo {
 B:"Enable Logo"=true
 S:"Texture Path"=packmenu:textures/gui/logo.png
 S:"Anchor Point"=MIDDLE_LEFT
 I:"X Offset"=120
 I:"Y Offset"=-550
 I:Width=220
 I:Height=34
 I:"Texture Width"=1100
 I:"Texture Height"=170
}
''')
p=ROOT/'pack/options.txt';options=dict(line.split(':',1) for line in p.read_text().splitlines() if ':' in line)
options.update(guiScale='3',tutorialStep='none');p.write_text('\n'.join(k+':'+v for k,v in options.items())+'\n')
print('Wombatcraft menu generated: 6 native buttons')
