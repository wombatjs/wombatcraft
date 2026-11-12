# Microsoft sign-in registration

The launcher is a public desktop client. It opens Microsoft's sign-in page in the system browser, uses PKCE and a verified state value, and encrypts its saved refresh token with Windows Data Protection for the current Windows user.

Before public sign-in can work:

1. In Microsoft Entra's app registrations, register **Wombatcraft Launcher** with support for personal Microsoft accounts.
2. Register `http://localhost:8529/callback` under **Mobile and desktop applications**. If the portal rejects the URI or shows it under **Web**, open **Manifest** and move that exact entry from `web.redirectUris` to `publicClient.redirectUris`, preserving the other entries. Microsoft supports HTTP for loopback redirects. [Redirect URI rules](https://learn.microsoft.com/en-us/entra/identity-platform/reply-url#localhost-exceptions) · [Manifest fields](https://learn.microsoft.com/en-us/entra/identity-platform/reference-microsoft-graph-app-manifest#publicclient-attribute)
3. Request Minecraft API access for the application through the [Minecraft app review form](https://aka.ms/mce-reviewappid).
4. Set `microsoft_client_id` in `launcher/config.json` to the approved application/client ID. The ID is public configuration, not a secret.
5. Rebuild the launcher. Test a licensed account's browser sign-in, launch, token refresh, sign-out, cancellation and a rejected/no-entitlement account before calling authentication validated.

Wombatcraft's registered client ID is configured in the launcher. Minecraft API approval and a full sign-in with a licensed account still need verification. Signed-out players can use Play to install the pack and sign in through the official Minecraft Launcher.

References: [library authentication documentation](https://minecraft-launcher-lib.readthedocs.io/en/stable/modules/microsoft_account.html), [Microsoft app registration](https://learn.microsoft.com/en-us/entra/identity-platform/quickstart-register-app).
