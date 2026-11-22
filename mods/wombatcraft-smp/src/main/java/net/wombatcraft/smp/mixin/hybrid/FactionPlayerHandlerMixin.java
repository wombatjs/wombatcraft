package net.wombatcraft.smp.mixin.hybrid;

import de.teamlapen.vampirism.api.entity.factions.IFaction;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.config.VampirismConfig;
import de.teamlapen.vampirism.core.ModAdvancements;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.util.VampirismEventFactory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;
import net.wombatcraft.smp.HybridFactions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FactionPlayerHandler.class, remap = false)
abstract class FactionPlayerHandlerMixin implements HybridFactions.WerewolfProgress {
    @Shadow @Final private Player player;
    @Shadow private IPlayableFaction<?> currentFaction;
    @Shadow private int currentLevel;
    @Shadow private int currentLordLevel;
    @Shadow private void updateCache() { throw new AssertionError(); }
    @Shadow private void sync(boolean all) { throw new AssertionError(); }

    @Unique private static final String WOLF_LEVEL = "wombatcraft_werewolf_level";
    @Unique private static final String WOLF_LORD = "wombatcraft_werewolf_lord";
    @Unique private int wombatcraft$wolfLevel;
    @Unique private int wombatcraft$wolfLord;
    @Unique private int wombatcraft$oldWolfLevel;

    @Override
    public int wombatcraft$wolfLevel() {
        return wombatcraft$wolfLevel;
    }

    @Override
    public int wombatcraft$wolfLord() {
        return wombatcraft$wolfLord;
    }

    @Override
    public boolean wombatcraft$advanceWolfLord(int level) {
        var faction = HybridFactions.werewolf();
        if (wombatcraft$wolfLevel != faction.getHighestReachableLevel()
                || level != wombatcraft$wolfLord + 1 || level > faction.getHighestLordLevel()) return false;
        var wolf = faction.getPlayerCapability(player).resolve().orElse(null);
        if (wolf == null) return false;
        wombatcraft$wolfLord = level;
        wolf.getSkillHandler().addSkillPoints((int) VampirismConfig.BALANCE.skillPointsPerLordLevel.get().doubleValue());
        wolf.getSkillHandler().enableRootSkills();
        if (player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.TRIGGER_FACTION.trigger(serverPlayer, faction, wombatcraft$wolfLevel, level);
        }
        sync(true);
        return true;
    }

    @Inject(method = "canJoin", at = @At("HEAD"), cancellable = true)
    private void wombatcraft$allowSecondFaction(IPlayableFaction<?> faction, CallbackInfoReturnable<Boolean> result) {
        if ((currentFaction == HybridFactions.vampire() && faction == HybridFactions.werewolf())
                || (currentFaction == HybridFactions.werewolf() && faction == HybridFactions.vampire())) {
            result.setReturnValue(wombatcraft$wolfLevel == 0 && VampirismEventFactory.fireCanJoinFactionEvent(
                    wombatcraft$self(), currentFaction, faction) != Event.Result.DENY);
        }
    }

    @Inject(method = "isInFaction", at = @At("HEAD"), cancellable = true)
    private void wombatcraft$hasWerewolfFaction(IFaction<?> faction, CallbackInfoReturnable<Boolean> result) {
        if (faction == HybridFactions.werewolf() && wombatcraft$wolfLevel > 0) result.setReturnValue(true);
    }

    @Inject(method = "getCurrentLevel(Lde/teamlapen/vampirism/api/entity/factions/IPlayableFaction;)I",
            at = @At("HEAD"), cancellable = true)
    private void wombatcraft$werewolfLevel(IPlayableFaction<?> faction, CallbackInfoReturnable<Integer> result) {
        if (faction == HybridFactions.werewolf() && wombatcraft$wolfLevel > 0) result.setReturnValue(wombatcraft$wolfLevel);
    }

    @Inject(method = "setFactionLevel", at = @At("HEAD"), cancellable = true)
    private void wombatcraft$levelSecondaryFaction(IPlayableFaction<?> faction, int level, CallbackInfoReturnable<Boolean> result) {
        if (faction == HybridFactions.werewolf() && wombatcraft$wolfLevel > 0) {
            result.setReturnValue(wombatcraft$setWolfLevel(level));
        }
    }

    @Inject(method = "setFactionAndLevel", at = @At("HEAD"), cancellable = true)
    private void wombatcraft$changeFaction(IPlayableFaction<?> faction, int level, CallbackInfoReturnable<Boolean> result) {
        if (player.level().isClientSide) return;
        if (currentFaction == HybridFactions.vampire() && faction == HybridFactions.werewolf()) {
            result.setReturnValue(wombatcraft$setWolfLevel(level));
        } else if (currentFaction == HybridFactions.werewolf() && faction == HybridFactions.vampire() && level > 0) {
            result.setReturnValue(wombatcraft$addVampire(level));
        } else if (wombatcraft$wolfLevel > 0 && (faction == null || faction == HybridFactions.vampire() && level == 0)) {
            result.setReturnValue(wombatcraft$cureVampire());
        } else if (wombatcraft$wolfLevel > 0 && faction != HybridFactions.vampire()) {
            result.setReturnValue(false);
        }
    }

