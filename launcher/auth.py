import http.server,json,threading,time,urllib.parse,webbrowser
import keyring
from minecraft_launcher_lib import microsoft_account

SERVICE='Wombatcraft Microsoft account'
def require_registration(config):
 if not config.get('microsoft_client_id'):raise RuntimeError('Microsoft sign-in is awaiting Wombatcraft app approval. You can install the pack now, or use the Modrinth download with your existing launcher.')

def store(account):
 # Refuse fallback plaintext/null stores; Windows uses Credential Manager.
 backend=keyring.get_keyring()
 if 'Windows' not in type(backend).__name__ and 'WinVault' not in type(backend).__name__:raise RuntimeError('Windows Credential Manager is unavailable. Sign-in was not saved.')
 keyring.set_password(SERVICE,'refresh_token',account['refresh_token'])
 return {'id':account['id'],'name':account['name'],'access_token':account['access_token']}

def refresh(config):
 require_registration(config)
 token=keyring.get_password(SERVICE,'refresh_token')
 if not token:raise RuntimeError('Sign in with Microsoft before playing.')
 return store(microsoft_account.complete_refresh(config['microsoft_client_id'],None,None,token))

def login(config):
 require_registration(config)
 url,state,verifier=microsoft_account.get_secure_login_data(config['microsoft_client_id'],config['redirect_uri'])
 received={};done=threading.Event()
 class Callback(http.server.BaseHTTPRequestHandler):
  def log_message(self,*args):pass
  def do_GET(self):
   if urllib.parse.urlsplit(self.path).path!='/callback':self.send_error(404);return
   try:
    received['code']=microsoft_account.parse_auth_code_url(config['redirect_uri'].split('/callback')[0]+self.path,state)
    self.send_response(200);self.send_header('Content-Type','text/html; charset=utf-8');self.end_headers();self.wfile.write(b'<title>Wombatcraft</title><h1>Return to Wombatcraft</h1><p>You can close this tab.</p>');done.set()
   except Exception:self.send_error(400,'The sign-in response could not be verified. Return to Wombatcraft and try again.')
 server=http.server.HTTPServer(('127.0.0.1',8529),Callback);server.timeout=1
 try:
  webbrowser.open(url);deadline=time.monotonic()+180
  while not done.is_set() and time.monotonic()<deadline:server.handle_request()
 finally:server.server_close()
 if not done.is_set():raise RuntimeError('Sign-in timed out. Choose Sign in to try again.')
 return store(microsoft_account.complete_login(config['microsoft_client_id'],None,config['redirect_uri'],received['code'],verifier))

def logout():
 try:keyring.delete_password(SERVICE,'refresh_token')
 except keyring.errors.PasswordDeleteError:pass
