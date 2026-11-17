package net.wombatcraft.smp.client;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.wombatcraft.smp.PadNetwork;
import whocraft.tardis_refined.client.screen.components.GenericMonitorSelectionList;
import whocraft.tardis_refined.client.screen.components.SelectionListEntry;
import java.util.List;

/** Native Minecraft controls, with the directory inserted into Refined's existing monitor list. */
public final class PublicPadsScreen extends Screen {
    private final Screen parent;
    private final long session;
    private JsonObject data=new JsonObject();
    private String search="",draftName="";
    private boolean available,visible,settings,waiting=true;
    private int page,ticks,left,top,panelWidth;
    private EditBox input;
    private PublicPadsScreen(Screen parent,long session,boolean settings){super(Component.literal(settings?"Landing Pad Settings":"Public Landing Pads"));this.parent=parent;this.session=session;this.settings=settings;}
    static void preview(JsonObject fixture) {
        PublicPadsScreen screen=new PublicPadsScreen(null,1,"settings".equals(fixture.get("mode").getAsString()));screen.accept(fixture);Minecraft.getInstance().setScreen(screen);
    }
    public static void extend(GenericMonitorSelectionList<SelectionListEntry> list) {
        int x=Minecraft.getInstance().getWindow().getGuiScaledWidth()/2-20;
        SelectionListEntry entry=new SelectionListEntry(Component.literal("Public Landing Pads"),ignored->{
            var mc=Minecraft.getInstance();PublicPadsScreen screen=new PublicPadsScreen(mc.screen,System.nanoTime(),false);mc.setScreen(screen);screen.request("list","");
        },x);
        entry.setTooltip(Component.literal("Browse shared landing pads and check availability."));
        list.children().add(Math.min(3,list.children().size()),entry);
    }
    public static void receive(PadNetwork.View view) {
        var mc=Minecraft.getInstance();JsonObject json=JsonParser.parseString(view.json()).getAsJsonObject();
        if(view.open()) {PublicPadsScreen screen=new PublicPadsScreen(null,view.session(),true);screen.accept(json);mc.setScreen(screen);}
        else if(mc.screen instanceof PublicPadsScreen screen&&screen.session==view.session())screen.accept(json);
    }
    private String str(JsonObject o,String key){return o.has(key)?o.get(key).getAsString():"";}
    private boolean bool(JsonObject o,String key){return o.has(key)&&o.get(key).getAsBoolean();}
    private void accept(JsonObject json) {
        data=json;waiting=false;
        if(settings&&data.has("pad")){var p=data.getAsJsonObject("pad");draftName=str(p,"name");visible=bool(p,"visible");}
        if(data.has("page"))page=data.get("page").getAsInt();if(minecraft!=null)rebuildWidgets();
    }
    private void request(String action,String id) {
        waiting=true;ticks=0;
        PadNetwork.send(new PadNetwork.Request(action,id,draftName,visible,data.has("revision")?data.get("revision").getAsLong():0,page,search,available,session));
    }
    private Button button(String label,int x,int y,int w,Button.OnPress press){return addRenderableWidget(Button.builder(Component.literal(label),press).bounds(x,y,w,20).build());}
    @Override protected void init() {
        panelWidth=Math.min(410,width-16);left=(width-panelWidth)/2;top=Math.max(4,(height-236)/2);
        if(settings) {
            input=addRenderableWidget(new EditBox(font,left+12,top+56,panelWidth-24,20,Component.literal("Landing pad name")));input.setMaxLength(40);input.setValue(draftName);input.setResponder(v->draftName=v);input.setEditable(bool(data,"editable"));
            Button toggle=button("Public visibility: "+(visible?"On":"Off"),left+12,top+88,panelWidth-24,b->{visible=!visible;b.setMessage(Component.literal("Public visibility: "+(visible?"On":"Off")));});toggle.active=bool(data,"editable");
            Button save=button("Save",left+12,top+174,(panelWidth-32)/2,b->{if(!waiting&&data.has("pad"))request("save",str(data.getAsJsonObject("pad"),"id"));});save.active=bool(data,"editable");
            button("Done",left+20+(panelWidth-32)/2,top+174,(panelWidth-32)/2,b->onClose());
        } else {
            input=addRenderableWidget(new EditBox(font,left+12,top+30,panelWidth-126,20,Component.literal("Search landing pads")));input.setMaxLength(40);input.setHint(Component.literal("Search name, owner or dimension"));input.setValue(search);input.setResponder(v->search=v);
            button("Search",left+panelWidth-106,top+30,94,b->{page=0;request("list","");});
            button(available?"Available only: On":"Available only: Off",left+12,top+54,156,b->{available=!available;page=0;request("list","");b.setMessage(Component.literal(available?"Available only: On":"Available only: Off"));});
            button("Refresh",left+panelWidth-106,top+54,94,b->request("list",""));
            JsonArray rows=data.has("pads")?data.getAsJsonArray("pads"):new JsonArray();
            for(int i=0;i<rows.size();i++) {
                JsonObject row=rows.get(i).getAsJsonObject();int y=top+80+i*27;
                Button select=button(str(row,"status").equals("Unverified")?"Check & set":"Set destination",left+panelWidth-112,y,100,b->{if(!waiting)request("select",str(row,"id"));});select.active=bool(row,"selectable");
                select.setTooltip(Tooltip.create(Component.literal(str(row,"coordinates")+" | "+str(row,"dimension"))));
            }
            Button prev=button("<",left+12,top+194,26,b->{page--;request("list","");});prev.active=page>0;
            Button next=button(">",left+92,top+194,26,b->{page++;request("list","");});next.active=data.has("pages")&&page+1<data.get("pages").getAsInt();
            button("Back",left+panelWidth-106,top+194,94,b->request("monitor",""));
        }
    }
    @Override public void tick() {super.tick();ticks++;if(!settings&&ticks>=100){ticks=0;request("list","");}if(settings&&waiting&&ticks>100)waiting=false;}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        renderBackground(g);g.fill(left,top,left+panelWidth,top+230,0xf0192025);g.fill(left,top,left+panelWidth,top+24,0xff37464a);
        g.drawString(font,title,left+12,top+8,0xffe8d6a6,false);
        if(settings) {
            g.drawString(font,"Name",left+12,top+42,0xffeeeeee,false);
            if(data.has("pad")) {
                var p=data.getAsJsonObject("pad");g.drawString(font,font.plainSubstrByWidth("Owner: "+str(p,"owner"),panelWidth-24),left+12,top+120,0xffd7e3e5,false);
                g.drawString(font,"Status: "+str(p,"status"),left+12,top+134,0xffd7e3e5,false);
            }
            int helpY=top+150;
            for(var line:font.split(Component.literal("Hidden pads can still be reached by coordinates."),panelWidth-24)){g.drawString(font,line,left+12,helpY,0xffd7e3e5,false);helpY+=10;}
        } else {
            JsonArray rows=data.has("pads")?data.getAsJsonArray("pads"):new JsonArray();
            for(int i=0;i<rows.size();i++) {
                var r=rows.get(i).getAsJsonObject();int y=top+80+i*27;
                String state=str(r,"status");int stateWidth=font.width(state),right=left+panelWidth-120;
                g.drawString(font,font.plainSubstrByWidth(str(r,"name"),Math.max(20,panelWidth-144-stateWidth)),left+12,y,0xffeeeeee,false);
                g.drawString(font,state,right-stateWidth,y,state.equals("Available")?0xffa8e3bd:0xffffdf93,false);
                g.drawString(font,font.plainSubstrByWidth(str(r,"owner")+" | "+str(r,"dimension"),panelWidth-132),left+12,y+11,0xffb8ced2,false);
            }
            if(rows.isEmpty())g.drawString(font,waiting?"Checking public landing pads...":"No public pads match your search.",left+12,top+100,0xffd7e3e5,false);
            g.drawCenteredString(font,(page+1)+" / "+(data.has("pages")?data.get("pages").getAsInt():1),left+65,top+200,0xffeeeeee);
        }
        super.render(g,mx,my,delta);
        String message=str(data,"message");if(!message.isBlank()) {
            int y=top+(settings?204:218);
            g.drawString(font,font.plainSubstrByWidth(message,panelWidth-24),left+12,y,0xffffdf93,false);
            if(mx>=left+12&&mx<left+panelWidth-12&&my>=y&&my<y+10)g.renderTooltip(font,font.split(Component.literal(message),panelWidth-24),mx,my);
        }
    }
}
