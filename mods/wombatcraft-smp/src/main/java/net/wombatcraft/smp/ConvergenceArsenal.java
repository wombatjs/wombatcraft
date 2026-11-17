package net.wombatcraft.smp;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Survival equipment; combat uses normal attacks so protection and other mods remain authoritative. */
public final class ConvergenceArsenal {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, WombatcraftSmp.ID);
    private static final Map<String, RegistryObject<Item>> CONTENT = new LinkedHashMap<>();
    public static final ArmorMaterial ASTRAL = new Material("astralium", 45, new int[]{4,9,7,4}, 3, 0.1F);
    public static final ArmorMaterial CONVERGENT = new Material("convergium", 80, new int[]{6,10,8,6}, 4, 0.2F);
    private static final Tier TOOLS = TierSortingRegistry.registerTier(
        new ForgeTier(4, 4096, 18, 6, 24, BlockTags.NEEDS_DIAMOND_TOOL, () -> ingredient("convergium_ingot")),
        new ResourceLocation(WombatcraftSmp.ID, "convergium"), List.of(Tiers.NETHERITE), List.of());

    static {
        add("convergent_armament", () -> new Item(properties()));
        for (ArmorItem.Type part : ArmorItem.Type.values()) {
            String suffix = part.getName();
            if (part != ArmorItem.Type.BOOTS) add("astralium_"+suffix, () -> armor(ASTRAL, part));
            add("convergium_"+suffix, () -> armor(CONVERGENT, part));
        }
        add("aetherium_saber", () -> new Blade(Tiers.NETHERITE, 7, -2.0F, false));
        add("astralium_blade", () -> new Blade(Tiers.NETHERITE, 13, -2.2F, false));
        add("convergium_greatblade", () -> new Blade(TOOLS, 25, -2.6F, true));
        add("convergium_longbow", Longbow::new);
        add("convergium_pickaxe", () -> new PickaxeItem(TOOLS, 2, -2.8F, properties()));
        add("convergium_axe", () -> new AxeItem(TOOLS, 15, -3.1F, properties()));
        add("convergium_shovel", () -> new ShovelItem(TOOLS, 2, -3.0F, properties()));
        add("convergence_aegis", Aegis::new);
    }

    private static Item.Properties properties() { return new Item.Properties().fireResistant().rarity(Rarity.EPIC); }
    private static void add(String id, java.util.function.Supplier<Item> factory) { CONTENT.put(id, ITEMS.register(id, factory)); }
    static Ingredient ingredient(String name) {
        return Ingredient.of(ForgeRegistries.ITEMS.getValue(new ResourceLocation("kubejs", name)));
    }
    static ArmorItem armor(ArmorMaterial material, ArmorItem.Type part) {
        return new ArmorItem(material, part, properties()) {
            @Override public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
                return "minecraft:textures/models/armor/netherite_layer_"+(slot==EquipmentSlot.LEGS ? "2" : "1")+".png";
            }
            @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
                boolean endgame=material==CONVERGENT;
                lines.add(Component.literal(endgame ? "Full set: 35% less ordinary non-player damage." : "Full set: 15% less ordinary non-player damage."));
                lines.add(Component.literal(endgame ? "Full set: 75% less fall damage. Repair: Convergium." : "Boots: 50% less fall damage. Repair: Astralium."));
                lines.add(Component.literal("Does not reduce void, sacrifice or armor-bypassing damage."));
            }
        };
    }
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ConvergenceArsenal::creative);
        MinecraftForge.EVENT_BUS.addListener(ConvergenceArsenal::hurt);
        MinecraftForge.EVENT_BUS.addListener(ConvergenceArsenal::fall);
    }
    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey()==CreativeModeTabs.COMBAT) CONTENT.values().forEach(event::accept);
    }
    static boolean fullSet(Player player, String prefix) {
        for (ArmorItem.Type part : ArmorItem.Type.values()) {
            Item expected = prefix.equals("astralium") && part==ArmorItem.Type.BOOTS
                ? ConvergenceContent.ASTRAL_BOOTS.get() : CONTENT.get(prefix+"_"+part.getName()).get();
            if (!player.getItemBySlot(part.getSlot()).is(expected)) return false;
        }
        return true;
    }
    static void hurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
            || event.getSource().getEntity() instanceof Player
            || event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)
            || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (fullSet(player,"convergium")) event.setAmount(event.getAmount()*0.65F);
        else if (fullSet(player,"astralium")) event.setAmount(event.getAmount()*0.85F);
    }
    static void fall(net.minecraftforge.event.entity.living.LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide && fullSet(player,"convergium"))
            event.setDamageMultiplier(event.getDamageMultiplier()*0.25F);
    }

    // Cooldowns belong to the player and item type, so swapping stacks does not reset them.
    private static boolean charge(Player player, Item ability) {
        if (player.getCooldowns().isOnCooldown(ability)) return false;
        Item charge=ForgeRegistries.ITEMS.getValue(new ResourceLocation("kubejs","field_charge"));
        if (player.getAbilities().instabuild) return true;
        for (int i=0;i<player.getInventory().getContainerSize();i++) {
            if (player.getInventory().getItem(i).is(charge)) { player.getInventory().removeItem(i,1); return true; }
        }
        player.displayClientMessage(Component.literal("Carry a Field Charge to activate this ability."),true);
        return false;
    }
    static final class Blade extends SwordItem {
        private final boolean empowered;
        Blade(Tier tier, int damage, float speed, boolean empowered) { super(tier,damage,speed,properties()); this.empowered=empowered; }
        @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (!empowered) return super.use(level,player,hand);
            ItemStack stack=player.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!charge(player,this)) return InteractionResultHolder.fail(stack);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,200,2));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,200,1));
            player.getCooldowns().addCooldown(this,600);
            return InteractionResultHolder.consume(stack);
        }
        @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
            if (empowered) lines.add(Component.literal("Use: 1 Field Charge for Strength III and Speed II, 10s. Cooldown: 30s."));
        }
    }
    static final class Longbow extends BowItem {
        Longbow() { super(properties().durability(3072)); }
        @Override public AbstractArrow customArrow(AbstractArrow arrow) { arrow.setBaseDamage(arrow.getBaseDamage()*2.5); return arrow; }
        @Override public boolean isValidRepairItem(ItemStack stack, ItemStack repair) { return ingredient("convergium_ingot").test(repair); }
        @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
            lines.add(Component.literal("2.5x arrow base damage. Normal draw, ammo and enchantment rules."));
        }
    }
    static final class Aegis extends Item {
        Aegis() { super(properties().stacksTo(1)); }
        @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack=player.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!charge(player,this)) return InteractionResultHolder.fail(stack);
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,400,3));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,100,1));
            player.getCooldowns().addCooldown(this,1200);
            return InteractionResultHolder.consume(stack);
        }
        @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
            lines.add(Component.literal("Use: 1 Field Charge for 8 absorption hearts (20s) and Regeneration II (5s)."));
            lines.add(Component.literal("60s cooldown. Can be activated from either hand."));
        }
    }
    private record Material(String name, int multiplier, int[] defense, float toughness, float knockback) implements ArmorMaterial {
        @Override public int getDurabilityForType(ArmorItem.Type type) { return switch(type) { case HELMET -> 11; case CHESTPLATE -> 16; case LEGGINGS -> 15; case BOOTS -> 13; } * multiplier; }
        @Override public int getDefenseForType(ArmorItem.Type type) { return defense[switch(type) { case HELMET -> 0; case CHESTPLATE -> 1; case LEGGINGS -> 2; case BOOTS -> 3; }]; }
        @Override public int getEnchantmentValue() { return 24; }
        @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_NETHERITE; }
        @Override public Ingredient getRepairIngredient() { return ingredient(name+"_ingot"); }
        @Override public String getName() { return WombatcraftSmp.ID+":"+name; }
        @Override public float getToughness() { return toughness; }
        @Override public float getKnockbackResistance() { return knockback; }
    }
}
