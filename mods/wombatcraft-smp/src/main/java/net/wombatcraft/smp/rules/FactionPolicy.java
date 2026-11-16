package net.wombatcraft.smp.rules;

import java.util.*;

/** Additional restrictions evaluated against FTB's CURRENT claim owner, not a second claim database. */
public final class FactionPolicy {
    public enum Role { VISITOR, ALLY, MEMBER, BUILDER, OFFICER, OWNER }
    public enum Action { BUILD, CONTAINER, MACHINE, DOOR, ENTITY, TRADE, MANAGE_CHUNK, INVITE, MANAGE_ROLES }
    public record Chunk(String dimension, int x, int z) {}
    public record Rule(UUID owner, Map<Action, Set<Role>> access) {
        public Rule {
            Objects.requireNonNull(owner);
            EnumMap<Action,Set<Role>> copy=new EnumMap<>(Action.class);
            access.forEach((a,r)->copy.put(a,Set.copyOf(r)));
            access=Collections.unmodifiableMap(copy);
        }
    }
    private final Map<Chunk,Rule> chunks=new HashMap<>();
    public static Rule privateFactory(UUID owner) {
        EnumMap<Action,Set<Role>> a=new EnumMap<>(Action.class);
        for (Action action:Action.values()) a.put(action, EnumSet.of(Role.OWNER,Role.OFFICER));
        a.put(Action.BUILD,EnumSet.of(Role.OWNER,Role.OFFICER,Role.BUILDER));
        for (Action action:List.of(Action.CONTAINER,Action.MACHINE,Action.DOOR,Action.ENTITY,Action.TRADE))
            a.put(action,EnumSet.of(Role.OWNER,Role.OFFICER,Role.BUILDER,Role.MEMBER));
        a.put(Action.MANAGE_ROLES,EnumSet.of(Role.OWNER));
        return new Rule(owner,a);
    }
    public static Rule publicShop(UUID owner) {
        Map<Action,Set<Role>> a=new EnumMap<>(privateFactory(owner).access);
        a.put(Action.DOOR,EnumSet.allOf(Role.class));
        a.put(Action.TRADE,EnumSet.allOf(Role.class));
        // Public trade does not grant public chest/machine extraction.
        return new Rule(owner,a);
    }
    public boolean allows(Chunk chunk, UUID authoritativeOwner, Role role, Action action) {
        if (authoritativeOwner==null) return true; // FTB / arena / spawn retain independent protections.
        Rule r=chunks.get(chunk);
        if (r==null || !r.owner.equals(authoritativeOwner)) r=privateFactory(authoritativeOwner);
        if (role==Role.OWNER) return true;
        return r.access.getOrDefault(action,Set.of()).contains(role);
    }
    public void set(Chunk chunk, UUID authoritativeOwner, Role actor, Rule rule) {
        if (authoritativeOwner==null || !authoritativeOwner.equals(rule.owner))
            throw new SecurityException("Claim ownership changed");
        if (actor!=Role.OWNER && actor!=Role.OFFICER) throw new SecurityException("Officer permission required");
        // Per-chunk settings cannot grant faction administration to visitors/members.
        for (Action a:List.of(Action.MANAGE_CHUNK,Action.INVITE,Action.MANAGE_ROLES))
            if (!privateFactory(authoritativeOwner).access.get(a).containsAll(rule.access.getOrDefault(a,Set.of())))
                throw new SecurityException("Chunk policy cannot elevate faction roles");
        chunks.put(chunk,rule);
    }
    public void unclaimed(Chunk chunk) { chunks.remove(chunk); }
}
