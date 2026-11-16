package net.wombatcraft.smp.rules;

import java.util.*;

/** Reservation/settlement rules; inventory and currency are handled by a durable server adapter. */
public final class Contracts {
    public enum Scope { PERSONAL, COMMUNITY, FACTION }
    public enum State { PREPARED, COMMITTED, ABORTED }
    public record Definition(String id, Scope scope, Map<String,Long> requirements, long rewardCopper) {
        public Definition {
            if(id==null || !id.matches("[a-z0-9_]+") || scope==null || rewardCopper<0 || requirements.isEmpty())
                throw new IllegalArgumentException("Invalid contract");
            requirements=Map.copyOf(requirements);
            requirements.forEach((item,count)->{
                if(!item.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || count<=0 || count>1_000_000)
                    throw new IllegalArgumentException("Invalid requirement");
            });
        }
    }
    public record Key(String contract, long period, UUID owner) {}
    public record Delivery(UUID operation, Key key, UUID player, String item, long count, State state) {}
    private final Map<Key,Map<String,Long>> delivered=new HashMap<>();
    private final Map<Key,Map<UUID,Long>> contributors=new HashMap<>();
    private final Map<UUID,Delivery> operations=new HashMap<>();
    private final Map<Key,Map<String,Long>> reservedCounts=new HashMap<>();
    private final Set<Key> rewardReserved=new HashSet<>();
    public Delivery prepare(UUID op,Definition definition,Key key,UUID player,UUID currentFaction,
                            long currentPeriod,String item,long requested) {
        if (!definition.id.equals(key.contract) || key.period!=currentPeriod || requested<=0 || requested>64)
            throw new IllegalArgumentException("Stale or malformed delivery");
        UUID expected=switch(definition.scope) {
            case PERSONAL -> player;
            case COMMUNITY -> new UUID(0,0);
            case FACTION -> Objects.requireNonNull(currentFaction,"Faction membership required");
        };
        if(!expected.equals(key.owner))throw new SecurityException("Contract owner mismatch");
        Delivery old=operations.get(op);
        if(old!=null) {
            if(!old.key.equals(key)||!old.player.equals(player)||!old.item.equals(item)||old.count!=requested)
                throw new SecurityException("Operation token reused for a different delivery");
            return old;
        }
        long target=definition.requirements.getOrDefault(item,0L);
        long reserved=reservedCounts.getOrDefault(key,Map.of()).getOrDefault(item,0L);
        long done=delivered.getOrDefault(key,Map.of()).getOrDefault(item,0L);
        if(requested>target-done-reserved)throw new IllegalArgumentException("Delivery exceeds remaining demand");
        Delivery d=new Delivery(op,key,player,item,requested,State.PREPARED);operations.put(op,d);
        reservedCounts.computeIfAbsent(key,k->new HashMap<>()).merge(item,requested,Math::addExact);return d;
    }
    public boolean commit(UUID operation) {
        Delivery d=Objects.requireNonNull(operations.get(operation),"Unknown operation");
        if(d.state!=State.PREPARED)return false;
        release(d);
        delivered.computeIfAbsent(d.key,k->new HashMap<>()).merge(d.item,d.count,Math::addExact);
        contributors.computeIfAbsent(d.key,k->new HashMap<>()).merge(d.player,d.count,Math::addExact);
        operations.put(operation,new Delivery(d.operation,d.key,d.player,d.item,d.count,State.COMMITTED));return true;
    }
    public boolean abort(UUID operation) {
        Delivery d=Objects.requireNonNull(operations.get(operation),"Unknown operation");
        if(d.state!=State.PREPARED)return false;
        release(d);
        operations.put(operation,new Delivery(d.operation,d.key,d.player,d.item,d.count,State.ABORTED));return true;
    }
    private void release(Delivery d) {
        var counts=reservedCounts.get(d.key);
        long remaining=counts.get(d.item)-d.count;
        if(remaining==0)counts.remove(d.item);else counts.put(d.item,remaining);
        if(counts.isEmpty())reservedCounts.remove(d.key);
    }
    public boolean complete(Definition definition,Key key) {
        if(!definition.id.equals(key.contract))return false;
        Map<String,Long> progress=delivered.getOrDefault(key,Map.of());
        return definition.requirements.entrySet().stream().allMatch(e->progress.getOrDefault(e.getKey(),0L)>=e.getValue());
    }
    /** Reserve once before creating a durable payout; never issue coins directly from this call. */
    public boolean reserveReward(Definition definition,Key key) {
        return complete(definition,key)&&rewardReserved.add(key);
    }
    public long delivered(Key key,String item){return delivered.getOrDefault(key,Map.of()).getOrDefault(item,0L);}
    public Map<UUID,Long> contributors(Key key){return Map.copyOf(contributors.getOrDefault(key,Map.of()));}
}
