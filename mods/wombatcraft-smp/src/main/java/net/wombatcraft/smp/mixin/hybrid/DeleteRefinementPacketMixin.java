package net.wombatcraft.smp.mixin.hybrid;

import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.network.ServerboundDeleteRefinementPacket;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullFunction;
import net.wombatcraft.smp.HybridFactions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ServerboundDeleteRefinementPacket.class, remap = false)
abstract class DeleteRefinementPacketMixin {
    @Redirect(method = "lambda$handle$1", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/common/util/LazyOptional;map(Lnet/minecraftforge/common/util/NonNullFunction;)Ljava/util/Optional;"))
    private static Optional<Optional<? extends IFactionPlayer<?>>> wombatcraft$currentMenuOwner(
            LazyOptional<FactionPlayerHandler> handler,
            NonNullFunction<FactionPlayerHandler, Optional<? extends IFactionPlayer<?>>> original,
            ServerPlayer player, ServerboundDeleteRefinementPacket packet) {
        return HybridFactions.isHybrid(player) ? Optional.of(HybridFactions.currentMenuPlayer(player)) : handler.map(original);
    }
}
