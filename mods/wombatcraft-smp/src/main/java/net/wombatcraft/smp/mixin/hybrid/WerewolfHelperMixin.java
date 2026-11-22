package net.wombatcraft.smp.mixin.hybrid;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.util.Helper;
import net.minecraft.world.entity.player.Player;
import net.wombatcraft.smp.HybridFactions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Helper.class, remap = false)
abstract class WerewolfHelperMixin {
    @Inject(method = "isWerewolf(Lnet/minecraft/world/entity/player/Player;)Z", at = @At("HEAD"), cancellable = true)
    private static void wombatcraft$isWerewolf(Player player, CallbackInfoReturnable<Boolean> result) {
        if (player != null) {
            result.setReturnValue(VampirismAPI.getFactionPlayerHandler(player)
                    .map(handler -> handler.getCurrentLevel(HybridFactions.werewolf()) > 0).orElse(false));
        }
    }
}
