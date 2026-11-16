package net.wombatcraft.smp;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import java.lang.ref.WeakReference;
import java.util.*;

/** Only tagged civic residents are tracked; no world/entity scans. */
final class CivicResidents {
    private static final String MAYOR="wombatcraft_mayor",SECRETARY="wombatcraft_mayors_secretary";
    private static final Map<UUID,WeakReference<Villager>> residents=new HashMap<>();
    static void register(){
        MinecraftForge.EVENT_BUS.addListener(CivicResidents::commands);
        MinecraftForge.EVENT_BUS.addListener(CivicResidents::joined);
        MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.HIGHEST,true,CivicResidents::interact);
        MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.HIGHEST,true,CivicResidents::interactAt);
        MinecraftForge.EVENT_BUS.addListener(CivicResidents::tick);
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent e)->residents.clear());
    }
    private static void commands(RegisterCommandsEvent e){
        var root=Commands.literal("wccivic").requires(s->WombatcraftSmp.ENABLED.get());
        var place=Commands.literal("place").requires(s->s.hasPermission(2));
        for(String role:List.of("mayor","secretary"))place.then(Commands.literal(role).executes(c->{
            ServerPlayer p=c.getSource().getPlayerOrException();
            if(!SmpNetwork.atSpawn(p)){c.getSource().sendFailure(Component.literal("Place civic residents inside the spawn claim."));return 0;}
            Villager v=EntityType.VILLAGER.create(p.serverLevel());if(v==null)return 0;
            v.moveTo(p.getX(),p.getY(),p.getZ(),p.getYRot()+180,0);v.setNoAi(true);v.setInvulnerable(true);v.setPersistenceRequired();v.setSilent(true);v.setCustomNameVisible(true);
            v.addTag("wombatcraft_spawn_npc");v.addTag(role.equals("mayor")?MAYOR:SECRETARY);
            v.setVillagerData(v.getVillagerData().setProfession(role.equals("mayor")?VillagerProfession.CARTOGRAPHER:VillagerProfession.LIBRARIAN).setLevel(5));
            update(v);p.serverLevel().addFreshEntity(v);
            c.getSource().sendSuccess(()->Component.literal("Placed the "+role+". The mayor follows election results automatically."),false);return 1;
        }));
        root.then(place);
        root.then(Commands.literal("remove").requires(s->s.hasPermission(2)).executes(c->{
            var p=c.getSource().getPlayerOrException();
            var nearest=p.serverLevel().getEntitiesOfClass(Villager.class,p.getBoundingBox().inflate(4),CivicResidents::isResident).stream().min(Comparator.comparingDouble(p::distanceToSqr));
            if(nearest.isEmpty()){c.getSource().sendFailure(Component.literal("No civic resident within four blocks."));return 0;}
            nearest.get().discard();return 1;
        }));
        e.getDispatcher().register(root);
    }
    private static boolean isResident(Villager v){return v.getTags().contains(MAYOR)||v.getTags().contains(SECRETARY);}
    private static void joined(EntityJoinLevelEvent e){
        if(!e.getLevel().isClientSide()&&e.getEntity() instanceof Villager v&&isResident(v)){residents.put(v.getUUID(),new WeakReference<>(v));update(v);}
    }
    private static void update(Villager v){
        if(v.getServer()==null)return;
        if(v.getTags().contains(SECRETARY))v.setCustomName(Component.literal("Elowen | Mayor's Secretary").withStyle(ChatFormatting.LIGHT_PURPLE));
        else {
            var civic=CivicState.get(v.getServer());civic.advance();var profile=CandidateCatalog.ALL.get(civic.election.mayor());
            v.setCustomName(Component.literal(profile.get("name").getAsString()+" | Town Mayor").withStyle(ChatFormatting.GOLD));
        }
    }
    private static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!WombatcraftSmp.ENABLED.get()||e.getServer().getTickCount()%200!=0)return;
        residents.entrySet().removeIf(entry->{Villager v=entry.getValue().get();if(v==null||v.isRemoved())return true;update(v);return false;});
    }
    private static void interactAt(PlayerInteractEvent.EntityInteractSpecific e){
        if(!(e.getTarget() instanceof Villager v)||!isResident(v)||!(e.getEntity() instanceof ServerPlayer))return;
        if(e.getEntity() instanceof ServerPlayer p)open(p,v,e.getHand());
        e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
    }
    private static void interact(PlayerInteractEvent.EntityInteract e){
        if(!(e.getTarget() instanceof Villager v)||!isResident(v)||!(e.getEntity() instanceof ServerPlayer))return;
        if(e.getEntity() instanceof ServerPlayer p)open(p,v,e.getHand());
        e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
    }
    private static void open(ServerPlayer p,Villager v,InteractionHand hand){
        if(hand!=InteractionHand.MAIN_HAND||!WombatcraftSmp.ENABLED.get())return;
        if(!SmpNetwork.atSpawn(p)){p.sendSystemMessage(Component.literal("Visit the civic office inside spawn."));return;}
        update(v);
        if(v.getTags().contains(MAYOR)){
            var civic=CivicState.get(p.server);var profile=CandidateCatalog.ALL.get(civic.election.mayor());
            p.sendSystemMessage(Component.literal(profile.get("policy").getAsString()+": "+profile.get("description").getAsString()).withStyle(ChatFormatting.GOLD));
        }
        SmpNetwork.view(p,0,"",0,3);
    }
}
