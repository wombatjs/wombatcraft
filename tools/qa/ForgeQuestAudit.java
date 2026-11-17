package local.qa;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.TickEvent;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
@Mod("forge_quest_audit")
public class ForgeQuestAudit {
 private Object frontierServer;
 private int frontierWaitTicks;
 public ForgeQuestAudit(){
  try { Object bus=Class.forName("net.minecraftforge.common.MinecraftForge").getField("EVENT_BUS").get(null);Class.forName("net.minecraftforge.eventbus.api.IEventBus").getMethod("register",Object.class).invoke(bus,this); } catch(Exception e){throw new RuntimeException(e);}
 }
 @SubscribeEvent public void started(ServerStartedEvent event){audit();auditJourney(event);auditRecipes(event);auditAdvancements(event);auditCommands(event);auditPortalChunkTracking(event);auditPortalPolicy();
  try {
   for(String name:List.of("leonus.tardis_rooms.room.RoomGrowth","leonus.tardis_rooms.network.InterfacePayload","leonus.tardis_rooms.network.ManagePayloads$Rename","leonus.tardis_rooms.iface.InterfaceTools","dev.jeryn.mc.rwf.common.entity.FlightTracker")) Class.forName(name);
   frontierServer = call(event, "getServer"); FrontierAudit.prepare(frontierServer); }
  catch (Exception e) { throw new IllegalStateException("Frontier gameplay audit failed", e); }
 }
 @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
  if (event.phase != TickEvent.Phase.END || frontierServer == null) return;
  try {
   // Saved entity sections load asynchronously. ServerStartedEvent runs before
   // normal ticks can make those sections visible to the reward entity query.
   if (++frontierWaitTicks < 20 || !FrontierAudit.ready(frontierServer)) {
    if (frontierWaitTicks > 600) throw new IllegalStateException("Trial fixture entity chunks did not become ready");
    return;
   }
   Object server = frontierServer; frontierServer = null;
   FrontierAudit.run(server);
  } catch (Exception e) { throw new IllegalStateException("Frontier gameplay audit failed", e); }
 }
 static void auditAdvancements(Object event){
  try{
   Object server=call(event,"getServer"),manager=null;
   for(Method m:server.getClass().getMethods())if(m.getParameterCount()==0 && m.getReturnType().getName().equals("net.minecraft.server.ServerAdvancementManager")){manager=m.invoke(server);break;}
   if(manager==null)throw new IllegalStateException("Advancement manager not found");
   Collection<?> all=null;
   for(Method m:manager.getClass().getMethods())if(m.getParameterCount()==0 && m.getReturnType()==Collection.class){all=(Collection<?>)m.invoke(manager);break;}
   if(all==null)throw new IllegalStateException("Advancement collection not found");
   Method id=null;for(Method m:Class.forName("net.minecraft.advancements.Advancement").getMethods())if(m.getParameterCount()==0 && m.getReturnType().getName().equals("net.minecraft.resources.ResourceLocation")){id=m;break;}
   List<String> ids=new ArrayList<>();for(Object advancement:all)ids.add(id.invoke(advancement).toString());Collections.sort(ids);Files.write(Path.of("advancement-registry.txt"),ids);
   System.out.println("ADVANCEMENT_AUDIT_COMPLETE advancements="+ids.size());
  }catch(Exception e){throw new IllegalStateException("Advancement audit failed",e);}
 }
 static void auditPortalPolicy(){
  try{
   Class<?> global=Class.forName("qouteall.imm_ptl.core.IPGlobal");
   for(String name:List.of("netherPortalMode","endPortalMode"))
    if(!global.getField(name).get(null).toString().equals("vanilla"))throw new IllegalStateException(name+" was not applied");
   Class<?> serializers=Class.forName("com.dairymoose.sd.SerializerDebugCommon");
   if(serializers.getField("ignoreSerializerError").getBoolean(null))throw new IllegalStateException("Serializer errors must not be suppressed");
   if(!serializers.getField("reorderClientIds").getBoolean(null))throw new IllegalStateException("Serializer synchronization is disabled");
   System.out.println("PORTAL_POLICY_AUDIT_COMPLETE");
  }catch(Exception e){throw new IllegalStateException("Portal policy audit failed",e);}
 }
 static void auditPortalChunkTracking(Object event){
  try{
   Object server=call(event,"getServer");
   Class<?> serverType=Class.forName("net.minecraft.server.MinecraftServer");
   Class<?> levelType=Class.forName("net.minecraft.server.level.ServerLevel");
   Class<?> playerType=Class.forName("net.minecraft.server.level.ServerPlayer");
   Class<?> profileType=Class.forName("com.mojang.authlib.GameProfile");
   Object level=serverType.getMethod("m_129783_").invoke(server);
   Object profile=profileType.getConstructor(UUID.class,String.class).newInstance(UUID.randomUUID(),"PortalAudit");
   Object player=playerType.getConstructor(serverType,levelType,profileType).newInstance(server,level,profile);
   Class<?> graph=Class.forName("qouteall.imm_ptl.core.chunk_loading.NewChunkTrackingGraph");
   Field playerInfo=graph.getDeclaredField("playerInfoMap");
   playerInfo.setAccessible(true);
   Map<?,?> trackedPlayers=(Map<?,?>)playerInfo.get(null);
   Method tickPlayer=graph.getDeclaredMethod("lambda$tick$12",long.class,playerType);
   tickPlayer.setAccessible(true);
   int entityId=(Integer)playerType.getMethod("m_19879_").invoke(player);
   long offCycleTick=(entityId+1L)%13;
   tickPlayer.invoke(null,offCycleTick,player);
   if(!trackedPlayers.containsKey(player))throw new IllegalStateException("Active player was skipped");
   trackedPlayers.remove(player);
   Class<?> reason=Class.forName("net.minecraft.world.entity.Entity$RemovalReason");
   playerType.getMethod("m_142467_",reason).invoke(player,reason.getField("KILLED").get(null));
   tickPlayer.invoke(null,offCycleTick,player);
   if(trackedPlayers.containsKey(player))throw new IllegalStateException("Removed player was tracked again");
   System.out.println("PORTAL_CHUNK_AUDIT_COMPLETE");
  }catch(Exception e){throw new IllegalStateException("Portal chunk tracking audit failed",e);}
 }
 static void auditCommands(Object event){
  try{
   Object server=call(event,"getServer");
   Object commands=null;
   for(Method method:server.getClass().getMethods()){
    if(method.getParameterCount()==0 && method.getReturnType().getName().equals("net.minecraft.commands.Commands")){
     commands=method.invoke(server);
     break;
    }
   }
   if(commands==null)throw new IllegalStateException("Server commands not found");
   Object dispatcher=null;
   for(Method method:commands.getClass().getMethods()){
    if(method.getParameterCount()==0 && method.getReturnType().getName().equals("com.mojang.brigadier.CommandDispatcher")){
     dispatcher=method.invoke(commands);
     break;
    }
   }
   if(dispatcher==null)throw new IllegalStateException("Command dispatcher not found");
   List<String> names=new ArrayList<>();
   for(Object node:(Iterable<?>)call(call(dispatcher,"getRoot"),"getChildren"))names.add(call(node,"getName").toString());
   Collections.sort(names);
   Files.write(Path.of("command-registry.txt"),names);
   System.out.println("COMMAND_AUDIT_COMPLETE commands="+names.size());
  }catch(Exception e){e.printStackTrace();}
 }
 static void auditRecipes(Object event){
  try{
   Object server=event.getClass().getMethod("getServer").invoke(event);Object manager=null;
   for(Method m:server.getClass().getMethods())if(m.getParameterCount()==0 && m.getReturnType().getName().equals("net.minecraft.world.item.crafting.RecipeManager")){manager=m.invoke(server);break;}
   if(manager==null)throw new IllegalStateException("Recipe manager not found");
   Collection<?> recipes=null;
   for(Method m:manager.getClass().getMethods())if(m.getParameterCount()==0 && m.getReturnType()==Collection.class){recipes=(Collection<?>)m.invoke(manager);break;}
   if(recipes==null)throw new IllegalStateException("Recipe collection not found");
   Method idMethod=null;for(Method m:Class.forName("net.minecraft.world.item.crafting.Recipe").getMethods())if(m.getParameterCount()==0 && m.getReturnType().getName().equals("net.minecraft.resources.ResourceLocation")){idMethod=m;break;}
   List<String> ids=new ArrayList<>();for(Object recipe:recipes)ids.add(idMethod.invoke(recipe).toString());Collections.sort(ids);Files.write(Path.of("recipe-registry.txt"),ids);
   System.out.println("RECIPE_AUDIT_COMPLETE recipes="+ids.size());
  }catch(Exception e){e.printStackTrace();}
 }
 static Object call(Object o,String name)throws Exception{return o.getClass().getMethod(name).invoke(o);}
 static void auditJourney(Object event){
  try{
   Object file=Class.forName("dev.ftb.mods.ftbquests.quest.ServerQuestFile").getField("INSTANCE").get(null);
   Class<?> base=Class.forName("dev.ftb.mods.ftbquests.quest.BaseQuestFile"),teamType=Class.forName("dev.ftb.mods.ftbquests.quest.TeamData"),objectType=Class.forName("dev.ftb.mods.ftbquests.quest.QuestObject"),questType=Class.forName("dev.ftb.mods.ftbquests.quest.Quest");
   Object team=teamType.getConstructor(UUID.class,base).newInstance(UUID.randomUUID(),file);
   Class<?> service=Class.forName("net.wombatcraft.smp.JourneyService");
   Object server=call(event,"getServer");
   List<?> catalog=(List<?>)service.getMethod("catalog",Class.forName("net.minecraft.server.MinecraftServer")).invoke(null,server);
   if(catalog.size()!=72)throw new IllegalStateException("Journey catalog size");
   Method recommend=service.getMethod("recommend",Object.class,Object.class,List.class,String.class);
   String before=call(team,"serializeNBT").toString();
   List<?> suggestions=(List<?>)recommend.invoke(null,file,team,catalog,"all");
   if(suggestions.isEmpty())throw new IllegalStateException("New team has no next objective");
   for(Object suggestion:suggestions){
    Object quest=file.getClass().getMethod("getQuest",long.class).invoke(file,Long.parseLong(call(suggestion,"id").toString(),16));
    if(!(Boolean)teamType.getMethod("canStartTasks",questType).invoke(team,quest))throw new IllegalStateException("Unreachable suggestion");
   }
   for(Object suggestion:(List<?>)recommend.invoke(null,file,team,catalog,"magic"))
    if(!call(suggestion,"branch").equals("magic"))throw new IllegalStateException("Branch filter failed");
   if(!before.equals(call(team,"serializeNBT").toString()))throw new IllegalStateException("Recommendations mutated progress");
   String completed=call(suggestions.get(0),"id").toString();
   teamType.getMethod("setCompleted",long.class,Date.class).invoke(team,Long.parseLong(completed,16),new Date());
   teamType.getMethod("clearCachedProgress").invoke(team);
   for(Object suggestion:(List<?>)recommend.invoke(null,file,team,catalog,"all"))
    if(call(suggestion,"id").equals(completed))throw new IllegalStateException("Completed quest suggested again");
   List<String> links=new ArrayList<>();
   for(Object chapter:(Iterable<?>)call(file,"getAllChapters"))for(Object link:(Iterable<?>)call(chapter,"getQuestLinks")){
    Object target=((Optional<?>)call(link,"getQuest")).orElseThrow();
    String id=call(target,"getCodeString").toString();
    if(target!=file.getClass().getMethod("getQuest",long.class).invoke(file,Long.parseLong(id,16)))throw new IllegalStateException("Link cloned quest");
    // QuestLink itself is a navigation wrapper with no task progress. Its
    // resolved Quest is the same instance used by the source chapter and UI.
    if(id.equals(completed)&&!(Boolean)teamType.getMethod("isCompleted",objectType).invoke(team,target))throw new IllegalStateException("Resolved link lost source completion");
    links.add(call(link,"getCodeString")+"\t"+id);
   }
   if(links.size()!=91)throw new IllegalStateException("Journey links missing: "+links.size());
   int simulated=1;
   for(int wave=0;wave<4096;wave++){
    List<?> next=(List<?>)recommend.invoke(null,file,team,catalog,"all");
    if(next.isEmpty())break;
    for(Object suggestion:next){
     teamType.getMethod("setCompleted",long.class,Date.class).invoke(team,Long.parseLong(call(suggestion,"id").toString(),16),new Date());
     simulated++;
    }
    teamType.getMethod("clearCachedProgress").invoke(team);
   }
   for(Object milestone:catalog){
    Object quest=file.getClass().getMethod("getQuest",long.class).invoke(file,Long.parseLong(call(milestone,"id").toString(),16));
    if(!(Boolean)teamType.getMethod("isCompleted",objectType).invoke(team,quest))throw new IllegalStateException("Guide cannot reach "+call(milestone,"title"));
   }
   Files.write(Path.of("quest-link-runtime-audit.tsv"),links);
   System.out.println("JOURNEY_AUDIT_COMPLETE milestones="+catalog.size()+" links="+links.size()+" simulated_steps="+simulated+" read_only=true");
  }catch(Exception e){throw new IllegalStateException("Journey audit failed",e);}
 }
 static void audit(){
  try{
   // Registry IDs validate item goals independently of lenient quest codecs.
   Object registry=Class.forName("net.minecraftforge.registries.ForgeRegistries").getField("ITEMS").get(null);
   java.util.Set<?> keys=(java.util.Set<?>)Class.forName("net.minecraftforge.registries.IForgeRegistry").getMethod("getKeys").invoke(registry);
   List<String> ids=new ArrayList<>();for(Object id:keys)ids.add(id.toString());Collections.sort(ids);Files.write(Path.of("item-registry.txt"),ids);
   System.out.println("REGISTRY_AUDIT_COMPLETE items="+ids.size());
   // Verify the sickle fix through the actual loaded item implementation.
   Class<?> location=Class.forName("net.minecraft.resources.ResourceLocation");
   Class<?> stack=Class.forName("net.minecraft.world.item.ItemStack");
   Method lookup=Class.forName("net.minecraftforge.registries.IForgeRegistry").getMethod("getValue",location);
   for(String name:List.of("silentwood_sickle","aurorian_stone_sickle","moonstone_sickle")){
    Object item=lookup.invoke(registry,location.getConstructor(String.class).newInstance("theaurorian:"+name));
    Object defaultStack=item.getClass().getMethod("m_7968_").invoke(item);
    if(item.getClass().getMethod("getCraftingRemainingItem",stack).invoke(item,defaultStack)==null)throw new IllegalStateException("Null sickle crafting remainder: "+name);
   }
   System.out.println("AURORIAN_CRAFTING_AUDIT_COMPLETE");
   Object entityRegistry=Class.forName("net.minecraftforge.registries.ForgeRegistries").getField("ENTITY_TYPES").get(null);
   Set<?> entityKeys=(Set<?>)Class.forName("net.minecraftforge.registries.IForgeRegistry").getMethod("getKeys").invoke(entityRegistry);
   List<String> entityIds=new ArrayList<>();for(Object id:entityKeys)entityIds.add(id.toString());Collections.sort(entityIds);Files.write(Path.of("entity-registry.txt"),entityIds);
   Object file=Class.forName("dev.ftb.mods.ftbquests.quest.ServerQuestFile").getField("INSTANCE").get(null);
   List<String> lines=new ArrayList<>();
   for(Object chapter:(Iterable<?>)call(file,"getAllChapters")){
    for(Object q:(Iterable<?>)call(chapter,"getQuests")){
     lines.add(call(q,"getCodeString")+"\t"+call(q,"getTasks")+"\t"+call(q,"getRewards"));
    }
   }
   Files.write(Path.of("quest-runtime-audit.tsv"),lines);
   System.out.println("QUEST_AUDIT_COMPLETE quests="+lines.size());
  }catch(Exception e){throw new IllegalStateException("Quest registry audit failed",e);}
 }
}
