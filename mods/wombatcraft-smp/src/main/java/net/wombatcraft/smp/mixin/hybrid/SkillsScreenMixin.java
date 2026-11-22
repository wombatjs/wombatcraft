package net.wombatcraft.smp.mixin.hybrid;

import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.client.gui.screens.skills.SkillsScreen;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import java.util.Optional;
import net.wombatcraft.smp.HybridFactions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = SkillsScreen.class, remap = false)
abstract class SkillsScreenMixin {
    @Shadow @Final private IFactionPlayer<?> factionPlayer;

    @Redirect(method = "lambda$init$5", at = @At(value = "INVOKE",
            target = "Lde/teamlapen/vampirism/entity/factions/FactionPlayerHandler;getCurrentFactionPlayer()Ljava/util/Optional;"))
    private Optional<? extends IFactionPlayer<?>> wombatcraft$skillTreeOwner(FactionPlayerHandler handler) {
        return HybridFactions.playerFor(handler.getPlayer(), factionPlayer.getFaction());
    }
}
