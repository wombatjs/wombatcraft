package net.wombatcraft.smp;

import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;

/** Read-only guidance using the player's own FTB Quests state. */
public final class JourneyService {
    public record Milestone(String id,String title,String branch,int stage,String purpose) {}
    public record Suggestion(String id,String title,String branch,String goal,String purpose,int progress,boolean started,int order) {}
    private static final Set<String> BRANCHES=Set.of("survival","technology","magic","exploration","space","nature","settlement","convergence");
    private static final Map<UUID,Long> LAST_REQUEST=new HashMap<>();
    private static Object resourceManager;
    private static List<Milestone> catalog=List.of();
    private JourneyService() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("journey")
            .executes(c->show(c.getSource().getPlayerOrException(),"all"))
            .then(Commands.argument("path",StringArgumentType.word()).suggests((c,b)->{
                for(String branch:new TreeSet<>(BRANCHES)) if(branch.startsWith(b.getRemaining())) b.suggest(branch);
                return b.buildFuture();
            }).executes(c->show(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"path")))));
    }

    public static void clear() { LAST_REQUEST.clear(); resourceManager=null; catalog=List.of(); }

    public static List<Milestone> catalog(MinecraftServer server) throws Exception {
        Object current=server.getResourceManager();
        if(current!=resourceManager) {
            var resource=server.getResourceManager().getResource(new ResourceLocation(WombatcraftSmp.ID,"journey/milestones.json")).orElseThrow();
            var loaded=new ArrayList<Milestone>();
            try(var reader=new InputStreamReader(resource.open(),StandardCharsets.UTF_8)) {
                for(var entry:JsonParser.parseReader(reader).getAsJsonArray()) {
                    var value=entry.getAsJsonObject();
                    loaded.add(new Milestone(value.get("quest").getAsString(),value.get("title").getAsString(),value.get("branch").getAsString(),value.get("stage").getAsInt(),value.get("purpose").getAsString()));
                }
            }
            catalog=List.copyOf(loaded); resourceManager=current;
        }
        return catalog;
    }

    private static int show(ServerPlayer player,String branch) {
        if(!branch.equals("all")&&!BRANCHES.contains(branch)) {
            player.sendSystemMessage(Component.literal("Choose: "+String.join(", ",new TreeSet<>(BRANCHES)))); return 0;
        }
        long now=player.serverLevel().getServer().overworld().getGameTime();
        Long last=LAST_REQUEST.get(player.getUUID());
        if(last!=null&&now-last>=0&&now-last<40) return 0;
        LAST_REQUEST.put(player.getUUID(),now);
        try {
            Object file=Class.forName("dev.ftb.mods.ftbquests.quest.ServerQuestFile").getField("INSTANCE").get(null);
            Object team=Class.forName("dev.ftb.mods.ftbquests.quest.TeamData").getMethod("get",Player.class).invoke(null,player);
            var milestones=catalog(player.serverLevel().getServer()); var bridge=new Bridge(file,team);
            int completed=0;
            for(var m:milestones) if(bridge.complete(bridge.quest(m.id()))) completed++;
            player.sendSystemMessage(Component.literal("Main Journey: "+completed+" / "+milestones.size()+" milestones. Your individual progress."));
            var suggestions=recommend(file,team,milestones,branch);
            if(suggestions.isEmpty()) {
                player.sendSystemMessage(Component.literal(completed==milestones.size()?"You completed the guided journey. Explore side chapters, trade or visit /contracts.":"No available step in this path right now. Open the Main Journey for dependencies, or try /journey for another path."));
            } else {
                var first=suggestions.get(0);
                player.sendSystemMessage(link("Next: "+first.title()+" ["+first.progress()+"%]",first.id()));
                player.sendSystemMessage(Component.literal(first.id().equals(milestones.stream().filter(m->m.title().equals(first.goal())).map(Milestone::id).findFirst().orElse(""))?first.purpose():"This prerequisite leads toward "+first.goal()+". "+first.purpose()));
                var shown=new HashSet<String>();shown.add(first.branch());
                int alternatives=0;
                for(var suggestion:suggestions) {
                    if(shown.add(suggestion.branch())) {
                        player.sendSystemMessage(link("Also available — "+suggestion.branch()+": "+suggestion.title(),suggestion.id()));
                        if(++alternatives==3) break;
                    }
                }
            }
            var lastGoal=milestones.get(milestones.size()-1);
            player.sendSystemMessage(link("Long-term goal: "+lastGoal.title(),lastGoal.id()));
            player.sendSystemMessage(Component.literal("Click an objective to open it. Pin it in the quest book to track it on screen."));
            return 1;
        } catch(Exception error) {
            WombatcraftSmp.LOGGER.error("Could not read journey progress",error);
            player.sendSystemMessage(Component.literal("The journey guide is unavailable. Your existing quests and progress are unchanged.")); return 0;
        }
    }

    private static Component link(String title,String id) {
        return Component.literal(title).withStyle(style->style.withColor(0x8DD6ED)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/ftbquests open_book "+id))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,Component.literal("Open the original quest; progress and rewards are shared."))));
    }

    public static List<Suggestion> recommend(Object file,Object team,List<Milestone> milestones,String branch) throws Exception {
        var bridge=new Bridge(file,team); var result=new ArrayList<Suggestion>(); var seen=new HashSet<String>();
        for(int i=0;i<milestones.size();i++) {
            var m=milestones.get(i);
            if(!branch.equals("all")&&!m.branch().equals(branch)) continue;
            Object goal=bridge.quest(m.id());
            if(bridge.complete(goal)) continue;
            Object next=bridge.available(goal,new HashSet<>());
            if(next==null) continue;
            String id=(String)next.getClass().getMethod("getCodeString").invoke(next);
            if(!seen.add(id)) continue;
            // Forge remaps the inherited getTitle method while Movable retains an
            // incompatible interface signature. Pack quests all have explicit titles.
            String title=(String)next.getClass().getMethod("getRawTitle").invoke(next);
            if(title.isBlank()) title="Quest "+id;
            int progress=(Integer)bridge.teamType.getMethod("getRelativeProgress",bridge.objectType).invoke(team,next);
            boolean started=(Boolean)bridge.teamType.getMethod("isStarted",bridge.objectType).invoke(team,next);
            result.add(new Suggestion(id,title,m.branch(),m.title(),m.purpose(),progress,started,i));
        }
        result.sort(Comparator.comparing((Suggestion s)->!s.started()).thenComparingInt(Suggestion::order));
        return result;
    }

    private static final class Bridge {
        final Object file,team;
        final Class<?> objectType=Class.forName("dev.ftb.mods.ftbquests.quest.QuestObject");
        final Class<?> questType=Class.forName("dev.ftb.mods.ftbquests.quest.Quest");
        final Class<?> teamType=Class.forName("dev.ftb.mods.ftbquests.quest.TeamData");
        final Map<String,Object> ready=new HashMap<>();
        Bridge(Object file,Object team) throws Exception { this.file=file;this.team=team; }
        Object quest(String id) throws Exception {
            Object quest=file.getClass().getMethod("getQuest",long.class).invoke(file,Long.parseLong(id,16));
            if(quest==null) throw new IllegalStateException("Missing journey quest "+id); return quest;
        }
        boolean complete(Object q) throws Exception { return (Boolean)teamType.getMethod("isCompleted",objectType).invoke(team,q); }
        Object available(Object q,Set<String> visited) throws Exception {
            String id=(String)q.getClass().getMethod("getCodeString").invoke(q);
            if(ready.containsKey(id)) return ready.get(id);
            if(!visited.add(id)||visited.size()>4096||complete(q)) return null;
            if((Boolean)teamType.getMethod("isExcludedByOtherQuestline",objectType).invoke(team,q)) return null;
            if(questType.isInstance(q)&&(Boolean)teamType.getMethod("canStartTasks",questType).invoke(team,q)
                &&(Boolean)q.getClass().getMethod("isVisible",teamType).invoke(q,team)) { ready.put(id,q); return q; }
            if(questType.isInstance(q)) {
                try(var dependencies=(Stream<?>)q.getClass().getMethod("streamDependencies").invoke(q)) {
                    for(Object dependency:dependencies.toList()) {
                        Object found=available(dependency,visited);
                        if(found!=null) { ready.put(id,found); return found; }
                    }
                }
            } else {
                for(Object child:(Collection<?>)q.getClass().getMethod("getChildren").invoke(q)) {
                    if(!questType.isInstance(child)) continue;
                    Object found=available(child,visited); if(found!=null) { ready.put(id,found);return found; }
                }
            }
            ready.put(id,null); return null;
        }
    }
}
