// PackMenu's exact-class check misses the anonymous vanilla title screen used
// by this client stack. Keep its native menu for vanilla title-screen variants.
if (Platform.isClientEnvironment()) {
  var WombatMenu = Java.loadClass('dev.shadowsoffire.packmenu.ExtendedMenuScreen');
  ForgeEvents.onEvent('net.minecraftforge.client.event.ScreenEvent$Opening', event => {
    var screen = event.newScreen;
    if (screen && String(screen.getClass().getName()).startsWith('net.minecraft.client.gui.screens.TitleScreen')) {
      event.newScreen = new WombatMenu(false);
    }
  });
}
