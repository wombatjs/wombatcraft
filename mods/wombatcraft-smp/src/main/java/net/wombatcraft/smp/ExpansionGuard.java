package net.wombatcraft.smp;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.registries.ForgeRegistries;

final class ExpansionGuard {
    static void register(){MinecraftForge.EVENT_BUS.addListener(ExpansionGuard::breakBlock);}
    private static void breakBlock(BlockEvent.BreakEvent event){
        ResourceLocation id=ForgeRegistries.BLOCKS.getKey(event.getState().getBlock());
        if(id==null||!id.toString().equals("lucky:lucky_block")||!(event.getLevel() instanceof Level level))return;
        // Check claim metadata only when a lucky block opens; never load surrounding chunks.
        BlockPos center=event.getPos();
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(Factions.owner(level,center.offset(x*16,0,z*16)).isPresent()){
            event.setCanceled(true);event.getPlayer().sendSystemMessage(Component.literal("Open lucky blocks at least three chunks away from claimed land."));return;
        }
        if(level.dimension().location().toString().equals(ArenaGuard.DIMENSION))event.setCanceled(true);
    }
}
