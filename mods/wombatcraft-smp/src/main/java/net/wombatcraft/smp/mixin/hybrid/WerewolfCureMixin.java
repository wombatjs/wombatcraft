package net.wombatcraft.smp.mixin.hybrid;

import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.werewolves.effects.UnWerewolfEffect;
import net.wombatcraft.smp.HybridFactions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = UnWerewolfEffect.class, remap = false)
abstract class WerewolfCureMixin {
    @Redirect(method = "lambda$applyEffectTick$0", at = @At(value = "INVOKE",
            target = "Lde/teamlapen/vampirism/entity/factions/FactionPlayerHandler;setFactionAndLevel(Lde/teamlapen/vampirism/api/entity/factions/IPlayableFaction;I)Z"))
    private static boolean wombatcraft$cureWerewolf(FactionPlayerHandler handler, IPlayableFaction<?> ignored, int level) {
        return handler.setFactionLevel(HybridFactions.werewolf(), level);
    }
}
