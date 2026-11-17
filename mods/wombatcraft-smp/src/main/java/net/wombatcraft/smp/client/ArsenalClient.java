package net.wombatcraft.smp.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid="wombatcraft_smp", bus=Mod.EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class ArsenalClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            var bow=ForgeRegistries.ITEMS.getValue(new ResourceLocation("wombatcraft_smp","convergium_longbow"));
            ItemProperties.register(bow,new ResourceLocation("pull"),(stack,level,entity,seed) ->
                entity!=null && entity.getUseItem()==stack ? (stack.getUseDuration()-entity.getUseItemRemainingTicks())/20F : 0F);
            ItemProperties.register(bow,new ResourceLocation("pulling"),(stack,level,entity,seed) ->
                entity!=null && entity.isUsingItem() && entity.getUseItem()==stack ? 1F : 0F);
        });
    }
}
