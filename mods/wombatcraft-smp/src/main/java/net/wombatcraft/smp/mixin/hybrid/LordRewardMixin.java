package net.wombatcraft.smp.mixin.hybrid;

import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.entity.player.tasks.reward.LordLevelReward;
import net.wombatcraft.smp.HybridFactions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LordLevelReward.class, remap = false)
abstract class LordRewardMixin {
    @Shadow @Final private int targetLevel;

    @Inject(method = "applyReward", at = @At("HEAD"), cancellable = true)
    private void wombatcraft$werewolfLordReward(IFactionPlayer<?> recipient, CallbackInfo info) {
        var player = recipient.getRepresentingPlayer();
        if (recipient.getFaction() == HybridFactions.werewolf() && HybridFactions.isHybrid(player)) {
            var hybrid = (HybridFactions.WerewolfProgress) FactionPlayerHandler.get(player);
            hybrid.wombatcraft$advanceWolfLord(targetLevel);
            info.cancel();
        }
    }
}
