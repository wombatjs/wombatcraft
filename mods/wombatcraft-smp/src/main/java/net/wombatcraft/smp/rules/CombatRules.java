package net.wombatcraft.smp.rules;

import java.util.*;

/** Server-thread-only state. Callers supply monotonic milliseconds, never client time. */
public final class CombatRules {
    public static final long COMBAT_MS = 60_000, EXIT_MS = 60_000, LOOT_MS = 90_000;
    public record Position(String dimension, double x, double y, double z) {
        public Position {
            Objects.requireNonNull(dimension);
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
                throw new IllegalArgumentException("Non-finite position");
        }
        boolean movedFrom(Position other) {
            return !dimension.equals(other.dimension) || Math.abs(x-other.x)>0.001
                    || Math.abs(y-other.y)>0.001 || Math.abs(z-other.z)>0.001;
        }
    }
    public record Exit(Position origin, long readyAt) {}
    public record Logout(UUID token, UUID player, long expiresAt) {}
    public enum ExitResult { NOT_PENDING, WAITING, CANCELLED, READY }
    private final Map<UUID, Long> combatUntil = new HashMap<>();
    private final Map<UUID, Exit> exits = new HashMap<>();
    private final Map<UUID, Logout> logouts = new HashMap<>();

    public void pvp(UUID attacker, UUID victim, long now, boolean damageAllowed) {
        if (!damageAllowed || attacker.equals(victim)) return;
        combatUntil.put(attacker, Math.addExact(now, COMBAT_MS));
        combatUntil.put(victim, Math.addExact(now, COMBAT_MS));
        interrupt(attacker); interrupt(victim);
    }
    public boolean inCombat(UUID player, long now) { return combatUntil.getOrDefault(player, 0L)>now; }
    public void interrupt(UUID player) { exits.remove(player); }
    public void beginExit(UUID player, Position position, long now) {
        // Repeated clicks must not shorten or replace the existing countdown.
        exits.putIfAbsent(player, new Exit(position, Math.addExact(now, EXIT_MS)));
    }
    public ExitResult checkExit(UUID player, Position position, long now) {
        Exit exit = exits.get(player);
        if (exit == null) return ExitResult.NOT_PENDING;
        // Movement takes precedence, even on the exact completion tick.
        if (position.movedFrom(exit.origin)) { exits.remove(player); return ExitResult.CANCELLED; }
        if (now < exit.readyAt) return ExitResult.WAITING;
        exits.remove(player); return ExitResult.READY;
    }
    public long exitRemaining(UUID player, long now) {
        Exit exit=exits.get(player); return exit==null ? 0 : Math.max(0,exit.readyAt-now);
    }
    public Optional<Logout> logout(UUID player, long now, boolean stoppingServer) {
        interrupt(player);
        if (stoppingServer || !inCombat(player, now)) return Optional.empty();
        // Exactly one inventory-bearing body per account; duplicate callbacks reuse its token.
        return Optional.of(logouts.computeIfAbsent(player, id -> new Logout(UUID.randomUUID(),id,
                Math.addExact(now,COMBAT_MS))));
    }
    public Optional<Logout> body(UUID player) { return Optional.ofNullable(logouts.get(player)); }
    /** Remove only after the inventory handoff/death transaction has been durably settled. */
    public boolean settle(UUID player, UUID token) {
        Logout current=logouts.get(player);
        if (current==null || !current.token.equals(token)) return false;
        logouts.remove(player); return true;
    }
    public Set<UUID> pendingExits() { return Set.copyOf(exits.keySet()); }
    public List<Logout> expiredBodies(long now) {
        return logouts.values().stream().filter(b->b.expiresAt<=now).toList();
    }
    public void prune(long now) { combatUntil.values().removeIf(deadline->deadline<=now); }
    public void clear() { combatUntil.clear(); exits.clear(); logouts.clear(); }
}
