package net.wombatcraft.smp.rules;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RulesTest {
    final UUID a=UUID.randomUUID(),b=UUID.randomUUID(),other=UUID.randomUUID();
    final CombatRules.Position origin=new CombatRules.Position("wombatcraft:arena",1,80,1);
    @Test void combatTagsBothParticipantsButNotBlockedOrSelfDamage(){
        CombatRules c=new CombatRules();c.pvp(a,b,100,true);
        assertTrue(c.inCombat(a,60_099));assertTrue(c.inCombat(b,60_099));assertFalse(c.inCombat(a,60_100));
        c.pvp(a,other,60_100,false);assertFalse(c.inCombat(other,60_100));
        c.pvp(a,a,60_100,true);assertFalse(c.inCombat(a,60_100));
    }
    @Test void combatRefreshAndBothExitTimersCancel(){
        CombatRules c=new CombatRules();c.beginExit(a,origin,0);c.beginExit(b,origin,0);
        c.pvp(a,b,59_999,true);
        assertEquals(CombatRules.ExitResult.NOT_PENDING,c.checkExit(a,origin,60_000));
        assertEquals(CombatRules.ExitResult.NOT_PENDING,c.checkExit(b,origin,60_000));
        assertTrue(c.inCombat(a,119_998));
    }
    @Test void exitNeedsFullMinuteAndCannotBeClickedToShorten(){
        CombatRules c=new CombatRules();c.beginExit(a,origin,0);c.beginExit(a,origin,20_000);
        assertEquals(CombatRules.ExitResult.WAITING,c.checkExit(a,origin,59_999));
        assertEquals(CombatRules.ExitResult.READY,c.checkExit(a,origin,60_000));
        assertEquals(CombatRules.ExitResult.NOT_PENDING,c.checkExit(a,origin,60_001));
    }
    @Test void movementWinsOverCompletionAndDimensionChangeCancels(){
        CombatRules c=new CombatRules();c.beginExit(a,origin,0);
        assertEquals(CombatRules.ExitResult.CANCELLED,c.checkExit(a,new CombatRules.Position("wombatcraft:arena",1.1,80,1),60_000));
        c.beginExit(a,origin,0);
        assertEquals(CombatRules.ExitResult.CANCELLED,c.checkExit(a,new CombatRules.Position("minecraft:overworld",1,80,1),1));
    }
    @Test void damageOrAttackCancelsEvenWithoutPvpTag(){
        CombatRules c=new CombatRules();c.beginExit(a,origin,0);c.interrupt(a);
        assertEquals(CombatRules.ExitResult.NOT_PENDING,c.checkExit(a,origin,60_000));
    }
    @Test void logoutBodyHasFullMinuteAndUniqueInventoryToken(){
        CombatRules c=new CombatRules();c.pvp(a,b,0,true);
        var body=c.logout(a,59_000,false).orElseThrow();
        assertEquals(119_000,body.expiresAt());
        assertEquals(body,c.logout(a,59_100,false).orElseThrow());
        assertFalse(c.settle(a,UUID.randomUUID()));assertTrue(c.body(a).isPresent());
        assertTrue(c.expiredBodies(118_999).isEmpty());assertEquals(1,c.expiredBodies(119_000).size());
        assertTrue(c.settle(a,body.token()));assertFalse(c.settle(a,body.token()));
    }
    @Test void routineShutdownOrUntaggedLogoutDoesNotCreateBody(){
        CombatRules c=new CombatRules();assertTrue(c.logout(a,0,false).isEmpty());
        c.pvp(a,b,0,true);assertTrue(c.logout(a,1,true).isEmpty());
    }
    @Test void forgedPositionRejected(){assertThrows(IllegalArgumentException.class,()->new CombatRules.Position("arena",Double.NaN,0,0));}
    @Test void duelNeedsCorrectRecipientAndBothPlayersPresent(){
        Duel d=new Duel(a,b,0);assertThrows(IllegalStateException.class,()->d.accept(a,1));
        assertThrows(IllegalStateException.class,()->d.accept(other,1));d.accept(b,1);
        assertThrows(IllegalStateException.class,()->d.start(false));d.start(true);
        assertFalse(d.death(other,2));assertTrue(d.death(a,2));assertEquals(b,d.winner());
    }
    @Test void duelLootExclusiveForNinetySecondsAndDeathIsIdempotent(){
        Duel d=new Duel(a,b,0);d.accept(b,1);d.start(true);assertTrue(d.death(a,10));
        assertFalse(d.death(b,11));assertFalse(d.mayCollect(a,20));assertFalse(d.mayCollect(other,20));
        assertTrue(d.mayCollect(b,90_009));assertFalse(d.finish(90_009));
        assertFalse(d.mayCollect(b,90_010));assertTrue(d.finish(90_010));assertFalse(d.finish(90_011));
    }
    @Test void duelInvitationExpires(){assertThrows(IllegalStateException.class,()->new Duel(a,b,0).accept(b,60_000));}
    @Test void factionPermissionsSeparateTradingFromStorage(){
        FactionPolicy p=new FactionPolicy();var chunk=new FactionPolicy.Chunk("overworld",-1,0);
        p.set(chunk,a,FactionPolicy.Role.OWNER,FactionPolicy.publicShop(a));
        assertTrue(p.allows(chunk,a,FactionPolicy.Role.VISITOR,FactionPolicy.Action.DOOR));
        assertFalse(p.allows(chunk,a,FactionPolicy.Role.VISITOR,FactionPolicy.Action.CONTAINER));
        assertFalse(p.allows(chunk,a,FactionPolicy.Role.VISITOR,FactionPolicy.Action.ENTITY));
        assertTrue(p.allows(chunk,a,FactionPolicy.Role.VISITOR,FactionPolicy.Action.TRADE));
        assertFalse(p.allows(chunk,a,FactionPolicy.Role.MEMBER,FactionPolicy.Action.BUILD));
        assertTrue(p.allows(chunk,a,FactionPolicy.Role.BUILDER,FactionPolicy.Action.BUILD));
    }
    @Test void ownershipTransferDoesNotInheritOldPublicPermissions(){
        FactionPolicy p=new FactionPolicy();var chunk=new FactionPolicy.Chunk("overworld",0,0);
        p.set(chunk,a,FactionPolicy.Role.OWNER,FactionPolicy.publicShop(a));
        assertFalse(p.allows(chunk,b,FactionPolicy.Role.VISITOR,FactionPolicy.Action.DOOR));
        assertThrows(SecurityException.class,()->p.set(chunk,b,FactionPolicy.Role.OWNER,FactionPolicy.publicShop(a)));
    }
    @Test void chunkSettingsCannotElevateFactionAdministrators(){
        FactionPolicy p=new FactionPolicy();var chunk=new FactionPolicy.Chunk("overworld",0,0);
        assertThrows(SecurityException.class,()->p.set(chunk,a,FactionPolicy.Role.MEMBER,FactionPolicy.privateFactory(a)));
        var grant=new FactionPolicy.Rule(a,Map.of(FactionPolicy.Action.MANAGE_ROLES,Set.of(FactionPolicy.Role.MEMBER)));
        assertThrows(SecurityException.class,()->p.set(chunk,a,FactionPolicy.Role.OFFICER,grant));
    }
    @Test void votingUsesOneBallotPerAccountAndFourDayBoundary(){
        Election e=new Election(100,List.of("brasswick","willow","hearth"),"hearth");
        e.vote(a,"brasswick",0,101);e.vote(a,"willow",0,102);
        assertEquals(1,e.snapshot().votes().size());assertFalse(e.advance(100+Election.TERM_MS-1));
        assertTrue(e.advance(100+Election.TERM_MS));assertEquals("willow",e.mayor());assertEquals(1,e.term());
        assertTrue(e.snapshot().votes().isEmpty());
    }
    @Test void electionRestoresWithoutDoubleSettlementAndRejectsStaleBallot(){
        Election e=new Election(0,List.of("a","b"),"a");e.vote(a,"b",0,1);
        Election restored=Election.restore(e.snapshot(),List.of("a","b"));restored.advance(Election.TERM_MS*3);
        assertEquals("b",restored.mayor());assertEquals(3,restored.term());
        assertFalse(restored.advance(0));assertThrows(IllegalStateException.class,()->restored.vote(b,"a",0,Election.TERM_MS*3));
        assertThrows(IllegalArgumentException.class,()->restored.vote(b,"forged",3,Election.TERM_MS*3));
    }
    @Test void electionTieIndependentOfVoteArrivalOrder(){
        Election e=new Election(0,List.of("a","b"),"b"),f=new Election(0,List.of("a","b"),"b");
        e.vote(a,"a",0,1);e.vote(b,"b",0,1);f.vote(b,"b",0,1);f.vote(a,"a",0,1);
        e.advance(Election.TERM_MS);f.advance(Election.TERM_MS);assertEquals(e.mayor(),f.mayor());
    }
    @Test void acceptedTpaReceiptIsRecipientBoundAndExpires(){
        TpaReceipts r=new TpaReceipts();assertFalse(r.wasAccepted("abc",a,0));r.accepted("abc",a,0);
        assertTrue(r.wasAccepted("abc",a,59_999));assertFalse(r.wasAccepted("abc",b,1));
        assertFalse(r.wasAccepted("abc",a,60_000));r.prune(60_000);assertFalse(r.wasAccepted("abc",a,1));
    }
    @Test void contractDeliveryCannotReplayOrPayTwice(){
        Contracts c=new Contracts();var def=new Contracts.Definition("steel",Contracts.Scope.PERSONAL,Map.of("minecraft:iron_ingot",64L),10);
        var key=new Contracts.Key("steel",1,a);UUID op=UUID.randomUUID();
        c.prepare(op,def,key,a,null,1,"minecraft:iron_ingot",64);assertFalse(c.complete(def,key));
        assertTrue(c.commit(op));assertFalse(c.commit(op));assertEquals(64,c.delivered(key,"minecraft:iron_ingot"));
        assertTrue(c.reserveReward(def,key));assertFalse(c.reserveReward(def,key));
        assertEquals(64,c.contributors(key).get(a));
    }
    @Test void reservationsCannotOversupplyAndAbortReleasesDemand(){
        Contracts c=new Contracts();var def=new Contracts.Definition("stone",Contracts.Scope.COMMUNITY,Map.of("minecraft:stone",64L),10);
        var key=new Contracts.Key("stone",1,new UUID(0,0));UUID op=UUID.randomUUID();
        c.prepare(op,def,key,a,null,1,"minecraft:stone",64);
        assertThrows(IllegalArgumentException.class,()->c.prepare(UUID.randomUUID(),def,key,b,null,1,"minecraft:stone",1));
        assertTrue(c.abort(op));assertFalse(c.commit(op));
        c.prepare(UUID.randomUUID(),def,key,b,null,1,"minecraft:stone",64);
    }
    @Test void factionSwitchCannotDeliverToOldFactionAndStalePeriodRejected(){
        Contracts c=new Contracts();var def=new Contracts.Definition("factory",Contracts.Scope.FACTION,Map.of("create:precision_mechanism",32L),30);
        var key=new Contracts.Key("factory",1,b);
        assertThrows(SecurityException.class,()->c.prepare(UUID.randomUUID(),def,key,a,other,1,"create:precision_mechanism",1));
        assertThrows(IllegalArgumentException.class,()->c.prepare(UUID.randomUUID(),def,key,a,b,2,"create:precision_mechanism",1));
        assertThrows(IllegalArgumentException.class,()->c.prepare(UUID.randomUUID(),def,key,a,b,1,"minecraft:dirt",1));
    }
    @Test void forgedDeliveryReplayAndNegativeAmountsRejected(){
        Contracts c=new Contracts();var def=new Contracts.Definition("iron",Contracts.Scope.PERSONAL,Map.of("minecraft:iron_ingot",64L),10);
        var key=new Contracts.Key("iron",1,a);UUID op=UUID.randomUUID();
        c.prepare(op,def,key,a,null,1,"minecraft:iron_ingot",32);
        assertThrows(SecurityException.class,()->c.prepare(op,def,key,a,null,1,"minecraft:iron_ingot",64));
        assertThrows(IllegalArgumentException.class,()->c.prepare(UUID.randomUUID(),def,key,a,null,1,"minecraft:iron_ingot",-1));
    }
}
