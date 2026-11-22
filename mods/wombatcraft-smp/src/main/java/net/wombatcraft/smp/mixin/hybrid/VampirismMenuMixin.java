package net.wombatcraft.smp.mixin.hybrid;

import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.inventory.VampirismMenu;
import java.util.Optional;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.wombatcraft.smp.HybridFactions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VampirismMenu.class, remap = false)
abstract class VampirismMenuMixin implements HybridFactions.MenuOwner {
    @Shadow @Final private IFactionPlayer<?> factionPlayer;

    @Override
    public IFactionPlayer<?> wombatcraft$menuOwner() {
        return factionPlayer;
    }

    @Redirect(method = "lambda$static$4", at = @At(value = "INVOKE",
            target = "Lde/teamlapen/vampirism/entity/factions/FactionPlayerHandler;getCurrentFactionPlayer(Lnet/minecraft/world/entity/player/Player;)Ljava/util/Optional;"))
    private static Optional<? extends IFactionPlayer<?>> wombatcraft$selectRefinementSlots(Player player) {
        return HybridFactions.constructingMenuPlayer(player);
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE",
            target = "Lde/teamlapen/vampirism/entity/factions/FactionPlayerHandler;getCurrentFactionPlayer()Ljava/util/Optional;"))
    private Optional<? extends IFactionPlayer<?>> wombatcraft$selectMenuPlayer(FactionPlayerHandler handler) {
        return HybridFactions.constructingMenuPlayer(handler.getPlayer());
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void wombatcraft$menuConstructed(int id, Inventory inventory, CallbackInfo info) {
        HybridFactions.finishMenuConstruction(inventory.player);
    }
}
