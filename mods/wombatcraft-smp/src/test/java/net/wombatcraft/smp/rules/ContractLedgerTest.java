package net.wombatcraft.smp.rules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ContractLedgerTest {
    @TempDir Path directory;
    final String player=UUID.randomUUID().toString();
    @Test void crashBeforePlayerSaveDoesNotConsumeOrPay()throws Exception {
        Path file=directory.resolve("ledger.json");var ledger=new ContractLedger(file);
        ledger.prepare(player,"daily/1/player","minecraft:stone",64,8*ContractLedger.COIN);
        ledger=new ContractLedger(file);ledger.settle(player,"");
        assertEquals(0,ledger.balance(player));assertEquals(0,ledger.delivered("daily/1/player","minecraft:stone"));
    }
    @Test void crashAfterPlayerSaveCommitsOnce()throws Exception {
        Path file=directory.resolve("ledger.json");var ledger=new ContractLedger(file);
        var op=ledger.prepare(player,"daily/1/player","minecraft:stone",64,8*ContractLedger.COIN);
        ledger=new ContractLedger(file);assertTrue(ledger.settle(player,op.id()));assertFalse(ledger.settle(player,op.id()));
        ledger=new ContractLedger(file);assertEquals(8*ContractLedger.COIN,ledger.balance(player));assertEquals(64,ledger.delivered("daily/1/player","minecraft:stone"));
    }
    @Test void payoutCannotRepeatAfterRestart()throws Exception {
        Path file=directory.resolve("ledger.json");var ledger=new ContractLedger(file);
        var delivery=ledger.prepare(player,"daily/1/player","minecraft:stone",64,8*ContractLedger.COIN);ledger.settle(player,delivery.id());
        var payment=ledger.prepare(player,"","",0,-8*ContractLedger.COIN);
        assertThrows(IllegalStateException.class,()->new ContractLedger(file).prepare(player,"","",0,-ContractLedger.COIN));
        ledger=new ContractLedger(file);ledger.settle(player,payment.id());
        assertEquals(0,ledger.balance(player));assertThrows(IllegalArgumentException.class,()->new ContractLedger(file).prepare(player,"","",0,-ContractLedger.COIN));
    }
    @Test void failedPayoutReturnsCreditWithoutCreatingCoins()throws Exception {
        var ledger=new ContractLedger(directory.resolve("ledger.json"));var delivery=ledger.prepare(player,"daily/1/player","minecraft:stone",64,8*ContractLedger.COIN);ledger.settle(player,delivery.id());
        ledger.prepare(player,"","",0,-8*ContractLedger.COIN);ledger.settle(player,delivery.id());assertEquals(8*ContractLedger.COIN,ledger.balance(player));
    }
    @Test void corruptedFileIsNotReplaced()throws Exception {
        Path file=directory.resolve("ledger.json");Files.writeString(file,"{\"schema\":99}");assertThrows(java.io.IOException.class,()->new ContractLedger(file));assertTrue(Files.readString(file).contains("99"));
    }
    @Test void creatingAnotherFactionCannotResetWeeklyIncome()throws Exception {
        var ledger=new ContractLedger(directory.resolve("ledger.json"));
        var first=ledger.prepare(player,"factory_steel/300/teamA","minecraft:iron_ingot",64,4096*ContractLedger.COIN);ledger.settle(player,first.id());
        assertThrows(IllegalStateException.class,()->ledger.prepare(player,"factory_steel/300/teamB","minecraft:iron_ingot",64,ContractLedger.COIN));
        assertNotNull(ledger.prepare(player,"factory_steel/301/teamB","minecraft:iron_ingot",64,ContractLedger.COIN));
    }
}
