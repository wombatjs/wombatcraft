package net.wombatcraft.smp.rules;

import java.util.*;

public final class Duel {
    public enum Phase { INVITED, ACCEPTED, FIGHTING, LOOTING, COMPLETE, CANCELLED }
    private final UUID first,second;
    private final long invitationExpiry;
    private Phase phase=Phase.INVITED;
    private UUID winner;
    private long lootUntil;
    public Duel(UUID first,UUID second,long now) {
        if(first.equals(second)) throw new IllegalArgumentException("Cannot duel yourself");
        this.first=first;this.second=second;invitationExpiry=now+60_000;
    }
    public void accept(UUID actor,long now) {
        if(phase!=Phase.INVITED || !actor.equals(second) || now>=invitationExpiry)
            throw new IllegalStateException("Duel invitation is not available");
        phase=Phase.ACCEPTED;
    }
    public void start(boolean bothInAssignedArena) {
        if(phase!=Phase.ACCEPTED || !bothInAssignedArena) throw new IllegalStateException("Duel is not ready");
        phase=Phase.FIGHTING;
    }
    public boolean death(UUID loser,long now) {
        if(phase!=Phase.FIGHTING || !participant(loser)) return false;
        winner=loser.equals(first)?second:first;lootUntil=now+CombatRules.LOOT_MS;phase=Phase.LOOTING;return true;
    }
    public boolean mayCollect(UUID player,long now) {
        return phase==Phase.LOOTING && now<lootUntil && player.equals(winner);
    }
    public boolean finish(long now) {
        if(phase!=Phase.LOOTING || now<lootUntil)return false;
        phase=Phase.COMPLETE;return true;
    }
    public boolean participant(UUID p){return first.equals(p)||second.equals(p);}
    public Phase phase(){return phase;}
    public UUID winner(){return winner;}
}
