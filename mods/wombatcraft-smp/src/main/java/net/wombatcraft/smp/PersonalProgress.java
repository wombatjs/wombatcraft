package net.wombatcraft.smp;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.TeamManagerImpl;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.event.PlayerLoggedInAfterTeamEvent;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.server.ServerLifecycleHooks;

/** FTB quests keep their native save format, but its owner is always a player UUID. */
public final class PersonalProgress {
    public static final String RESEARCH="wombatcraft_personal_research_v1";
    private static final String LEGACY_RESEARCH="wombatcraft_convergence_v1";
    private PersonalProgress() {}
    public static Object data(Object file,Entity player) {
        try { return file.getClass().getMethod("getOrCreateTeamData",UUID.class).invoke(file,player.getUUID()); }
        catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot resolve personal quest progress",e);}
    }
    public static boolean owns(Player player,Object data) {
        try {return player.getUUID().equals(data.getClass().getMethod("getTeamId").invoke(data));}
        catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    public static Team loginTeam(PlayerLoggedInAfterTeamEvent event) {
        return FTBTeamsAPI.api().getManager().getPlayerTeamForPlayerID(event.getPlayer().getUUID()).orElseThrow();
    }
    public static Collection<ServerPlayer> online(Object data) {
        try {
            MinecraftServer server=ServerLifecycleHooks.getCurrentServer();
            if(server==null)return List.of();
            UUID id=(UUID)data.getClass().getMethod("getTeamId").invoke(data);
            ServerPlayer player=server.getPlayerList().getPlayer(id);
            return player==null?List.of():List.of(player);
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    public static Optional<Team> lookup(dev.ftb.mods.ftbteams.api.TeamManager manager,ServerPlayer player) {
        return manager.getPlayerTeamForPlayerID(player.getUUID()).map(t->(Team)t);
    }
    public static Team personal(ServerPlayer player) {
        return FTBTeamsAPI.api().getManager().getPlayerTeamForPlayerID(player.getUUID()).orElseThrow(()->new IllegalStateException("Personal progress is not available yet"));
    }
    public static void initialize(MinecraftServer server) {
        var storage=server.overworld().getDataStorage();
        var state=storage.computeIfAbsent(Migration::load,Migration::new,"wombatcraft_personal_progress");
        if(state.done)return;
        try {
            Object file=Class.forName("dev.ftb.mods.ftbquests.quest.ServerQuestFile").getField("INSTANCE").get(null);
            Class<?> type=Class.forName("dev.ftb.mods.ftbquests.quest.TeamData");
            var manager=(TeamManagerImpl)FTBTeamsAPI.api().getManager();
            int migrated=0;
            // Eagerly snapshot every known member, including offline players. Later
            // faction joins cannot inherit another person's progression.
            for(var entry:manager.getKnownPlayerTeams().entrySet()) {
                UUID id=entry.getKey();Team personal=entry.getValue();
                Team effective=manager.getTeamForPlayerID(id).orElse(personal);
                if(migrateMember(file,id,personal,effective))migrated++;
            }
            // Save destination data before committing the migration marker. A retry
            // only merges maxima and existing claim records, never awards items.
            file.getClass().getMethod("saveNow").invoke(file);manager.saveNow();
            state.done=true;state.setDirty();storage.save();
            WombatcraftSmp.LOGGER.info("Individual progression migration complete: {} legacy members; source records retained",migrated);
        }catch(Exception e){throw new IllegalStateException("Individual progression migration failed; legacy records retained",e);}
    }
    static boolean migrateMember(Object file,UUID id,Team personal,Team effective)throws Exception {
        Class<?> type=Class.forName("dev.ftb.mods.ftbquests.quest.TeamData");
        Object target=file.getClass().getMethod("getOrCreateTeamData",UUID.class).invoke(file,id);
        boolean migrated=false;
        if(!effective.getId().equals(id)) {
            Object legacy=file.getClass().getMethod("getNullableTeamData",UUID.class).invoke(file,effective.getId());
            if(legacy!=null) {
                type.getMethod("mergeData",type).invoke(target,legacy);
                type.getMethod("setLocked",boolean.class).invoke(target,(Boolean)type.getMethod("isLocked").invoke(target)||(Boolean)type.getMethod("isLocked").invoke(legacy));
                type.getMethod("setRewardsBlocked",boolean.class).invoke(target,(Boolean)type.getMethod("areRewardsBlocked").invoke(target)||(Boolean)type.getMethod("areRewardsBlocked").invoke(legacy));
                mergeCounters(target,legacy,"completionCount");mergeCounters(target,legacy,"questRepeatableTime");
                type.getMethod("clearCachedProgress").invoke(target);type.getMethod("markDirty").invoke(target);migrated=true;
            }
        }
        int research=personal.getExtraData().getInt(RESEARCH)|personal.getExtraData().getInt(LEGACY_RESEARCH)|effective.getExtraData().getInt(LEGACY_RESEARCH);
        personal.getExtraData().putInt(RESEARCH,research & 0x3fff);personal.markDirty();return migrated;
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void mergeCounters(Object target,Object source,String name)throws Exception {
        var field=target.getClass().getDeclaredField(name);field.setAccessible(true);
        Map dest=(Map)field.get(target),src=(Map)field.get(source);
        for(Object key:src.keySet()) {
            Number value=(Number)src.get(key),old=(Number)dest.get(key);
            if(old==null||value.longValue()>old.longValue())dest.put(key,value);
        }
    }
    public static final class Migration extends SavedData {
        boolean done;
        static Migration load(CompoundTag tag){var state=new Migration();state.done=tag.getBoolean("done");return state;}
        @Override public CompoundTag save(CompoundTag tag){tag.putBoolean("done",done);return tag;}
    }
}
