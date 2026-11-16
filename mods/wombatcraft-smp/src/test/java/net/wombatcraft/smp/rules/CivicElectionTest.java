package net.wombatcraft.smp.rules;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;
class CivicElectionTest {
    final List<String> roster=IntStream.range(0,50).mapToObj(i->"candidate_"+i).toList();
    final UUID a=UUID.randomUUID(),b=UUID.randomUUID();
    CivicElection fresh(){return new CivicElection(0,roster,roster.get(49));}
    @Test void allFiftyGetAChanceWithinSevenCycles(){
        var e=fresh();Set<String> seen=new HashSet<>();
        for(int i=0;i<7;i++){e.advance(i*CivicElection.TERM);assertEquals(8,e.eligible().size());seen.addAll(e.eligible());}
        assertEquals(new HashSet<>(roster),seen);
    }
    @Test void primaryProducesThreeFinalistsAndResetsBallot(){
        var e=fresh();e.vote(a,"candidate_7",0,1);e.vote(b,"candidate_6",0,1);
        e.advance(CivicElection.PRIMARY);assertEquals(List.of("candidate_6","candidate_7","candidate_0"),e.eligible());
        assertEquals("",e.voteOf(a));assertEquals(1,e.revision());
        assertThrows(IllegalArgumentException.class,()->e.vote(a,"candidate_1",1,CivicElection.PRIMARY));
        assertThrows(IllegalStateException.class,()->e.vote(a,"candidate_6",0,CivicElection.PRIMARY));
    }
    @Test void finalElectsAndAChangedVoteDoesNotDuplicate(){
        var e=fresh();e.advance(CivicElection.PRIMARY);e.vote(a,"candidate_0",1,CivicElection.PRIMARY);e.vote(a,"candidate_2",1,CivicElection.PRIMARY);
        e.advance(CivicElection.TERM);assertEquals("candidate_2",e.mayor());assertEquals(2,e.revision());
    }
    @Test void offlineSkippingFinalDoesNotTurnPrimaryVotesIntoWinner(){
        var e=fresh();e.vote(a,"candidate_7",0,1);e.advance(CivicElection.TERM*9);assertEquals("candidate_49",e.mayor());
    }
    @Test void restartRestoresBothPhasesWithoutDoubleSettlement(){
        var e=fresh();e.vote(a,"candidate_7",0,1);e=CivicElection.restore(e.snapshot(),roster);e.advance(CivicElection.PRIMARY);
        e.vote(b,"candidate_7",1,CivicElection.PRIMARY);e=CivicElection.restore(e.snapshot(),roster);e.advance(CivicElection.TERM);
        assertEquals("candidate_7",e.mayor());assertFalse(e.advance(0));assertFalse(e.advance(CivicElection.TERM));
    }
    @Test void ineligibleAndCorruptBallotsFailClosed(){
        var e=fresh();assertThrows(IllegalArgumentException.class,()->e.vote(a,"candidate_49",0,1));
        var bad=new CivicElection.Snapshot(0,0,CivicElection.Phase.FINAL,"candidate_0",List.of("candidate_0","candidate_0","candidate_1"),Map.of(),Map.of());
        assertThrows(IllegalArgumentException.class,()->CivicElection.restore(bad,roster));
    }
}
