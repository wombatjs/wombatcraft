# Public Minecraft proxy

The only player endpoint is `74.208.117.88:25565`. A dedicated HAProxy process
forwards native Minecraft TCP over the existing WireGuard tunnel to
`10.77.5.2:25565`. The home router needs no public port forwarding. UDP voice
continues through the existing `wombatcraft-forwarding` rules.

Install the distribution's `haproxy` package. The generic `haproxy.service`
is disabled; Wombatcraft uses its own unit and configuration:

- `ops/wombatcraft-proxy.service` -> `/etc/systemd/system/wombatcraft-proxy.service`
- `ops/wombatcraft-proxy.cfg` -> `/etc/wombatcraft/proxy.cfg`

Validate with `haproxy -c -f /etc/wombatcraft/proxy.cfg` before activation.
The legacy `wombatcraft-proxy.socket` must stay disabled because the dedicated
service owns the same public listening port. Enable `wombatcraft-proxy.service`
at boot. For configuration changes, validate first and reload the dedicated
service; HAProxy's master-worker mode supports graceful reloads.

TCP mode disables Nagle on both sides of the relay. Do not enable a PROXY
protocol header: the current Forge server expects the Minecraft handshake.
The proxy has no public administration listener and no alternate player route.

## November 13, 2026 investigation

Checks ran without opening Minecraft after the user requested no local testing.
The old systemd relay did not explicitly disable Nagle on upstream sockets.
Replacing it improved a controlled alternating split-request sample only
modestly: old median 158.4 ms / peak 464.9 ms; new median 151.8 ms / peak
353.1 ms (40 samples per relay on the proxy, over the same tunnel). This is
not evidence that gameplay lag is resolved.

A separately approved temporary pause of the 11 Supabase containers measured:

| State | Tunnel average | Tunnel maximum |
| --- | ---: | ---: |
| Before pause | 51.9 ms | 221.0 ms |
| Supabase paused | 38.4 ms | 84.1 ms |
| After resume | 55.2 ms | 277.9 ms |

All paused containers were resumed and verified running. The test supports
shared workload contention as a contributor, without excluding WAN jitter.
An IRQ-only affinity experiment was inconclusive and was reverted.

An approved CPU-separation test confined Supabase to CPUs 1-3 and the proxy
and network interrupts to CPU 0. Tunnel averages/maxima were 57.4/284.3 ms
before, 48.5/180.4 ms during, and 45.9/128.1 ms after restoring the original
allocation. There was no consistent improvement attributable to separation,
so all container CPU sets, proxy affinities and IRQ masks were restored.
The remaining host contention requires a workload placement decision; neither
this change nor the empty-server probes prove that player rubberbanding is fixed.

The previous proxy units are backed up on the VPS under
`/var/backups/wombatcraft-proxy-20261114-nodelay/`. To roll back, stop and disable
the dedicated HAProxy unit, restore the saved service file, reload systemd,
then enable/start the saved socket unit. Do not run both listeners together.

References: [HAProxy TCP-mode behavior](https://www.haproxy.com/documentation/haproxy-configuration-manual/new/latest/management/),
[systemd 255 relay source](https://github.com/systemd/systemd/blob/v255/src/socket-proxy/socket-proxyd.c).
