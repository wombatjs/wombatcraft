package net.wombatcraft.smp.mixin.hybrid;

import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.network.ServerboundSimpleInputEvent;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullFunction;
import net.wombatcraft.smp.HybridFactions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ServerboundSimpleInputEvent.class, remap = false)
abstract class SimpleInputPacketMixin {
    @Redirect(method = "lambda$handle$4", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/common/util/LazyOptional;map(Lnet/minecraftforge/common/util/NonNullFunction;)Ljava/util/Optional;"))
    private static Optional<Optional<? extends IFactionPlayer<?>>> wombatcraft$resetSelectedFaction(
            LazyOptional<FactionPlayerHandler> handler,
            NonNullFunction<FactionPlayerHandler, Optional<? extends IFactionPlayer<?>>> original,
            ServerPlayer player, ServerboundSimpleInputEvent packet) {
        if (packet.type() == ServerboundSimpleInputEvent.Type.RESET_SKILLS && HybridFactions.isHybrid(player)) {
            return Optional.of(HybridFactions.currentMenuPlayer(player));
        }
        return handler.map(original);
    }
}
