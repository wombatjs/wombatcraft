package net.wombatcraft.smp.mixin.hybrid;

import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.network.ServerboundUnlockSkillPacket;
import de.teamlapen.vampirism.util.RegUtil;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullFunction;
import net.wombatcraft.smp.HybridFactions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ServerboundUnlockSkillPacket.class, remap = false)
abstract class UnlockSkillPacketMixin {
    @Redirect(method = "lambda$handle$1", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/common/util/LazyOptional;map(Lnet/minecraftforge/common/util/NonNullFunction;)Ljava/util/Optional;"))
    private static Optional<Optional<? extends IFactionPlayer<?>>> wombatcraft$routeSkill(
            LazyOptional<FactionPlayerHandler> handler,
            NonNullFunction<FactionPlayerHandler, Optional<? extends IFactionPlayer<?>>> original,
            ServerPlayer player, ServerboundUnlockSkillPacket packet) {
        var skill = RegUtil.getSkill(packet.skillId());
        if (skill != null && skill.getFaction().isPresent() && HybridFactions.isHybrid(player)) {
            return Optional.of(HybridFactions.playerFor(player, skill.getFaction().get()));
        }
        return handler.map(original);
    }
}
