package net.wombatcraft.smp.mixin.hybrid;

import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.client.core.ModKeys;
import net.wombatcraft.smp.HybridFactions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ModKeys.class, remap = false)
abstract class ModKeysMixin {
    @ModifyVariable(method = "toggleBoundAction", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private IFactionPlayer<?> wombatcraft$boundActionOwner(IFactionPlayer<?> original, IFactionPlayer<?> current, IAction<?> action) {
        if (action == null || !HybridFactions.isHybrid(original.getRepresentingPlayer())) return original;
        return action.getFaction().flatMap(faction -> HybridFactions.playerFor(original.getRepresentingPlayer(), faction))
                .orElse(original);
    }
}
