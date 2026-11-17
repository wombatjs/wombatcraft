package net.wombatcraft.smp;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import java.util.ArrayList;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/** Bounded immigration into actual generated villages, independent of faction progression. */
public final class VillagePopulation {
    private static final int INTERVAL=1200; // One minute of server ticks, not a scan per villager.
    private static final int DAILY_ARRIVALS=8;
    private VillagePopulation() {}
    public static void register() { MinecraftForge.EVENT_BUS.addListener(VillagePopulation::tick); }

    private static boolean village(ServerLevel level, Structure structure) {
        var registry=level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var id=registry.getKey(structure);
        if (id==null || id.getPath().contains("zombie") || id.getPath().contains("abandoned")) return false;
        return registry.getResourceKey(structure).map(k->registry.getHolderOrThrow(k).is(StructureTags.VILLAGE)).orElse(false)
            || ((id.getNamespace().equals("ctov") || id.getNamespace().equals("towns_and_towers")) && id.getPath().contains("village"));
    }

    private static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase!=TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
            || !WombatcraftSmp.VILLAGE_SETTLERS.get() || level.getGameTime()%INTERVAL!=0
            || !level.isDay() || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
        var visited=new HashSet<String>();
        for (var player:level.players()) {
            if (player.isSpectator()) continue;
            var chunk=level.getChunkSource().getChunkNow(player.chunkPosition().x,player.chunkPosition().z);
            if (chunk==null) continue;
            // Read only resident chunks. StructureManager lookups can load distant start chunks.
            for (var entry:chunk.getAllReferences().entrySet()) {
                if (!village(level,entry.getKey())) continue;
                for (long reference:entry.getValue()) {
                    var startChunk=level.getChunkSource().getChunkNow(ChunkPos.getX(reference),ChunkPos.getZ(reference));
                    if (startChunk==null) continue;
                    var start=startChunk.getAllStarts().get(entry.getKey());
                    if (start==null || !start.isValid()) continue;
                    var box=start.getBoundingBox();
                    if (player.getX()<box.minX()-16 || player.getX()>box.maxX()+16
                        || player.getZ()<box.minZ()-16 || player.getZ()>box.maxZ()+16) continue;
                    String key=level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(entry.getKey())+"/"+reference;
                    if (visited.add(key)) settle(level,start,key);
                }
            }
        }
    }

    private static void settle(ServerLevel level, StructureStart start, String key) {
        var box=start.getBoundingBox();
        if (box.getXSpan()>256 || box.getZSpan()>256) return;
        // Vanilla abandoned villages use zombie template pieces under the ordinary village structure ID.
        for (var piece:start.getPieces()) if (piece instanceof PoolElementStructurePiece pool) {
            String template=pool.getElement().toString();
            if (template.contains("/zombie/") || template.contains("abandoned")) return;
        }
        var center=box.getCenter();
        if (level.getRaidAt(center)!=null) return;
        int minX=(box.minX()-16)>>4, maxX=(box.maxX()+16)>>4;
        int minZ=(box.minZ()-16)>>4, maxZ=(box.maxZ()+16)>>4;
        // Count only after the whole village and its immediate outskirts have loaded entities.
        // This prevents duplicate arrivals while residents are simply in unloaded houses.
        for (int x=minX;x<=maxX;x++) for (int z=minZ;z<=maxZ;z++)
            if (level.getChunkSource().getChunkNow(x,z)==null || !level.areEntitiesLoaded(ChunkPos.asLong(x,z))) return;
        var ledger=level.getDataStorage().computeIfAbsent(Ledger::load,Ledger::new,"wombatcraft_village_population");
        CompoundTag record=ledger.villages.getCompound(key);
        long now=level.getGameTime(), day=now/24000;
        if (record.getLong("next")>now) return;
        if (record.getLong("day")!=day) { record.putLong("day",day); record.putInt("arrivals",0); }
        record.putLong("next",now+INTERVAL);
        ledger.villages.put(key,record); ledger.setDirty();
        int allowance=DAILY_ARRIVALS-record.getInt("arrivals");
        if (allowance<=0) return;

        var homes=new ArrayList<BlockPos>();
        for (int x=minX;x<=maxX;x++) for (int z=minZ;z<=maxZ;z++) {
            level.getPoiManager().getInChunk(h->h.is(PoiTypes.HOME),new ChunkPos(x,z),PoiManager.Occupancy.ANY)
                .map(p->p.getPos()).filter(box::isInside)
                .filter(p->level.getBlockState(p).getBlock() instanceof BedBlock)
                .forEach(homes::add);
        }
        int target=Math.min(WombatcraftSmp.VILLAGE_RESIDENT_TARGET.get(),homes.size());
        if (target<2) return;
        var area=new AABB(box.minX()-16,box.minY()-16,box.minZ()-16,box.maxX()+17,box.maxY()+17,box.maxZ()+17);
        int population=level.getEntitiesOfClass(Villager.class,area).size()
            +level.getEntitiesOfClass(ZombieVillager.class,area).size(); // Reserve room for residents that can be cured.
        int needed=Math.min(2,Math.min(allowance,target-population));
        if (needed<=0) return;
        // Prefer unoccupied beds; existing residents keep their homes, trades, names and professions.
        var vacant=new HashSet<BlockPos>();
        for (int x=minX;x<=maxX;x++) for (int z=minZ;z<=maxZ;z++)
            level.getPoiManager().getInChunk(h->h.is(PoiTypes.HOME),new ChunkPos(x,z),PoiManager.Occupancy.HAS_SPACE)
                .map(p->p.getPos()).filter(box::isInside).forEach(vacant::add);
        java.util.Collections.shuffle(homes,new java.util.Random(level.random.nextLong()));
        int added=0;
        for (var home:homes) {
            if (added>=needed) break;
            if (!vacant.contains(home) || claimed(level,home)) continue;
            var position=landing(level,home,area);
            if (position==null) continue;
            var villager=EntityType.VILLAGER.create(level);
            if (villager==null) break;
            villager.moveTo(position.getX()+0.5,position.getY(),position.getZ()+0.5,level.random.nextFloat()*360,0);
            villager.finalizeSpawn(level,level.getCurrentDifficultyAt(position),MobSpawnType.EVENT,null,null);
            villager.setVillagerData(villager.getVillagerData().setType(VillagerType.byBiome(level.getBiome(position))));
            villager.setPersistenceRequired();
            villager.addTag("wombatcraft_settler");
            if (level.addFreshEntity(villager)) added++;
        }
        if (added>0) {
            record.putInt("arrivals",record.getInt("arrivals")+added); ledger.setDirty();
            WombatcraftSmp.LOGGER.info("Village settlers: {} arrivals in {} at {}, {}; population {} -> {}, target {}",
                added,level.dimension().location(),center.getX(),center.getZ(),population,population+added,target);
        }
    }

    private static boolean claimed(ServerLevel level, BlockPos pos) {
        var api=FTBChunksAPI.api();
        return !api.isManagerLoaded() || api.getManager().getChunk(new ChunkDimPos(level.dimension(),pos.getX()>>4,pos.getZ()>>4))!=null;
    }
    private static BlockPos landing(ServerLevel level, BlockPos home, AABB village) {
        for (int dy=0;dy<=1;dy++) for (int dx=-2;dx<=2;dx++) for (int dz=-2;dz<=2;dz++) {
            var p=home.offset(dx,dy,dz);
            if (!village.contains(p.getX()+0.5,p.getY(),p.getZ()+0.5) || claimed(level,p)) continue;
            var floor=level.getBlockState(p.below());
            if (!floor.isFaceSturdy(level,p.below(),Direction.UP) || floor.is(Blocks.MAGMA_BLOCK)
                || floor.is(Blocks.CACTUS) || floor.is(Blocks.CAMPFIRE) || floor.is(Blocks.SOUL_CAMPFIRE)) continue;
            if (!level.getFluidState(p).isEmpty() || !level.getFluidState(p.above()).isEmpty()) continue;
            if (hazard(level,p) || hazard(level,p.above())) continue;
            var body=new AABB(p.getX()+0.2,p.getY(),p.getZ()+0.2,p.getX()+0.8,p.getY()+1.95,p.getZ()+0.8);
            if (!level.noCollision(body) || !level.getEntitiesOfClass(Monster.class,body.inflate(12,6,12)).isEmpty()) continue;
            if (!level.getEntitiesOfClass(Villager.class,body.inflate(1)).isEmpty()) continue;
            return p;
        }
        return null;
    }

    private static boolean hazard(ServerLevel level,BlockPos pos) {
        var state=level.getBlockState(pos);
        return state.is(BlockTags.FIRE) || state.is(Blocks.SWEET_BERRY_BUSH)
            || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.POWDER_SNOW);
    }

    private static final class Ledger extends SavedData {
        private CompoundTag villages=new CompoundTag();
        static Ledger load(CompoundTag tag) { var data=new Ledger(); data.villages=tag.getCompound("villages"); return data; }
        @Override public CompoundTag save(CompoundTag tag) { tag.put("villages",villages); return tag; }
    }
}
