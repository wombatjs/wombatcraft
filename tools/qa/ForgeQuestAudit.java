package local.qa;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
@Mod("forge_quest_audit")
public class ForgeQuestAudit {
 public ForgeQuestAudit(){
  try { Object bus=Class.forName("net.minecraftforge.common.MinecraftForge").getField("EVENT_BUS").get(null);Class.forName("net.minecraftforge.eventbus.api.IEventBus").getMethod("register",Object.class).invoke(bus,this); } catch(Exception e){throw new RuntimeException(e);}
 }
 @SubscribeEvent public void started(ServerStartedEvent event){audit();auditRecipes(event);auditCommands(event);}
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
 static void audit(){
  try{
   // Registry IDs validate item goals independently of lenient quest codecs.
   Object registry=Class.forName("net.minecraftforge.registries.ForgeRegistries").getField("ITEMS").get(null);
   java.util.Set<?> keys=(java.util.Set<?>)Class.forName("net.minecraftforge.registries.IForgeRegistry").getMethod("getKeys").invoke(registry);
   List<String> ids=new ArrayList<>();for(Object id:keys)ids.add(id.toString());Collections.sort(ids);Files.write(Path.of("item-registry.txt"),ids);
   System.out.println("REGISTRY_AUDIT_COMPLETE items="+ids.size());
   Object file=Class.forName("dev.ftb.mods.ftbquests.quest.ServerQuestFile").getField("INSTANCE").get(null);
   List<String> lines=new ArrayList<>();
   for(Object chapter:(Iterable<?>)call(file,"getAllChapters")){
    for(Object q:(Iterable<?>)call(chapter,"getQuests")){
     lines.add(call(q,"getCodeString")+"\t"+call(q,"getTasks")+"\t"+call(q,"getRewards"));
    }
   }
   Files.write(Path.of("quest-runtime-audit.tsv"),lines);
   System.out.println("QUEST_AUDIT_COMPLETE quests="+lines.size());
  }catch(Exception e){e.printStackTrace();}
 }
}
