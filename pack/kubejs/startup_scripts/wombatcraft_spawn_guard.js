// The dedicated server team defines the safe zone; ordinary player claims do not.
const WCSpawnAPI = Java.loadClass('dev.ftb.mods.ftbchunks.api.FTBChunksAPI');
const WCSpawnPos = Java.loadClass('dev.ftb.mods.ftblibrary.math.ChunkDimPos');
const WCSpawnEnemy = Java.loadClass('net.minecraft.world.entity.monster.Enemy');
const WCSpawnCategory = Java.loadClass('net.minecraft.world.entity.MobCategory');
const WCSpawnPlayer = Java.loadClass('net.minecraft.world.entity.player.Player');
function wcSpawnInside(entity) {
  if (!entity || entity.level.isClientSide()) return false;
  const api = WCSpawnAPI.api();
  if (!api.isManagerLoaded()) return false;
  const claim = api.getManager().getChunk(new WCSpawnPos(entity));
  return claim != null && String(claim.getTeamData().getTeam().getId()) === '2de87e97-dce4-41d0-b02d-c24a395997a1';
}
const wcHostileTypes = {};
function wcSpawnHostile(entity) {
  if (entity instanceof WCSpawnEnemy) return true;
  const id = String(entity.getType());
  if (wcHostileTypes[id] === undefined) {
    const type = entity.getEntityType();
    wcHostileTypes[id] = type != null && type.getCategory() === WCSpawnCategory.MONSTER;
  }
  return wcHostileTypes[id];
}
ForgeEvents.onEvent('net.minecraftforge.event.entity.EntityJoinLevelEvent', event => {
  if (!event.getLevel().isClientSide() && wcSpawnHostile(event.getEntity()) && wcSpawnInside(event.getEntity())) event.setCanceled(true);
});
['LivingAttackEvent', 'LivingHurtEvent', 'LivingDamageEvent'].forEach(name => {
  ForgeEvents.onEvent('net.minecraftforge.event.entity.living.' + name, event => {
    const target = event.getEntity();
    const attacker = event.getSource().getActual();
    if (wcSpawnInside(target) || (attacker instanceof WCSpawnPlayer && wcSpawnInside(attacker))) event.setCanceled(true);
  });
});
ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingKnockBackEvent', event => {
  if (wcSpawnInside(event.getEntity())) event.setCanceled(true);
});
// Residents cannot be carried away from their posts.
ForgeEvents.onEvent('tschipp.carryon.events.EntityPickupEvent', event => {
  if (event.target.getTags().contains('wombatcraft_spawn_npc')) event.setCanceled(true);
});
// One sweep each second over loaded overworld entities, never loading new chunks.
ForgeEvents.onEvent('net.minecraftforge.event.TickEvent$ServerTickEvent', event => {
  if (String(event.phase) !== 'END') return;
  const server = event.getServer();
  if (server.getTickCount() % 20 !== 0) return;
  const world = server.getOverworld();
  const remove = [];
  for (const entity of world.getAllEntities()) {
    if (wcSpawnHostile(entity) && wcSpawnInside(entity)) remove.push(entity);
    else if (entity instanceof WCSpawnPlayer && wcSpawnInside(entity)) entity.extinguish();
  }
  remove.forEach(entity => entity.discard());
});
console.info('[Wombatcraft] Spawn claim safe-zone handlers registered.');
