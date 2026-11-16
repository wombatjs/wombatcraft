ServerEvents.recipes(event => {
  // Keep automated terrain lighting and excessive mining sizes out of survival.
  ['torchmaster:feral_flare_lantern', 'torchmaster:frozen_pearl',
   'mininggadgets:upgrade_size_2', 'mininggadgets:upgrade_size_3'].forEach(id => event.remove({output:id}));
});
