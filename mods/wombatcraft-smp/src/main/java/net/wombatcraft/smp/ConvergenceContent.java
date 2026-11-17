package net.wombatcraft.smp;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Pack-specific content. Cross-mod ingredients remain data-driven in KubeJS. */
public final class ConvergenceContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, WombatcraftSmp.ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, WombatcraftSmp.ID);
    public static final RegistryObject<Block> ORE = BLOCKS.register("resonant_ore",
        () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.IRON_ORE), UniformInt.of(1, 3)));
    public static final RegistryObject<Item> ORE_ITEM = ITEMS.register("resonant_ore", () -> new BlockItem(ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> CRYSTAL = ITEMS.register("resonant_crystal", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HELMET = armor("aetherium_helmet", ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> CHESTPLATE = armor("aetherium_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> LEGGINGS = armor("aetherium_leggings", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> BOOTS = armor("aetherium_boots", ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> ASTRAL_BOOTS = ITEMS.register("astralium_boots", () -> new ArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.BOOTS, new Item.Properties()) {
        @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.Level level, java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
            lines.add(net.minecraft.network.chat.Component.literal("Halves fall damage. Falls can still be fatal."));
            lines.add(net.minecraft.network.chat.Component.literal("Does not count toward the Aetherium set bonus."));
        }
    });
    public static final RegistryObject<Item> PROJECTOR = ITEMS.register("field_projector", ConvergenceTools.FieldProjector::new);
    public static final RegistryObject<Item> PROSPECTOR = ITEMS.register("resonant_prospector", ConvergenceTools.Prospector::new);
    private static final TagKey<DamageType> ARCANE = TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(WombatcraftSmp.ID, "arcane_damage"));

    private ConvergenceContent() {}

    private static RegistryObject<Item> armor(String id, ArmorItem.Type type) {
        // Midgame armor stays at diamond defense; its advantage is a specialized full-set bonus.
        return ITEMS.register(id, () -> new ArmorItem(ArmorMaterials.DIAMOND, type, new Item.Properties()) {
            @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.Level level, java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
                lines.add(net.minecraft.network.chat.Component.literal("Full Aetherium set: 20% less non-player magic damage."));
                lines.add(net.minecraft.network.chat.Component.literal("Diamond-level protection. Repair with diamonds."));
            }
        });
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(ConvergenceContent::creative);
        MinecraftForge.EVENT_BUS.addListener(ConvergenceContent::hurt);
        MinecraftForge.EVENT_BUS.addListener(ConvergenceTools::fall);
    }

    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) event.accept(CRYSTAL);
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) event.accept(ORE_ITEM);
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(HELMET); event.accept(CHESTPLATE); event.accept(LEGGINGS); event.accept(BOOTS);
            event.accept(ASTRAL_BOOTS);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) { event.accept(PROJECTOR); event.accept(PROSPECTOR); event.accept(ConvergenceResearch.TERMINAL_ITEM); }
    }

    static void hurt(LivingHurtEvent event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide || !(entity instanceof Player) || event.getSource().getEntity() instanceof Player) return;
        if (!event.getSource().is(ARCANE)) return;
        if (entity.getItemBySlot(EquipmentSlot.HEAD).is(HELMET.get())
            && entity.getItemBySlot(EquipmentSlot.CHEST).is(CHESTPLATE.get())
            && entity.getItemBySlot(EquipmentSlot.LEGS).is(LEGGINGS.get())
            && entity.getItemBySlot(EquipmentSlot.FEET).is(BOOTS.get())) {
            event.setAmount(event.getAmount() * 0.8F);
        }
    }
}
