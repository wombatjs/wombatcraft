package net.wombatcraft.smp;

import dev.ftb.mods.ftbchunks.api.*;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.*;
import net.wombatcraft.smp.rules.FactionPolicy;
import java.util.*;

/** FTB's own protection pipeline calls this hook. -1 keeps the original protection decision. */
public final class ClaimHooks {
    private static final UUID SPAWN=UUID.fromString("2de87e97-dce4-41d0-b02d-c24a395997a1");
    private static final String KEY="wombatcraft_chunk_policies";
    static String key(ClaimedChunk claim){return claim.getPos().dimension().location()+"/"+claim.getPos().x()+"/"+claim.getPos().z();}
    static ClaimedChunk claim(ServerPlayer player){
        return FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(player));
    }
    static String preset(ClaimedChunk claim){
        if(claim==null)return "unclaimed";
        CompoundTag rule=claim.getTeamData().getTeam().getExtraData().getCompound(KEY).getCompound(key(claim));
        return rule.getLong("claimed_at")==claim.getTimeClaimed() ? rule.getString("preset") : "ftb_default";
    }
    static void setPreset(ServerPlayer actor,String preset){
        if(!Set.of("private_factory","public_shop","officer_storage","ftb_default").contains(preset))throw new IllegalArgumentException("Unknown policy");
        ClaimedChunk claim=claim(actor);
        if(claim==null)throw new IllegalStateException("Stand inside a faction claim");
        Team team=claim.getTeamData().getTeam();
        if(team.isServerTeam())throw new SecurityException("Server claims are managed separately");
        var role=Factions.role(team,actor.getUUID());
        if(role!=FactionPolicy.Role.OWNER&&role!=FactionPolicy.Role.OFFICER)throw new SecurityException("Officer permission required");
        CompoundTag all=team.getExtraData().getCompound(KEY);
        if(preset.equals("ftb_default"))all.remove(key(claim));
        else {
            CompoundTag rule=new CompoundTag();rule.putLong("claimed_at",claim.getTimeClaimed());rule.putString("preset",preset);
            all.put(key(claim),rule);
        }
        team.getExtraData().put(KEY,all);team.markDirty();
    }
    public static int decide(Entity actor,InteractionHand hand,BlockPos pos,Protection protection,Entity target){
        if(!WombatcraftSmp.ENABLED.get() || !WombatcraftSmp.CHUNK_POLICIES.get() || actor==null || actor.level().isClientSide())return -1;
        var api=FTBChunksAPI.api();if(!api.isManagerLoaded())return 1;
        ClaimedChunk claim=api.getManager().getChunk(new ChunkDimPos(actor.level().dimension(),pos.getX()>>4,pos.getZ()>>4));
        if(claim==null)return -1;
        Team team=claim.getTeamData().getTeam();
        if(team.isServerTeam()||SPAWN.equals(team.getId()))return -1;
        String preset=preset(claim);if(preset.equals("ftb_default")||preset.isEmpty())return -1;
        if(!(actor instanceof ServerPlayer player))return 1;
        if(api.getManager().getBypassProtection(player.getUUID()))return -1;
        FactionPolicy.Role role=Factions.role(team,player.getUUID());
        if(role==FactionPolicy.Role.OWNER||role==FactionPolicy.Role.OFFICER)return 0;
        if(preset.equals("officer_storage"))return 1;
        FactionPolicy.Action action;
        if(protection==Protection.EDIT_BLOCK || protection==Protection.EDIT_FLUID || protection==Protection.EDIT_AND_INTERACT_BLOCK)
            action=FactionPolicy.Action.BUILD;
        else if(protection==Protection.INTERACT_BLOCK){
            Block block=actor.level().getBlockState(pos).getBlock();
            action=block instanceof DoorBlock||block instanceof FenceGateBlock||block instanceof TrapDoorBlock
                    ?FactionPolicy.Action.DOOR:FactionPolicy.Action.MACHINE;
        } else if(protection==Protection.INTERACT_ENTITY)action=target instanceof net.minecraft.world.entity.npc.AbstractVillager
                ?FactionPolicy.Action.TRADE:FactionPolicy.Action.ENTITY;
        else if(protection==Protection.ATTACK_NONLIVING_ENTITY)action=FactionPolicy.Action.BUILD;
        else return 1; // Unknown item interactions cannot bypass a configured policy.
        var rule=preset.equals("public_shop")?FactionPolicy.publicShop(team.getId()):FactionPolicy.privateFactory(team.getId());
        return rule.access().getOrDefault(action,Set.of()).contains(role)?0:1;
    }
}
