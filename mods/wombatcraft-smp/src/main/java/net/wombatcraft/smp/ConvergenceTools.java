package net.wombatcraft.smp;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class ConvergenceTools {
    private ConvergenceTools() {}
    static void fall(LivingFallEvent event) {
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof Player
            && event.getEntity().getItemBySlot(EquipmentSlot.FEET).is(ConvergenceContent.ASTRAL_BOOTS.get())) {
            event.setDamageMultiplier(event.getDamageMultiplier() * 0.5F);
        }
    }

    public static final class FieldProjector extends Item {
        FieldProjector() { super(new Properties().stacksTo(1)); }
        @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack held=player.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(held);
            if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(held);
            Item charge=ForgeRegistries.ITEMS.getValue(new ResourceLocation("kubejs", "field_charge"));
            int slot=-1;
            if (charge!=null) for (int i=0;i<player.getInventory().getContainerSize();i++) if (player.getInventory().getItem(i).is(charge)) { slot=i; break; }
            if (slot<0 && !player.getAbilities().instabuild) {
                player.displayClientMessage(Component.literal("Carry a Field Charge from the Arcane Infuser."), true);
                return InteractionResultHolder.fail(held);
            }
            if (!player.getAbilities().instabuild) player.getInventory().removeItem(slot,1);
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,3600,0));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,3600,0));
            player.getCooldowns().addCooldown(this,200);
            player.displayClientMessage(Component.literal("Expedition field: water breathing and night vision for 3 minutes."), true);
            return InteractionResultHolder.consume(held);
        }
        @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
            lines.add(Component.literal("Use: spend 1 Field Charge for 3 minutes of"));
            lines.add(Component.literal("water breathing and night vision. 10s cooldown."));
        }
    }

    public static final class Prospector extends Item {
        Prospector() { super(new Properties().durability(128)); }
        @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack held=player.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(held);
            if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(held);
            BlockPos origin=player.blockPosition(), nearest=null;
            double distance=Double.MAX_VALUE;
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8,-8,-8),origin.offset(8,8,8))) {
                if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(ConvergenceContent.ORE.get())) continue;
                double candidate=pos.distSqr(origin);
                if (candidate<distance) { distance=candidate; nearest=pos.immutable(); }
            }
            player.displayClientMessage(Component.literal(nearest==null ? "No resonant deposits within 8 blocks in loaded terrain." : "Resonance at "+nearest.getX()+", "+nearest.getY()+", "+nearest.getZ()+"."), true);
            player.getCooldowns().addCooldown(this,100);
            held.hurtAndBreak(1,player,p->p.broadcastBreakEvent(hand));
            return InteractionResultHolder.consume(held);
        }
        @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
            lines.add(Component.literal("Use: locate Resonant Ore within an 8-block cube."));
            lines.add(Component.literal("Loaded terrain only. 128 scans; 5s cooldown."));
        }
    }
}
