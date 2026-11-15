// Refined/Immersive Portals can register a new level after Forge caches its
// ticking worlds. Refresh that cache once per newly observed interior.
// Interiors use local coordinates and must not inherit an overworld spawn border.
var wcRefinedLevels = new Map();
var wcRefinedCheckTicks = 0;
ServerEvents.tick(event => {
  if (wcRefinedCheckTicks++ % 20 !== 0) return;
  var server = event.server;
  var refreshWorlds = false;
  var overworldBorder = null;
  server.getAllLevels().forEach(level => {
    if (String(level.dimension).indexOf(' / minecraft:overworld]') >= 0) overworldBorder = level.getWorldBorder();
  });
  server.getAllLevels().forEach(level => {
    var key = String(level.dimension);
    if (key.indexOf('tardis_refined:') < 0) return;
    var previous = wcRefinedLevels.get(key);
    var newLevel = previous !== level;
    var border = level.getWorldBorder();
    if (newLevel && overworldBorder !== null) {
      // Vanilla DelegateBorderChangeListener would otherwise immediately copy
      // later overworld border commands back into this pocket dimension.
      // These are verified Forge 1.20.1 fields; remove only this interior's delegate.
      var listenersField = overworldBorder.getClass().getDeclaredField('f_61905_');
      listenersField.setAccessible(true);
      var listeners = listenersField.get(overworldBorder).toArray();
      listeners.forEach(listener => {
        if (String(listener.getClass().getName()) !== 'net.minecraft.world.level.border.BorderChangeListener$DelegateBorderChangeListener') return;
        var targetField = listener.getClass().getDeclaredField('f_61864_');
        targetField.setAccessible(true);
        if (targetField.get(listener) === border) {
          overworldBorder.removeListener(listener);
          console.info('[Wombatcraft] Detached overworld border delegate: ' + key);
        }
      });
    }
    var resetBorder = border.getCenterX() !== 0 || border.getCenterZ() !== 0 || border.getSize() !== 59999968;
    if (resetBorder) {
      border.setCenter(0, 0);
      border.setSize(59999968);
      console.info('[Wombatcraft] Restored interior border: ' + key);
    }
    if (newLevel) {
      wcRefinedLevels.set(key, level);
      refreshWorlds = true;
    }
  });
  if (refreshWorlds) {
    server.markWorldsDirty();
    console.info('[Wombatcraft] Refreshed Forge ticking worlds for TARDIS interiors.');
  }
});
