package net.wombatcraft.smp.mixin.hybrid;

import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.network.ServerboundTaskActionPacket;
import java.util.Optional;
import net.minecraft.world.entity.player.Player;
import net.wombatcraft.smp.HybridFactions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ServerboundTaskActionPacket.class, remap = false)
abstract class TaskActionPacketMixin {
    @Redirect(method = "lambda$handle$1", at = @At(value = "INVOKE",
            target = "Lde/teamlapen/vampirism/entity/factions/FactionPlayerHandler;getCurrentFactionPlayer(Lnet/minecraft/world/entity/player/Player;)Ljava/util/Optional;"))
    private static Optional<? extends IFactionPlayer<?>> wombatcraft$currentMenuOwner(Player player) {
        return HybridFactions.currentMenuPlayer(player);
    }
}
