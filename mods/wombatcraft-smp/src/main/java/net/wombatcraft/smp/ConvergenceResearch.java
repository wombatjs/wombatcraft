package net.wombatcraft.smp;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Progress belongs to the individual player. Carried evidence is never consumed or rewarded. */
public final class ConvergenceResearch {
    private static final String KEY = PersonalProgress.RESEARCH;
    public static final String[] EVIDENCE = {"kubejs:resonant_alloy", "kubejs:mechanized_core", "kubejs:arcane_core",
        "kubejs:stellar_core", "kubejs:ancient_core", "kubejs:primal_core", "kubejs:temporal_core",
        "kubejs:aetherium_ingot", "kubejs:astralium_ingot", "kubejs:convergium_ingot", "kubejs:convergence_core",
        "wombatcraft:arcane_infuser", "wombatcraft:dimensional_stabilizer", "wombatcraft:convergence_matrix"};
    public static final String[] LABELS = {"Resonance", "Industry", "Magic", "Space", "Adventure", "Nature", "Dimensions",
        "Aetherium", "Astralium", "Convergium", "Convergence Core", "Arcane Infuser", "Dimensional Stabilizer", "Convergence Matrix"};
    public static final String[] GUIDES = {
        "Craft andesite alloy, copper, redstone and amethyst into Resonant Alloy. Resonant Crystals double the yield.",
        "Combine Resonant Alloy, a precision mechanism, an advanced control circuit and an engineering processor.",
        "Combine Resonant Alloy, a mana diamond, a source gem and manasteel. Arcane Cores supply infusion and catalysts.",
        "Combine Resonant Alloy with desh, ostrum and calorite. Explore the planets or trade with a spacefarer.",
        "Combine Resonant Alloy, a Thornwood Crest and a Nether Star. The Stabilizer offers an expensive renewable route later.",
        "Combine Resonant Alloy, supremium essence, a honey bottle and a golden carrot. Farms supply both catalysts and the Matrix.",
        "Combine Resonant Alloy, High Sky and Echo Crests and an ender pearl. All three Trials are repeatable.",
        "Forge four Resonant Alloy, an Arcane Core and a Temporal Core. Opens the Infuser and Aetherium equipment.",
        "Forge four Aetherium, a Stellar Core and a Mechanized Core. Opens the Stabilizer and fall-protection boots.",
        "Forge four Astralium, a Living Relic and Mechanized, Arcane, Stellar and Temporal Cores. Living Relics come from the Infuser.",
        "Combine Convergium with the four legacy sigils, awakened Draconic cores and Nether Stars. Build a Matrix with the result.",
        "Craft with Aetherium, an Arcane Core, a mana pool, a source gem and a precision mechanism. Wall frames: manasteel; caps: amethyst.",
        "Craft with Astralium, a Temporal Core, ostrum, an eye of ender and an engineering processor. Wall frames: ostrum; caps: crying obsidian.",
        "Craft with a Convergence Core, four Convergium and Stellar, Ancient, Primal and Temporal Cores. Wall frames: netherite; caps: beacons. Keep buying specialist consumables."
    };
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, WombatcraftSmp.ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, WombatcraftSmp.ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, WombatcraftSmp.ID);
    public static final RegistryObject<Block> TERMINAL = BLOCKS.register("research_terminal", () -> new Terminal());
    public static final RegistryObject<Item> TERMINAL_ITEM = ITEMS.register("research_terminal", () -> new BlockItem(TERMINAL.get(), new Item.Properties()));
    public static final RegistryObject<MenuType<ResearchMenu>> MENU = MENUS.register("convergence_research", () -> IForgeMenuType.create(ResearchMenu::new));

    public static void register(IEventBus bus) { BLOCKS.register(bus); ITEMS.register(bus); MENUS.register(bus); }

    public static int known(ServerPlayer player) {
        return PersonalProgress.personal(player).getExtraData().getInt(KEY) & 0x3fff;
    }

    public static int record(ServerPlayer player) {
        var team = PersonalProgress.personal(player);
        int old = known(player), found = old;
        for (int i = 0; i < EVIDENCE.length; i++) {
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(EVIDENCE[i]));
            if (item != null) for (int slot=0;slot<player.getInventory().getContainerSize();slot++) {
                if (player.getInventory().getItem(slot).is(item)) { found |= 1 << i; break; }
            }
        }
        if (found != old) {
            team.getExtraData().putInt(KEY, found);
            team.markDirty();
        }
        return found;
    }

    private static final class Terminal extends Block {
        Terminal() { super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)); }
        @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider((id, inv, p) -> new ResearchMenu(id, inv, pos), Component.literal("Convergence Research")), pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    public static final class ResearchMenu extends AbstractContainerMenu {
        private final BlockPos pos;
        private final Player viewer;
        private final Level level;
        private final ContainerData data = new SimpleContainerData(1);
        public ResearchMenu(int id, Inventory inventory, FriendlyByteBuf buffer) { this(id, inventory, buffer.readBlockPos()); }
        public ResearchMenu(int id, Inventory inventory, BlockPos pos) {
            super(MENU.get(), id); this.pos = pos.immutable(); viewer = inventory.player; level = viewer.level();
            addDataSlots(data); refresh();
        }
        private void refresh() { if (viewer instanceof ServerPlayer player) data.set(0, known(player)); }
        public int discoveries() { return data.get(0); }
        @Override public void broadcastChanges() { refresh(); super.broadcastChanges(); }
        @Override public boolean stillValid(Player player) {
            return player == viewer && player.level() == level && level.getBlockState(pos).is(TERMINAL.get()) && player.distanceToSqr(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5) <= 64;
        }
        @Override public boolean clickMenuButton(Player player, int button) {
            if (button != 0 || !stillValid(player) || !(player instanceof ServerPlayer serverPlayer)) return false;
            int before = known(serverPlayer), after = record(serverPlayer);
            data.set(0, after);
            player.displayClientMessage(Component.literal(Integer.bitCount(after & ~before) + " new discoveries recorded in your personal journal. Items kept."), false);
            broadcastChanges(); return true;
        }
        @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    }
}
