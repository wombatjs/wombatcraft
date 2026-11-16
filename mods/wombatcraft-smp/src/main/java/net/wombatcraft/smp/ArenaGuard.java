package net.wombatcraft.smp;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Defense in depth. World-edit bypass exists only for the scoped server build operation. */
@Mod.EventBusSubscriber(modid=WombatcraftSmp.ID)
public final class ArenaGuard {
    public static final String DIMENSION="wombatcraft_smp:arena";
    private static final ResourceLocation ARENA_ID=new ResourceLocation(DIMENSION);
    private static final ThreadLocal<Integer> BUILD_DEPTH=ThreadLocal.withInitial(()->0);
    public static boolean arena(Level level){return level.dimension().location().equals(ARENA_ID);}
    public static boolean allowLevel(Level level){return level.isClientSide()||!arena(level)||BUILD_DEPTH.get()>0;}
    public static boolean allowChunk(LevelChunk chunk){return allowLevel(chunk.getLevel());}
    static void build(Runnable action){
        BUILD_DEPTH.set(BUILD_DEPTH.get()+1);
        try{action.run();}finally{int depth=BUILD_DEPTH.get()-1;if(depth==0)BUILD_DEPTH.remove();else BUILD_DEPTH.set(depth);}
    }
    @SubscribeEvent public static void breaking(BlockEvent.BreakEvent e){if(e.getLevel() instanceof Level l&&!allowLevel(l))e.setCanceled(true);}
    @SubscribeEvent public static void placing(BlockEvent.EntityPlaceEvent e){if(e.getLevel() instanceof Level l&&!allowLevel(l))e.setCanceled(true);}
    @SubscribeEvent public static void piston(PistonEvent.Pre e){if(e.getLevel() instanceof Level l&&!allowLevel(l))e.setCanceled(true);}
    @SubscribeEvent public static void explosion(ExplosionEvent.Detonate e){if(!allowLevel(e.getLevel()))e.getAffectedBlocks().clear();}
    @SubscribeEvent public static void destruction(LivingDestroyBlockEvent e){if(!allowLevel(e.getEntity().level()))e.setCanceled(true);}
    @SubscribeEvent public static void griefing(EntityMobGriefingEvent e){if(!allowLevel(e.getEntity().level()))e.setResult(Event.Result.DENY);}
}
