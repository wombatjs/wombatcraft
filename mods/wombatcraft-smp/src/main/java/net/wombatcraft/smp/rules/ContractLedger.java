package net.wombatcraft.smp.rules;

import com.google.gson.Gson;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Write-ahead ledger. The adapter durably saves the player's operation marker before commit. */
public final class ContractLedger {
    public static final long COIN=1_000_000L;
    public record Operation(String id,String player,String key,String item,long count,long credit) {}
    private static final Gson GSON=new Gson();
    private static final class Data {
        int schema=1;
        Map<String,Long> credits=new HashMap<>();
        Map<String,Map<String,Long>> progress=new HashMap<>();
        Map<String,Operation> pending=new HashMap<>();
        Map<String,Long> factoryEarnings=new HashMap<>();
    }
    private final Path path;
    private Data data;
    private boolean failed;
    public ContractLedger(Path path)throws IOException {
        this.path=path;
        data=Files.exists(path)?GSON.fromJson(Files.readString(path),Data.class):new Data();
        if(data==null||data.schema!=1||data.credits==null||data.progress==null||data.pending==null)throw new IOException("Invalid contract ledger; refusing reset");
        if(data.credits.values().stream().anyMatch(v->v==null||v<0))throw new IOException("Invalid contract balance");
    }
    public long balance(String player){return data.credits.getOrDefault(player,0L);}
    public long delivered(String key,String item){return data.progress.getOrDefault(key,Map.of()).getOrDefault(item,0L);}
    public List<Operation> pending(){return List.copyOf(data.pending.values());}
    public Operation pending(String player){return data.pending.get(player);}
    public Operation prepare(String player,String key,String item,long count,long credit)throws IOException {
        if(failed)throw new IOException("Contract storage needs recovery");
        UUID.fromString(player);
        if(data.pending.containsKey(player))throw new IllegalStateException("Your previous delivery is still being saved");
        if(count<0||count>2304||credit<0&&credit==Long.MIN_VALUE)throw new IllegalArgumentException("Invalid delivery");
        if(key.isEmpty()&&(count!=0||credit>=0||balance(player)<-credit))throw new IllegalArgumentException("Invalid payout");
        if(!key.isEmpty()&&(count==0||credit<0||item.isEmpty()))throw new IllegalArgumentException("Invalid delivery");
        if(key.startsWith("factory_")&&data.factoryEarnings.getOrDefault(factoryBudget(player,key),0L)+credit>4096*COIN)
            throw new IllegalStateException("Your weekly factory earnings limit is reached; changing factions does not reset it");
        Operation op=new Operation(UUID.randomUUID().toString(),player,key,item,count,credit);
        data.pending.put(player,op);save();return op;
    }
    public boolean settle(String player,String savedOperation)throws IOException {
        Operation op=data.pending.get(player);if(op==null)return false;
        if(op.id.equals(savedOperation)) {
            long balance=Math.addExact(balance(player),op.credit);
            if(balance<0)throw new IOException("Contract balance underflow");
            if(!op.key.isEmpty())data.progress.computeIfAbsent(op.key,k->new HashMap<>()).merge(op.item,op.count,Math::addExact);
            data.credits.put(player,balance);
            if(op.key.startsWith("factory_"))data.factoryEarnings.merge(factoryBudget(player,op.key),op.credit,Math::addExact);
        }
        data.pending.remove(player);save();return true;
    }
    private static String factoryBudget(String player,String key){return player+"/"+key.split("/",3)[1];}
    private void save()throws IOException {
        try {
            Files.createDirectories(path.getParent());Path temp=path.resolveSibling(path.getFileName()+".tmp");
            byte[] bytes=GSON.toJson(data).getBytes(StandardCharsets.UTF_8);
            try(FileChannel channel=FileChannel.open(temp,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)){
                ByteBuffer buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())channel.write(buffer);channel.force(true);
            }
            Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }catch(IOException e){failed=true;throw e;}
    }
}
