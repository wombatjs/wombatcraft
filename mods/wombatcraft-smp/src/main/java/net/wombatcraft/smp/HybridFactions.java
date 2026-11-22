package net.wombatcraft.smp;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.inventory.VampirismMenu;
import de.teamlapen.werewolves.api.WReference;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;

public final class HybridFactions {
    private static final String OLD_BOND = "wombatcraft_smp:moonblood";
    private static final String OLD_FEED_COOLDOWN = "wombatcraft_smp:moonblood_next_feed";
    private static final ThreadLocal<Player> WOLF_MENU = new ThreadLocal<>();
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(WombatcraftSmp.ID, "hybrid"), () -> "1", "1"::equals, "1"::equals);

    private HybridFactions() {}

    static void register() {
        MinecraftForge.EVENT_BUS.addListener(HybridFactions::migrateBond);
        MinecraftForge.EVENT_BUS.addListener(HybridFactions::remapLegacyItems);
        CHANNEL.messageBuilder(OpenWolfMenu.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> {})
                .decoder(buffer -> new OpenWolfMenu())
                .consumerMainThread((packet, context) -> openWolfMenu(context.get().getSender()))
                .add();
        CHANNEL.messageBuilder(PrepareWolfMenu.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> buffer.writeVarInt(packet.playerId()))
                .decoder(buffer -> new PrepareWolfMenu(buffer.readVarInt()))
                .consumerMainThread((packet, context) -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                        net.minecraftforge.api.distmarker.Dist.CLIENT,
                        () -> () -> net.wombatcraft.smp.client.HybridInventory.prepareWolfMenu(packet.playerId())))
                .add();
    }

    public static IPlayableFaction<?> vampire() {
        return VReference.VAMPIRE_FACTION;
    }

    public static IPlayableFaction<?> werewolf() {
        return WReference.WEREWOLF_FACTION;
    }

    public static boolean isHybrid(Player player) {
        return FactionPlayerHandler.getOpt(player)
                .map(handler -> handler.getCurrentLevel(vampire()) > 0 && handler.getCurrentLevel(werewolf()) > 0)
                .orElse(false);
    }

    public static Optional<IFactionPlayer<?>> playerFor(Player player, IPlayableFaction<?> faction) {
        if (faction == null) return FactionPlayerHandler.getCurrentFactionPlayer(player).map(fp -> fp);
        return FactionPlayerHandler.getOpt(player).resolve()
                .filter(handler -> handler.getCurrentLevel(faction) > 0)
                .flatMap(handler -> faction.getPlayerCapability(player).resolve().map(fp -> fp));
    }

    public static void requestWolfMenu() {
        CHANNEL.sendToServer(new OpenWolfMenu());
    }

    private static void openWolfMenu(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.isSpectator() || !isHybrid(player)) return;
        playerFor(player, werewolf()).ifPresent(wolf -> {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PrepareWolfMenu(player.getId()));
            prepareWolfMenu(player);
            try {
                wolf.getTaskManager().openVampirismMenu();
            } finally {
                finishMenuConstruction(player);
            }
        });
    }

    public static void prepareWolfMenu(Player player) {
        WOLF_MENU.set(player);
    }

    public static Optional<? extends IFactionPlayer<?>> constructingMenuPlayer(Player player) {
        return WOLF_MENU.get() == player ? playerFor(player, werewolf())
                .or(() -> playerFor(player, null))
                : FactionPlayerHandler.getCurrentFactionPlayer(player);
    }

    public static void finishMenuConstruction(Player player) {
        if (WOLF_MENU.get() == player) WOLF_MENU.remove();
    }

    public static IFactionPlayer<?> menuOwner(VampirismMenu menu) {
        return ((MenuOwner) menu).wombatcraft$menuOwner();
    }

    public static Optional<? extends IFactionPlayer<?>> currentMenuPlayer(Player player) {
        if (player.containerMenu instanceof VampirismMenu menu) {
            return playerFor(player, menuOwner(menu).getFaction());
        }
        return FactionPlayerHandler.getCurrentFactionPlayer(player);
    }

    private static void migrateBond(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (data.getBoolean(OLD_BOND)) {
            var handler = FactionPlayerHandler.get(player);
            if (handler.getCurrentFaction() == werewolf() && !handler.setFactionAndLevel(vampire(), 1)) return;
            data.remove(OLD_BOND);
        }
        data.remove(OLD_FEED_COOLDOWN);
    }

    private static void remapLegacyItems(MissingMappingsEvent event) {
        for (MissingMappingsEvent.Mapping<Item> mapping : event.getMappings(ForgeRegistries.Keys.ITEMS, WombatcraftSmp.ID)) {
            switch (mapping.getKey().getPath()) {
                case "moonblood_draught", "severing_tonic" -> mapping.remap(Items.GLASS_BOTTLE);
                case "sunward_charm" -> mapping.remap(Items.GOLD_INGOT);
            }
        }
    }

    public interface MenuOwner {
        IFactionPlayer<?> wombatcraft$menuOwner();
    }

    public interface WerewolfProgress {
        int wombatcraft$wolfLevel();
        int wombatcraft$wolfLord();
        boolean wombatcraft$advanceWolfLord(int level);
    }

    private record OpenWolfMenu() {}
    private record PrepareWolfMenu(int playerId) {}
}
