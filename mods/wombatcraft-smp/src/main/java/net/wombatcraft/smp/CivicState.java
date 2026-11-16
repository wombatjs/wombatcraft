package net.wombatcraft.smp;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.wombatcraft.smp.rules.CivicElection;
import java.util.*;
final class CivicState extends SavedData {
 static final List<String> CANDIDATES=List.copyOf(CandidateCatalog.ALL.keySet());
 CivicElection election;
 CompoundTag legacy=new CompoundTag();
 CivicState(){election=new CivicElection(System.currentTimeMillis(),CANDIDATES,"hearth");setDirty();}
 static CivicState get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(CivicState::load,CivicState::new,"wombatcraft_civic");}
 static CivicState load(CompoundTag tag){
  CivicState state=new CivicState();
  if(tag.getInt("schema")==1){
   // Preserve old staging ballots; never reinterpret a single-stage vote as a primary or final.
   state.legacy=tag.copy();String mayor=tag.getString("mayor");
   state.election=new CivicElection(System.currentTimeMillis(),CANDIDATES,CANDIDATES.contains(mayor)?mayor:"hearth");return state;
  }
  if(tag.getInt("schema")!=2)throw new IllegalStateException("Unsupported civic data schema; refusing reset");
  List<String> finalists=new ArrayList<>();ListTag f=tag.getList("finalists",Tag.TAG_STRING);
  for(int i=0;i<f.size();i++)finalists.add(f.getString(i));
  state.legacy=tag.getCompound("legacy").copy();
  state.election=CivicElection.restore(new CivicElection.Snapshot(tag.getLong("epoch"),tag.getLong("cycle"),CivicElection.Phase.valueOf(tag.getString("phase")),tag.getString("mayor"),finalists,readVotes(tag,"primary_votes"),readVotes(tag,"final_votes")),CANDIDATES);return state;
 }
 private static Map<UUID,String> readVotes(CompoundTag tag,String key){
  Map<UUID,String> votes=new HashMap<>();ListTag list=tag.getList(key,Tag.TAG_COMPOUND);
  for(int i=0;i<list.size();i++){CompoundTag v=list.getCompound(i);if(votes.put(v.getUUID("player"),v.getString("candidate"))!=null)throw new IllegalStateException("Duplicate ballot");}return votes;
 }
 private static ListTag votes(Map<UUID,String> map){ListTag list=new ListTag();map.forEach((id,c)->{CompoundTag v=new CompoundTag();v.putUUID("player",id);v.putString("candidate",c);list.add(v);});return list;}
 void advance(){if(election.advance(System.currentTimeMillis()))setDirty();}
 @Override public CompoundTag save(CompoundTag tag){
  var s=election.snapshot();tag.putInt("schema",2);tag.putLong("epoch",s.epoch());tag.putLong("cycle",s.cycle());tag.putString("phase",s.phase().name());tag.putString("mayor",s.mayor());
  ListTag finalists=new ListTag();s.finalists().forEach(c->finalists.add(StringTag.valueOf(c)));tag.put("finalists",finalists);
  tag.put("primary_votes",votes(s.primaryVotes()));tag.put("final_votes",votes(s.finalVotes()));if(!legacy.isEmpty())tag.put("legacy",legacy.copy());return tag;
 }
}
