package net.wombatcraft.smp;

import com.google.gson.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

public final class PadNetwork {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(WombatcraftSmp.ID,"public_pads"),()->"1","1"::equals,"1"::equals);
    private static final Map<UUID,Long> last=new HashMap<>();
    public record Request(String action,String id,String name,boolean visible,long revision,int page,String search,boolean available,long session) {
        static Request read(FriendlyByteBuf b){return new Request(b.readUtf(16),b.readUtf(36),b.readUtf(40),b.readBoolean(),b.readLong(),b.readVarInt(),b.readUtf(40),b.readBoolean(),b.readLong());}
        void write(FriendlyByteBuf b){b.writeUtf(action,16);b.writeUtf(id,36);b.writeUtf(name,40);b.writeBoolean(visible);b.writeLong(revision);b.writeVarInt(page);b.writeUtf(search,40);b.writeBoolean(available);b.writeLong(session);}
    }
    public record View(String json,long session,boolean open) {
        static View read(FriendlyByteBuf b){return new View(b.readUtf(16384),b.readLong(),b.readBoolean());}
        void write(FriendlyByteBuf b){b.writeUtf(json,16384);b.writeLong(session);b.writeBoolean(open);}
    }
    static void register() {
        CHANNEL.registerMessage(0,Request.class,Request::write,Request::read,(r,ctx)->{
            ctx.get().enqueueWork(()->{ServerPlayer p=ctx.get().getSender();if(p!=null)handle(p,r);});ctx.get().setPacketHandled(true);
        },Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1,View.class,View::write,View::read,(v,ctx)->{
            ctx.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->net.wombatcraft.smp.client.PublicPadsScreen.receive(v)));ctx.get().setPacketHandled(true);
        },Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    public static void send(Request r){CHANNEL.sendToServer(r);}
    static void clear(){last.clear();}
    static void leave(UUID player){last.remove(player);}
    static void openSettings(ServerPlayer p,PadRegistry.Pad pad){reply(p,settings(p,pad,""),System.nanoTime(),true);}
    private static void reply(ServerPlayer p,JsonObject json,long session,boolean open){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new View(json.toString(),session,open));}
    private static JsonObject settings(ServerPlayer p,PadRegistry.Pad pad,String message) {
        JsonObject out=new JsonObject();out.addProperty("mode","settings");out.addProperty("message",message);
        if(pad==null){out.addProperty("editable",false);return out;}
        out.add("pad",row(p,pad,""));out.addProperty("revision",pad.revision);
        boolean edit=true;try{PublicPads.canEdit(p,pad);}catch(RuntimeException ex){edit=false;}
        out.addProperty("editable",edit);return out;
    }
    private static JsonObject row(ServerPlayer p,PadRegistry.Pad pad,String ship) {
        JsonObject j=new JsonObject();j.addProperty("id",pad.id.toString());j.addProperty("name",pad.name);j.addProperty("owner",pad.ownerName);j.addProperty("dimension",pad.dimension);j.addProperty("visible",pad.visible);
        j.addProperty("coordinates",pad.pos.getX()+", "+pad.pos.getY()+", "+pad.pos.getZ());
        var state=PublicPads.status(p.server,pad,ship,false);j.addProperty("status",PublicPads.label(state));
        j.addProperty("selectable",state==PublicPads.Status.UNVERIFIED||PublicPads.usable(state,PadRegistry.get(p.server),pad,ship));return j;
    }
    private static JsonObject directory(ServerPlayer p,Request r,String message) {
        var op=PublicPads.operator(p);String ship=op.getLevelKey().location().toString();PadRegistry data=PadRegistry.get(p.server);
        PublicPads.reconcile(p.server);String query=r.search.strip().toLowerCase(Locale.ROOT);
        List<PadRegistry.Pad> matching=data.pads.values().stream().filter(x->x.visible&&(x.name+" "+x.dimension+" "+x.ownerName).toLowerCase(Locale.ROOT).contains(query))
            .filter(x->!r.available||PublicPads.status(p.server,x,ship,false)==PublicPads.Status.AVAILABLE).sorted(Comparator.comparing((PadRegistry.Pad x)->x.name.toLowerCase(Locale.ROOT)).thenComparing(x->x.id)).toList();
        int pages=Math.max(1,(matching.size()+3)/4),page=Math.max(0,Math.min(r.page,pages-1));
        JsonObject out=new JsonObject();out.addProperty("mode","directory");out.addProperty("message",message);out.addProperty("page",page);out.addProperty("pages",pages);out.addProperty("total",matching.size());
        JsonArray rows=new JsonArray();matching.stream().skip((long)page*4).limit(4).forEach(x->rows.add(row(p,x,ship)));out.add("pads",rows);return out;
    }
    private static void handle(ServerPlayer p,Request r) {
        long now=p.server.getTickCount();if(now-last.getOrDefault(p.getUUID(),-100L)<3)return;last.put(p.getUUID(),now);
        String message="";PadRegistry data=PadRegistry.get(p.server);PadRegistry.Pad pad=null;
        try {
            switch(r.action) {
                case "list" -> {}
                case "monitor" -> {
                    var op=PublicPads.operator(p);
                    new whocraft.tardis_refined.common.network.messages.screens.S2COpenMonitor(op.getInteriorManager().isWaitingToGenerate(),op.getPilotingManager().getCurrentLocation(),op.getPilotingManager().getTargetLocation(),op.getUpgradeHandler(),op.getAestheticHandler().getShellTheme()).send(p);
                    return;
                }
                case "select" -> {PublicPads.select(p,UUID.fromString(r.id));message="Destination set. Use the console to fly there.";}
                case "save","settings" -> {
                    pad=data.pads.get(UUID.fromString(r.id));PublicPads.canEdit(p,pad);
                    if(r.action.equals("save")) {
                        if(pad.revision!=r.revision)throw new IllegalStateException("Settings changed. Review the current values and save again.");
                        String name=PadRegistry.validName(r.name);
                        if(r.visible&&PublicPads.moving(p.serverLevel(),pad.pos))throw new IllegalStateException("Moving pads cannot be listed publicly.");
                        pad.name=name;pad.visible=r.visible;pad.revision++;data.setDirty();message="Landing pad saved.";
                    }
                }
                default -> throw new IllegalArgumentException("Unknown landing-pad action.");
            }
        }catch(IllegalArgumentException|IllegalStateException|SecurityException ex){message=ex.getMessage();}
        catch(Exception ex){WombatcraftSmp.LOGGER.error("Landing pad request failed",ex);message="Could not update the pad. Reopen the menu and try again.";}
        if(r.action.equals("save")||r.action.equals("settings")){reply(p,settings(p,pad,message),r.session,false);return;}
        try {reply(p,directory(p,r,message),r.session,false);}
        catch(RuntimeException ex){JsonObject out=new JsonObject();out.addProperty("mode","directory");out.addProperty("message",ex.getMessage());out.add("pads",new JsonArray());out.addProperty("page",0);out.addProperty("pages",1);out.addProperty("total",0);reply(p,out,r.session,false);}
    }
}
