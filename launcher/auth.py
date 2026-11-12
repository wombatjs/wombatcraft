"""Microsoft browser sign-in and Windows user-protected session storage."""

import hmac
import http.server
import os
import re
import time
import urllib.parse
import webbrowser

import pywintypes
import requests
import win32crypt
from keyring.backends.Windows import WinVaultKeyring
from keyring.errors import PasswordDeleteError
from minecraft_launcher_lib import microsoft_account


SERVICE = 'Wombatcraft Microsoft account'
SESSION_FILE = 'microsoft-session.dpapi'
TOKEN_URL = 'https://login.microsoftonline.com/consumers/oauth2/v2.0/token'
SCOPE = 'XboxLive.signin offline_access'
NO_UI = 1  # CRYPTPROTECT_UI_FORBIDDEN


class SignInRequired(RuntimeError):
    """The player needs to sign in or use another launcher to play."""


def _require_registration(config):
    if not config.get('microsoft_client_id'):
        raise SignInRequired('Use Minecraft Launcher to sign in and play.')


def _store_token(data, token):
    target = data / SESSION_FILE
    temporary = target.with_suffix('.tmp')
    try:
        encrypted = win32crypt.CryptProtectData(token.encode('utf-8'), SERVICE, None, None, None, NO_UI)
        temporary.write_bytes(encrypted)
        os.replace(temporary, target)
    except (OSError, pywintypes.error):
        raise RuntimeError('Minecraft accepted sign-in, but Windows could not save the encrypted session.') from None
    finally:
        temporary.unlink(missing_ok=True)


def _load_token(data):
    try:
        encrypted = (data / SESSION_FILE).read_bytes()
        _, token = win32crypt.CryptUnprotectData(encrypted, None, None, None, NO_UI)
        return token.decode('utf-8')
    except FileNotFoundError:
        # A successful refresh migrates sessions saved by earlier builds.
        return WinVaultKeyring().get_password(SERVICE, 'refresh_token')
    except (OSError, UnicodeError, pywintypes.error):
        raise SignInRequired('Windows could not read your saved session. Sign in again.') from None


def _request(service, request, *args, **kwargs):
    try:
        result = request(*args, **kwargs)
    except requests.Timeout:
        raise RuntimeError(f'{service} timed out. Try signing in again.') from None
    except (requests.RequestException, ValueError):
        raise RuntimeError(f'{service} could not complete sign-in. Try again in a moment.') from None
    if not isinstance(result, dict):
        raise RuntimeError(f'{service} returned an unexpected sign-in response.')
    return result


def _exchange_microsoft(config, grant_type, **grant):
    def exchange():
        return requests.post(
            TOKEN_URL,
            data={'client_id': config['microsoft_client_id'], 'scope': SCOPE, 'grant_type': grant_type, **grant},
            timeout=25,
        ).json()

    result = _request('Microsoft', exchange)
    if result.get('access_token') and result.get('refresh_token'):
        return result
    description = str(result.get('error_description', ''))
    code = re.search(r'\bAADSTS[0-9]{5,9}\b', description)
    if (code and code.group() == 'AADSTS7000218') or result.get('error') == 'invalid_client':
        raise RuntimeError('Microsoft rejected the app configuration. Check that the callback is registered under Mobile and desktop applications in Entra.')
    if result.get('error') == 'invalid_grant':
        raise SignInRequired('Microsoft rejected the sign-in session. Start a fresh sign-in.')
    suffix = f' ({code.group()})' if code else ''
    raise RuntimeError(f'Microsoft could not exchange the sign-in code{suffix}.')


