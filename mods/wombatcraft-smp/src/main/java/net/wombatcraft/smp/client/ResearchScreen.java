package net.wombatcraft.smp.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.wombatcraft.smp.ConvergenceResearch;
import net.wombatcraft.smp.WombatcraftSmp;

@Mod.EventBusSubscriber(modid=WombatcraftSmp.ID, bus=Mod.EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class ResearchScreen extends AbstractContainerScreen<ConvergenceResearch.ResearchMenu> {
    public ResearchScreen(ConvergenceResearch.ResearchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 310; imageHeight = 238;
    }
    @SubscribeEvent public static void register(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ConvergenceResearch.MENU.get(), ResearchScreen::new));
    }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Record carried discoveries"), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos+42, topPos+207, 226, 20).build());
    }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos+imageWidth, topPos+imageHeight, 0xff13212b);
        g.fill(leftPos+1, topPos+1, leftPos+imageWidth-1, topPos+33, 0xff24434c);
        for (int i=0; i<ConvergenceResearch.LABELS.length; i++) {
            int x=leftPos+12+(i/7)*148, y=topPos+45+(i%7)*19;
            boolean known=(menu.discoveries() & (1<<i)) != 0;
            g.drawString(font, (known ? "+ " : "- ")+ConvergenceResearch.LABELS[i], x, y, known?0xff8de1b0:0xffa1abb5, false);
        }
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 12, 12, 0xffedf6f3, false);
        g.drawString(font, "Your personal discoveries", 12, 181, 0xffb9cbd2, false);
        g.drawString(font, "Hover a discovery for its production route.", 12, 193, 0xffb9cbd2, false);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g); super.render(g, mouseX, mouseY, partial); renderTooltip(g, mouseX, mouseY);
        for (int i=0;i<ConvergenceResearch.GUIDES.length;i++) {
            int x=leftPos+12+(i/7)*148,y=topPos+45+(i%7)*19;
            if(mouseX>=x&&mouseX<x+143&&mouseY>=y&&mouseY<y+16) {
                g.renderTooltip(font,font.split(Component.literal(ConvergenceResearch.GUIDES[i]),260),mouseX,mouseY);
            }
        }
    }
}
