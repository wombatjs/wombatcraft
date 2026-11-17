"""Explicit one-time, offline corridor reset. Never called by routine updates.
Requires a stopped server and a fresh complete backup. Console chunks -24..24
on both axes are retained byte-for-byte; only TARDIS dimension regions change.
"""
import argparse,hashlib,json,os,re,subprocess,uuid
from pathlib import Path
import nbtlib

def keep(cx,cz): return -24<=cx<=24 and -24<=cz<=24

def rewrite_region(path):
 data=path.read_bytes()
 if not data:return data,0,0
 if len(data)<8192:raise ValueError('Invalid region header: '+str(path))
 match=re.fullmatch(r'r\.(-?\d+)\.(-?\d+)\.mca',path.name)
 if not match:raise ValueError('Invalid region filename')
 rx,rz=map(int,match.groups());out=bytearray(data);removed=retained=0
 for i in range(1024):
  entry=data[i*4:i*4+4]
  if entry==b'\0'*4:continue
  if keep(rx*32+i%32,rz*32+i//32):retained+=1;continue
  out[i*4:i*4+4]=b'\0'*4;out[4096+i*4:4100+i*4]=b'\0'*4;removed+=1
 # Payload sectors stay intact in this file; the backup also retains the original index.
 assert out[8192:]==data[8192:]
 return bytes(out),removed,retained

def main():
 a=argparse.ArgumentParser();a.add_argument('--instance',required=True,type=Path);a.add_argument('--container',required=True);a.add_argument('--backup',required=True,type=Path);a.add_argument('--apply',action='store_true');args=a.parse_args()
 root=args.instance.resolve();world=root/'world';base=world/'dimensions/tardis_refined';report={'dimensions':[],'relocated_players':0,'apply':args.apply}
 assert world.is_dir() and base.is_dir()
 assert args.backup.is_file() and args.backup.stat().st_size>1000000,'Full backup required'
 assert subprocess.check_output(['docker','inspect','--format','{{.State.Running}}',args.container],text=True).strip()=='false','Stop server first'
 assert (root/'mods/tardis_rooms-forge-1.20.1-1.2.0.jar').is_file(),'Install Rooms before resetting terrain'
 assert 'disableProceduralRooms = true' in (root/'config/tardis_rooms/common.toml').read_text()
 assert 'simplifyHub = true' in (root/'config/tardis_rooms/common.toml').read_text()
 dims=set();changes=[]
 for dim in sorted(base.iterdir()):
  if not dim.is_dir():continue
  uuid.UUID(dim.name);assert dim.resolve().is_relative_to(base.resolve());dims.add('tardis_refined:'+dim.name)
  stats={'dimension':dim.name,'removed':{},'preserved':{}}
  for kind in ('region','entities','poi'):
   gone=saved=0
   for path in (dim/kind).glob('r.*.*.mca'):
    data,n,k=rewrite_region(path);gone+=n;saved+=k
    if n:changes.append((path,data))
   stats['removed'][kind]=gone;stats['preserved'][kind]=saved
  # Existing Rooms data would require a different migration, not silent deletion.
  assert not list((dim/'data').glob('tardis_rooms*.dat')),'Rooms already initialized; refuse repeat reset'
  report['dimensions'].append(stats)
 players=[]
 for path in (world/'playerdata').glob('*.dat'):
  n=nbtlib.load(path)
  if str(n.get('Dimension','')) not in dims:continue
  pos=n.get('Pos')
  if pos is None or keep(int(float(pos[0])//16),int(float(pos[2])//16)):continue
  inventory=str(n.get('Inventory'));n['Pos']=nbtlib.List[nbtlib.Double]([1013.5,99.0,5.5]);n['Motion']=nbtlib.List[nbtlib.Double]([0,0,0]);n['FallDistance']=nbtlib.Float(0);n.pop('RootVehicle',None)
  assert str(n.get('Inventory'))==inventory;players.append((path,n));report['relocated_players']+=1
 report['backup']=str(args.backup);report['changed_region_files']=len(changes)
 if args.apply:
  for path,data in changes:
   temp=path.with_suffix('.corridor-reset');temp.write_bytes(data);os.replace(temp,path)
  for path,n in players:
   temp=path.with_suffix('.corridor-reset');n.save(temp,gzipped=True);os.replace(temp,path)
  (root/'wombatcraft-corridor-migration.json').write_text(json.dumps(report,indent=2))
 print(json.dumps(report,indent=2))
if __name__=='__main__':main()
