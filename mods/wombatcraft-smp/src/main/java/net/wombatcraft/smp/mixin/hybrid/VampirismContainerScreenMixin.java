package net.wombatcraft.smp.mixin.hybrid;

import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.client.gui.screens.VampirismContainerScreen;
import de.teamlapen.vampirism.inventory.VampirismMenu;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.wombatcraft.smp.HybridFactions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = VampirismContainerScreen.class, remap = false)
abstract class VampirismContainerScreenMixin {
    @Shadow @Final private IFactionPlayer<?> factionPlayer;

    @Redirect(method = "<init>", at = @At(value = "INVOKE",
            target = "Lde/teamlapen/vampirism/entity/factions/FactionPlayerHandler;getCurrentFactionPlayer(Lnet/minecraft/world/entity/player/Player;)Ljava/util/Optional;"))
    private Optional<? extends IFactionPlayer<?>> wombatcraft$showMenuOwner(
            Player player, VampirismMenu menu, Inventory inventory, Component title) {
        return Optional.of(HybridFactions.menuOwner(menu));
    }

    @Redirect(method = "lambda$init$7", at = @At(value = "INVOKE",
            target = "Lde/teamlapen/vampirism/entity/factions/FactionPlayerHandler;getCurrentFactionPlayer(Lnet/minecraft/world/entity/player/Player;)Ljava/util/Optional;"))
    private Optional<? extends IFactionPlayer<?>> wombatcraft$openOwnersSkillTree(Player player) {
        return HybridFactions.playerFor(player, factionPlayer.getFaction());
    }
}
