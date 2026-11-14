// Keep Minecraft reads on the client thread; Discord's worker reads snapshots only.
const WCPlaceholderEngine = Java.loadClass('com.hypherionmc.simplerpc.api.variables.PlaceholderEngine')
const WCAtomicReference = Java.loadClass('java.util.concurrent.atomic.AtomicReference')
const WCStats = Java.loadClass('net.minecraft.stats.Stats')
const WCStatsRequest = Java.loadClass('net.minecraft.network.protocol.game.ServerboundClientCommandPacket')
const WCStatsAction = Java.loadClass('net.minecraft.network.protocol.game.ServerboundClientCommandPacket$Action')
const wcDetails = new WCAtomicReference('Exploring the frontier')
const wcState = new WCAtomicReference('A world worth making yours')
let wcNextSample = 0
let wcNextStats = 0
let wcJoinedAt = 0
let wcPresenceError = false

ClientEvents.tick(event => {
  const now = Date.now()
  if (now < wcNextSample) return
  wcNextSample = now + 1000
  try {
    const engine = WCPlaceholderEngine.INSTANCE
    // Simple RPC rejects duplicate registrations and clears these on config reload.
    if (String(engine.resolvePlaceholders('{{wombatcraft.details}}')) === 'Unknown Placeholder') {
      engine.registerPlaceholder('wombatcraft.details', 'Exploring the frontier', () => wcDetails.get())
    }
    if (String(engine.resolvePlaceholders('{{wombatcraft.state}}')) === 'Unknown Placeholder') {
      engine.registerPlaceholder('wombatcraft.state', 'A world worth making yours', () => wcState.get())
    }
    const player = Client.player
    if (!player) {
      wcJoinedAt = 0
      wcNextStats = 0
      return
    }
    if (!wcJoinedAt) wcJoinedAt = now
    if (now >= wcNextStats) {
      Client.getConnection().send(new WCStatsRequest(WCStatsAction.REQUEST_STATS))
      wcNextStats = now + 60000
    }
    const stats = player.getStats()
    const deaths = stats.getValue(WCStats.CUSTOM.get(WCStats.DEATHS))
    const kills = stats.getValue(WCStats.CUSTOM.get(WCStats.MOB_KILLS))
    const jumps = stats.getValue(WCStats.CUSTOM.get(WCStats.JUMP))
    const minutes = Math.floor(stats.getValue(WCStats.CUSTOM.get(WCStats.PLAY_TIME)) / 1200)
    const cards = [
      ['Exploring {{world.name}}', '{{world.biome}} | Day {{world.time.day}}'],
      ['Tales of survival', deaths + ' deaths | ' + kills + ' mobs defeated'],
      ['Making a home in {{world.name}}', Math.floor(minutes / 60) + 'h ' + (minutes % 60) + 'm played in this world'],
      ['Out on the frontier', jumps + ' jumps | {{player.health.current}} / {{player.health.max}} health'],
      ['Adventure in {{world.name}}', '{{world.weather}} | {{world.time.24}} in the world']
    ]
    const card = cards[Math.floor((now - wcJoinedAt) / 60000) % cards.length]
    wcDetails.set(engine.resolvePlaceholders(card[0]))
    wcState.set(engine.resolvePlaceholders(card[1]))
  } catch (error) {
    if (!wcPresenceError) console.warn('Wombatcraft presence stats unavailable: ' + error)
    wcPresenceError = true
  }
})
