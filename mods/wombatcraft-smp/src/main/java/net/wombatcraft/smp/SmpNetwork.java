package net.wombatcraft.smp;

import com.google.gson.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import dev.ftb.mods.ftbteams.api.Team;
import java.util.*;
import java.util.function.Supplier;

public final class SmpNetwork {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(WombatcraftSmp.ID,"communities"),()->"1","1"::equals,"1"::equals);
    private static final Map<UUID,Long> LAST_REQUEST=new HashMap<>();
    public record Request(String action,String target,long revision,int page,long session,String territory) {
        static Request read(FriendlyByteBuf b){return new Request(b.readUtf(32),b.readUtf(96),b.readVarLong(),b.readVarInt(),b.readLong(),b.readUtf(160));}
        void write(FriendlyByteBuf b){b.writeUtf(action,32);b.writeUtf(target,96);b.writeVarLong(revision);b.writeVarInt(page);b.writeLong(session);b.writeUtf(territory,160);}
    }
    public record View(String json,long session) {
        static View read(FriendlyByteBuf b){return new View(b.readUtf(16384),b.readLong());}
        void write(FriendlyByteBuf b){b.writeUtf(json,16384);b.writeLong(session);}
    }
    static void register(){
        CHANNEL.registerMessage(0,Request.class,Request::write,Request::read,SmpNetwork::request,Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1,View.class,View::write,View::read,(packet,ctx)->{
            ctx.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->net.wombatcraft.smp.client.FactionScreen.receive(packet.json,packet.session)));
            ctx.get().setPacketHandled(true);
        },Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    public static void send(String action,String target,long revision,int page,long session){send(action,target,revision,page,session,"");}
    public static void send(String action,String target,long revision,int page,long session,String territory){CHANNEL.sendToServer(new Request(action,target,revision,page,session,territory));}
    private static void request(Request r,Supplier<NetworkEvent.Context> context){
        var ctx=context.get();ctx.enqueueWork(()->{
            ServerPlayer p=ctx.getSender();if(p==null||!WombatcraftSmp.ENABLED.get())return;
            long now=System.nanoTime()/1_000_000;
            if(now-LAST_REQUEST.getOrDefault(p.getUUID(),-1000L)<200)return;
            LAST_REQUEST.put(p.getUUID(),now);
            String message="";
            try {
                switch(r.action){
                    case "view" -> {}
                    case "builder" -> Factions.builder(p,UUID.fromString(r.target),true);
                    case "member" -> Factions.builder(p,UUID.fromString(r.target),false);
                    case "chat" -> {
                        Team team=Factions.playerTeam(p).orElseThrow(()->new IllegalStateException("Join a faction first"));
                        if(!team.isPartyTeam())throw new IllegalStateException("Join a faction first");
                        if(r.target.isBlank()||r.target.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Invalid message");
                        team.sendMessage(p.getUUID(),r.target);
                    }
                    case "policy" -> {
                        if(!WombatcraftSmp.CHUNK_POLICIES.get())throw new IllegalStateException("Territory policies are not open yet");
                        var claim=ClaimHooks.claim(p);
                        if(claim==null||claim.getTimeClaimed()!=r.revision||!territory(p).equals(r.territory))throw new IllegalStateException("Claim changed; refresh the territory page");
                        ClaimHooks.setPreset(p,r.target);
                    }
                    case "deliver" -> {ContractService.get(p.server).deliver(p,r.target,r.revision);message="Delivery recorded. Earnings are ready at the contract desk.";}
                    case "collect" -> {ContractService.get(p.server).collect(p);message="Coins collected.";}
                    case "vote" -> {
                        if(!WombatcraftSmp.ELECTIONS.get())throw new IllegalStateException("Voting is not open yet");
                        if(!atSpawn(p))throw new IllegalStateException("Visit the town hall at spawn to vote");
                        CivicState state=CivicState.get(p.server);state.election.vote(p.getUUID(),r.target,r.revision,System.currentTimeMillis());state.setDirty();
                        message="Your vote has been recorded.";
                    }
                    default -> throw new IllegalArgumentException("Unknown faction action");
                }
            }catch(SecurityException|IllegalArgumentException|IllegalStateException e){message=e.getMessage();}
            catch(Exception e){WombatcraftSmp.LOGGER.error("Faction request failed",e);message="Unable to complete that action. Please contact an administrator.";}
            view(p,Math.max(0,Math.min(63,r.page)),message,r.session);
        });ctx.setPacketHandled(true);
    }
    static boolean atSpawn(ServerPlayer p){return Factions.owner(p.level(),p.blockPosition()).map(t->t.getId().toString().equals(WombatcraftSmp.SPAWN_TEAM.get())).orElse(false);}
    static void view(ServerPlayer p,int page,String message){
        view(p,page,message,0);
    }
    static void view(ServerPlayer p,int page,String message,long session){view(p,page,message,session,-1);}
    static void view(ServerPlayer p,int page,String message,long session,int openTab){
        JsonObject out=new JsonObject();if(openTab>=0)out.addProperty("open_tab",openTab);out.addProperty("message",message);out.addProperty("at_spawn",atSpawn(p));out.addProperty("page",page);
        Optional<Team> found=Factions.playerTeam(p);JsonArray members=new JsonArray();
        if(found.isPresent()){
            Team team=found.get();out.addProperty("name",limit(team.getName().getString(),64));out.addProperty("role",Factions.role(team,p.getUUID()).name());
            out.addProperty("party",team.isPartyTeam());out.addProperty("owner",team.getOwner().equals(p.getUUID()));out.addProperty("member_count",team.getMembers().size());
            team.getMembers().stream().sorted().skip((long)page*8).limit(8).forEach(id->{
                JsonObject member=new JsonObject();member.addProperty("id",id.toString());
                var online=p.server.getPlayerList().getPlayer(id);var profile=p.server.getProfileCache().get(id);
                member.addProperty("name",online!=null?online.getGameProfile().getName():profile.map(x->x.getName()).orElse(id.toString().substring(0,8)));
                member.addProperty("role",Factions.role(team,id).name());member.addProperty("online",online!=null);members.add(member);
            });
        }else{out.addProperty("name","Your faction");out.addProperty("role","VISITOR");out.addProperty("party",false);out.addProperty("owner",false);out.addProperty("member_count",0);}
        out.add("members",members);
        var claim=ClaimHooks.claim(p);out.addProperty("chunk",p.chunkPosition().x+", "+p.chunkPosition().z);
        out.addProperty("dimension",p.level().dimension().location().toString());out.addProperty("policy",ClaimHooks.preset(claim));
        out.addProperty("territory_key",territory(p));out.addProperty("claim_revision",claim==null?0:claim.getTimeClaimed());out.addProperty("policies_open",WombatcraftSmp.CHUNK_POLICIES.get());
        out.addProperty("claim_owner",claim==null?"Unclaimed":limit(claim.getTeamData().getTeam().getName().getString(),64));
        out.addProperty("can_edit_claim",claim!=null&&!claim.getTeamData().getTeam().isServerTeam()&&claim.getTeamData().getTeam().getRankForPlayer(p.getUUID()).isOfficerOrBetter());
        out.addProperty("elections_open",WombatcraftSmp.ELECTIONS.get());
        if(WombatcraftSmp.ELECTIONS.get()){
            var civic=CivicState.get(p.server);civic.advance();var e=civic.election;
            out.addProperty("mayor",e.mayor());out.addProperty("term",e.revision());out.addProperty("election_at",e.nextElection());
            out.addProperty("vote",e.voteOf(p.getUUID()));
            out.addProperty("phase",e.phase().name());out.addProperty("phase_at",e.nextPhase());
            out.addProperty("mayor_name",CandidateCatalog.ALL.get(e.mayor()).get("name").getAsString());
            JsonArray candidates=new JsonArray();e.eligible().forEach(id->candidates.add(CandidateCatalog.ALL.get(id).deepCopy()));out.add("candidates",candidates);
        }
        out.addProperty("contracts_open",WombatcraftSmp.CONTRACTS.get());
        if(WombatcraftSmp.CONTRACTS.get())try {out.add("contracts",ContractService.get(p.server).view(p,page));}
        catch(Exception e){WombatcraftSmp.LOGGER.error("Contract view failed",e);out.addProperty("contracts_open",false);out.addProperty("message","The contract desk is unavailable while its records are checked.");}
        CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new View(out.toString(),session));
    }
    private static String territory(ServerPlayer p){return p.level().dimension().location()+"/"+p.chunkPosition().x+"/"+p.chunkPosition().z;}
    private static String limit(String s,int length){return s.length()>length?s.substring(0,length):s;}
    static void clear(){LAST_REQUEST.clear();}
    static void leave(UUID player){LAST_REQUEST.remove(player);}
}
