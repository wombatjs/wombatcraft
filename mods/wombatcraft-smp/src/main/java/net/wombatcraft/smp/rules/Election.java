package net.wombatcraft.smp.rules;

import java.util.*;

/** UTC epoch-based terms. No per-player ticker and no fake elections during downtime. */
public final class Election {
    public static final long TERM_MS=4L*24*60*60*1000;
    public record Snapshot(long epoch, long term, String mayor, Map<UUID,String> votes) {}
    private final long epoch;
    private final List<String> candidates;
    private long term;
    private String mayor;
    private final Map<UUID,String> votes=new HashMap<>();
    public Election(long epoch,List<String> candidates,String initialMayor) {
        if (candidates.size()<2 || new HashSet<>(candidates).size()!=candidates.size()
                || !candidates.contains(initialMayor)) throw new IllegalArgumentException("Candidate slate");
        this.epoch=epoch;this.candidates=List.copyOf(candidates);this.mayor=initialMayor;
    }
    public boolean advance(long now) {
        long expected=now<epoch ? 0 : (now-epoch)/TERM_MS;
        if (expected<=term) return false; // Clock rollback cannot re-open settled terms.
        if (!votes.isEmpty()) {
            Map<String,Integer> count=new HashMap<>();votes.values().forEach(c->count.merge(c,1,Integer::sum));
            int best=count.values().stream().max(Integer::compare).orElse(0);
            // Rotating deterministic tie order; independent of hash map iteration.
            for (int i=0;i<candidates.size();i++) {
                String c=candidates.get((int)((term+i)%candidates.size()));
                if (count.getOrDefault(c,0)==best) {mayor=c;break;}
            }
        }
        term=expected;votes.clear();return true;
    }
    public void vote(UUID voter,String candidate,long expectedTerm,long now) {
        advance(now);
        if (expectedTerm!=term) throw new IllegalStateException("Election changed; refresh the ballot");
        if (!candidates.contains(candidate)) throw new IllegalArgumentException("Unknown candidate");
        votes.put(Objects.requireNonNull(voter),candidate); // Changing a vote replaces it.
    }
    public Snapshot snapshot() {return new Snapshot(epoch,term,mayor,Map.copyOf(votes));}
    public static Election restore(Snapshot snapshot,List<String> candidates) {
        Election e=new Election(snapshot.epoch,candidates,snapshot.mayor);
        if(snapshot.term<0 || !candidates.containsAll(snapshot.votes.values())) throw new IllegalArgumentException("Corrupt election");
        e.term=snapshot.term;e.votes.putAll(snapshot.votes);return e;
    }
    public long term(){return term;}
    public String mayor(){return mayor;}
    public long nextElection(){return Math.addExact(epoch,Math.multiplyExact(term+1,TERM_MS));}
}
