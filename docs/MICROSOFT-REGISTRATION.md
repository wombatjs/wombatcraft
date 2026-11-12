# Microsoft sign-in registration

The launcher is a public desktop client. It opens Microsoft's sign-in page in the system browser, uses PKCE and a verified state value, and stores the refresh token in Windows Credential Manager. It does not collect account passwords or embed a client secret.

Before public sign-in can work:

1. In Microsoft Entra's app registrations, register **Wombatcraft Launcher** with support for personal Microsoft accounts.
2. Add the desktop/mobile redirect URI `http://localhost:8529/callback`. Enable the appropriate public-client authentication configuration; do not create or distribute a client secret.
3. Follow the current Minecraft third-party application approval process for that application's client ID. A normal Microsoft app registration alone may still be rejected by Minecraft Services.
4. Set `microsoft_client_id` in `launcher/config.json` to the approved application/client ID. The ID is public configuration, not a secret.
5. Rebuild the launcher. Test a licensed account's browser sign-in, launch, token refresh, sign-out, cancellation and a rejected/no-entitlement account before calling authentication validated.

Current state: no existing registration was supplied. Microsoft approval and a real licensed-account acceptance test are outstanding. Players can use `Wombatcraft.mrpack` in Modrinth App in the meantime.

References: [library authentication documentation](https://minecraft-launcher-lib.readthedocs.io/en/stable/modules/microsoft_account.html), [Microsoft app registration](https://learn.microsoft.com/en-us/entra/identity-platform/quickstart-register-app).
