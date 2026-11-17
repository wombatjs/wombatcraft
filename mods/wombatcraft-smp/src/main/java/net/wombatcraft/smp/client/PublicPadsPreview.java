package net.wombatcraft.smp.client;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wombatcraft.smp.WombatcraftSmp;
import whocraft.tardis_refined.client.screen.main.MonitorScreen;
import whocraft.tardis_refined.common.capability.tardis.upgrades.UpgradeHandler;
import whocraft.tardis_refined.common.tardis.TardisNavLocation;

/** Explicit opt-in, offline rendering fixture. Never runs in an ordinary game session. */
@Mod.EventBusSubscriber(modid=WombatcraftSmp.ID,value=Dist.CLIENT)
public final class PublicPadsPreview {
    private static int ticks,stage=-1;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("wombatcraft.padsPreview")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.level!=null)return;
        org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
        if(stage<0) {
            if(mc.getOverlay()!=null||mc.screen==null||++ticks<20)return;
            // Exercise the actual transformed monitor method, not a copied menu.
            // Refined's monitor constructor needs a loaded world's shell renderer. This fixture
            // executes only its real list-building method, with the three inputs that method reads.
            MonitorScreen monitor;
            try {
                var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
                monitor=(MonitorScreen)((sun.misc.Unsafe)field.get(null)).allocateInstance(MonitorScreen.class);
                net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(net.minecraft.client.gui.screens.Screen.class,monitor,mc,"f_96541_");
                var upgrades=MonitorScreen.class.getDeclaredField("upgradeHandler");upgrades.setAccessible(true);upgrades.set(monitor,new UpgradeHandler());
                monitor.width=mc.getWindow().getGuiScaledWidth();monitor.height=mc.getWindow().getGuiScaledHeight();
            }catch(ReflectiveOperationException ex){throw new IllegalStateException("Cannot construct offline monitor fixture",ex);}
            var entries=monitor.createSelectionList().children();
            if(!entries.get(3).getNarration().getString().equals("Public Landing Pads"))throw new IllegalStateException("Public pads not beneath Waypoints");
            System.out.println("PUBLIC_PADS_MONITOR_CLIENT_PASS");stage=0;ticks=0;show(mc);return;
        }
        if(++ticks==30)Screenshot.grab(mc.gameDirectory,"public-pads-"+stage+".png",mc.getMainRenderTarget(),message->{});
        if(ticks<50)return;
        ticks=0;if(++stage>=6){System.out.println("PUBLIC_PADS_PREVIEW_COMPLETE");mc.stop();return;}show(mc);
    }
    private static void show(Minecraft mc) {
        mc.options.guiScale().set(stage%2==0?2:3);mc.resizeDisplay();
        JsonObject data=new JsonObject();data.addProperty("mode",stage<2?"directory":"settings");data.addProperty("page",0);data.addProperty("pages",2);data.addProperty("total",8);data.addProperty("editable",stage<4);data.addProperty("revision",0);
        data.addProperty("message",stage>=4?"Only the pad owner or an administrator can edit it.":"");
        JsonArray rows=new JsonArray();String[] names={"Spawn station","The excessively long landing pad name","Overworld market","Moon outpost"};String[] statuses={"Available","Occupied","Incoming","Unverified"};
        for(int i=0;i<4;i++){JsonObject row=new JsonObject();row.addProperty("id","00000000-0000-0000-0000-00000000000"+i);row.addProperty("name",names[i]);row.addProperty("owner","PadOwner");row.addProperty("dimension",i==3?"ad_astra:moon":"minecraft:overworld");row.addProperty("status",statuses[i]);row.addProperty("coordinates","120, 64, 120");row.addProperty("visible",true);row.addProperty("selectable",i==0||i==3);rows.add(row);}
        data.add("pads",rows);data.add("pad",rows.get(0));PublicPadsScreen.preview(data);
    }
}
