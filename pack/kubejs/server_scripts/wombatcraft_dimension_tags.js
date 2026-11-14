// Remove stale references before tag resolution; preserve every valid contributor.
ServerEvents.tags('painting_variant', function(event) {
  event.remove("eternal_starlight:placeable", ["eternal_starlight:the_catalyst", "eternal_starlight:crystalborn", "eternal_starlight:voidstone_apple", "eternal_starlight:pungency_fruit_feast", "eternal_starlight:starflowers", "eternal_starlight:cold_anomaly", "eternal_starlight:northland_hut", "eternal_starlight:rat_head", "eternal_starlight:original_creteor", "eternal_starlight:twilight_squid", "eternal_starlight:power", "eternal_starlight:pot_of_crests", "eternal_starlight:thirsty", "eternal_starlight:stack", "eternal_starlight:carved_slate", "eternal_starlight:insect_specimen", "eternal_starlight:sword_of_the_lake", "eternal_starlight:skeleton_lonestar", "eternal_starlight:hymn_of_the_rats", "eternal_starlight:cooler", "eternal_starlight:riven_welkin", "eternal_starlight:umbrous_alchemist", "eternal_starlight:extinguished_sun", "eternal_starlight:novus_sol"]);
});
ServerEvents.tags('block', function(event) {
  event.remove("minecraft:impermeable", ["divinerpg:stained_glass2", "divinerpg:stained_glass3", "divinerpg:stained_glass4", "divinerpg:stained_glass5", "divinerpg:stained_glass6", "divinerpg:stained_glass7", "divinerpg:stained_glass8"]);
});
ServerEvents.tags('entity_type', function(event) {
  event.remove("minecraft:arthropod", ["callfromthedepth_:deepspider"]);
});
ServerEvents.tags('item', function(event) {
  event.remove("minecraft:chest_armor", ["callfromthedepth_:depth_armor_chestplate"]);
  event.remove("minecraft:foot_armor", ["callfromthedepth_:depth_armor_boots"]);
  event.remove("minecraft:head_armor", ["callfromthedepth_:depth_armor_helmet"]);
  event.remove("minecraft:leg_armor", ["callfromthedepth_:depth_armor_leggings"]);
  event.remove("minecraft:enchantable/durability", ["callfromthedepth_:pillsfordarkness"]);
});
