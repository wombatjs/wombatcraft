package net.wombatcraft.smp.mixin.hybrid;

import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.items.IFactionExclusiveItem;
import de.teamlapen.vampirism.api.items.IFactionLevelItem;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.entity.player.ModPlayerEventHandler;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullFunction;
import net.wombatcraft.smp.HybridFactions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ModPlayerEventHandler.class, remap = false)
abstract class FactionItemMixin {
    @Redirect(method = "checkItemUsePerm", at = @At(value = "INVOKE", ordinal = 1,
            target = "Lnet/minecraftforge/common/util/LazyOptional;map(Lnet/minecraftforge/common/util/NonNullFunction;)Ljava/util/Optional;"))
    private Optional<Integer> wombatcraft$levelForItem(LazyOptional<FactionPlayerHandler> handler,
            NonNullFunction<FactionPlayerHandler, Integer> original, ItemStack stack, Player player) {
        if (HybridFactions.isHybrid(player) && stack.getItem() instanceof IFactionExclusiveItem item
                && item.getExclusiveFaction(stack) instanceof IPlayableFaction<?> faction) {
            return handler.map(value -> value.getCurrentLevel(faction));
        }
        return handler.map(original);
    }

    @Redirect(method = "checkItemUsePerm", at = @At(value = "INVOKE",
            target = "Ljava/util/Optional;flatMap(Ljava/util/function/Function;)Ljava/util/Optional;"))
    private Optional<? extends IFactionPlayer<?>> wombatcraft$skillsForItem(Optional<FactionPlayerHandler> handler,
            Function<FactionPlayerHandler, Optional<? extends IFactionPlayer<?>>> original, ItemStack stack, Player player) {
        if (HybridFactions.isHybrid(player) && stack.getItem() instanceof IFactionLevelItem<?> item) {
            var skill = item.getRequiredSkill(stack);
            if (skill != null && skill.getFaction().isPresent()) {
                return HybridFactions.playerFor(player, skill.getFaction().get());
            }
        }
        return handler.flatMap(original);
    }
}
