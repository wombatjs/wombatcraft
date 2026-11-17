package net.wombatcraft.smp;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.Protection;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Action-only checks against the pinned Rooms API; no ticking or chunk loads. */
public final class RoomsGuard {
    public static boolean interact(Level level, Player player, BlockPos pos) {
        return player.level()==level && level.mayInteract(player,pos) && UtilityGuard.edit(player,pos);
    }
    public static boolean hop(BlockPos pos,Level level,net.minecraft.world.entity.Entity entity) {
        if(!(entity instanceof ServerPlayer p))return !ArenaGuard.arena(level);
        // Interior returns must remain available. Local hops stay in the pilot's world.
        if(level!=p.level())return !ArenaGuard.arena(level);
        return UtilityGuard.travel(p)&&UtilityGuard.edit(p,pos)&&UtilityGuard.edit(p,pos.above());
    }
    private static boolean area(ServerPlayer p,Object raw) {
        var api=FTBChunksAPI.api();
        if(!api.isManagerLoaded() || ArenaGuard.arena(p.level()))return false;
        Map<?,?> map=(Map<?,?>)raw;
        int count=0;
        for(var entry:map.entrySet())for(Object v:(Set<?>)entry.getValue()) {
            if(++count>4096)return false;
            long key=(Long)v;int x=(int)(key>>32)*24-16,z=(int)key*24-16,y=(Integer)entry.getKey();
            // Include adjacent passage panels; claims are horizontal chunk columns.
            for(int cx=(x-1)>>4;cx<=(x+24)>>4;cx++)for(int cz=(z-1)>>4;cz<=(z+24)>>4;cz++) {
                BlockPos at=new BlockPos(cx*16+8,y,cz*16+8);
                if(!p.level().getWorldBorder().isWithinBounds(at) || api.getManager().shouldPreventInteraction(p,InteractionHand.MAIN_HAND,at,Protection.EDIT_BLOCK,null))return false;
            }
        }
        return true;
    }
    private static Object piece(ServerPlayer p,BlockPos core)throws ReflectiveOperationException {
        Class<?> reg=Class.forName("leonus.tardis_rooms.room.ModuleRegistry");
        Object r=reg.getMethod("get",ServerLevel.class).invoke(null,p.serverLevel());
        return reg.getMethod("pieceAtCore",BlockPos.class).invoke(r,core);
    }
    public static boolean remove(ServerPlayer p,BlockPos core) {
        if(!UtilityGuard.edit(p,core))return false;
        try {Object part=piece(p,core);return part!=null&&area(p,part.getClass().getField("footprint").get(part));}
        catch(ReflectiveOperationException|RuntimeException e){return false;}
    }
    public static boolean grow(ServerPlayer p,BlockPos pos,ResourceLocation id,BlockPos replace) {
        if(!UtilityGuard.edit(p,pos))return false;
        try {
            Class<?> node=Class.forName("leonus.tardis_rooms.room.Node");
            Optional<?> n=(Optional<?>)node.getMethod("locate",BlockPos.class).invoke(null,pos);
            if(n.isEmpty())return false;
            Class<?> catalog=Class.forName("leonus.tardis_rooms.room.PieceCatalog");
            Optional<?> plan=(Optional<?>)catalog.getMethod("plan",ResourceLocation.class,node).invoke(null,id,n.get());
            if(plan.isEmpty()||!area(p,plan.get().getClass().getMethod("footprint").invoke(plan.get())))return false;
            return replace==null||replace.equals(BlockPos.ZERO)||remove(p,replace);
        }catch(ReflectiveOperationException|RuntimeException e){return false;}
    }
}
