package net.wombatcraft.smp;

import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.*;
import java.nio.file.Files;
import java.util.*;

/** A world-scoped arrival point, independent of beds and the normal respawn point. */
final class TutorialSpawn {
    private record Waiting(ResourceKey<Level> dimension, Vec3 start, long due) {}
    private static final Map<UUID,Waiting> waiting=new HashMap<>();
    private static final Map<UUID,Long> combat=new HashMap<>();
    static final class State extends SavedData {
        final Set<UUID> seen=new HashSet<>();
        String dimension=""; double x,y,z; float yaw,pitch;
        static State load(CompoundTag t){
            State s=new State();s.dimension=t.getString("dimension");s.x=t.getDouble("x");s.y=t.getDouble("y");s.z=t.getDouble("z");s.yaw=t.getFloat("yaw");s.pitch=t.getFloat("pitch");
            for(Tag v:t.getList("seen",Tag.TAG_INT_ARRAY))s.seen.add(NbtUtils.loadUUID(v));return s;
        }
        public CompoundTag save(CompoundTag t){
            t.putString("dimension",dimension);t.putDouble("x",x);t.putDouble("y",y);t.putDouble("z",z);t.putFloat("yaw",yaw);t.putFloat("pitch",pitch);
            ListTag list=new ListTag();seen.forEach(id->list.add(NbtUtils.createUUID(id)));t.put("seen",list);return t;
        }
    }
    private static State state(MinecraftServer s){return s.overworld().getDataStorage().computeIfAbsent(State::load,State::new,"wombatcraft_tutorial");}
    static void register(){
        MinecraftForge.EVENT_BUS.addListener(TutorialSpawn::commands);
        MinecraftForge.EVENT_BUS.addListener(TutorialSpawn::started);
        MinecraftForge.EVENT_BUS.addListener(TutorialSpawn::joined);
        MinecraftForge.EVENT_BUS.addListener(TutorialSpawn::hurt);
        MinecraftForge.EVENT_BUS.addListener(TutorialSpawn::tick);
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e)->waiting.remove(e.getEntity().getUUID()));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent e)->{waiting.clear();combat.clear();});
    }
    private static void started(ServerStartedEvent e){
        // Seed historical accounts before anybody logs in. Installing this feature must not relocate returning players.
        State s=state(e.getServer());
        var folder=e.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR);
        if(Files.isDirectory(folder))try(var files=Files.list(folder)){
            files.filter(p->p.getFileName().toString().endsWith(".dat")).forEach(p->{
                String name=p.getFileName().toString();try{s.seen.add(UUID.fromString(name.substring(0,name.length()-4)));}catch(IllegalArgumentException ignored){}
            });s.setDirty();
        }catch(java.io.IOException ex){throw new IllegalStateException("Cannot identify existing tutorial players; refusing to relocate them",ex);}
    }
    private static void joined(PlayerEvent.PlayerLoggedInEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        State s=state(p.server);if(!s.seen.add(p.getUUID()))return;s.setDirty();
        if(!s.dimension.isEmpty())travel(p,s);
    }
    private static void commands(RegisterCommandsEvent e){
        if(Boolean.getBoolean("wombatcraft.qa"))e.getDispatcher().register(Commands.literal("wctutorialaudit").requires(s->s.hasPermission(4)).executes(c->audit(c.getSource().getServer())));
        e.getDispatcher().register(Commands.literal("settutorialspawn").requires(s->s.hasPermission(2)).executes(c->{
            ServerPlayer p=c.getSource().getPlayerOrException();
            if(!p.onGround()||p.isInWaterOrBubble()||p.isInLava()||ArenaGuard.arena(p.level())||!p.serverLevel().getWorldBorder().isWithinBounds(p.blockPosition())){
                c.getSource().sendFailure(Component.literal("Stand on safe solid ground outside the arena to set the tutorial arrival."));return 0;
            }
            State s=state(p.server);s.dimension=p.level().dimension().location().toString();s.x=p.getX();s.y=p.getY();s.z=p.getZ();s.yaw=p.getYRot();s.pitch=p.getXRot();s.setDirty();
            p.server.overworld().getDataStorage().save();
            c.getSource().sendSuccess(()->Component.literal("Tutorial arrival saved here. New players arrive once; /tutorial returns here without changing beds or respawns."),false);return 1;
        }));
        e.getDispatcher().register(Commands.literal("tutorial").executes(c->{
            ServerPlayer p=c.getSource().getPlayerOrException();
            if(state(p.server).dimension.isEmpty()){c.getSource().sendFailure(Component.literal("The tutorial entrance has not been set yet."));return 0;}
            if(!p.isAlive()||p.isPassenger()||p.isSleeping()||ArenaGuard.arena(p.level())||combat.getOrDefault(p.getUUID(),0L)>p.server.getTickCount()){
                c.getSource().sendFailure(Component.literal("You cannot travel to the tutorial while in combat, riding, sleeping or inside the arena."));return 0;
            }
            waiting.put(p.getUUID(),new Waiting(p.level().dimension(),p.position(),p.server.getTickCount()+100L));
            p.sendSystemMessage(Component.literal("Returning to the tutorial in 5 seconds. Stay still and avoid damage."));return 1;
        }));
    }
    private static void hurt(LivingHurtEvent e){
        if(e.getEntity() instanceof ServerPlayer p){
            cancel(p);if(e.getSource().getEntity() instanceof Player)combat.put(p.getUUID(),p.server.getTickCount()+1200L);
        }
        if(e.getSource().getEntity() instanceof ServerPlayer attacker){
            cancel(attacker);if(e.getEntity() instanceof Player)combat.put(attacker.getUUID(),attacker.server.getTickCount()+1200L);
        }
    }
    private static void cancel(ServerPlayer p){if(waiting.remove(p.getUUID())!=null)p.sendSystemMessage(Component.literal("Tutorial travel cancelled."));}
    private static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        long now=e.getServer().getTickCount();
        var iterator=waiting.entrySet().iterator();
        while(iterator.hasNext()){
            var entry=iterator.next();ServerPlayer p=e.getServer().getPlayerList().getPlayer(entry.getKey());var w=entry.getValue();
            if(p==null){iterator.remove();continue;}
            if(!p.isAlive()||p.isPassenger()||!p.level().dimension().equals(w.dimension)||p.position().distanceToSqr(w.start)>0.01){iterator.remove();p.sendSystemMessage(Component.literal("Tutorial travel cancelled."));continue;}
            if(now>=w.due){iterator.remove();travel(p,state(e.getServer()));}
        }
        if(now%1200==0)combat.values().removeIf(until->until<=now);
    }
    private static boolean travel(ServerPlayer p,State s){
        var id=ResourceLocation.tryParse(s.dimension);var level=id==null?null:p.server.getLevel(ResourceKey.create(Registries.DIMENSION,id));
        if(level==null){p.sendSystemMessage(Component.literal("The tutorial destination is unavailable. Ask an administrator."));return false;}
        BlockPos pos=BlockPos.containing(s.x,s.y,s.z);level.getChunkAt(pos);
        var box=p.getBoundingBox().move(s.x-p.getX(),s.y-p.getY(),s.z-p.getZ());
        if(!level.getWorldBorder().isWithinBounds(pos)||!level.noCollision(p,box)||level.getBlockState(pos.below()).getCollisionShape(level,pos.below()).isEmpty()||!level.getFluidState(pos).isEmpty()){
            p.sendSystemMessage(Component.literal("The tutorial landing is obstructed or unsafe. Ask an administrator to reset it."));return false;
        }
        p.teleportTo(level,s.x,s.y,s.z,s.yaw,s.pitch);p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);return true;
    }
    private static int audit(MinecraftServer server){
        State s=state(server);State original=State.load(s.save(new CompoundTag()));
        var level=server.overworld();BlockPos floor=new BlockPos(0,250,0);
        var oldFloor=level.getBlockState(floor);var oldFeet=level.getBlockState(floor.above());var oldHead=level.getBlockState(floor.above(2));
        try{
            level.setBlockAndUpdate(floor,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(floor.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(floor.above(2),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            s.dimension="minecraft:overworld";s.x=0.5;s.y=251;s.z=0.5;s.yaw=45;s.pitch=0;
            var newcomer=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"TutorialNewQA"));
            newcomer.setPos(5,251,5);joined(new PlayerEvent.PlayerLoggedInEvent(newcomer));
            if(newcomer.position().distanceToSqr(new Vec3(s.x,s.y,s.z))>0.001)throw new IllegalStateException("New player did not arrive at tutorial");
            newcomer.setPos(5,251,5);joined(new PlayerEvent.PlayerLoggedInEvent(newcomer));
            if(newcomer.getX()!=5)throw new IllegalStateException("Returning player was relocated");
            State restored=State.load(s.save(new CompoundTag()));
            if(!restored.seen.contains(newcomer.getUUID())||restored.x!=0.5||!restored.dimension.equals("minecraft:overworld"))throw new IllegalStateException("Tutorial persistence failed");
            level.setBlockAndUpdate(floor.above(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            if(travel(newcomer,s)||newcomer.getX()!=5)throw new IllegalStateException("Unsafe landing was accepted");
            WombatcraftSmp.LOGGER.info("TUTORIAL_AUDIT_PASS: first arrival, returning account, persistence and obstructed landing");return 1;
        }finally{
            level.setBlockAndUpdate(floor,oldFloor);level.setBlockAndUpdate(floor.above(),oldFeet);level.setBlockAndUpdate(floor.above(2),oldHead);
            s.seen.clear();s.seen.addAll(original.seen);s.dimension=original.dimension;s.x=original.x;s.y=original.y;s.z=original.z;s.yaw=original.yaw;s.pitch=original.pitch;s.setDirty();
        }
    }
}
