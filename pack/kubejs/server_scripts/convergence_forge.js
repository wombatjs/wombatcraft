ServerEvents.recipes(event => {
  event.shaped("wombatcraft:convergence_forge", ["BAB", "EMP", "COC"], {
    A: "ae2:engineering_processor",
    B: "create:brass_block",
    C: "minecraft:crying_obsidian",
    E: "mekanism:elite_energy_cube",
    M: "botania:mana_pool",
    O: "minecraft:nether_star",
    P: "create:precision_mechanism"
  }).id("wombatcraft:convergence/controller");

  const forge = (name, inputs, result, energy, mana, ticks) => {
    const stack = ([item, count]) => ({
      content: {type: "mbd2:sized", count: count, ingredient: {item: item}}
    });
    event.custom({
      type: "wombatcraft:convergence_forging",
      duration: ticks,
      inputs: {
        item: inputs.map(stack),
        forge_energy: [{content: energy}],
        botania_mana: [{content: mana}]
      },
      outputs: {item: [stack(result)]}
    }).id("wombatcraft:convergence/" + name);
  };

  forge("alloy", [
    ["create:brass_ingot", 8], ["botania:terrasteel_ingot", 1], ["ad_astra:ostrum_ingot", 2]
  ], ["kubejs:convergence_alloy", 4], 200000, 12000, 200);
  forge("lens", [
    ["kubejs:convergence_alloy", 2], ["aether:ambrosium_shard", 8], ["botania:mana_diamond", 2]
  ], ["kubejs:convergence_lens", 1], 400000, 30000, 280);
  forge("keystone", [
    ["kubejs:convergence_lens", 1], ["kubejs:trial_twilight_crest", 1], ["kubejs:trial_aether_crest", 1],
    ["kubejs:trial_otherside_crest", 1], ["ad_astra:calorite_ingot", 2], ["botania:gaia_ingot", 1]
  ], ["kubejs:convergence_keystone", 1], 1000000, 75000, 600);

  event.shapeless("4x waystones:warp_stone", ["kubejs:convergence_lens", "minecraft:amethyst_shard", "minecraft:amethyst_shard"])
    .id("wombatcraft:convergence/wayfarers_lens");
  event.shapeless("minecraft:beacon", ["kubejs:convergence_keystone", "minecraft:obsidian", "minecraft:glass"])
    .id("wombatcraft:convergence/trials_beacon");
});
