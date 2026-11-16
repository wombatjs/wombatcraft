package net.wombatcraft.smp.rules;

import java.util.*;

/** Only records verified successful acceptance; missing/expired requests remain errors. */
public final class TpaReceipts {
    private record Receipt(UUID recipient,long expires) {}
    private final Map<String,Receipt> accepted=new HashMap<>();
    public void accepted(String request,UUID recipient,long now) {
        prune(now);
        if(accepted.size()>=4096) throw new IllegalStateException("TPA receipt capacity reached");
        accepted.put(request,new Receipt(recipient,now+60_000));
    }
    public boolean wasAccepted(String request,UUID recipient,long now) {
        Receipt r=accepted.get(request);return r!=null&&r.expires>now&&r.recipient.equals(recipient);
    }
    public void prune(long now){accepted.values().removeIf(r->r.expires<=now);}
    public void clear(){accepted.clear();}
}
