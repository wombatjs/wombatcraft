package net.wombatcraft.smp;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
final class CandidateCatalog {
 static final Map<String,JsonObject> ALL=load();
 private static Map<String,JsonObject> load() {
  try(var stream=Objects.requireNonNull(CandidateCatalog.class.getResourceAsStream("/data/wombatcraft_smp/civic/candidates.json"));var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)) {
   Map<String,JsonObject> result=new LinkedHashMap<>();
   for(var element:JsonParser.parseReader(reader).getAsJsonArray()) {
    JsonObject c=element.getAsJsonObject();String id=c.get("id").getAsString();
    if(!id.matches("[a-z_]+")||result.put(id,c)!=null)throw new IllegalArgumentException("Duplicate candidate");
    int bonus=c.get("bonus_percent").getAsInt();if(bonus<0||bonus>30)throw new IllegalArgumentException("Invalid civic bonus");
   }
   if(result.size()!=50)throw new IllegalArgumentException("Expected 50 candidates");return Collections.unmodifiableMap(result);
  }catch(IOException e){throw new IllegalStateException("Cannot load candidates",e);}
 }
 static int bonus(String mayor,String scope,Set<String> tags,String item) {
  JsonObject c=ALL.get(mayor);if(c==null)return 0;
  String restricted=c.get("scope").getAsString(),tag=c.get("tag").getAsString();int bonus=c.get("bonus_percent").getAsInt();
  if(!restricted.isEmpty())return restricted.equals(scope)?bonus:0;
  if(!tag.isEmpty())return tags.contains(tag)?bonus:0;
  if(!item.startsWith(c.get("namespace").getAsString()+":"))return 0;
  JsonArray filters=c.getAsJsonArray("item_contains");
  if(!filters.isEmpty()&&java.util.stream.StreamSupport.stream(filters.spliterator(),false).noneMatch(f->item.contains(f.getAsString())))return 0;
  return bonus;
 }
}
