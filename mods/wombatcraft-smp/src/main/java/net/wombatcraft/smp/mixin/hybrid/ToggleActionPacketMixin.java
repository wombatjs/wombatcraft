package net.wombatcraft.smp.mixin.hybrid;

import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.network.ServerboundToggleActionPacket;
import de.teamlapen.vampirism.util.RegUtil;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullFunction;
import net.wombatcraft.smp.HybridFactions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ServerboundToggleActionPacket.class, remap = false)
abstract class ToggleActionPacketMixin {
    @Redirect(method = "lambda$handle$4", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/common/util/LazyOptional;map(Lnet/minecraftforge/common/util/NonNullFunction;)Ljava/util/Optional;"))
    private static Optional<Optional<? extends IFactionPlayer<?>>> wombatcraft$routeAction(
            LazyOptional<FactionPlayerHandler> handler,
            NonNullFunction<FactionPlayerHandler, Optional<? extends IFactionPlayer<?>>> original,
            ServerPlayer player, ServerboundToggleActionPacket packet) {
        var action = RegUtil.getAction(packet.actionId());
        if (action != null && action.getFaction().isPresent() && HybridFactions.isHybrid(player)) {
            return Optional.of(HybridFactions.playerFor(player, action.getFaction().get()));
        }
        return handler.map(original);
    }
}
