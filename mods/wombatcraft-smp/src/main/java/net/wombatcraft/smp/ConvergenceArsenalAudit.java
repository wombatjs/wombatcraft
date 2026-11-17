package net.wombatcraft.smp;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Executed only in the disposable full-pack server, after recipes and registries have loaded. */
final class ConvergenceArsenalAudit {
    private static void require(boolean ok,String message) { if (!ok) throw new IllegalStateException("Arsenal: "+message); }
    private static ItemStack stack(String id) {
        Item item=ForgeRegistries.ITEMS.getValue(new ResourceLocation(id.contains(":") ? id : "wombatcraft_smp:"+id));
        require(item!=null && item!=Items.AIR,"missing item "+id); return new ItemStack(item);
    }
    private static ServerPlayer player(MinecraftServer server) {
        var player=new ServerPlayer(server,server.overworld(),new GameProfile(UUID.randomUUID(),"ArsenalQA"));
        player.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),player);
        return player;
    }
    static void run(MinecraftServer server) throws Exception {
        if (!Boolean.getBoolean("wombatcraft.qa")) throw new IllegalStateException("Disposable QA only");
        var p=player(server); var attacker=player(server);
        var combat=Class.forName("net.bettercombat.logic.WeaponRegistry").getMethod("getAttributes",ItemStack.class);
        for (String id:new String[]{"aetherium_saber","astralium_blade","convergium_greatblade","convergium_axe"}) {
            var attributes=combat.invoke(null,stack(id));
            require(attributes!=null,"missing Better Combat moveset "+id);
            if (id.equals("convergium_greatblade")) require((Boolean)attributes.getClass().getMethod("isTwoHanded").invoke(attributes),"greatblade is not two-handed");
        }
        for (String tier:new String[]{"astralium","convergium"}) {
            int defense=0; float toughness=0;
            for (ArmorItem.Type part:ArmorItem.Type.values()) {
                var item=stack(tier+"_"+part.getName()); p.setItemSlot(part.getSlot(),item);
                var armor=(ArmorItem)item.getItem(); defense+=armor.getDefense(); toughness+=armor.getToughness();
                require(armor.isValidRepairItem(item,stack("kubejs:"+tier+"_ingot")),"armor repair");
            }
            require(defense==(tier.equals("astralium")?24:30),"defense total");
            require(toughness==(tier.equals("astralium")?12:16),"toughness total");
            var physical=new LivingHurtEvent(p,p.damageSources().mobAttack(new net.minecraft.world.entity.monster.Zombie(server.overworld())),20);
            ConvergenceArsenal.hurt(physical);
            require(Math.abs(physical.getAmount()-(tier.equals("astralium")?17:13))<0.001,"physical reduction");
            for (var source:java.util.List.of(p.damageSources().playerAttack(attacker),p.damageSources().fellOutOfWorld(),p.damageSources().magic())) {
                var event=new LivingHurtEvent(p,source,20); ConvergenceArsenal.hurt(event);
                require(event.getAmount()==20,"PvP/bypass protection");
            }
            var fall=new LivingFallEvent(p,20,1); ConvergenceTools.fall(fall); ConvergenceArsenal.fall(fall);
            require(fall.getDamageMultiplier()==(tier.equals("astralium")?0.5F:0.25F),"fall reduction");
            p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
            var partial=new LivingHurtEvent(p,physical.getSource(),20); ConvergenceArsenal.hurt(partial);
            require(partial.getAmount()==20,"partial set grants resistance");
        }
        for (String id:new String[]{"convergium_greatblade","convergence_aegis"}) {
            var held=stack(id); p.setItemInHand(InteractionHand.MAIN_HAND,held);
            require(held.getItem().use(p.level(),p,InteractionHand.MAIN_HAND).getResult()==net.minecraft.world.InteractionResult.FAIL,"free activation");
            p.getInventory().add(stack("kubejs:field_charge")); p.getInventory().add(stack("kubejs:field_charge"));
            held.getItem().use(p.level(),p,InteractionHand.MAIN_HAND);
            require(p.getInventory().countItem(stack("kubejs:field_charge").getItem())==1,"charge consumption");
            var copy=stack(id); p.setItemInHand(InteractionHand.MAIN_HAND,copy);
            copy.getItem().use(p.level(),p,InteractionHand.MAIN_HAND);
            require(p.getInventory().countItem(stack("kubejs:field_charge").getItem())==1,"cooldown bypass by swapping");
            if (id.equals("convergium_greatblade")) require(p.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier()==2,"blade buff");
            else require(p.getEffect(MobEffects.ABSORPTION).getAmplifier()==3 && p.getEffect(MobEffects.REGENERATION).getAmplifier()==1,"aegis buffs");
            p.getInventory().clearContent();
        }
        var bow=(BowItem)stack("convergium_longbow").getItem(); var arrow=new Arrow(server.overworld(),p);
        double base=arrow.getBaseDamage(); bow.customArrow(arrow);
        require(arrow.getBaseDamage()==base*2.5,"bow damage");
        for (String id:new String[]{"convergium_helmet","convergium_chestplate","convergium_leggings","convergium_boots","convergium_greatblade","convergium_pickaxe","convergium_axe","convergium_shovel","convergium_longbow","astralium_blade"}) {
            var recipe=(SmithingTransformRecipe)server.getRecipeManager().byKey(new ResourceLocation("wombatcraft_smp",id)).orElseThrow();
            String baseId=id.equals("astralium_blade")?"aetherium_saber":id.equals("convergium_greatblade")?"astralium_blade":
                id.equals("convergium_longbow")?"minecraft:bow":id.endsWith("pickaxe")||id.endsWith("axe")||id.endsWith("shovel")?"minecraft:netherite_"+id.substring(11):id.replace("convergium","astralium");
            var original=stack(baseId); original.setHoverName(Component.literal("Personal heirloom")); original.setDamageValue(100); original.enchant(Enchantments.UNBREAKING,3);
            var inventory=new SimpleContainer(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),original,stack(id.equals("astralium_blade")?"kubejs:astralium_ingot":"convergent_armament"));
            require(recipe.matches(inventory,server.overworld()),"upgrade does not match "+id);
            var result=recipe.assemble(inventory,server.registryAccess());
            require(result.is(stack(id).getItem()) && result.getDamageValue()==100 && result.getHoverName().getString().equals("Personal heirloom") && result.isEnchanted(),"upgrade loses identity "+id);
        }
        System.out.println("CONVERGENCE_ARSENAL_AUDIT_COMPLETE armor=true pvp=true bypass=true partial=true charges=true cooldown=true bow=true smithing=10 better_combat=4");
    }
}
