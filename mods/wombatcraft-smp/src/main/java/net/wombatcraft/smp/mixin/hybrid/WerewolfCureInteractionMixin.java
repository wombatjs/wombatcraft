package net.wombatcraft.smp.mixin.hybrid;

import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.werewolves.entities.player.ModPlayerEventHandler;
import java.util.Optional;
import java.util.function.Function;
import net.wombatcraft.smp.HybridFactions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ModPlayerEventHandler.class, remap = false)
abstract class WerewolfCureInteractionMixin {
    @Redirect(method = "onItemUseBlock", at = @At(value = "INVOKE",
            target = "Ljava/util/Optional;map(Ljava/util/function/Function;)Ljava/util/Optional;"))
    private Optional<IPlayableFaction<?>> wombatcraft$selectWerewolf(
            Optional<FactionPlayerHandler> handler, Function<FactionPlayerHandler, IPlayableFaction<?>> original) {
        return handler.map(value -> value.isInFaction(HybridFactions.werewolf())
                ? HybridFactions.werewolf() : original.apply(value));
    }
}