def _minecraft_session(tokens, data):
    xbox = _request('Xbox Live', microsoft_account.authenticate_with_xbl, tokens['access_token'])
    try:
        xbox_token = xbox['Token']
        user_hash = xbox['DisplayClaims']['xui'][0]['uhs']
    except (KeyError, IndexError, TypeError):
        raise RuntimeError('Xbox Live rejected sign-in. Check that this Microsoft account has an Xbox profile.') from None

    xsts = _request('Xbox authorization', microsoft_account.authenticate_with_xsts, xbox_token)
    if not xsts.get('Token'):
        code = xsts.get('XErr')
        suffix = f' ({code})' if isinstance(code, int) else ''
        raise RuntimeError(f'Xbox authorization rejected the account{suffix}. Check the account’s Xbox profile and family settings.')

    session = _request('Minecraft Services', microsoft_account.authenticate_with_minecraft, user_hash, xsts['Token'])
    if not session.get('access_token'):
        if 'invalid app registration' in str(session.get('errorMessage', '')).lower():
            raise SignInRequired('Wombatcraft is waiting for Minecraft API approval. You can sign in through Minecraft Launcher.')
        raise RuntimeError('Minecraft Services rejected the Xbox session. Try signing in again.')

    profile = _request('Minecraft profile', microsoft_account.get_profile, session['access_token'])
    if profile.get('error') == 'NOT_FOUND':
        raise RuntimeError('Sign in with the Microsoft account that has your Minecraft: Java Edition profile.')
    if not profile.get('name') or not profile.get('id'):
        raise RuntimeError('Minecraft Services could not retrieve your Java Edition profile.')

    _store_token(data, tokens['refresh_token'])
    return {'id': profile['id'], 'name': profile['name'], 'access_token': session['access_token']}


def refresh(config, data):
    _require_registration(config)
    token = _load_token(data)
    if not token:
        raise SignInRequired('Sign in with your Minecraft account to play.')
    tokens = _exchange_microsoft(config, 'refresh_token', refresh_token=token)
    return _minecraft_session(tokens, data)


def login(config, data):
    _require_registration(config)
    redirect_uri = config['redirect_uri']
    redirect = urllib.parse.urlsplit(redirect_uri)
    url, state, verifier = microsoft_account.get_secure_login_data(config['microsoft_client_id'], redirect_uri)
    received = {}

    class Callback(http.server.BaseHTTPRequestHandler):
        def log_message(self, *_args):
            pass

        def do_GET(self):
            target = urllib.parse.urlsplit(self.path)
            if target.path != redirect.path:
                self.send_error(404)
                return
            query = urllib.parse.parse_qs(target.query)
            states = query.get('state', [])
            if len(states) != 1 or not hmac.compare_digest(states[0].encode(), state.encode()):
                self.send_error(400, 'The sign-in response could not be verified.')
                return
            codes = query.get('code', [])
            if len(codes) == 1:
                received['code'] = codes[0]
            else:
                received['error'] = True
            self.send_response(200)
            self.send_header('Content-Type', 'text/html; charset=utf-8')
            self.send_header('Cache-Control', 'no-store')
            self.send_header('Referrer-Policy', 'no-referrer')
            self.end_headers()
            self.wfile.write(
                b'<title>Wombatcraft</title><script>history.replaceState(null,"","/complete")</script>'
                b'<h1>Return to Wombatcraft</h1><p>Check the launcher for your sign-in result.</p>'
            )

    try:
        server = http.server.HTTPServer(('127.0.0.1', redirect.port), Callback)
    except OSError:
        raise RuntimeError('The local sign-in port is busy. Close any other Wombatcraft launcher and retry.') from None
    with server:
        server.timeout = 1
        if not webbrowser.open(url):
            raise RuntimeError('Could not open the default browser for Microsoft sign-in.')
        deadline = time.monotonic() + 180
        while not received and time.monotonic() < deadline:
            server.handle_request()
    if not received:
        raise RuntimeError('Sign-in timed out. Choose Sign in to try again.')
    if 'code' not in received:
        raise RuntimeError('Microsoft sign-in was canceled or declined. Choose Sign in to try again.')
    tokens = _exchange_microsoft(
        config, 'authorization_code', code=received['code'], redirect_uri=redirect_uri, code_verifier=verifier,
    )
    return _minecraft_session(tokens, data)


def logout(data):
    try:
        WinVaultKeyring().delete_password(SERVICE, 'refresh_token')
    except PasswordDeleteError:
        pass
    (data / SESSION_FILE).unlink(missing_ok=True)
