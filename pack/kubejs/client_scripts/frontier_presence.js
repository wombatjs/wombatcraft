// Keep Minecraft reads on the client thread; Discord's worker reads snapshots only.
var WCPlaceholderEngine = Java.loadClass('com.hypherionmc.simplerpc.api.variables.PlaceholderEngine')
var WCAtomicReference = Java.loadClass('java.util.concurrent.atomic.AtomicReference')
var WCStatsRequest = Java.loadClass('net.minecraft.network.protocol.game.ServerboundClientCommandPacket')
var WCStatsAction = Java.loadClass('net.minecraft.network.protocol.game.ServerboundClientCommandPacket$Action')
var wcDetails = new WCAtomicReference('Exploring the frontier')
var wcState = new WCAtomicReference('A world worth making yours')
var wcNextSample = 0
var wcNextStats = 0
var wcJoinedAt = 0
var wcPresenceError = false

ClientEvents.tick(event => {
  var now = Date.now()
  if (now < wcNextSample) return
  wcNextSample = now + 1000
  try {
    var engine = WCPlaceholderEngine.INSTANCE
    // Simple RPC rejects duplicate registrations and clears these on config reload.
    if (String(engine.resolvePlaceholders('{{wombatcraft.details}}')) === 'Unknown Placeholder') {
      engine.registerPlaceholder('wombatcraft.details', 'Exploring the frontier', () => wcDetails.get())
    }
    if (String(engine.resolvePlaceholders('{{wombatcraft.state}}')) === 'Unknown Placeholder') {
      engine.registerPlaceholder('wombatcraft.state', 'A world worth making yours', () => wcState.get())
    }
    var player = Client.player
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
    var stats = player.getStats()
    var deaths = stats.getDeaths()
    var kills = stats.getMobKills()
    var jumps = stats.getJumps()
    var minutes = Math.floor(stats.getPlayTime() / 1200)
    var cards = [
      ['Exploring {{world.name}}', '{{world.biome}} | Day {{world.time.day}}'],
      ['Tales of survival', deaths + ' deaths | ' + kills + ' mobs defeated'],
      ['Making a home in {{world.name}}', Math.floor(minutes / 60) + 'h ' + (minutes % 60) + 'm played in this world'],
      ['Out on the frontier', jumps + ' jumps | {{player.health.current}} / {{player.health.max}} health'],
      ['Adventure in {{world.name}}', '{{world.weather}} | {{world.time.24}} in the world']
    ]
    var card = cards[Math.floor((now - wcJoinedAt) / 60000) % cards.length]
    wcDetails.set(engine.resolvePlaceholders(card[0]))
    wcState.set(engine.resolvePlaceholders(card[1]))
  } catch (error) {
    if (!wcPresenceError) console.warn('Wombatcraft presence stats unavailable: ' + error)
    wcPresenceError = true
  }
})
