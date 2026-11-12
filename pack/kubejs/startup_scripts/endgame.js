// Original neutral endgame items. Restart required after changing this file.
StartupEvents.registry("item", event => {
  event.create("industrial_sigil").displayName("Industrial Sigil").texture("mekanism:item/ultimate_control_circuit").glow(true).maxStackSize(16);
  event.create("arcane_sigil").displayName("Arcane Sigil").texture("botania:item/gaia_ingot").glow(true).maxStackSize(16);
  event.create("voyager_sigil").displayName("Voyager Sigil").texture("minecraft:item/ender_eye").glow(true).maxStackSize(16);
  event.create("prosperity_sigil").displayName("Prosperity Sigil").texture("lightmanscurrency:item/coin_netherite").glow(true).maxStackSize(16);
  event.create("convergence_core").displayName("Convergence Core").texture("minecraft:item/nether_star").glow(true).maxStackSize(16);
});