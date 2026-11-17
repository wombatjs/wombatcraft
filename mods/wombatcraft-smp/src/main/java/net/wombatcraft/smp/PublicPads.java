package net.wombatcraft.smp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import whocraft.tardis_refined.common.block.device.LandingPadBlock;
import whocraft.tardis_refined.common.blockentity.shell.ShellBaseBlockEntity;
import whocraft.tardis_refined.common.capability.tardis.TardisLevelOperator;
import whocraft.tardis_refined.common.tardis.TardisNavLocation;
import whocraft.tardis_refined.registry.TRUpgrades;
import java.util.*;

public final class PublicPads {
    public enum Status { AVAILABLE, INCOMING, OCCUPIED, BLOCKED, UNVERIFIED, MISSING, MOVING }
    static final Map<UUID,Long> notices=new HashMap<>();
    public static void register() {
        PadNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,PublicPads::placed);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,PublicPads::interact);
        MinecraftForge.EVENT_BUS.addListener(PublicPads::tick);
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.server.ServerStartedEvent e)->{
            if(Boolean.getBoolean("wombatcraft.padsQa"))try{PublicPadsAudit.run(e.getServer());}catch(Exception ex){WombatcraftSmp.LOGGER.error("PUBLIC_PADS_AUDIT_FAILED",ex);}
        });
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.server.ServerStoppedEvent e)->{notices.clear();PadNetwork.clear();});
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e)->PadNetwork.leave(e.getEntity().getUUID()));
    }
    static ServerLevel level(MinecraftServer server,String dimension) {
        ResourceLocation id=ResourceLocation.tryParse(dimension);
        return id==null?null:server.getLevel(ResourceKey.create(Registries.DIMENSION,id));
    }
    static boolean moving(ServerLevel level,BlockPos pos) {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("valkyrienskies"))return false;
        try {return (boolean)Class.forName("whocraft.tardis_refined.compat.valkyrienskies.VSHelper").getMethod("isBlockInShipyard",Level.class,BlockPos.class).invoke(null,level,pos);}
        catch(ReflectiveOperationException ex){return true;}
    }
    private static void placed(BlockEvent.EntityPlaceEvent e) {
        if(e.isCanceled()||!(e.getLevel() instanceof ServerLevel l)||!(e.getEntity() instanceof ServerPlayer p)||!(e.getPlacedBlock().getBlock() instanceof LandingPadBlock))return;
        try {PadRegistry.get(l.getServer()).add(p.getUUID(),p.getGameProfile().getName(),l.dimension().location().toString(),e.getPos());}
        catch(IllegalStateException ex){p.sendSystemMessage(Component.literal(ex.getMessage()));}
    }
    private static void interact(PlayerInteractEvent.RightClickBlock e) {
        if(e.isCanceled()||e.getHand()!=InteractionHand.MAIN_HAND||!e.getItemStack().isEmpty()||!(e.getLevel().getBlockState(e.getPos()).getBlock() instanceof LandingPadBlock))return;
        if(e.getEntity() instanceof ServerPlayer p) {
            if(!p.level().mayInteract(p,e.getPos())||!UtilityGuard.edit(p,e.getPos()))return;
            PadRegistry data=PadRegistry.get(p.server);PadRegistry.Pad pad=data.at(p.level().dimension().location().toString(),e.getPos());
            try {
                // Legacy pads can be adopted only where the player's claim permissions allow editing.
                if(pad==null)pad=data.add(p.getUUID(),p.getGameProfile().getName(),p.level().dimension().location().toString(),e.getPos());
                PadNetwork.openSettings(p,pad);
            }catch(IllegalStateException ex){p.sendSystemMessage(Component.literal(ex.getMessage()));}
        }
        e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);
    }
    /** Injected into the pad's block removal callback, including explosions, commands and pistons. */
    public static void removed(Level level,BlockPos pos,BlockState oldState,BlockState newState) {
        if(!(level instanceof ServerLevel l)||oldState.is(newState.getBlock()))return;
        PadRegistry data=PadRegistry.get(l.getServer());PadRegistry.Pad pad=data.at(l.dimension().location().toString(),pos);
        if(pad!=null)data.remove(pad);
    }
    static void canEdit(ServerPlayer p,PadRegistry.Pad pad) {
        if(pad==null)throw new IllegalStateException("This landing pad was removed. Reopen its settings.");
        if(!pad.dimension.equals(p.level().dimension().location().toString())||p.distanceToSqr(pad.pos.getX()+.5,pad.pos.getY()+.5,pad.pos.getZ()+.5)>64
            ||!(p.level().getBlockState(pad.pos).getBlock() instanceof LandingPadBlock))throw new SecurityException("Stand beside the landing pad to edit it.");
        if(!pad.owner.equals(p.getUUID())&&!p.hasPermissions(2))throw new SecurityException("Only the pad owner or an administrator can edit it.");
        if(!p.level().mayInteract(p,pad.pos)||!UtilityGuard.edit(p,pad.pos))throw new SecurityException("You cannot edit this claimed area.");
    }
    static Status status(MinecraftServer server,PadRegistry.Pad pad,String ship,boolean verify) {
        ServerLevel l=level(server,pad.dimension);if(l==null)return Status.UNVERIFIED;
        if(moving(l,pad.pos))return Status.MOVING;
        String reserved=PadRegistry.get(server).reservations.get(pad.id);
        if(!l.hasChunkAt(pad.pos)) {if(!verify)return reserved==null?Status.UNVERIFIED:Status.INCOMING;l.getChunkAt(pad.pos);}
        if(!(l.getBlockState(pad.pos).getBlock() instanceof LandingPadBlock))return Status.MISSING;
        for(int y=1;y<=2;y++)if(l.getBlockEntity(pad.pos.above(y)) instanceof ShellBaseBlockEntity)return Status.OCCUPIED;
        if(reserved!=null&&!reserved.equals(ship))return Status.INCOMING;
        BlockPos base=pad.pos.above();
        if(!l.getWorldBorder().isWithinBounds(base)||l.isOutsideBuildHeight(base.above()))return Status.BLOCKED;
        for(int y=1;y<=2;y++)if(!l.getBlockState(pad.pos.above(y)).isAir())return Status.BLOCKED;
        Direction facing=l.getBlockState(pad.pos).getValue(HorizontalDirectionalBlock.FACING);
        BlockPos exit=base.relative(facing);
        // Check the doorway as well as the two shell blocks; no permanent chunk tickets.
        if(!l.hasChunkAt(exit)) {if(!verify)return Status.UNVERIFIED;l.getChunkAt(exit);}
        if(!l.getBlockState(exit).getCollisionShape(l,exit).isEmpty()||!l.getBlockState(exit.above()).getCollisionShape(l,exit.above()).isEmpty()
            ||!l.getFluidState(exit).isEmpty()||!l.getFluidState(exit.above()).isEmpty())return Status.BLOCKED;
        // Refined Flight represents its exterior with the pilot entity. Counting players here
        // would make the pilot block their own landing; occupancy is determined by shell blocks.
        return reserved!=null?Status.INCOMING:Status.AVAILABLE;
    }
    static boolean usable(Status status,PadRegistry data,PadRegistry.Pad pad,String ship) {
        return status==Status.AVAILABLE||(status==Status.INCOMING&&ship.equals(data.reservations.get(pad.id)));
    }
    static TardisLevelOperator operator(ServerPlayer p) {
        if(!UtilityGuard.travel(p))throw new SecurityException("You cannot set a destination right now.");
        TardisLevelOperator op=TardisLevelOperator.get(p.serverLevel()).orElseThrow(()->new IllegalStateException("Open Public Landing Pads inside your TARDIS."));
        if(op.getPilotingManager().getCurrentConsole()==null||p.distanceToSqr(op.getPilotingManager().getCurrentConsole().getBlockPos().getX()+.5,op.getPilotingManager().getCurrentConsole().getBlockPos().getY()+.5,op.getPilotingManager().getCurrentConsole().getBlockPos().getZ()+.5)>256)
            throw new SecurityException("Stand near the TARDIS console to use its directory.");
        return op;
    }
    static void select(ServerPlayer p,UUID id) {
        TardisLevelOperator op=operator(p);var pilot=op.getPilotingManager();PadRegistry data=PadRegistry.get(p.server);PadRegistry.Pad pad=data.pads.get(id);
        if(pad==null||!pad.visible)throw new IllegalStateException("This pad is no longer public. Refresh the directory.");
        if(!TRUpgrades.LANDING_PAD.get().isUnlocked(op.getUpgradeHandler()))throw new IllegalStateException("Unlock the Landing Pad upgrade first.");
        ServerLevel target=level(p.server,pad.dimension);
        if(target==null||!whocraft.tardis_refined.common.util.DimensionUtil.isAllowedDimension(target.dimension())||!op.getProgressionManager().isLevelDiscovered(target.dimension()))throw new IllegalStateException("Your TARDIS has not unlocked this dimension.");
        if(pilot.isLanding()||pilot.isTakingOff()||pilot.isCrashing()||!pilot.canUseControls())throw new IllegalStateException("Wait until the TARDIS controls are available.");
        reconcile(p.server);String ship=op.getLevelKey().location().toString();Status state=status(p.server,pad,ship,true);
        if(!usable(state,data,pad,ship))throw new IllegalStateException("Landing pad is "+label(state).toLowerCase(Locale.ROOT)+". Choose another pad.");
        if(pilot.isInFlight()&&!data.reserve(pad.id,ship))throw new IllegalStateException("Another TARDIS reserved this pad.");
        if(!pilot.isInFlight())data.release(ship);
        var destination=new TardisNavLocation(pad.pos.above(),target.getBlockState(pad.pos).getValue(HorizontalDirectionalBlock.FACING),target);destination.setName(pad.name);
        pilot.setTargetLocation(destination);pilot.recalculateFlightDistance();
        data.selections.put(ship,new PadRegistry.Selection(pad.id,pad.dimension,pad.pos));data.setDirty();op.tardisClientData().sync();
    }
    public static String label(Status state) {return switch(state){case AVAILABLE->"Available";case INCOMING->"Incoming";case OCCUPIED->"Occupied";case BLOCKED->"Blocked";case UNVERIFIED->"Unverified";case MISSING->"Removed";case MOVING->"Moving pad unsupported";};}
    static boolean matches(PadRegistry.Selection s,TardisNavLocation loc) {return loc!=null&&s.dimension().equals(loc.getDimensionKey().location().toString())&&s.pos().above().equals(loc.getPosition());}
    static PadRegistry.Pad target(PadRegistry data,TardisNavLocation loc) {return loc==null?null:data.at(loc.getDimensionKey().location().toString(),loc.getPosition().below());}
    /** Runs synchronously on the server thread before takeoff or materialization. */
    public static boolean flight(TardisLevelOperator op,boolean arrival) {
        if(!(op.getLevel() instanceof ServerLevel l))return true;
        var pilot=op.getPilotingManager();
        if(!arrival&&(!pilot.canBeginFlight()||pilot.getFuel()<50))return true;
        PadRegistry data=PadRegistry.get(l.getServer());String ship=op.getLevelKey().location().toString();var loc=pilot.getTargetLocation();
        reconcile(l.getServer());PadRegistry.Selection selected=data.selections.get(ship);PadRegistry.Pad pad=target(data,loc);
        if(selected!=null&&matches(selected,loc)) {
            pad=data.pads.get(selected.pad());
            if(pad==null||(!arrival&&!pad.visible)){notice(op,"The selected public pad was removed or hidden. Choose another destination.");return false;}
        }
        if(pad==null)return true;
        // Taking off while still targeting the current location is normal Refined behavior.
        ServerLevel destination=level(l.getServer(),pad.dimension);
        if(!arrival&&destination!=null&&destination.getBlockEntity(pad.pos.above()) instanceof ShellBaseBlockEntity shell&&op.getLevelKey().equals(shell.getTardisId()))return data.reserve(pad.id,ship);
        Status state=status(l.getServer(),pad,ship,true);
        if(!usable(state,data,pad,ship)||!data.reserve(pad.id,ship)) {
            notice(op,"Landing pad is "+label(state).toLowerCase(Locale.ROOT)+". Choose another destination.");return false;
        }
        return true;
    }
    /** Prevent Refined's nearby-position fallback from silently missing a selected pad. */
    public static Optional<TardisNavLocation> exactLanding(TardisLevelOperator op,TardisNavLocation location) {
        if(!(op.getLevel() instanceof ServerLevel l))return null;
        PadRegistry data=PadRegistry.get(l.getServer());PadRegistry.Pad pad=target(data,location);if(pad==null)return null;
        String ship=op.getLevelKey().location().toString();
        if(!usable(status(l.getServer(),pad,ship,true),data,pad,ship))return Optional.empty();
        return Optional.of(location.copy().setDirection(level(l.getServer(),pad.dimension).getBlockState(pad.pos).getValue(HorizontalDirectionalBlock.FACING)));
    }
    public static boolean placement(TardisLevelOperator op,TardisNavLocation loc) {
        if(!(op.getLevel() instanceof ServerLevel l))return true;
        PadRegistry data=PadRegistry.get(l.getServer());PadRegistry.Pad pad=target(data,loc);if(pad==null)return true;
        ServerLevel destination=level(l.getServer(),pad.dimension);
        // Shell/theme refresh is allowed only for the ship already occupying this exact pad.
        if(destination!=null&&destination.getBlockEntity(loc.getPosition()) instanceof ShellBaseBlockEntity shell&&op.getLevelKey().equals(shell.getTardisId()))return true;
        if(!usable(status(l.getServer(),pad,op.getLevelKey().location().toString(),true),data,pad,op.getLevelKey().location().toString()))return false;
        loc.setDirection(destination.getBlockState(pad.pos).getValue(HorizontalDirectionalBlock.FACING));
        return true;
    }
    public static void requirePlacement(TardisLevelOperator op,TardisNavLocation loc) {
        if(!placement(op,loc))throw new IllegalStateException("Landing pad is unavailable; exterior placement refused.");
    }
    public static boolean freeFlight(ServerPlayer player,boolean transition) {
        if(transition)return true;
        try {
            Class<?> tracker=Class.forName("dev.jeryn.mc.rwf.common.entity.FlightTracker");
            Object dimension=tracker.getMethod("getTardisDimensionFor",UUID.class).invoke(null,player.getUUID());if(dimension==null)return true;
            @SuppressWarnings("unchecked") ServerLevel interior=player.server.getLevel((ResourceKey<Level>)dimension);if(interior==null)return true;
            var op=TardisLevelOperator.get(interior);if(op.isEmpty())return true;
            var find=tracker.getDeclaredMethod("findGroundBelow",ServerLevel.class,BlockPos.class);find.setAccessible(true);
            BlockPos pos=(BlockPos)find.invoke(null,player.serverLevel(),player.blockPosition());
            if(placement(op.get(),new TardisNavLocation(pos,player.getDirection(),player.serverLevel())))return true;
            player.displayClientMessage(Component.literal("Landing pad unavailable. Move to clear ground before exiting flight."),true);return false;
        }catch(ReflectiveOperationException ex){WombatcraftSmp.LOGGER.error("Cannot validate exterior flight landing",ex);return false;}
    }
    private static void notice(TardisLevelOperator op,String message) {
        for(var p:op.getLevel().players()) {
            long now=op.getLevel().getGameTime();if(now-notices.getOrDefault(p.getUUID(),-100L)<60)continue;
            notices.put(p.getUUID(),now);p.displayClientMessage(Component.literal(message),true);
        }
    }
    static void reconcile(MinecraftServer server) {
        PadRegistry data=PadRegistry.get(server);
        boolean changed=data.reservations.entrySet().removeIf(e->{
            ServerLevel interior=level(server,e.getValue());if(interior==null)return true;
            var op=TardisLevelOperator.get(interior);PadRegistry.Pad pad=data.pads.get(e.getKey());
            return pad==null||op.isEmpty()||!op.get().getPilotingManager().isInFlight()||target(data,op.get().getPilotingManager().getTargetLocation())!=pad;
        });
        changed|=data.selections.entrySet().removeIf(e->{ServerLevel interior=level(server,e.getKey());if(interior==null)return false;var op=TardisLevelOperator.get(interior);return op.isPresent()&&!matches(e.getValue(),op.get().getPilotingManager().getTargetLocation());});
        if(changed)data.setDirty();
    }
    private static void tick(TickEvent.ServerTickEvent e) {if(e.phase==TickEvent.Phase.END&&e.getServer().getTickCount()%20==0)reconcile(e.getServer());}
}
