package net.wombatcraft.smp.mixin.hybrid;

import de.teamlapen.vampirism.entity.player.vampire.VampirePlayer;
import de.teamlapen.vampirism.entity.player.vampire.actions.VampireActions;
import de.teamlapen.werewolves.api.entities.player.IWerewolfPlayer;
import de.teamlapen.werewolves.api.entities.werewolf.WerewolfForm;
import de.teamlapen.werewolves.entities.player.werewolf.actions.WerewolfFormAction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WerewolfFormAction.class, remap = false)
abstract class WerewolfFormMixin {
    @Shadow @Final private WerewolfForm form;

    @Inject(method = "canBeUsedBy(Lde/teamlapen/werewolves/api/entities/player/IWerewolfPlayer;)Z",
            at = @At("HEAD"), cancellable = true)
    private void wombatcraft$preventOverlappingForms(IWerewolfPlayer wolf, CallbackInfoReturnable<Boolean> result) {
        if (form.isTransformed() && VampirePlayer.getOpt(wolf.getRepresentingPlayer())
                .map(vampire -> vampire.getLevel() > 0 && vampire.getActionHandler().isActionActive(VampireActions.BAT.get()))
                .orElse(false)) result.setReturnValue(false);
    }
}
