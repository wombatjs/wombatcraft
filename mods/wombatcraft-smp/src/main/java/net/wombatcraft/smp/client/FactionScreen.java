package net.wombatcraft.smp.client;

import com.google.gson.*;
import dev.ftb.mods.ftbchunks.client.gui.ChunkScreen;
import dev.ftb.mods.ftblibrary.ui.ScreenWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wombatcraft.smp.SmpNetwork;
import net.wombatcraft.smp.WombatcraftSmp;
import java.util.*;

@Mod.EventBusSubscriber(modid=WombatcraftSmp.ID,value=Dist.CLIENT)
public final class FactionScreen extends Screen {
    private static boolean nativeOnce;
    private static Screen mapOwner;
    private static Button mapLink;
    private static final int TEXT=0xfff5efff,MUTED=0xffc4b6d8;
    private static final String[] TABS={"Faction","Members","Territory","Town hall"};
    private static final String[][] CANDIDATES={
            {"brasswick","Brasswick","Create / Public Works"},{"flux","Director Flux","Mekanism & AE2 / Procurement"},
            {"willow","Archmage Willow","Ars Nouveau & Botania / Research"},{"briar","Ranger Briar","Twilight & Aether / Expeditions"},
            {"pebble","Professor Pebble","Cobblemon / Field Research"},{"hearth","Steward Hearth","Settlements / Civic Supplies"}};
    private JsonObject data=new JsonObject();
    private final Screen previous;
    private final long session=UUID.randomUUID().getMostSignificantBits()|1L;
    private int tab,page,left,top,panelW,panelH,bodyX,bodyW;
    private long requestedAt=System.currentTimeMillis();
    private boolean waiting=true;
    public FactionScreen(Screen previous){super(Component.literal("Wombatcraft Factions"));this.previous=previous;}
    public static void receive(String json,long session){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null)return;
        if(session!=0 && (!(mc.screen instanceof FactionScreen s)||s.session!=session))return;
        FactionScreen screen;
        if(mc.screen instanceof FactionScreen existing)screen=existing;
        else{screen=new FactionScreen(mc.screen);mc.setScreen(screen);}
        try{screen.data=JsonParser.parseString(json).getAsJsonObject();screen.waiting=false;screen.rebuildWidgets();}
        catch(RuntimeException e){screen.waiting=false;screen.data=new JsonObject();screen.data.addProperty("message","Could not read faction data. Refresh to retry.");}
    }
    @SubscribeEvent public static void opening(ScreenEvent.Opening event){
        if(Minecraft.getInstance().level==null || !WombatcraftSmp.clientEnabled() || nativeOnce){nativeOnce=false;return;}
        if(event.getNewScreen() instanceof ScreenWrapper wrapper
                && wrapper.getGui().getClass().getName().equals("dev.ftb.mods.ftbteams.client.gui.MyTeamScreen")){
            FactionScreen screen=new FactionScreen(event.getNewScreen());event.setNewScreen(screen);SmpNetwork.send("view","",0,0,screen.session);
        }
    }
    @SubscribeEvent public static void mapButton(ScreenEvent.Init.Post event){
        if(Minecraft.getInstance().level==null || !WombatcraftSmp.clientEnabled())return;
        if(event.getScreen() instanceof ScreenWrapper wrapper && wrapper.getGui() instanceof ChunkScreen){
            mapOwner=event.getScreen();
            mapLink=Button.builder(Component.literal("Faction territory"),b->{
                FactionScreen screen=new FactionScreen(event.getScreen());screen.tab=2;
                Minecraft.getInstance().setScreen(screen);SmpNetwork.send("view","",0,0,screen.session);
            }).bounds(event.getScreen().width-118,event.getScreen().height-28,110,20).build();
        }
    }
    // FTB's wrapper paints and handles its own widget tree rather than vanilla screen children.
    @SubscribeEvent public static void mapRender(ScreenEvent.Render.Post event){
        if(event.getScreen()==mapOwner&&mapLink!=null)
            mapLink.render(event.getGuiGraphics(),event.getMouseX(),event.getMouseY(),event.getPartialTick());
    }
    @SubscribeEvent public static void mapClick(ScreenEvent.MouseButtonPressed.Pre event){
        if(event.getScreen()==mapOwner&&mapLink!=null&&mapLink.mouseClicked(event.getMouseX(),event.getMouseY(),event.getButton()))event.setCanceled(true);
    }
    @Override protected void init(){
        panelW=Math.min(490,width-16);panelH=Math.min(300,height-16);left=(width-panelW)/2;top=(height-panelH)/2;
        bodyX=left+104;bodyW=panelW-116;
        for(int i=0;i<TABS.length;i++){
            final int selected=i;Button button=button(left+8,top+40+i*25,86,TABS[i],()->{tab=selected;rebuildWidgets();});button.active=tab!=i;
        }
        button(left+8,top+panelH-27,86,"Close",this::onClose);
        button(left+panelW-66,top+8,58,"Refresh",()->request("view","",0));
        if(waiting)return;
        if(tab==0){
            button(bodyX,top+90,Math.min(bodyW,190),bool("party")?"Manage invitations & allies":"Create or join a faction",()->{
                nativeOnce=true;
                new dev.ftb.mods.ftbteams.net.OpenGUIMessage().sendToServer();
            });
            EditBox chat=new EditBox(font,bodyX,top+140,Math.max(80,bodyW-56),20,Component.literal("Faction chat"));chat.setMaxLength(96);chat.setHint(Component.literal("Message your faction"));addRenderableWidget(chat);
            button(bodyX+bodyW-50,top+140,50,"Send",()->{if(!chat.getValue().isBlank()){request("chat",chat.getValue(),0);chat.setValue("");}});
        }else if(tab==1){
            var members=data.has("members")?data.getAsJsonArray("members"):new JsonArray();
            for(int i=0;i<members.size();i++){
                JsonObject member=members.get(i).getAsJsonObject();String role=member.get("role").getAsString();
                if(bool("owner")&&(role.equals("MEMBER")||role.equals("BUILDER"))){
                    button(bodyX+bodyW-68,top+49+i*17,68,role.equals("BUILDER")?"Member":"Builder",
                            ()->request(role.equals("BUILDER")?"member":"builder",member.get("id").getAsString(),0)).setHeight(16);
                }
            }
            button(bodyX,top+panelH-48,48,"Prev",()->{page--;request("view","",0);}).active=page>0;
            button(bodyX+54,top+panelH-48,48,"Next",()->{page++;request("view","",0);}).active=(page+1)*8<number("member_count");
        }else if(tab==2){
            String[] presets={"private_factory","public_shop","officer_storage","ftb_default"};
            for(int i=0;i<presets.length;i++){
                String preset=presets[i];Button b=button(bodyX,top+91+i*23,Math.min(bodyW,195),label(preset),()->request("policy",preset,number("claim_revision")));
                b.active=bool("can_edit_claim")&&bool("policies_open")&&!preset.equals(string("policy"));
            }
            button(bodyX+Math.max(0,bodyW-92),top+panelH-27,92,"Claim map",ChunkScreen::openChunkScreen);
        }else{
            if(bool("elections_open"))for(int i=0;i<CANDIDATES.length;i++){
                String[] c=CANDIDATES[i];Button b=button(bodyX+bodyW-51,top+65+i*24,51,c[0].equals(string("vote"))?"Voted":"Vote",()->request("vote",c[0],number("term")));
                b.active=bool("at_spawn")&&!c[0].equals(string("vote"));b.setHeight(18);
            }
        }
    }
    private Button button(int x,int y,int w,String label,Runnable action){return addRenderableWidget(Button.builder(Component.literal(label),b->action.run()).bounds(x,y,w,20).build());}
    private void request(String action,String target,long revision){waiting=true;requestedAt=System.currentTimeMillis();SmpNetwork.send(action,target,revision,page,session,string("territory_key"));}
    private boolean bool(String key){return data.has(key)&&data.get(key).getAsBoolean();}
    private long number(String key){return data.has(key)?data.get(key).getAsLong():0;}
    private String string(String key){return data.has(key)?data.get(key).getAsString():"";}
    private static String label(String id){return switch(id){case "private_factory"->"Private factory";case "public_shop"->"Public shop";case "officer_storage"->"Officer storage";case "ftb_default"->"Existing FTB rules";default->id;};}
    private void line(GuiGraphics g,String text,int y,int color){g.drawString(font,font.plainSubstrByWidth(text,bodyW),bodyX,y,color,false);}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);g.fill(left,top,left+panelW,top+panelH,0xff171220);g.fill(left,top,left+98,top+panelH,0xff21192d);
        g.fill(left+98,top,left+99,top+panelH,0xff59436e);g.drawString(font,"Wombatcraft Factions",left+10,top+15,TEXT,false);
        if(waiting){line(g,System.currentTimeMillis()-requestedAt>5000?"No reply yet. Refresh to retry.":"Loading faction data...",top+49,MUTED);}
        else if(tab==0){
            line(g,bool("party")?string("name"):"Find your people",top+45,TEXT);
            line(g,bool("party")?"Your role: "+string("role").toLowerCase(Locale.ROOT):"Create a faction or accept an invitation.",top+61,MUTED);
            if(bool("party"))line(g,number("member_count")+(number("member_count")==1?" member":" members"),top+75,MUTED);
            line(g,"Faction chat",top+125,TEXT);
            line(g,"Factory orders will appear here when opened.",top+176,MUTED);
        }else if(tab==1){
            line(g,"Members",top+36,TEXT);var members=data.has("members")?data.getAsJsonArray("members"):new JsonArray();
            for(int i=0;i<members.size();i++){
                JsonObject m=members.get(i).getAsJsonObject();String name=m.get("name").getAsString();
                g.drawString(font,font.plainSubstrByWidth(name,Math.max(50,bodyW-74)),bodyX,top+53+i*17,m.get("online").getAsBoolean()?TEXT:MUTED,false);
            }
        }else if(tab==2){
            line(g,string("claim_owner"),top+39,TEXT);line(g,string("dimension")+" / "+string("chunk"),top+54,MUTED);
            line(g,"Policy: "+label(string("policy")),top+70,MUTED);
        }else{
            line(g,"Town elections",top+37,TEXT);
            if(!bool("elections_open")){line(g,"The next election has not opened.",top+57,MUTED);}
            else{
                line(g,"Current mayor: "+string("mayor"),top+51,MUTED);
                for(int i=0;i<CANDIDATES.length;i++){
                    g.drawString(font,CANDIDATES[i][1],bodyX,top+66+i*24,TEXT,false);
                    g.drawString(font,font.plainSubstrByWidth(CANDIDATES[i][2],bodyW-60),bodyX,top+77+i*24,MUTED,false);
                }
            }
        }
        if(!string("message").isEmpty())line(g,string("message"),top+panelH-13,0xffffce8f);
        super.render(g,mx,my,partial);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.setScreen(null);}
}
