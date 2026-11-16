package net.wombatcraft.smp;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.wombatcraft.smp.rules.Election;
import java.util.*;

final class CivicState extends SavedData {
    static final List<String> CANDIDATES=List.of("brasswick","flux","willow","briar","pebble","hearth");
    Election election;
    CivicState(){election=new Election(System.currentTimeMillis(),CANDIDATES,"hearth");setDirty();}
    static CivicState get(MinecraftServer server){
        return server.overworld().getDataStorage().computeIfAbsent(CivicState::load,CivicState::new,"wombatcraft_civic");
    }
    static CivicState load(CompoundTag tag){
        if(tag.getInt("schema")!=1)throw new IllegalStateException("Unsupported civic data schema; refusing reset");
        Map<UUID,String> votes=new HashMap<>();ListTag list=tag.getList("votes",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++){CompoundTag v=list.getCompound(i);votes.put(v.getUUID("player"),v.getString("candidate"));}
        CivicState state=new CivicState();state.election=Election.restore(new Election.Snapshot(tag.getLong("epoch"),tag.getLong("term"),tag.getString("mayor"),votes),CANDIDATES);return state;
    }
    void advance(){if(election.advance(System.currentTimeMillis()))setDirty();}
    @Override public CompoundTag save(CompoundTag tag){
        var s=election.snapshot();tag.putInt("schema",1);tag.putLong("epoch",s.epoch());tag.putLong("term",s.term());tag.putString("mayor",s.mayor());
        ListTag votes=new ListTag();s.votes().forEach((id,c)->{CompoundTag v=new CompoundTag();v.putUUID("player",id);v.putString("candidate",c);votes.add(v);});tag.put("votes",votes);return tag;
    }
}
