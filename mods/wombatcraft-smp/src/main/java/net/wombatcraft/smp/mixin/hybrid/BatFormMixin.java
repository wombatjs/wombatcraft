package net.wombatcraft.smp.mixin.hybrid;

import de.teamlapen.vampirism.api.entity.player.vampire.IVampirePlayer;
import de.teamlapen.vampirism.entity.player.vampire.actions.BatVampireAction;
import de.teamlapen.werewolves.entities.player.werewolf.WerewolfPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BatVampireAction.class, remap = false)
abstract class BatFormMixin {
    @Inject(method = "canBeUsedBy(Lde/teamlapen/vampirism/api/entity/player/vampire/IVampirePlayer;)Z",
            at = @At("HEAD"), cancellable = true)
    private void wombatcraft$preventOverlappingForms(IVampirePlayer vampire, CallbackInfoReturnable<Boolean> result) {
        if (WerewolfPlayer.getOptSave(vampire.getRepresentingPlayer()).map(wolf -> wolf.getForm().isTransformed()).orElse(false)) {
            result.setReturnValue(false);
        }
    }
}
