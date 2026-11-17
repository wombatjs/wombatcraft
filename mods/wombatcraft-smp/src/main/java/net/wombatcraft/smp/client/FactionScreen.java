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
    private static final String[] TABS={"Faction","Members","Territory"};
    private JsonObject data=new JsonObject();
    private final Screen previous;
    private final int mode;
    private final long session=UUID.randomUUID().getMostSignificantBits()|1L;
    private int ballotPage,tab,page,left,top,panelW,panelH,bodyX,bodyW;
    private int ballotRows(){return panelH<280?3:4;}
    private long requestedAt=System.currentTimeMillis();
    private boolean waiting=true;
    public FactionScreen(Screen previous){this(previous,0);}
    public FactionScreen(Screen previous,int mode){super(Component.literal(mode==4?"Contracts":mode==3?"Town Hall":"Factions"));this.previous=previous;this.mode=mode;this.tab=mode;}
    public static void receive(String json,long session){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null)return;
        if(session!=0 && (!(mc.screen instanceof FactionScreen s)||s.session!=session))return;
        try {
            JsonObject data=JsonParser.parseString(json).getAsJsonObject();
            int mode=data.has("menu")?data.get("menu").getAsInt():0;
            FactionScreen screen;
            if(mc.screen instanceof FactionScreen existing && existing.mode==mode)screen=existing;
            else {screen=new FactionScreen(mc.screen,mode);mc.setScreen(screen);}
            screen.data=data;screen.waiting=false;screen.rebuildWidgets();
        } catch(RuntimeException e) {WombatcraftSmp.LOGGER.error("Could not read menu data",e);}
    }
    @SubscribeEvent public static void pauseButtons(ScreenEvent.Init.Post event){
        if(!(event.getScreen() instanceof net.minecraft.client.gui.screens.PauseScreen)||!WombatcraftSmp.clientEnabled())return;
        String[] labels={"Factions","Contracts","Town Hall","Journey"};
        String[] commands={"faction","contracts","townhall","journey"};
        int w=Math.min(74,(event.getScreen().width-20)/4);
        for(int i=0;i<labels.length;i++){
            final String command=commands[i];
            event.addListener(Button.builder(Component.literal(labels[i]),b->{
                Minecraft mc=Minecraft.getInstance();mc.setScreen(null);
                if(mc.player!=null)mc.player.connection.sendCommand(command);
            }).bounds((event.getScreen().width-w*4)/2+i*w,event.getScreen().height-26,w-2,20).build());
        }
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
        bodyX=left+(mode==0?104:12);bodyW=panelW-(mode==0?116:24);
        for(int i=0;mode==0&&i<TABS.length;i++){
            final int selected=i;Button button=button(left+8,top+40+i*25,86,TABS[i],()->{tab=selected;page=0;request("view","",0);});button.active=tab!=i;
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
                if(bool("owner")&&bool("policies_open")&&(role.equals("MEMBER")||role.equals("BUILDER"))){
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
        }else if(tab==4){
            if(bool("contracts_open")&&data.has("contracts")){
                JsonObject contracts=data.getAsJsonObject("contracts");JsonArray orders=contracts.getAsJsonArray("orders");
                for(int i=0;i<orders.size();i++){
                    JsonObject order=orders.get(i).getAsJsonObject();
                    Button deliver=button(bodyX+bodyW-65,top+69+i*24,65,"Deliver",()->request("deliver",order.get("id").getAsString()+"|"+order.get("item").getAsString(),order.get("period").getAsLong()));
                    deliver.active=bool("at_spawn")&&order.get("delivered").getAsLong()<order.get("required").getAsLong();
                    deliver.setTooltip(Tooltip.create(Component.literal("Deliver available plain items, up to the remaining demand. Earnings are proportional to your contribution. Full order base budget: "+order.get("reward").getAsLong()+" copper.")));
                }
                button(bodyX,top+panelH-48,48,"Prev",()->{page--;request("view","",0);}).active=page>0;
                button(bodyX+54,top+panelH-48,48,"Next",()->{page++;request("view","",0);}).active=(page+1)*4<contracts.get("rows").getAsInt();
                button(bodyX+110,top+panelH-48,Math.min(120,bodyW-110),"Collect coins",()->request("collect","",0)).active=bool("at_spawn")&&contracts.get("balance").getAsLong()>0;
            }
        }else{
            if(bool("elections_open")) {
                JsonArray candidates=candidates();
                for(int i=0;i<Math.min(ballotRows(),candidates.size()-ballotPage*ballotRows());i++) {
                    JsonObject c=candidates.get(ballotPage*ballotRows()+i).getAsJsonObject();String id=c.get("id").getAsString();
                    Button b=button(bodyX+bodyW-51,top+81+i*24,51,id.equals(string("vote"))?"Voted":"Vote",()->request("vote",id,number("term")));
                    b.active=bool("at_spawn")&&!id.equals(string("vote"));b.setHeight(18);
                    b.setTooltip(Tooltip.create(Component.literal(c.get("description").getAsString())));
                }
                button(bodyX,top+panelH-48,48,"Prev",()->{ballotPage--;rebuildWidgets();}).active=ballotPage>0;
                button(bodyX+54,top+panelH-48,48,"Next",()->{ballotPage++;rebuildWidgets();}).active=(ballotPage+1)*ballotRows()<candidates.size();
            }
        }
    }
    private JsonArray candidates(){
        JsonArray list=data.has("candidates")?data.getAsJsonArray("candidates"):new JsonArray();
        ballotPage=Math.max(0,Math.min(ballotPage,Math.max(0,(list.size()-1)/ballotRows())));return list;
    }
    private Button button(int x,int y,int w,String label,Runnable action){return addRenderableWidget(Button.builder(Component.literal(label),b->action.run()).bounds(x,y,w,20).build());}
    private void request(String action,String target,long revision){waiting=true;requestedAt=System.currentTimeMillis();SmpNetwork.send(action,target,revision,page,session,string("territory_key"),mode);}
    private boolean bool(String key){return data.has(key)&&data.get(key).getAsBoolean();}
    private long number(String key){return data.has(key)?data.get(key).getAsLong():0;}
    private String string(String key){return data.has(key)?data.get(key).getAsString():"";}
    private static String scopeLabel(String s){return switch(s){case "PERSONAL"->"Daily";case "COMMUNITY"->"Weekly project";case "FACTION"->"Factory";default->s;};}
    private static String label(String id){return switch(id){case "private_factory"->"Private factory";case "public_shop"->"Public shop";case "officer_storage"->"Officer storage";case "ftb_default"->"Existing FTB rules";default->id;};}
    private void line(GuiGraphics g,String text,int y,int color){g.drawString(font,font.plainSubstrByWidth(text,bodyW),bodyX,y,color,false);}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);g.fill(left,top,left+panelW,top+panelH,0xff171220);if(mode==0)g.fill(left,top,left+98,top+panelH,0xff21192d);
        if(mode==0)g.fill(left+98,top,left+99,top+panelH,0xff59436e);g.drawString(font,getTitle(),left+10,top+15,TEXT,false);
        if(waiting){line(g,System.currentTimeMillis()-requestedAt>5000?"No reply yet. Refresh to retry.":"Loading...",top+49,MUTED);}
        else if(tab==0){
            line(g,bool("party")?string("name"):"Find your people",top+45,TEXT);
            line(g,bool("party")?"Your role: "+string("role").toLowerCase(Locale.ROOT):"Create a faction or accept an invitation.",top+61,MUTED);
            if(bool("party"))line(g,number("member_count")+(number("member_count")==1?" member":" members"),top+75,MUTED);
            line(g,"Faction chat",top+125,TEXT);
            line(g,"Your faction manages members and shared land.",top+176,MUTED);
        }else if(tab==1){
            line(g,"Members",top+36,TEXT);var members=data.has("members")?data.getAsJsonArray("members"):new JsonArray();
            for(int i=0;i<members.size();i++){
                JsonObject m=members.get(i).getAsJsonObject();String name=m.get("name").getAsString();
                g.drawString(font,font.plainSubstrByWidth(name,Math.max(50,bodyW-74)),bodyX,top+53+i*17,m.get("online").getAsBoolean()?TEXT:MUTED,false);
            }
        }else if(tab==2){
            line(g,string("claim_owner"),top+39,TEXT);line(g,string("dimension")+" / "+string("chunk"),top+54,MUTED);
            line(g,"Policy: "+label(string("policy")),top+70,MUTED);
        }else if(tab==4){
            line(g,bool("at_spawn")?"Contracts":"Contracts - deliveries at spawn",top+37,TEXT);
            if(bool("contracts_open")&&data.has("contracts")){
                JsonObject contracts=data.getAsJsonObject("contracts");
                line(g,"Earnings: "+contracts.get("balance").getAsLong()+" copper",top+53,MUTED);
                JsonArray orders=contracts.getAsJsonArray("orders");
                for(int i=0;i<orders.size();i++){
                    JsonObject order=orders.get(i).getAsJsonObject();
                    g.drawString(font,font.plainSubstrByWidth(order.get("name").getAsString(),bodyW-70),bodyX,top+71+i*24,TEXT,false);
                    g.drawString(font,scopeLabel(order.get("scope").getAsString())+"  "+order.get("delivered").getAsLong()+" / "+order.get("required").getAsLong(),bodyX,top+83+i*24,MUTED,false);
                }
            }else line(g,"The contract desk has not opened.",top+57,MUTED);
        }else{
            line(g,"Town elections",top+37,TEXT);
            if(!bool("elections_open")){line(g,"The next election has not opened.",top+57,MUTED);}
            else{
                line(g,"Mayor: "+string("mayor_name"),top+51,MUTED);
                long minutes=Math.max(0,(number("phase_at")-System.currentTimeMillis())/60000);
                line(g,string("phase")+" closes in "+minutes/60+"h "+minutes%60+"m",top+65,MUTED);
                JsonArray candidates=candidates();
                for(int i=0;i<Math.min(ballotRows(),candidates.size()-ballotPage*ballotRows());i++){
                    JsonObject c=candidates.get(ballotPage*ballotRows()+i).getAsJsonObject();
                    g.drawString(font,font.plainSubstrByWidth(c.get("name").getAsString(),bodyW-60),bodyX,top+83+i*24,TEXT,false);
                    g.drawString(font,font.plainSubstrByWidth(c.get("policy").getAsString(),bodyW-60),bodyX,top+95+i*24,MUTED,false);
                }
            }
        }
        if(!string("message").isEmpty())line(g,string("message"),top+panelH-13,0xffffce8f);
        super.render(g,mx,my,partial);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.setScreen(null);}
}
