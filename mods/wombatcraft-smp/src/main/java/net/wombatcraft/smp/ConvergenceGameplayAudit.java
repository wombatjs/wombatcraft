package net.wombatcraft.smp;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.PartyTeam;
import dev.ftb.mods.ftbteams.data.PlayerTeam;
import dev.ftb.mods.ftbteams.data.TeamManagerImpl;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Disposable-server acceptance fixture. Never runs without the explicit QA JVM flag. */
public final class ConvergenceGameplayAudit {
    private static final Map<UUID,String> MESSAGES=new java.util.HashMap<>();
    private static final UUID TEAM=UUID.fromString("f59b95eb-df74-4fd2-82c0-66a4c2668000");
    private static final UUID FIRST=UUID.fromString("f59b95eb-df74-4fd2-82c0-66a4c2668001");
    private static final UUID SECOND=UUID.fromString("f59b95eb-df74-4fd2-82c0-66a4c2668002");
    private static void require(boolean condition, String reason) { if (!condition) throw new IllegalStateException(reason); }
    private static ItemStack stack(String id, int count) {
        var item=ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        require(item!=null, "Missing acceptance item "+id); return new ItemStack(item,count);
    }
    private static ServerPlayer player(MinecraftServer server, UUID id, String name) {
        var p=new ServerPlayer(server,server.overworld(),new GameProfile(id,name)) {
            @Override public void displayClientMessage(net.minecraft.network.chat.Component message, boolean overlay) { MESSAGES.put(getUUID(),message.getString()); }
        };
        p.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),p);
        p.setPos(12.5,180.5,8.5); return p;
    }

    @SuppressWarnings("unchecked")
    public static void run(MinecraftServer server) throws Exception {
        if (!Boolean.getBoolean("wombatcraft.qa")) throw new IllegalStateException("Disposable QA only");
        var manager=(TeamManagerImpl)FTBTeamsAPI.api().getManager();
        var field=TeamManagerImpl.class.getDeclaredField("knownPlayers"); field.setAccessible(true);
        var known=(Map<UUID,PlayerTeam>)field.get(manager);
        var p1=player(server,FIRST,"ConvergenceQA1"); var p2=player(server,SECOND,"ConvergenceQA2");
        PlayerTeam personal1=known.computeIfAbsent(FIRST,id->new PlayerTeam(manager,id));
        PlayerTeam personal2=known.computeIfAbsent(SECOND,id->new PlayerTeam(manager,id));
        personal1.setPlayerName("ConvergenceQA1"); personal2.setPlayerName("ConvergenceQA2");
        manager.getTeamMap().put(FIRST,personal1); manager.getTeamMap().put(SECOND,personal2);
        var party=(PartyTeam)manager.getTeamMap().computeIfAbsent(TEAM,id->new PartyTeam(manager,id));
        var owner=PartyTeam.class.getDeclaredField("owner"); owner.setAccessible(true); owner.set(party,FIRST);
        boolean restored=personal1.getExtraData().getBoolean("personal_qa_saved");
        personal1.setEffectiveTeam(party); personal2.setEffectiveTeam(party);
        if (restored) require((ConvergenceResearch.known(p1)&5)==5, "Saved personal discoveries did not survive restart");
        personal1.getExtraData().putInt(PersonalProgress.RESEARCH,0);
        personal2.getExtraData().putInt(PersonalProgress.RESEARCH,0);
        var traded=stack("kubejs:resonant_alloy",4); traded.setHoverName(net.minecraft.network.chat.Component.literal("Traded alloy"));
        p1.getInventory().add(traded);
        p1.getInventory().add(stack("kubejs:arcane_core",1));
        require(ConvergenceResearch.record(p1)==5, "Evidence was not recorded for the player");
        require(ConvergenceResearch.known(p2)==0, "Research leaked to a faction member");
        require(ConvergenceResearch.record(p1)==5, "Repeated recording changed evidence");
        require(p1.getInventory().countItem(stack("kubejs:resonant_alloy",1).getItem())==4, "Research consumed items");
        personal2.setEffectiveTeam(personal2);
        require(ConvergenceResearch.known(p2)==0, "Research leaked into a different team");
        personal2.setEffectiveTeam(party);
        require(ConvergenceResearch.known(p2)==0, "Joining a faction granted discoveries");
        personal1.setEffectiveTeam(personal1);
        require(ConvergenceResearch.known(p1)==5,"Leaving a faction lost personal research");
        personal1.setEffectiveTeam(party);
        auditPersonalQuests(server,p1,p2,personal1,personal2,party);
        var reloaded=new PlayerTeam(manager,FIRST); reloaded.deserializeNBT(personal1.serializeNBT());
        require(reloaded.getExtraData().getInt(PersonalProgress.RESEARCH)==5, "FTB serialization lost discoveries");
        var pos=new BlockPos(12,180,8); var level=server.overworld(); var original=level.getBlockState(pos);
        try {
            level.setBlockAndUpdate(pos,ConvergenceResearch.TERMINAL.get().defaultBlockState());
            var menu=new ConvergenceResearch.ResearchMenu(71,p1.getInventory(),pos);
            require(menu.stillValid(p1), "Nearby terminal menu is invalid");
            require(!menu.clickMenuButton(p1,99), "Forged terminal action accepted");
            require(!menu.clickMenuButton(p2,0), "Another player can operate this menu");
            require(menu.clickMenuButton(p1,0), "Valid record action rejected");
            p1.setPos(100,180,100);
            require(!menu.clickMenuButton(p1,0), "Remote terminal action accepted");
            p1.setPos(12.5,180.5,8.5);
            level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            require(!menu.clickMenuButton(p1,0), "Removed terminal remains usable");
        } finally { level.setBlockAndUpdate(pos,original); }
        personal1.getExtraData().putBoolean("personal_qa_saved",true); party.markDirty();
        personal1.markDirty(); personal2.markDirty(); manager.markDirty(); manager.saveNow();
        auditTools(p1);
        auditSpellDamage(p1,p2);
        ConvergenceArsenalAudit.run(server);
        GeneratedDataAudit.run();
        MESSAGES.clear();
        System.out.println("CONVERGENCE_GAMEPLAY_AUDIT_COMPLETE individual=true isolation=true serialized=true restored="+restored+" terminal_security=true tools=true");
    }

    private static void auditPersonalQuests(MinecraftServer server,ServerPlayer p1,ServerPlayer p2,PlayerTeam personal1,PlayerTeam personal2,PartyTeam party)throws Exception {
        Class<?> type=Class.forName("dev.ftb.mods.ftbquests.quest.TeamData"),object=Class.forName("dev.ftb.mods.ftbquests.quest.QuestObject");
        Object file=Class.forName("dev.ftb.mods.ftbquests.quest.ServerQuestFile").getField("INSTANCE").get(null);
        Object a=type.getMethod("get",net.minecraft.world.entity.player.Player.class).invoke(null,p1);
        Object b=type.getMethod("get",net.minecraft.world.entity.player.Player.class).invoke(null,p2);
        require(a!=b,"Faction members share quest progress");
        require(type.getMethod("getTeamId").invoke(a).equals(FIRST),"Quest owner is not player UUID");
        require(PersonalProgress.owns(p1,a)&&!PersonalProgress.owns(p2,a),"Cross-player quest ownership permitted");
        require(PersonalProgress.lookup(dev.ftb.mods.ftbteams.api.FTBTeamsAPI.api().getManager(),p1).orElseThrow()==personal1,"Inventory lookup used faction");
        Object quest=file.getClass().getMethod("getQuest",long.class).invoke(file,Long.parseLong("0919D581FCDD2595",16));
        // Use a fresh synthetic task identity to avoid previous smoke progress.
        long probe=(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        type.getMethod("setCompleted",long.class,java.util.Date.class).invoke(a,probe,new java.util.Date());
        require(((java.util.Optional<?>)type.getMethod("getCompletedTime",long.class).invoke(b,probe)).isEmpty(),"Completion leaked to faction member");
        String other=type.getMethod("serializeNBT").invoke(b).toString();
        personal1.setEffectiveTeam(personal1);
        require(type.getMethod("get",net.minecraft.world.entity.player.Player.class).invoke(null,p1)==a,"Leaving faction changed journal");
        personal1.setEffectiveTeam(party);
        file.getClass().getMethod("playerChangedTeam",dev.ftb.mods.ftbteams.api.event.PlayerChangedTeamEvent.class).invoke(file,new dev.ftb.mods.ftbteams.api.event.PlayerChangedTeamEvent(party,personal1,FIRST,p1));
        require(type.getMethod("get",net.minecraft.world.entity.player.Player.class).invoke(null,p1)==a,"Rejoining changed journal");
        require(other.equals(type.getMethod("serializeNBT").invoke(b).toString()),"Faction switching mutated another journal");
        Object event=new dev.ftb.mods.ftbteams.api.event.PlayerLoggedInAfterTeamEvent(party,p1);
        require(PersonalProgress.loginTeam((dev.ftb.mods.ftbteams.api.event.PlayerLoggedInAfterTeamEvent)event)==personal1,"Login selected faction journal");
        Object alloyQuest=file.getClass().getMethod("getQuest",long.class).invoke(file,Long.parseLong("51FB071ECC6345D0",16));
        Object alloyTask=((java.util.Collection<?>)alloyQuest.getClass().getMethod("getTasks").invoke(alloyQuest)).iterator().next();
        Class<?> taskType=Class.forName("dev.ftb.mods.ftbquests.quest.task.Task");
        for(Object journal:java.util.List.of(a,b)) {
            type.getMethod("setCompleted",long.class,java.util.Date.class).invoke(journal,Long.parseLong("51FB071ECC6345D0",16),null);
            type.getMethod("setCompleted",long.class,java.util.Date.class).invoke(journal,Long.parseLong("12364B02E613EF05",16),null);
            type.getMethod("clearCachedProgress").invoke(journal);
        }
        type.getMethod("resetProgress",taskType).invoke(a,alloyTask);
        type.getMethod("resetProgress",taskType).invoke(b,alloyTask);
        Class.forName("dev.ftb.mods.ftbquests.util.FTBQuestsInventoryListener").getMethod("detect",ServerPlayer.class,ItemStack.class,long.class).invoke(null,p1,ItemStack.EMPTY,0L);
        require((Long)type.getMethod("getProgress",taskType).invoke(a,alloyTask)==1L,"Inventory detection missed personal evidence");
        require((Long)type.getMethod("getProgress",taskType).invoke(b,alloyTask)==0L,"Inventory detection credited faction member");
        // Legacy migration keeps completions and claim records, and is idempotent.
        Object legacy=file.getClass().getMethod("getOrCreateTeamData",UUID.class).invoke(file,party.getId());
        long oldProbe=(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        type.getMethod("setCompleted",long.class,java.util.Date.class).invoke(legacy,oldProbe,new java.util.Date());
        Object reward=((java.util.Collection<?>)quest.getClass().getMethod("getRewards").invoke(quest)).iterator().next();
        Class<?> rewardType=Class.forName("dev.ftb.mods.ftbquests.quest.reward.Reward");
        type.getMethod("markRewardAsClaimed",UUID.class,rewardType,long.class).invoke(legacy,FIRST,reward,System.currentTimeMillis());
        PersonalProgress.migrateMember(file,FIRST,personal1,party);
        require(((java.util.Optional<?>)type.getMethod("getCompletedTime",long.class).invoke(a,oldProbe)).isPresent(),"Migration lost old completion");
        require((Boolean)type.getMethod("isRewardClaimed",UUID.class,rewardType).invoke(a,FIRST,reward),"Migration reset reward claim");
        String migrated=type.getMethod("serializeNBT").invoke(a).toString();
        PersonalProgress.migrateMember(file,FIRST,personal1,party);
        require(migrated.equals(type.getMethod("serializeNBT").invoke(a).toString()),"Migration is not idempotent");
        require(((java.util.Optional<?>)type.getMethod("getCompletedTime",long.class).invoke(b,oldProbe)).isEmpty(),"New migration leaked to other journal");
        var factionView=SmpNetwork.snapshot(p1,0,"",0);
        var contractsView=SmpNetwork.snapshot(p1,0,"",4);
        var townView=SmpNetwork.snapshot(p1,0,"",3);
        require(factionView.has("members")&&!factionView.has("contracts")&&!factionView.has("elections_open"),"Faction menu includes unrelated data");
        require(!contractsView.has("members")&&contractsView.has("contracts_open")&&!contractsView.has("elections_open"),"Contracts menu not isolated");
        require(!townView.has("members")&&!townView.has("contracts_open")&&townView.has("elections_open"),"Town menu not isolated");
        personal2.setEffectiveTeam(personal2);
        require(ContractService.get(server).view(p2,0).get("rows").getAsInt()>0,"Solo player cannot view orders");
        personal2.setEffectiveTeam(party);
        var packet=new SmpNetwork.Request("view","",0,0,71,"",4);
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{packet.write(buffer);require(SmpNetwork.Request.read(buffer).equals(packet),"Menu packet roundtrip failed");}finally{buffer.release();}
        System.out.println("PERSONAL_PROGRESSION_AUDIT_COMPLETE isolated=true faction_switch=true login=true migration=true menus=true solo_contracts=true");
    }

    private static void auditSpellDamage(ServerPlayer player, ServerPlayer attacker) {
        player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ConvergenceContent.HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(ConvergenceContent.CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS,new ItemStack(ConvergenceContent.LEGGINGS.get()));
        player.setItemSlot(EquipmentSlot.FEET,new ItemStack(ConvergenceContent.BOOTS.get()));
        var registry=player.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE);
        var tag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,new ResourceLocation("wombatcraft_smp:arcane_damage"));
        int count=0;
        for(var holder:registry.getTagOrEmpty(tag)) {
            var magic=new net.minecraft.world.damagesource.DamageSource(holder);
            var event=new net.minecraftforge.event.entity.living.LivingHurtEvent(player,magic,10F);
            ConvergenceContent.hurt(event); require(event.getAmount()==8F,"Modded magic resistance failed: "+holder.unwrapKey());
            event=new net.minecraftforge.event.entity.living.LivingHurtEvent(player,new net.minecraft.world.damagesource.DamageSource(holder,attacker),10F);
            ConvergenceContent.hurt(event); require(event.getAmount()==10F,"Modded PvP resistance leaked: "+holder.unwrapKey());
            count++;
        }
        require(count==24,"Expected 24 audited arcane damage types, got "+count);
        for(String id:new String[]{"bloodmagic:sacrifice","bloodmagic:ritual","botania:relic_damage","irons_spellbooks:heartstop"}) {
            var holder=registry.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,new ResourceLocation(id)));
            var event=new net.minecraftforge.event.entity.living.LivingHurtEvent(player,new net.minecraft.world.damagesource.DamageSource(holder),10F);
            ConvergenceContent.hurt(event); require(event.getAmount()==10F,"Armor discounts a ritual or penalty: "+id);
        }
        System.out.println("CONVERGENCE_SPELL_AUDIT_COMPLETE types="+count+" pvp_excluded=true ritual_costs_unchanged=true");
    }

    private static void auditTools(ServerPlayer player) {
        player.getInventory().clearContent();
        var projector=ConvergenceContent.PROJECTOR.get();
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(projector));
        projector.use(player.level(),player,InteractionHand.MAIN_HAND);
        require(!player.hasEffect(MobEffects.WATER_BREATHING), "Projector grants a free effect without charge");
        var charge=stack("kubejs:field_charge",2); var chargeItem=charge.getItem(); player.getInventory().add(charge);
        projector.use(player.level(),player,InteractionHand.MAIN_HAND);
        require(player.hasEffect(MobEffects.WATER_BREATHING)&&player.hasEffect(MobEffects.NIGHT_VISION), "Charged projector failed");
        require(player.getInventory().countItem(chargeItem)==1, "Projector charge debit was not exactly one");
        projector.use(player.level(),player,InteractionHand.MAIN_HAND);
        require(player.getInventory().countItem(chargeItem)==1, "Cooldown consumed another charge");
        player.setItemSlot(EquipmentSlot.FEET,new ItemStack(ConvergenceContent.ASTRAL_BOOTS.get()));
        var fall=new LivingFallEvent(player,10,1); ConvergenceTools.fall(fall);
        require(fall.getDamageMultiplier()==0.5F, "Astral boots did not halve fall damage");
        player.setItemSlot(EquipmentSlot.FEET,new ItemStack(Items.DIAMOND_BOOTS));
        fall=new LivingFallEvent(player,10,1); ConvergenceTools.fall(fall);
        require(fall.getDamageMultiplier()==1F, "Fall bonus leaked onto ordinary boots");
        var prospector=ConvergenceContent.PROSPECTOR.get(); var tool=new ItemStack(prospector);
        player.setItemInHand(InteractionHand.MAIN_HAND,tool);
        var orePos=new BlockPos(14,180,8); var level=player.serverLevel(); var original=level.getBlockState(orePos);
        try {
            level.setBlockAndUpdate(orePos,ConvergenceContent.ORE.get().defaultBlockState());
            prospector.use(player.level(),player,InteractionHand.MAIN_HAND);
            require(MESSAGES.getOrDefault(player.getUUID(),"").contains("14, 180, 8"), "Prospector missed its nearby deposit");
            var pick=new ItemStack(Items.IRON_PICKAXE);
            var drops=net.minecraft.world.level.block.Block.getDrops(level.getBlockState(orePos),level,orePos,null,player,pick);
            require(drops.size()==1&&drops.get(0).is(ConvergenceContent.CRYSTAL.get())&&drops.get(0).getCount()==1, "Normal ore loot failed");
            pick.enchant(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,1);
            drops=net.minecraft.world.level.block.Block.getDrops(level.getBlockState(orePos),level,orePos,null,player,pick);
            require(drops.size()==1&&drops.get(0).is(ConvergenceContent.ORE_ITEM.get()), "Silk Touch ore loot failed");
            pick=new ItemStack(Items.DIAMOND_PICKAXE); pick.enchant(net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE,3);
            for (int i=0;i<16;i++) {
                drops=net.minecraft.world.level.block.Block.getDrops(level.getBlockState(orePos),level,orePos,null,player,pick);
                require(drops.size()==1&&drops.get(0).is(ConvergenceContent.CRYSTAL.get())&&drops.get(0).getCount()>=1&&drops.get(0).getCount()<=4, "Fortune ore loot invalid");
            }
        } finally { level.setBlockAndUpdate(orePos,original); }
        require(tool.getDamageValue()==1, "Prospector did not spend exactly one durability");
        prospector.use(player.level(),player,InteractionHand.MAIN_HAND);
        require(tool.getDamageValue()==1, "Prospector cooldown failed");
    }
}
