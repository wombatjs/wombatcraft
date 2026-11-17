package net.wombatcraft.smp;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** World-owned data, never a process-global directory. Coordinates identify a site; UUID identifies a pad. */
public final class PadRegistry extends SavedData {
    public static final int MAX_PADS=4096;
    public static final class Pad {
        public final UUID id, owner;
        public final String dimension, ownerName;
        public final BlockPos pos;
        public String name;
        public boolean visible;
        public long revision;
        Pad(UUID id, UUID owner, String ownerName, String dimension, BlockPos pos, String name) {
            this.id=id;this.owner=owner;this.ownerName=ownerName;this.dimension=dimension;this.pos=pos.immutable();this.name=name;
        }
    }
    public record Selection(UUID pad, String dimension, BlockPos pos) {}
    final Map<UUID,Pad> pads=new LinkedHashMap<>();
    final Map<String,Selection> selections=new HashMap<>();
    final Map<UUID,String> reservations=new HashMap<>();
    public static PadRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PadRegistry::load,PadRegistry::new,"wombatcraft_public_pads");
    }
    Pad at(String dimension,BlockPos pos) {
        return pads.values().stream().filter(p->p.dimension.equals(dimension)&&p.pos.equals(pos)).findFirst().orElse(null);
    }
    Pad add(UUID owner,String ownerName,String dimension,BlockPos pos) {
        Pad existing=at(dimension,pos);if(existing!=null)return existing;
        if(pads.size()>=MAX_PADS)throw new IllegalStateException("Landing-pad directory is full. Ask an administrator for help.");
        Pad pad=new Pad(UUID.randomUUID(),owner,ownerName,dimension,pos,"Landing Pad");pads.put(pad.id,pad);setDirty();return pad;
    }
    void remove(Pad pad) {pads.remove(pad.id);reservations.remove(pad.id);setDirty();}
    boolean reserve(UUID pad,String ship) {
        String other=reservations.get(pad);if(other!=null&&!other.equals(ship))return false;
        release(ship);reservations.put(pad,ship);setDirty();return true;
    }
    void release(String ship) {if(reservations.values().removeIf(ship::equals))setDirty();}
    static String validName(String raw) {
        String name=raw.strip();
        if(name.isEmpty()||name.length()>40||name.codePoints().anyMatch(c->Character.isISOControl(c)||c==0xA7||Character.getType(c)==Character.FORMAT))
            throw new IllegalArgumentException("Use a name of 1–40 plain characters.");
        return name;
    }
    static PadRegistry load(CompoundTag root) {
        PadRegistry data=new PadRegistry();
        for(var value:root.getList("pads",10)) {
            CompoundTag t=(CompoundTag)value;
            try {
                Pad p=new Pad(t.getUUID("id"),t.getUUID("owner"),t.getString("ownerName"),t.getString("dimension"),BlockPos.of(t.getLong("pos")),validName(t.getString("name")));
                p.visible=t.getBoolean("visible");p.revision=t.getLong("revision");data.pads.put(p.id,p);
            }catch(IllegalArgumentException ex){WombatcraftSmp.LOGGER.warn("Ignoring invalid landing pad record",ex);}
        }
        for(var value:root.getList("selections",10)) {
            CompoundTag t=(CompoundTag)value;
            if(t.hasUUID("pad"))data.selections.put(t.getString("ship"),new Selection(t.getUUID("pad"),t.getString("dimension"),BlockPos.of(t.getLong("pos"))));
        }
        for(var value:root.getList("reservations",10)) {
            CompoundTag t=(CompoundTag)value;if(t.hasUUID("pad")&&data.pads.containsKey(t.getUUID("pad")))data.reservations.put(t.getUUID("pad"),t.getString("ship"));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag root) {
        ListTag list=new ListTag();
        for(Pad p:pads.values()) {
            CompoundTag t=new CompoundTag();t.putUUID("id",p.id);t.putUUID("owner",p.owner);t.putString("ownerName",p.ownerName);t.putString("dimension",p.dimension);t.putLong("pos",p.pos.asLong());t.putString("name",p.name);t.putBoolean("visible",p.visible);t.putLong("revision",p.revision);list.add(t);
        }root.put("pads",list);
        ListTag selectionsTag=new ListTag();selections.forEach((ship,s)->{CompoundTag t=new CompoundTag();t.putString("ship",ship);t.putUUID("pad",s.pad);t.putString("dimension",s.dimension);t.putLong("pos",s.pos.asLong());selectionsTag.add(t);});root.put("selections",selectionsTag);
        ListTag reservationsTag=new ListTag();reservations.forEach((pad,ship)->{CompoundTag t=new CompoundTag();t.putUUID("pad",pad);t.putString("ship",ship);reservationsTag.add(t);});root.put("reservations",reservationsTag);
        return root;
    }
}
