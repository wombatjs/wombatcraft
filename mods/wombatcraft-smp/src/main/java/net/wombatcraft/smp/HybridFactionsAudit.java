package net.wombatcraft.smp;

import com.mojang.authlib.GameProfile;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.inventory.VampirismMenu;
import de.teamlapen.werewolves.util.Helper;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public final class HybridFactionsAudit {
    private HybridFactionsAudit() {}

    public static void run(MinecraftServer server) {
        if (!Boolean.getBoolean("wombatcraft.qa")) throw new IllegalStateException("Disposable QA only");
        var vampire = HybridFactions.vampire();
        var werewolf = HybridFactions.werewolf();
        var wolfFirst = player(server);
        var handler = FactionPlayerHandler.get(wolfFirst);
        require(handler.setFactionAndLevel(werewolf, 4), "Could not establish original werewolf");
        require(handler.canJoin(vampire), "Werewolf could not contract vampirism");
        handler.joinFaction(vampire);
        levels(wolfFirst, 1, 4);

        require(handler.setFactionLevel(werewolf, 5), "Werewolf altar could not level a hybrid");
        require(handler.setFactionLevel(vampire, 2), "Vampire altar could not level a hybrid");
        levels(wolfFirst, 2, 5);
        require(!handler.canJoin(werewolf), "Repeat infection could overwrite werewolf progression");

        var menu = new VampirismMenu(1, wolfFirst.getInventory());
        require(HybridFactions.menuOwner(menu).getFaction() == vampire, "Vampire inventory button routed to the wrong menu");
        HybridFactions.prepareWolfMenu(wolfFirst);
        var wolfMenu = new VampirismMenu(2, wolfFirst.getInventory());
        require(HybridFactions.menuOwner(wolfMenu).getFaction() == werewolf, "Werewolf inventory button routed to the wrong menu");
        require(HybridFactions.menuOwner(new VampirismMenu(3, wolfFirst.getInventory())).getFaction() == vampire,
                "Werewolf menu selection leaked into the next inventory");

        var restored = player(server);
        restored.load(wolfFirst.saveWithoutId(new CompoundTag()));
        levels(restored, 2, 5);
        var cloned = player(server);
        FactionPlayerHandler.get(cloned).copyFrom(restored);
        levels(cloned, 2, 5);
        require(FactionPlayerHandler.get(restored).setFactionLevel(vampire, 0), "Vampire cure failed");
        levels(restored, 0, 5);
        require(FactionPlayerHandler.get(cloned).setFactionLevel(werewolf, 0), "Werewolf cure failed");
        levels(cloned, 2, 0);

        var vampireFirst = player(server);
        var reverse = FactionPlayerHandler.get(vampireFirst);
        require(reverse.setFactionAndLevel(vampire, 4), "Could not establish original vampire");
        require(reverse.canJoin(werewolf), "Vampire could not contract lycanthropy");
        reverse.joinFaction(werewolf);
        levels(vampireFirst, 4, 1);
        System.out.println("HYBRID_FACTIONS_AUDIT_COMPLETE");
    }

    private static ServerPlayer player(MinecraftServer server) {
        var player = new ServerPlayer(server, server.overworld(), new GameProfile(UUID.randomUUID(), "HybridQA"));
        player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player);
        return player;
    }

    private static void levels(ServerPlayer player, int vampire, int werewolf) {
        var handler = FactionPlayerHandler.get(player);
        require(handler.getCurrentLevel(HybridFactions.vampire()) == vampire, "Unexpected vampire level");
        require(handler.getCurrentLevel(HybridFactions.werewolf()) == werewolf, "Unexpected werewolf level");
        require(Helper.isWerewolf(player) == (werewolf > 0), "Werewolves did not recognize membership");
        require(Helper.isVampire(player) == (vampire > 0), "Vampirism did not recognize membership");
    }

    private static void require(boolean condition, String reason) {
        if (!condition) throw new IllegalStateException(reason);
    }
}
