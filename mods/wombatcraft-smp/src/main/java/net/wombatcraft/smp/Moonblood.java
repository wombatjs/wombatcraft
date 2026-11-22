package net.wombatcraft.smp;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import de.teamlapen.werewolves.api.WReference;
import de.teamlapen.werewolves.api.entities.player.IWerewolfPlayer;
import java.util.List;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

final class Moonblood {
    private static final String HYBRID = "wombatcraft_smp:moonblood";
    private static final String NEXT_FEED = "wombatcraft_smp:moonblood_next_feed";
    private static final int SIGHT_COST = 4;
    private static final int SIGHT_TICKS = 60 * 20;
    private static final int SUN_INTERVAL = 4 * 20;
    private static ForgeConfigSpec.BooleanValue enabled;
    private static ForgeConfigSpec.IntValue minimumLevel;
    private static ForgeConfigSpec.IntValue feedingCooldown;
    private static ForgeConfigSpec.DoubleValue sunDamage;

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, WombatcraftSmp.ID);
    private static final RegistryObject<Item> DRAUGHT =
            ITEMS.register("moonblood_draught", () -> new RitualDrink(Moonblood::bind));
    private static final RegistryObject<Item> TONIC =
            ITEMS.register("severing_tonic", () -> new RitualDrink(Moonblood::sever));
    private static final RegistryObject<Item> CHARM =
            ITEMS.register("sunward_charm", () -> new MoonbloodItem(new Item.Properties().stacksTo(1)));

    private Moonblood() {}

    static void configure(ForgeConfigSpec.Builder config) {
        config.push("moonblood");
        enabled = config.comment("Enable Moonblood hybrid powers and the binding ritual.").define("enabled", true);
        minimumLevel = config.comment("Werewolf level required to bind Moonblood.")
                .defineInRange("minimumWerewolfLevel", 5, 1, 14);
        feedingCooldown = config.comment("Seconds between feeding on hostile mobs.")
                .defineInRange("feedingCooldownSeconds", 20, 5, 120);
        sunDamage = config.comment("Damage in exposed sunlight every four seconds. Set to zero to disable it.")
                .defineInRange("sunDamage", 1.0, 0.0, 4.0);
        config.pop();
    }

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(Moonblood::creativeTab);
        var events = MinecraftForge.EVENT_BUS;
        events.addListener(Moonblood::commands);
        events.addListener(Moonblood::factionChanged);
        events.addListener(Moonblood::playerTick);
        events.addListener(EventPriority.LOWEST, Moonblood::feed);
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(DRAUGHT);
            event.accept(TONIC);
            event.accept(CHARM);
        }
    }

    private static CompoundTag state(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static boolean bound(Player player) {
        return state(player).getBoolean(HYBRID);
    }

    private static IWerewolfPlayer wolf(Player player) {
        var handler = VampirismAPI.getFactionPlayerHandler(player).orElse(null);
        if (handler == null || handler.getCurrentFaction() != WReference.WEREWOLF_FACTION) {
            return null;
        }
        var factionPlayer = handler.getCurrentFactionPlayer().orElse(null);
        return factionPlayer instanceof IWerewolfPlayer wolf ? wolf : null;
    }

    private static boolean canUsePowers(ServerPlayer player) {
        return enabled.get() && !(player instanceof FakePlayer) && player.isAlive()
                && !player.isCreative() && !player.isSpectator() && bound(player) && wolf(player) != null;
    }

    private static Component bind(ServerPlayer player) {
        if (!enabled.get()) return message("disabled");
        if (bound(player)) return message("already_bound");
        var wolf = wolf(player);
        if (wolf == null || wolf.getLevel() < minimumLevel.get()) {
            return message("requires_level", minimumLevel.get());
        }
        return null;
    }

    private static Component sever(ServerPlayer player) {
        return bound(player) ? null : message("not_bound");
    }

    private static void factionChanged(PlayerFactionEvent.FactionLevelChanged event) {
        if (event.getPlayer().getPlayer() instanceof ServerPlayer player
                && event.getOldFaction() == WReference.WEREWOLF_FACTION
                && (event.getCurrentFaction() != WReference.WEREWOLF_FACTION || event.getNewLevel() == 0)
                && bound(player)) {
            state(player).remove(HYBRID);
            player.displayClientMessage(message("severed"), false);
        }
    }

    private static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % SUN_INTERVAL != 0 || !canUsePowers(player)) {
            return;
        }
        if (sunDamage.get() > 0 && !player.getMainHandItem().is(CHARM.get())
                && !player.getOffhandItem().is(CHARM.get())
                && VampirismAPI.sundamageRegistry().isGettingSundamage(player, player.serverLevel())) {
            player.hurt(player.damageSources().onFire(), sunDamage.get().floatValue());
        }
    }

    private static void feed(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Enemy)
                || !(event.getSource().getDirectEntity() instanceof ServerPlayer player)
                || !event.getSource().is(DamageTypes.PLAYER_ATTACK)
                || !canUsePowers(player) || !wolf(player).getForm().isTransformed()
                || !player.getFoodData().needsFood()) {
            return;
        }
        var data = state(player);
        long now = player.serverLevel().getGameTime();
        if (now < data.getLong(NEXT_FEED)) return;
        data.putLong(NEXT_FEED, now + feedingCooldown.get() * 20L);
        player.getFoodData().eat(2, 0);
        player.displayClientMessage(message("fed"), true);
    }

    private static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("moonblood")
                .executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    Component status = !enabled.get() ? message("disabled") :
                            bound(player) ? message("status_bound") : message("status_unbound", minimumLevel.get());
                    context.getSource().sendSuccess(() -> status, false);
                    return 1;
                })
                .then(Commands.literal("sight").executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    if (!canUsePowers(player)) {
                        context.getSource().sendFailure(message("sight_unavailable"));
                        return 0;
                    }
                    var hunger = player.getFoodData();
                    if (hunger.getFoodLevel() < 2 * SIGHT_COST) {
                        context.getSource().sendFailure(message("sight_hungry"));
                        return 0;
                    }
                    if (player.hasEffect(MobEffects.NIGHT_VISION)
                            || !player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, SIGHT_TICKS, 0))) {
                        context.getSource().sendFailure(message("sight_active"));
                        return 0;
                    }
                    hunger.setFoodLevel(hunger.getFoodLevel() - SIGHT_COST);
                    hunger.setSaturation(Math.min(hunger.getSaturationLevel(), hunger.getFoodLevel()));
                    return 1;
                })));
    }

    private static Component message(String key, Object... args) {
        return Component.translatable("moonblood.wombatcraft." + key, args);
    }

    private static class MoonbloodItem extends Item {
        MoonbloodItem(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flags) {
            lines.add(Component.translatable(getDescriptionId() + ".hint").withStyle(ChatFormatting.GRAY));
        }
    }

    private static final class RitualDrink extends MoonbloodItem {
        private final Function<ServerPlayer, Component> check;

        RitualDrink(Function<ServerPlayer, Component> check) {
            super(new Item.Properties().stacksTo(1));
            this.check = check;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer serverPlayer && !allowed(serverPlayer)) {
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            return ItemUtils.startUsingInstantly(level, player, hand);
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            return UseAnim.DRINK;
        }

        @Override
        public int getUseDuration(ItemStack stack) {
            return 32;
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            if (!(entity instanceof ServerPlayer player) || !allowed(player)) return stack;
            var data = state(player);
            if (stack.is(DRAUGHT.get())) {
                data.putBoolean(HYBRID, true);
                player.getPersistentData().put(Player.PERSISTED_NBT_TAG, data);
                player.displayClientMessage(message("bound"), false);
            } else {
                data.remove(HYBRID);
                player.displayClientMessage(message("severed"), false);
            }
            return ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE));
        }

        private boolean allowed(ServerPlayer player) {
            if (player instanceof FakePlayer || player.isSpectator() || !player.isAlive()) return false;
            var reason = check.apply(player);
            if (reason == null) return true;
            player.displayClientMessage(reason, true);
            return false;
        }
    }
}
