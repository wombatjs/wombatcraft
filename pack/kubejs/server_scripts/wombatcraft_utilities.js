ServerEvents.recipes(event => {
  // Keep automated terrain lighting and excessive mining sizes out of survival.
  // Exact IDs verified against the pinned jars. Output filters instantiate unrelated
  // Quark rune recipes in parallel and can race Universal Enchants' category cache.
  ['torchmaster:feral_flare_lantern', 'torchmaster:frozen_pearl',
   'mininggadgets:upgrade_size_2', 'mininggadgets:upgrade_size_3'].forEach(id => event.remove({id:id}));
});
