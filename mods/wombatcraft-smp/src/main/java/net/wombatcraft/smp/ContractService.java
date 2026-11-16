package net.wombatcraft.smp;

import com.google.gson.*;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.registries.ForgeRegistries;
import net.wombatcraft.smp.rules.ContractLedger;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

final class ContractService {
    private static final String MARKER="wombatcraft_contract_operation";
    private static ContractService instance;
    private final MinecraftServer server;
    private final ContractLedger ledger;
    private final List<JsonObject> definitions=new ArrayList<>();
    private final Map<UUID,Long> lastAction=new HashMap<>();
    private ContractService(MinecraftServer server)throws IOException {
        this.server=server;
        ledger=new ContractLedger(server.getWorldPath(LevelResource.ROOT).resolve("wombatcraft/contracts.json"));
        try(var stream=Objects.requireNonNull(getClass().getResourceAsStream("/data/wombatcraft_smp/civic/contracts.json"));var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
            Set<String> ids=new HashSet<>();
            for(var element:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("contracts")){
                JsonObject d=element.getAsJsonObject();
                if(!ids.add(d.get("id").getAsString()))throw new IOException("Duplicate contract");
                for(var entry:d.getAsJsonObject("requirements").entrySet())
                    if(!ForgeRegistries.ITEMS.containsKey(new ResourceLocation(entry.getKey()))||entry.getValue().getAsLong()<=0)throw new IOException("Invalid contract item: "+entry.getKey());
                definitions.add(d);
            }
        }
        for(var operation:ledger.pending())ledger.settle(operation.player(),savedMarker(operation.player()));
    }
    static ContractService get(MinecraftServer server)throws IOException {
        if(instance==null||instance.server!=server)instance=new ContractService(server);return instance;
    }
    static void clear(){instance=null;}
    private String savedMarker(String player)throws IOException {
        Path path=server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player+".dat");
        if(!Files.exists(path))return "";
        return NbtIo.readCompressed(path.toFile()).getCompound("ForgeData").getString(MARKER);
    }
    private UUID faction(ServerPlayer player){return Factions.playerTeam(player).filter(t->t.isPartyTeam()).map(t->t.getId()).orElse(null);}
    private long period(JsonObject d){return Math.floorDiv(System.currentTimeMillis(),d.get("scope").getAsString().equals("PERSONAL")?86_400_000L:604_800_000L);}
    private String owner(JsonObject d,ServerPlayer p){return switch(d.get("scope").getAsString()){
        case "PERSONAL"->p.getUUID().toString();case "COMMUNITY"->"town";
        case "FACTION"->Objects.requireNonNull(faction(p),"Join a faction for factory orders").toString();default->throw new IllegalArgumentException("Unknown contract scope");};}
    private String key(JsonObject d,ServerPlayer p){return d.get("id").getAsString()+"/"+period(d)+"/"+owner(d,p);}
    private List<JsonObject> available(ServerPlayer p){
        List<JsonObject> result=new ArrayList<>();
        for(String scope:List.of("PERSONAL","COMMUNITY","FACTION")){
            if(scope.equals("FACTION")&&faction(p)==null)continue;
            List<JsonObject> pool=definitions.stream().filter(d->d.get("scope").getAsString().equals(scope)).toList();
            if(pool.isEmpty())continue;
            long period=period(pool.get(0));String owner=owner(pool.get(0),p);
            // Stable across restarts; a new period rotates the slate without a ticking scheduler.
            int start=Math.floorMod(Objects.hash(owner,scope)+period,pool.size());
            List<JsonObject> selected=new ArrayList<>();
            if(WombatcraftSmp.ELECTIONS.get()){
                var civic=CivicState.get(server);civic.advance();String mayor=civic.election.mayor();
                for(JsonObject d:pool){Set<String> tags=new HashSet<>();d.getAsJsonArray("tags").forEach(t->tags.add(t.getAsString()));
                    if(d.getAsJsonObject("requirements").keySet().stream().anyMatch(item->CandidateCatalog.bonus(mayor,scope,tags,item)>0)){selected.add(d);break;}}
            }
            for(int i=0;selected.size()<Math.min(scope.equals("COMMUNITY")?1:3,pool.size())&&i<pool.size();i++){
                JsonObject d=pool.get((start+i)%pool.size());if(!selected.contains(d))selected.add(d);
            }
            result.addAll(selected);
        }
        return result;
    }
    JsonObject view(ServerPlayer p,int page){
        JsonObject out=new JsonObject();out.addProperty("balance",ledger.balance(p.getUUID().toString())/ContractLedger.COIN);
        List<JsonObject> rows=new ArrayList<>();
        for(JsonObject d:available(p))for(var requirement:d.getAsJsonObject("requirements").entrySet()){
            String item=requirement.getKey();JsonObject row=new JsonObject();row.addProperty("id",d.get("id").getAsString());row.addProperty("item",item);
            row.addProperty("name",ForgeRegistries.ITEMS.getValue(new ResourceLocation(item)).getDescription().getString());
            row.addProperty("scope",d.get("scope").getAsString());row.addProperty("period",period(d));
            row.addProperty("required",requirement.getValue().getAsLong());row.addProperty("delivered",ledger.delivered(key(d,p),item));
            row.addProperty("reward",d.get("reward_copper").getAsLong());rows.add(row);
        }
        out.addProperty("rows",rows.size());JsonArray selected=new JsonArray();rows.stream().skip((long)page*4).limit(4).forEach(selected::add);out.add("orders",selected);return out;
    }
    private void check(ServerPlayer p)throws IOException {
        if(!WombatcraftSmp.CONTRACTS.get()||!SmpNetwork.atSpawn(p))throw new IllegalStateException("Visit the town contract desk at spawn");
        long now=System.nanoTime();if(now-lastAction.getOrDefault(p.getUUID(),0L)<1_000_000_000L)throw new IllegalStateException("Please wait a moment before another delivery");lastAction.put(p.getUUID(),now);
        if(ledger.pending(p.getUUID().toString())!=null){
            savePlayer(p);
            String saved=savedMarker(p.getUUID().toString());
            if(!saved.equals(p.getPersistentData().getString(MARKER)))throw new IOException("Player save has not completed");
            ledger.settle(p.getUUID().toString(),saved);
        }
    }
    private static final java.lang.reflect.Method SAVE_PLAYER=net.minecraftforge.fml.util.ObfuscationReflectionHelper.findMethod(net.minecraft.server.players.PlayerList.class,"m_6765_",ServerPlayer.class);
    private void savePlayer(ServerPlayer p)throws IOException {
        try{SAVE_PLAYER.invoke(server.getPlayerList(),p);}catch(ReflectiveOperationException e){throw new IOException("Cannot save contract transaction",e);}
    }
    private void finish(ServerPlayer p,ContractLedger.Operation operation)throws IOException {
        p.getPersistentData().putString(MARKER,operation.id());p.getInventory().setChanged();p.containerMenu.broadcastChanges();
        savePlayer(p);
        if(!savedMarker(p.getUUID().toString()).equals(operation.id()))throw new IOException("Delivery is waiting for a player save; do not repeat it");
        ledger.settle(operation.player(),operation.id());
    }
    void deliver(ServerPlayer p,String target,long expectedPeriod)throws IOException {
        check(p);String[] parts=target.split("\\|",2);if(parts.length!=2)throw new IllegalArgumentException("Invalid order");
        JsonObject d=available(p).stream().filter(x->x.get("id").getAsString().equals(parts[0])).findFirst().orElseThrow(()->new IllegalStateException("That order has expired"));
        if(period(d)!=expectedPeriod)throw new IllegalStateException("That order has expired");
        String item=parts[1];JsonObject requirements=d.getAsJsonObject("requirements");if(!requirements.has(item))throw new IllegalArgumentException("Wrong delivery item");
        long remaining=requirements.get(item).getAsLong()-ledger.delivered(key(d,p),item);
        Item type=ForgeRegistries.ITEMS.getValue(new ResourceLocation(item));int available=0;
        for(ItemStack stack:p.getInventory().items)if(stack.is(type)&&!stack.hasTag())available+=stack.getCount();
        int count=(int)Math.min(remaining,Math.min(2304,available));if(count<=0)throw new IllegalStateException("Bring unmodified items for an unfinished order");
        Set<String> tags=new HashSet<>();d.getAsJsonArray("tags").forEach(t->tags.add(t.getAsString()));
        int bonus=0;if(WombatcraftSmp.ELECTIONS.get()){var civic=CivicState.get(server);civic.advance();bonus=CandidateCatalog.bonus(civic.election.mayor(),d.get("scope").getAsString(),tags,item);}
        long credit=Math.multiplyExact(d.get("reward_copper").getAsLong(),ContractLedger.COIN)*count*(100+bonus)/(100L*requirements.size()*requirements.get(item).getAsLong());
        var operation=ledger.prepare(p.getUUID().toString(),key(d,p),item,count,credit);
        int take=count;for(ItemStack stack:p.getInventory().items)if(stack.is(type)&&!stack.hasTag()){int n=Math.min(take,stack.getCount());stack.shrink(n);take-=n;if(take==0)break;}
        finish(p,operation);
    }
    void collect(ServerPlayer p)throws IOException {
        check(p);Item coin=ForgeRegistries.ITEMS.getValue(new ResourceLocation("lightmanscurrency:coin_copper"));
        if(coin==null||coin==Items.AIR)throw new IllegalStateException("Currency is unavailable");
        int coins=(int)Math.min(256,ledger.balance(p.getUUID().toString())/ContractLedger.COIN),space=0;
        for(ItemStack stack:p.getInventory().items)if(stack.isEmpty())space+=64;else if(stack.is(coin)&&!stack.hasTag())space+=stack.getMaxStackSize()-stack.getCount();
        coins=Math.min(coins,space);if(coins==0)throw new IllegalStateException("No whole coins to collect, or your inventory is full");
        var operation=ledger.prepare(p.getUUID().toString(),"","",0,-coins*ContractLedger.COIN);
        int give=coins;
        for(ItemStack stack:p.getInventory().items)if(stack.is(coin)&&!stack.hasTag()){int n=Math.min(give,stack.getMaxStackSize()-stack.getCount());stack.grow(n);give-=n;}
        for(int i=0;i<p.getInventory().items.size()&&give>0;i++)if(p.getInventory().items.get(i).isEmpty()){int n=Math.min(64,give);p.getInventory().items.set(i,new ItemStack(coin,n));give-=n;}
        finish(p,operation);
    }
}
