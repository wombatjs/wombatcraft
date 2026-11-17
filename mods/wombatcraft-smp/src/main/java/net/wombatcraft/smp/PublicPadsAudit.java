package net.wombatcraft.smp;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.block.Blocks;
import whocraft.tardis_refined.common.capability.tardis.TardisLevelOperator;
import whocraft.tardis_refined.common.tardis.TardisNavLocation;
import whocraft.tardis_refined.registry.TRBlockRegistry;
import java.util.*;

/** Opt-in disposable-world regression tests using real blocks, capabilities and transformed methods. */
final class PublicPadsAudit {
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private static void denied(Runnable action,String message){try{action.run();}catch(SecurityException|IllegalArgumentException|IllegalStateException expected){return;}throw new IllegalStateException(message);}
    static void run(MinecraftServer server) throws Exception {
        if(!Boolean.getBoolean("wombatcraft.padsQa"))throw new IllegalStateException("Disposable QA only");
        var level=server.overworld();var registry=PadRegistry.get(server);String dimension=level.dimension().location().toString();
        BlockPos pos=new BlockPos(120,300,120);UUID owner=UUID.fromString("10a66b72-e1bb-48a2-b98e-22ae918ab1bc");
        var saved=registry.at(dimension,pos);boolean restored=saved!=null;
        if(restored)require(saved.owner.equals(owner)&&saved.name.equals("QA restart pad")&&!saved.visible,"Pad settings lost across restart");
        var before=new HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(BlockPos point:BlockPos.betweenClosed(pos.offset(-1,0,-1),pos.offset(1,3,1)))before.put(point.immutable(),level.getBlockState(point));
        if(saved!=null)registry.remove(saved);
        try {
            for(BlockPos point:before.keySet())level.setBlockAndUpdate(point,Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos,TRBlockRegistry.LANDING_PAD.get().defaultBlockState());
            var pad=registry.add(owner,"PadQA",dimension,pos);require(!pad.visible,"New pad was public");
            require(PublicPads.status(server,pad,"",false)==PublicPads.Status.AVAILABLE,"Clear pad unavailable");
            pad.name=PadRegistry.validName("  Spawn station  ");pad.visible=true;pad.revision=7;registry.setDirty();
            require(registry.reserve(pad.id,"test:ship_a"),"First reservation failed");
            require(!registry.reserve(pad.id,"test:ship_b"),"Double reservation accepted");
            require(PublicPads.status(server,pad,"test:ship_b",false)==PublicPads.Status.INCOMING,"Incoming status missing");
            var roundTrip=PadRegistry.load(registry.save(new CompoundTag()));var copy=roundTrip.pads.get(pad.id);
            require(copy.owner.equals(owner)&&copy.name.equals("Spawn station")&&copy.visible&&copy.revision==7&&"test:ship_a".equals(roundTrip.reservations.get(pad.id)),"Serialization lost pad state");
            registry.release("test:ship_a");require(PublicPads.status(server,pad,"",false)==PublicPads.Status.AVAILABLE,"Reservation release failed");
            level.setBlockAndUpdate(pos.above(2),Blocks.STONE.defaultBlockState());
            require(PublicPads.status(server,pad,"",false)==PublicPads.Status.BLOCKED,"Headroom obstruction missed");
            var op=new TardisLevelOperator(level);var loc=new TardisNavLocation(pos.above(),Direction.NORTH,level);
            op.getPilotingManager().setTargetLocation(loc);op.getPilotingManager().setCurrentLocation(loc);
            require(op.getPilotingManager().findClosestValidPosition(loc,1).isEmpty(),"Blocked pad relocated through nearby fallback");
            require(!op.getPilotingManager().endFlight(true,false),"Forced arrival bypassed pad obstruction");
            denied(()->op.setOrUpdateExteriorBlock(loc,Optional.empty()),"Final placement guard missing");
            level.setBlockAndUpdate(pos.above(2),Blocks.AIR.defaultBlockState());
            require(op.getPilotingManager().findClosestValidPosition(loc,1).orElseThrow().getPosition().equals(pos.above()),"Pad arrival did not retain exact position");
            require(op.getPilotingManager().findClosestValidPosition(loc.copy().setDirection(Direction.SOUTH),1).orElseThrow().getDirection()==Direction.NORTH,"Pad arrival did not use the checked doorway direction");
            level.setBlockAndUpdate(pos.above(),TRBlockRegistry.GLOBAL_SHELL_BLOCK.get().defaultBlockState());
            require(PublicPads.status(server,pad,"",false)==PublicPads.Status.OCCUPIED,"Exterior occupancy missed");
            level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
            require(PublicPads.status(server,pad,"",false)==PublicPads.Status.AVAILABLE,"Departure did not free pad");
            level.setBlockAndUpdate(pos.above().north(),Blocks.STONE.defaultBlockState());
            require(PublicPads.status(server,pad,"",false)==PublicPads.Status.BLOCKED,"Doorway obstruction missed");
            level.setBlockAndUpdate(pos.above().north(),Blocks.AIR.defaultBlockState());
            var stranger=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"PadStranger"));stranger.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),stranger);stranger.setPos(120.5,301,120.5);
            denied(()->PublicPads.canEdit(stranger,pad),"Stranger edited pad");
            denied(()->PublicPads.operator(stranger),"Outside player used console directory");
            denied(()->PadRegistry.validName(" "),"Empty name allowed");denied(()->PadRegistry.validName("x".repeat(41)),"Oversized name allowed");denied(()->PadRegistry.validName("a\u00a7cb"),"Formatting code allowed");
            registry.reserve(pad.id,"test:missing_ship");PublicPads.reconcile(server);require(!registry.reservations.containsKey(pad.id),"Stale reservation retained");
            // Two real Refined capabilities exercise the synchronous takeoff/landing hooks together.
            var aLevel=whocraft.tardis_refined.common.dimension.DimensionHandler.getOrCreateInterior(level,new net.minecraft.resources.ResourceLocation("tardis_refined","10a66b72-e1bb-48a2-b98e-22ae918ab1b1"));
            var bLevel=whocraft.tardis_refined.common.dimension.DimensionHandler.getOrCreateInterior(level,new net.minecraft.resources.ResourceLocation("tardis_refined","10a66b72-e1bb-48a2-b98e-22ae918ab1b2"));
            var a=TardisLevelOperator.get(aLevel).orElseThrow();var b=TardisLevelOperator.get(bLevel).orElseThrow();
            var aPilot=a.getPilotingManager();var bPilot=b.getPilotingManager();
            CompoundTag aBefore=aPilot.saveData(new CompoundTag()),bBefore=bPilot.saveData(new CompoundTag());
            try {
                for(var pilot:List.of(aPilot,bPilot)){pilot.setFuel(500);pilot.setHandbrakeOn(false);pilot.setCurrentLocation(new TardisNavLocation(pos.offset(10,1,0),Direction.NORTH,level));pilot.setTargetLocation(loc.copy());}
                require(aPilot.beginFlight(true),"Real ship A could not take off");
                require(!bPilot.beginFlight(true),"Real ship B stole the incoming pad");
                require(PublicPads.status(server,pad,"",false)==PublicPads.Status.INCOMING,"Real takeoff did not reserve pad");
                require(aPilot.endFlight(true,false),"Real ship A could not materialize on its reservation");
                aPilot.onFlightEnd();PublicPads.reconcile(server);
                require(PublicPads.status(server,pad,"",false)==PublicPads.Status.OCCUPIED,"Actual materialization did not occupy pad");
                require(!bPilot.beginFlight(true),"Real ship B overwrote occupied pad");
                require(aPilot.beginFlight(true),"A ship cannot depart its own pad");
                a.getExteriorManager().removeExteriorBlock();aPilot.setTargetLocation(new TardisNavLocation(pos.offset(10,1,0),Direction.NORTH,level));PublicPads.reconcile(server);
                require(!registry.reservations.containsKey(pad.id),"Retargeting did not release reservation");
                require(PublicPads.status(server,pad,"",false)==PublicPads.Status.AVAILABLE,"Actual departure did not free pad");
            }finally{aPilot.loadData(aBefore);bPilot.loadData(bBefore);registry.release(a.getLevelKey().location().toString());registry.release(b.getLevelKey().location().toString());}
            UUID old=pad.id;level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());require(!registry.pads.containsKey(old),"Block removal did not unregister pad");
            level.setBlockAndUpdate(pos,TRBlockRegistry.LANDING_PAD.get().defaultBlockState());
            var replacement=registry.add(owner,"PadQA",dimension,pos);require(!replacement.id.equals(old),"Replacement inherited old identity");
            registry.remove(replacement);
        }finally{for(var e:before.entrySet())level.setBlockAndUpdate(e.getKey(),e.getValue());}
        // Persist a metadata fixture without altering terrain, checked again on the second boot.
        var fixture=registry.add(owner,"PadQA",dimension,pos);fixture.name="QA restart pad";fixture.visible=false;registry.setDirty();
        System.out.println("PUBLIC_PADS_AUDIT_COMPLETE restored="+restored+" reservations=true occupancy=true clearance=true permissions=true persistence=true removal=true exact_arrival=true");
    }
}
