package net.wombatcraft.smp.rules;

import java.util.*;

/** Eight rotating NPC candidates, a primary, then a three-candidate final. */
public final class CivicElection {
    public static final long DAY=86_400_000L, TERM=4*DAY, PRIMARY=2*DAY;
    public enum Phase { PRIMARY, FINAL }
    public record Snapshot(long epoch,long cycle,Phase phase,String mayor,List<String> finalists,
                           Map<UUID,String> primaryVotes,Map<UUID,String> finalVotes) {}
    private final long epoch;
    private final List<String> roster;
    private long cycle;
    private Phase phase=Phase.PRIMARY;
    private String mayor;
    private List<String> finalists=List.of();
    private final Map<UUID,String> primaryVotes=new HashMap<>(), finalVotes=new HashMap<>();
    public CivicElection(long epoch,List<String> roster,String mayor) {
        if(epoch<0||roster.size()<8||roster.size()>100||new HashSet<>(roster).size()!=roster.size()||!roster.contains(mayor))
            throw new IllegalArgumentException("Invalid election roster");
        this.epoch=epoch;this.roster=List.copyOf(roster);this.mayor=mayor;
    }
    public List<String> primarySlate() {
        List<String> slate=new ArrayList<>();
        int start=(int)((cycle%roster.size())*8%roster.size());
        for(int i=0;i<8;i++)slate.add(roster.get((start+i)%roster.size()));
        return List.copyOf(slate);
    }
    private static List<String> ranked(List<String> eligible,Map<UUID,String> votes) {
        Map<String,Integer> tally=new HashMap<>();votes.values().forEach(id->tally.merge(id,1,Integer::sum));
        List<String> order=new ArrayList<>(eligible);
        // Stable sort preserves the published slate order when votes tie.
        order.sort(Comparator.comparingInt((String id)->tally.getOrDefault(id,0)).reversed());return order;
    }
    public boolean advance(long now) {
        if(now<epoch)return false;
        long target=(now-epoch)/TERM;
        if(target<cycle)return false;
        boolean changed=false;
        if(target>cycle) {
            // Downtime cannot manufacture final votes out of primary ballots.
            if(phase==Phase.FINAL&&!finalVotes.isEmpty())mayor=ranked(finalists,finalVotes).get(0);
            cycle=target;phase=Phase.PRIMARY;finalists=List.of();primaryVotes.clear();finalVotes.clear();changed=true;
        }
        if(phase==Phase.PRIMARY&&now>=Math.addExact(epoch,Math.addExact(Math.multiplyExact(cycle,TERM),PRIMARY))) {
            finalists=List.copyOf(ranked(primarySlate(),primaryVotes).subList(0,3));phase=Phase.FINAL;changed=true;
        }
        return changed;
    }
    public void vote(UUID player,String candidate,long expectedRevision,long now) {
        advance(now);
        if(expectedRevision!=revision())throw new IllegalStateException("The ballot has changed; refresh before voting");
        if(!eligible().contains(candidate))throw new IllegalArgumentException("That candidate is not on this ballot");
        ballots().put(Objects.requireNonNull(player),candidate);
    }
    private Map<UUID,String> ballots(){return phase==Phase.PRIMARY?primaryVotes:finalVotes;}
    public String voteOf(UUID player){return ballots().getOrDefault(player,"");}
    public List<String> eligible(){return phase==Phase.PRIMARY?primarySlate():finalists;}
    public long revision(){return Math.addExact(Math.multiplyExact(cycle,2),phase.ordinal());}
    public long term(){return cycle;}
    public String mayor(){return mayor;}
    public Phase phase(){return phase;}
    public long nextElection(){return Math.addExact(epoch,Math.multiplyExact(cycle+1,TERM));}
    public long nextPhase(){return phase==Phase.PRIMARY?Math.addExact(epoch,Math.addExact(Math.multiplyExact(cycle,TERM),PRIMARY)):nextElection();}
    public Snapshot snapshot(){return new Snapshot(epoch,cycle,phase,mayor,List.copyOf(finalists),Map.copyOf(primaryVotes),Map.copyOf(finalVotes));}
    public static CivicElection restore(Snapshot s,List<String> roster) {
        CivicElection e=new CivicElection(s.epoch,roster,s.mayor);
        if(s.cycle<0||s.phase==null)throw new IllegalArgumentException("Invalid election state");
        e.cycle=s.cycle;e.phase=s.phase;e.finalists=List.copyOf(s.finalists);
        if(!e.primarySlate().containsAll(s.primaryVotes.values())||!e.finalists.containsAll(s.finalVotes.values())
            ||(e.phase==Phase.PRIMARY&&(!e.finalists.isEmpty()||!s.finalVotes.isEmpty()))
            ||(e.phase==Phase.FINAL&&(e.finalists.size()!=3||new HashSet<>(e.finalists).size()!=3||!e.primarySlate().containsAll(e.finalists))))
            throw new IllegalArgumentException("Invalid ballot state");
        e.primaryVotes.putAll(s.primaryVotes);e.finalVotes.putAll(s.finalVotes);return e;
    }
}