    @Unique
    private boolean wombatcraft$setWolfLevel(int level) {
        var faction = HybridFactions.werewolf();
        int old = wombatcraft$wolfLevel;
        if (level < 0 || level > faction.getHighestReachableLevel()) return false;
        if (level == old) return true;
        var wolf = faction.getPlayerCapability(player).resolve().orElse(null);
        if (wolf == null || level == 0 && !wolf.canLeaveFaction()) return false;
        if (VampirismEventFactory.fireChangeLevelOrFactionEvent(wombatcraft$self(),
                old == 0 ? null : faction, old, level == 0 ? null : faction, level)) return false;

        wombatcraft$wolfLevel = level;
        if (level < faction.getHighestReachableLevel()) wombatcraft$wolfLord = 0;
        if (level == 0) wolf.getTaskManager().reset();
        else wolf.getSkillHandler().enableRootSkills();
        wolf.onLevelChanged(level, old);
        if (player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.TRIGGER_FACTION.trigger(serverPlayer, faction, level, wombatcraft$wolfLord);
        }
        VampirismEventFactory.fireFactionLevelChangedEvent(wombatcraft$self(), old == 0 ? null : faction, old,
                level == 0 ? null : faction, level);
        sync(true);
        return true;
    }

    @Unique
    private boolean wombatcraft$addVampire(int level) {
        var vampire = HybridFactions.vampire();
        if (level > vampire.getHighestReachableLevel()) return false;
        var capability = vampire.getPlayerCapability(player).resolve().orElse(null);
        if (capability == null || VampirismEventFactory.fireChangeLevelOrFactionEvent(
                wombatcraft$self(), null, 0, vampire, level)) return false;

        wombatcraft$wolfLevel = currentLevel;
        wombatcraft$wolfLord = currentLordLevel;
        currentFaction = vampire;
        currentLevel = level;
        currentLordLevel = 0;
        updateCache();
        capability.getSkillHandler().enableRootSkills();
        capability.onLevelChanged(level, 0);
        if (player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.TRIGGER_FACTION.trigger(serverPlayer, vampire, level, 0);
        }
        VampirismEventFactory.fireFactionLevelChangedEvent(wombatcraft$self(), null, 0, vampire, level);
        sync(true);
        return true;
    }

    @Unique
    private boolean wombatcraft$cureVampire() {
        var vampire = HybridFactions.vampire();
        var capability = vampire.getPlayerCapability(player).resolve().orElse(null);
        int old = currentLevel;
        if (capability == null || !capability.canLeaveFaction() || VampirismEventFactory.fireChangeLevelOrFactionEvent(
                wombatcraft$self(), vampire, old, null, 0)) return false;

        capability.getTaskManager().reset();
        currentFaction = HybridFactions.werewolf();
        currentLevel = wombatcraft$wolfLevel;
        currentLordLevel = wombatcraft$wolfLord;
        wombatcraft$wolfLevel = 0;
        wombatcraft$wolfLord = 0;
        updateCache();
        capability.onLevelChanged(0, old);
        VampirismEventFactory.fireFactionLevelChangedEvent(wombatcraft$self(), vampire, old, null, 0);
        sync(true);
        return true;
    }

    @Inject(method = {"saveNBTData", "writeFullUpdateToNBT"}, at = @At("TAIL"))
    private void wombatcraft$writeOtherFaction(CompoundTag data, CallbackInfo info) {
        data.putInt(WOLF_LEVEL, wombatcraft$wolfLevel);
        data.putInt(WOLF_LORD, wombatcraft$wolfLord);
    }

    @Inject(method = {"loadNBTData", "loadUpdateFromNBT"}, at = @At("HEAD"))
    private void wombatcraft$readOtherFaction(CompoundTag data, CallbackInfo info) {
        var faction = HybridFactions.werewolf();
        wombatcraft$oldWolfLevel = wombatcraft$self().getCurrentLevel(faction);
        boolean vampire = HybridFactions.vampire().getID().toString().equals(data.getString("faction"));
        wombatcraft$wolfLevel = vampire ? Math.max(0, Math.min(faction.getHighestReachableLevel(), data.getInt(WOLF_LEVEL))) : 0;
        wombatcraft$wolfLord = wombatcraft$wolfLevel == faction.getHighestReachableLevel()
                ? Math.max(0, Math.min(faction.getHighestLordLevel(), data.getInt(WOLF_LORD))) : 0;
    }

    @Inject(method = {"loadNBTData", "loadUpdateFromNBT"}, at = @At("TAIL"))
    private void wombatcraft$refreshOtherFaction(CompoundTag data, CallbackInfo info) {
        if (currentFaction == HybridFactions.vampire() && (wombatcraft$oldWolfLevel > 0 || wombatcraft$wolfLevel > 0)) {
            int old = player.level().isClientSide ? wombatcraft$oldWolfLevel : wombatcraft$wolfLevel;
            HybridFactions.werewolf().getPlayerCapability(player).ifPresent(wolf -> wolf.onLevelChanged(wombatcraft$wolfLevel, old));
        }
    }

    @Inject(method = "copyFrom", at = @At("HEAD"))
    private void wombatcraft$copyOtherFaction(Player original, CallbackInfo info) {
        var old = (HybridFactions.WerewolfProgress) FactionPlayerHandler.get(original);
        wombatcraft$wolfLevel = old.wombatcraft$wolfLevel();
        wombatcraft$wolfLord = old.wombatcraft$wolfLord();
    }

    @Inject(method = "lambda$notifyFaction$5", at = @At("HEAD"), cancellable = true)
    private static void wombatcraft$keepActiveWerewolf(int oldLevel, IFactionPlayer<?> factionPlayer, CallbackInfo info) {
        if (factionPlayer.getFaction() == HybridFactions.werewolf() && factionPlayer.getLevel() > 0) info.cancel();
    }

    @Unique
    private FactionPlayerHandler wombatcraft$self() {
        return (FactionPlayerHandler) (Object) this;
    }
}
