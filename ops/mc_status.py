import json,socket,struct
def varint(n):
 out=b''
 while True:
  part=n&127;n>>=7;out+=bytes([part|(128 if n else 0)])
  if not n:return out
def read_exact(s,n):
 out=b''
 while len(out)<n:
  data=s.recv(n-len(out))
  if not data:raise ConnectionError('Server closed status connection')
  out+=data
 return out
def read_varint(s):
 n=0
 for i in range(5):
  b=read_exact(s,1)[0];n|=(b&127)<<(7*i)
  if not b&128:return n
 raise ValueError('Invalid status length')
def status(host,port=25565):
 with socket.create_connection((host,port),timeout=8) as s:
  name=host.encode();handshake=b'\x00'+varint(763)+varint(len(name))+name+struct.pack('>H',port)+b'\x01'
  s.sendall(varint(len(handshake))+handshake+b'\x01\x00')
  length=read_varint(s)
  if length>1024*1024:raise ValueError('Oversized status response')
  if read_varint(s)!=0:raise ValueError('Unexpected status packet')
  n=read_varint(s)
  if n>length:raise ValueError('Invalid status size')
  return json.loads(read_exact(s,n))
