package net.wombatcraft.smp;

import dev.ftb.mods.ftbteams.api.*;
import dev.ftb.mods.ftbchunks.api.*;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.wombatcraft.smp.rules.FactionPolicy;
import java.util.*;

/** Uses existing FTB ownership, membership and persistence. No migration or duplicated roster. */
final class Factions {
    static void register() {
        dev.ftb.mods.ftbteams.api.event.TeamEvent.PLAYER_LEFT_PARTY.register(event->{
            Team oldTeam=event.getTeam();
            var roles=oldTeam.getExtraData().getCompound("wombatcraft_roles");
            if(roles.contains(event.getPlayerId().toString())) {
                roles.remove(event.getPlayerId().toString());
                oldTeam.getExtraData().put("wombatcraft_roles",roles);oldTeam.markDirty();
            }
        });
    }
    static Optional<Team> playerTeam(ServerPlayer player) {
        var api=FTBTeamsAPI.api();
        return api.isManagerLoaded()?api.getManager().getTeamForPlayer(player):Optional.empty();
    }
    static Optional<Team> owner(Level level,BlockPos pos) {
        var api=FTBChunksAPI.api();
        if(!api.isManagerLoaded())return Optional.empty();
        ClaimedChunk claim=api.getManager().getChunk(new ChunkDimPos(level.dimension(),pos.getX()>>4,pos.getZ()>>4));
        return claim==null?Optional.empty():Optional.of(claim.getTeamData().getTeam());
    }
    static FactionPolicy.Role role(Team team,UUID player) {
        TeamRank rank=team.getRankForPlayer(player);
        if(rank==TeamRank.OWNER)return FactionPolicy.Role.OWNER;
        if(rank==TeamRank.OFFICER)return FactionPolicy.Role.OFFICER;
        if(rank==TeamRank.MEMBER) {
            return team.getExtraData().getCompound("wombatcraft_roles").getBoolean(player.toString())
                    ?FactionPolicy.Role.BUILDER:FactionPolicy.Role.MEMBER;
        }
        return rank==TeamRank.ALLY?FactionPolicy.Role.ALLY:FactionPolicy.Role.VISITOR;
    }
    static void builder(ServerPlayer actor,UUID member,boolean enabled) {
        Team team=playerTeam(actor).orElseThrow(()->new IllegalStateException("Create or join a faction first"));
        if(!team.getOwner().equals(actor.getUUID()))throw new SecurityException("Only the faction owner may assign roles");
        if(team.getRankForPlayer(member)!=TeamRank.MEMBER)throw new IllegalArgumentException("Choose a faction member");
        var roles=team.getExtraData().getCompound("wombatcraft_roles");
        roles.putBoolean(member.toString(),enabled);team.getExtraData().put("wombatcraft_roles",roles);team.markDirty();
    }
}
