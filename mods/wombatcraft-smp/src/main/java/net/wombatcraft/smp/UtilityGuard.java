package net.wombatcraft.smp;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.Protection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Narrow guards at utility actions. No world scans, chunk loads or per-player ticking. */
@Mod.EventBusSubscriber(modid=WombatcraftSmp.ID)
public final class UtilityGuard {
    private static final Map<UUID,Long> COMBAT=new HashMap<>();
    public static boolean permitted(Player player,BlockPos pos,Protection protection){
        if(!(player instanceof ServerPlayer p)||pos==null)return false;
        Level level=p.level();
        if(ArenaGuard.arena(level)||!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos))return false;
        var api=FTBChunksAPI.api();
        return api.isManagerLoaded()&&!api.getManager().shouldPreventInteraction(p,InteractionHand.MAIN_HAND,pos,protection,null);
    }
    public static boolean edit(Player player,BlockPos pos){return permitted(player,pos,Protection.EDIT_BLOCK);}
    public static boolean setMiningBlock(Level level,BlockPos pos,BlockState state,LivingEntity actor){
        return actor instanceof Player p&&p.level()==level&&edit(p,pos)&&level.setBlockAndUpdate(pos,state);
    }
    public static BlockPos gravePosition(BlockPos pos,ServerPlayer player){
        // The upstream handler leaves original drops untouched when placement returns null.
        return pos!=null&&edit(player,pos)&&edit(player,pos.below())?pos:null;
    }
    public static boolean allowGrave(ServerPlayer player){return !ArenaGuard.arena(player.level());}
    public static boolean elevator(ServerPlayer player,BlockPos from,BlockPos to){
        return player!=null&&!combat(player)&&from!=null&&to!=null&&Math.abs((long)to.getY()-from.getY())<=64
            &&permitted(player,from,Protection.INTERACT_BLOCK)&&permitted(player,to,Protection.INTERACT_BLOCK);
    }
    private static boolean combat(ServerPlayer p){return COMBAT.getOrDefault(p.getUUID(),0L)>p.server.getTickCount();}
    public static boolean market(ServerPlayer p){return SmpNetwork.atSpawn(p)&&!ArenaGuard.arena(p.level())&&!combat(p);}
    @SubscribeEvent public static void hit(LivingHurtEvent e){
        if(!e.isCanceled()&&e.getAmount()>0&&e.getEntity() instanceof ServerPlayer victim&&e.getSource().getEntity() instanceof ServerPlayer attacker&&victim!=attacker){
            long until=victim.server.getTickCount()+1800L;COMBAT.put(victim.getUUID(),until);COMBAT.put(attacker.getUUID(),until);
            for(ServerPlayer p:new ServerPlayer[]{victim,attacker})if(p.containerMenu.getClass().getName().startsWith("codechicken.enderstorage."))p.closeContainer();
        }
    }
    @SubscribeEvent public static void cleanup(TickEvent.ServerTickEvent e){
        if(e.phase==TickEvent.Phase.END&&e.getServer().getTickCount()%1200==0)COMBAT.values().removeIf(t->t<=e.getServer().getTickCount());
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){COMBAT.clear();}
    @SubscribeEvent public static void use(PlayerInteractEvent.RightClickItem e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        var id=ForgeRegistries.ITEMS.getKey(e.getItemStack().getItem());
        if(id!=null&&id.toString().equals("enderstorage:ender_pouch")&&(ArenaGuard.arena(p.level())||combat(p))){
            e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("Remote storage is locked in the arena and for 90 seconds after PvP damage."),true);
        }
    }
    @SubscribeEvent public static void torch(BlockEvent.EntityPlaceEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        var id=ForgeRegistries.BLOCKS.getKey(e.getPlacedBlock().getBlock());
        if(id==null||!id.getNamespace().equals("torchmaster"))return;
        if(id.getPath().equals("feral_flare_lantern")){e.setCanceled(true);return;}
        if(!id.getPath().equals("megatorch")&&!id.getPath().equals("dreadlamp"))return;
        // These affect a 32-block radius. Do not let a neighbour suppress another claim's mobs.
        for(int x=(e.getPos().getX()-32)>>4;x<=(e.getPos().getX()+32)>>4;x++)
            for(int z=(e.getPos().getZ()-32)>>4;z<=(e.getPos().getZ()+32)>>4;z++){
                var api=FTBChunksAPI.api();
                if(!api.isManagerLoaded()||api.getManager().shouldPreventInteraction(p,InteractionHand.MAIN_HAND,new BlockPos(x*16,e.getPos().getY(),z*16),Protection.EDIT_BLOCK,null)){
                    e.setCanceled(true);p.displayClientMessage(net.minecraft.network.chat.Component.literal("Move this lamp farther from land you cannot edit."),true);return;
                }
            }
        if(ArenaGuard.arena(p.level()))e.setCanceled(true);
    }
}
