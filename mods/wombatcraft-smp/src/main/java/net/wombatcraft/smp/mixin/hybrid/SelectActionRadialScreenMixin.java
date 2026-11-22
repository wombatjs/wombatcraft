package net.wombatcraft.smp.mixin.hybrid;

import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.entity.player.actions.IActionHandler;
import de.teamlapen.vampirism.client.ClientConfigHelper;
import de.teamlapen.vampirism.client.gui.screens.SelectActionRadialScreen;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.wombatcraft.smp.HybridFactions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = SelectActionRadialScreen.class, remap = false)
abstract class SelectActionRadialScreenMixin {
    @Shadow @Final private IFactionPlayer<?> player;

    @Redirect(method = "lambda$show$1", at = @At(value = "INVOKE",
            target = "Lde/teamlapen/vampirism/client/ClientConfigHelper;getActionOrder(Lde/teamlapen/vampirism/api/entity/factions/IPlayableFaction;)Ljava/util/List;"))
    private static List<IAction<?>> wombatcraft$combineActionOrder(
            IPlayableFaction<?> faction, KeyMapping key, IFactionPlayer<?> player) {
        List<IAction<?>> order = ClientConfigHelper.getActionOrder(faction);
        if (!HybridFactions.isHybrid(player.getRepresentingPlayer())) return order;
        var combined = new LinkedHashSet<>(order);
        combined.addAll(ClientConfigHelper.getActionOrder(HybridFactions.werewolf()));
        return new ArrayList<>(combined);
    }

    @Redirect(method = "lambda$show$0", at = @At(value = "INVOKE",
            target = "Lde/teamlapen/vampirism/api/entity/player/actions/IActionHandler;isActionUnlocked(Lde/teamlapen/vampirism/api/entity/player/actions/IAction;)Z"))
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean wombatcraft$isUnlocked(IActionHandler original, IAction action, IFactionPlayer<?> player, IAction<?> candidate) {
        return ((IActionHandler) wombatcraft$actionOwner(player, action).getActionHandler()).isActionUnlocked(action);
    }

    @Redirect(method = "drawSlice", at = @At(value = "INVOKE",
            target = "Lde/teamlapen/vampirism/api/entity/player/actions/IActionHandler;getPercentageForAction(Lde/teamlapen/vampirism/api/entity/player/actions/IAction;)F"))
    @SuppressWarnings({"rawtypes", "unchecked"})
    private float wombatcraft$cooldown(IActionHandler original, IAction action) {
        return ((IActionHandler) wombatcraft$actionOwner(player, action).getActionHandler()).getPercentageForAction(action);
    }

    @Redirect(method = "drawSlice", at = @At(value = "INVOKE",
            target = "Lde/teamlapen/vampirism/api/entity/player/actions/IAction;canUse(Lde/teamlapen/vampirism/api/entity/player/IFactionPlayer;)Lde/teamlapen/vampirism/api/entity/player/actions/IAction$PERM;"))
    @SuppressWarnings({"rawtypes", "unchecked"})
    private IAction.PERM wombatcraft$renderPermission(IAction action, IFactionPlayer<?> current) {
        return action.canUse(wombatcraft$actionOwner(current, action));
    }

    @Unique
    private static IFactionPlayer<?> wombatcraft$actionOwner(IFactionPlayer<?> current, IAction<?> action) {
        if (!HybridFactions.isHybrid(current.getRepresentingPlayer())) return current;
        return action.getFaction().flatMap(faction -> HybridFactions.playerFor(current.getRepresentingPlayer(), faction))
                .orElse(current);
    }
}
