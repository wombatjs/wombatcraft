# Docketrelay migration preparation

Prepared November 13, 2026 (America/Chicago). No public cutover has occurred.
This moves the Supabase workload off the Minecraft proxy while retaining
`docketrelay.aetherium.dev`, the public Minecraft address, and the existing
WireGuard tunnel. No home-router port forwarding is needed.

## Verified preparation

- Home server: 8 logical CPUs, approximately 11 GiB available RAM and 251 GB
  free disk at inspection. The staged Supabase stack used approximately 1.2 GiB.
- Exact source image digests were pinned for all 11 services.
- PostgreSQL `postgres` and `_supabase` databases were dumped in custom format,
  with a separate superuser globals/roles backup. Both archives were checked
  and successfully restored into the isolated destination.
- Schema, extension, public table, authentication-user and storage counts
  matched the source. The original encryption key and application configuration
  were retained. Its copied key requires owner/group `100:101` and mode `640`.
- All 11 staging containers became healthy. Auth, authenticated REST and storage
  returned HTTP 200; Realtime returned WebSocket HTTP 101. Anonymous REST schema
  discovery returns 403; the service-role check passes.
- The staging Docker network is internal-only. Published-port declarations on
  that network do not create host listeners; checks used the gateway's bridge IP
  from the home host. Scheduled database jobs are disabled in staging.
- The existing edge-runtime dependency cache was copied so functions can start
  without internet access. Email delivery and other outbound integrations have
  deliberately not been exercised.
- The proposed Caddy configuration and database TCP relay pass validation.

## Private artifacts (never commit their contents)

Proxy host:

- `/var/backups/docketrelay-migration-20261114/`: source configuration inventory,
  database archives, globals, application files, original Caddy file and candidates.
- `/var/backups/docketrelay-migration-20261114.tar.gz`: protected transfer archive.
- Candidates: `Caddyfile.cutover` and `database-proxy.cfg` in the backup directory.

Home server:

- `/var/backups/docketrelay-migration-20261114/`: matching archive, restore logs
  and `stage-validation.json`.
- `/opt/docketrelay-stage/`: restored, isolated test instance.
- `compose.stage.json` and `compose.production.json`: private resolved configuration
  containing credentials. Production candidate refers to `/opt/docketrelay-supabase/`.

The transferred archive SHA-256 is
`3eb828ef7018002238d8d0dd777f42d3ac0e9a244284d41efa83f21e78f0b93d`.
Source and destination matched. These preparation backups are not a final
write-frozen cutover snapshot; production remains writable on the proxy.

## Cutover sequence requiring approval

1. Confirm a short Docketrelay maintenance window and recheck home capacity.
   Snapshot current Caddy, Compose and source state again; do not overwrite the
   preparation backup or remove source volumes.
2. Put only Docketrelay's HTTP routes into maintenance. Stop its source application
   containers and pause scheduled database jobs, leaving PostgreSQL available
   for a fresh final dump. Account for direct local pooler clients before freezing
   writes. Preserve source scheduler settings for rollback.
3. Take new database/globals backups and copy the latest storage, functions,
   configuration and dependency cache. Verify checksums. Restore the final data
   on the home server with all destination application containers stopped.
   Wait for full database initialization/health before restoring; never run two
   restore jobs concurrently.
4. Stop and remove only the disposable staging containers/network (retain all
   bind-mounted data). Move the prepared directory to `/opt/docketrelay-supabase`.
   Activate the pinned production configuration explicitly. Its private listeners
   are `10.77.5.2:18000`, `:15432` and `:16543`; it restores outbound connectivity.
   Do not accidentally run the archived upstream Compose file.
5. Validate home services privately through WireGuard before opening public
   traffic. Confirm the home firewall permits only the intended private route.
   Stop the old source database/pooler once the final copy is verified.
6. Activate the prepared loopback-only database relay on the proxy, preserving
   `127.0.0.1:5432` and `:6543`. Validate and reload only the Docketrelay Caddy
   upstream from `127.0.0.1:8000` to `10.77.5.2:18000`.
7. Enable scheduled jobs only on the new primary. Verify public auth/API/storage,
   WebSockets, local pooler connections and application behavior. Measure the
   Minecraft tunnel and public status latency after the old Supabase stack stops.
8. Keep source backups/volumes intact. Before accepting destination writes,
   rollback can restore the old route and source services. After destination
   writes begin, do not blindly switch back: recover the new primary or reverse
   synchronize its data first to avoid losing new writes.

The source VPS and home stack remain separate until approval. This preparation
does not prove that all Minecraft gameplay lag will disappear after migration.
