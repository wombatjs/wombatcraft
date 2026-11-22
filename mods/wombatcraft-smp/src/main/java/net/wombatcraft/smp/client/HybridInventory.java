package net.wombatcraft.smp.client;

import de.teamlapen.vampirism.config.VampirismConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wombatcraft.smp.HybridFactions;
import net.wombatcraft.smp.WombatcraftSmp;

@Mod.EventBusSubscriber(modid = WombatcraftSmp.ID, value = Dist.CLIENT)
public final class HybridInventory {
    private static final ResourceLocation WOLF_ICON = new ResourceLocation("werewolves", "textures/gui/appearance_button.png");

    private HybridInventory() {}

    public static void prepareWolfMenu(int entityId) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.getId() == entityId) HybridFactions.prepareWolfMenu(player);
    }

    @SubscribeEvent
    public static void addWolfButton(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        var player = screen.getMinecraft().player;
        if (player == null || !VampirismConfig.CLIENT.guiSkillButton.get() || !HybridFactions.isHybrid(player)) return;
        event.addListener(new WolfMenuButton(screen));
    }

    @SubscribeEvent
    public static void repositionWolfButton(ScreenEvent.Render.Pre event) {
        if (event.getScreen() instanceof InventoryScreen screen) {
            for (var child : screen.children()) {
                if (child instanceof WolfMenuButton button) {
                    button.setPosition(x(screen), y(screen));
                }
            }
        }
    }

    private static int x(InventoryScreen screen) {
        return screen.getGuiLeft() + VampirismConfig.CLIENT.overrideGuiSkillButtonX.get() + 22;
    }

    private static int y(InventoryScreen screen) {
        return screen.height / 2 + VampirismConfig.CLIENT.overrideGuiSkillButtonY.get();
    }

    private static final class WolfMenuButton extends ImageButton {
        WolfMenuButton(InventoryScreen screen) {
            super(x(screen), y(screen), 20, 20, 0, 0, 20, WOLF_ICON, 20, 40,
                    button -> HybridFactions.requestWolfMenu(), HybridFactions.werewolf().getName());
            setTooltip(Tooltip.create(Component.translatable("gui.wombatcraft.werewolf_menu")));
        }
    }
}
