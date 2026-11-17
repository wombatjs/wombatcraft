package local.qa;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/** Exercises the real gateway engine and multiblock recipe handlers in the disposable world. */
final class FrontierAudit {
    private static final String[] TRIALS = {"twilight", "aether", "otherside"};
    private static final Class<?> LOCATION = type("net.minecraft.resources.ResourceLocation");
    private static final Class<?> POSITION = type("net.minecraft.core.BlockPos");
    private static final Class<?> ENTITY = type("net.minecraft.world.entity.Entity");
    private static final Class<?> STACK = type("net.minecraft.world.item.ItemStack");
    private static final Class<?> ITEM_LIKE = type("net.minecraft.world.level.ItemLike");

    static void prepare(Object server) throws Exception {
        Object source = call(server, "m_129893_");
        Object commands = find(server, "net.minecraft.commands.Commands");
        commands.getClass().getMethod("m_230957_", type("net.minecraft.commands.CommandSourceStack"), String.class)
            .invoke(commands, source, "forceload add 0 0 24 24");
        Object world = call(server, "m_129783_");
        for (int x : new int[]{0, 16}) {
            for (int z : new int[]{0, 16}) {
                world.getClass().getMethod("m_46745_", POSITION).invoke(world, position(x, 180, z));
            }
        }
    }

    static boolean ready(Object server) throws Exception {
        Object world = call(server, "m_129783_");
        for (int x = 0; x <= 1; x++) for (int z = 0; z <= 1; z++) {
            long chunk = (x & 0xffffffffL) | ((z & 0xffffffffL) << 32);
            if (!(Boolean) world.getClass().getMethod("m_143319_", long.class).invoke(world, chunk)) return false;
            if (!(Boolean) world.getClass().getMethod("m_143340_", POSITION).invoke(world, position(x * 16, 180, z * 16))) return false;
        }
        return true;
    }

    static void run(Object server) throws Exception {
        Object world = call(server, "m_129783_");
        Object source = call(server, "m_129893_");
        Object commands = find(server, "net.minecraft.commands.Commands");
        Method command = commands.getClass().getMethod("m_230957_", type("net.minecraft.commands.CommandSourceStack"), String.class);
        command.invoke(commands, source, "fill 0 179 0 24 179 24 minecraft:stone");

        try {
            auditForge(world, commands, source, command, "convergence_forge", "create:brass_block", "minecraft:crying_obsidian");
            auditForge(world, commands, source, command, "arcane_infuser", "botania:manasteel_block", "minecraft:amethyst_block");
            auditForge(world, commands, source, command, "dimensional_stabilizer", "ad_astra:ostrum_block", "minecraft:crying_obsidian");
            auditForge(world, commands, source, command, "convergence_matrix", "minecraft:netherite_block", "minecraft:beacon");
            auditArmor(server, world);
            type("net.wombatcraft.smp.ConvergenceGameplayAudit").getMethod("run", type("net.minecraft.server.MinecraftServer")).invoke(null, server);
            for (String trial : TRIALS) auditGate(server, world, trial);
        } finally {
            command.invoke(commands, source, "fill 0 179 0 24 184 24 minecraft:air");
            command.invoke(commands, source, "forceload remove 0 0 24 24");
        }
        System.out.println("FRONTIER_GAMEPLAY_AUDIT_COMPLETE stations=4 trials=3");
    }

    private static void auditArmor(Object server, Object world) throws Exception {
        Class<?> profile = type("com.mojang.authlib.GameProfile");
        var constructor = type("net.minecraft.server.level.ServerPlayer").getConstructor(
            type("net.minecraft.server.MinecraftServer"), type("net.minecraft.server.level.ServerLevel"), profile);
        Object player = constructor.newInstance(server, world, profile.getConstructor(UUID.class, String.class).newInstance(UUID.randomUUID(), "ArmorAudit"));
        Object attacker = constructor.newInstance(server, world, profile.getConstructor(UUID.class, String.class).newInstance(UUID.randomUUID(), "ArmorAttacker"));
        Class<?> slots = type("net.minecraft.world.entity.EquipmentSlot");
        Method equip = player.getClass().getMethod("m_8061_", slots, STACK);
        Object head = null;
        String[] names = {"HEAD", "CHEST", "LEGS", "FEET"};
        String[] parts = {"helmet", "chestplate", "leggings", "boots"};
        for (int i = 0; i < names.length; i++) {
            Object slot = null;
            for (Object candidate : slots.getEnumConstants()) if (((Enum<?>) candidate).name().equals(names[i])) slot = candidate;
            require(slot != null, "Missing armor slot " + names[i]);
            if (i == 0) head = slot;
            equip.invoke(player, slot, stack("wombatcraft_smp:aetherium_" + parts[i], 1));
        }
        Object sources = call(player, "m_269291_");
        Object magic = call(sources, "m_269425_");
        Object physical = call(sources, "m_269264_");
        Object pvp = sources.getClass().getMethod("m_269104_", ENTITY, ENTITY).invoke(sources, attacker, attacker);
        Class<?> hurtEvent = type("net.minecraftforge.event.entity.living.LivingHurtEvent");
        Method handler = type("net.wombatcraft.smp.ConvergenceContent").getDeclaredMethod("hurt", hurtEvent);
        handler.setAccessible(true);
        for (Object damage : new Object[]{magic, physical, pvp}) {
            Object event = hurtEvent.getConstructor(type("net.minecraft.world.entity.LivingEntity"), type("net.minecraft.world.damagesource.DamageSource"), float.class)
                .newInstance(player, damage, 10F);
            handler.invoke(null, event);
            require(Math.abs((Float) call(event, "getAmount") - (damage == magic ? 8F : 10F)) < 0.001F, "Incorrect armor magic/PvP/physical behavior");
        }
        equip.invoke(player, head, STACK.getField("f_41583_").get(null));
        Object partial = hurtEvent.getConstructor(type("net.minecraft.world.entity.LivingEntity"), type("net.minecraft.world.damagesource.DamageSource"), float.class)
            .newInstance(player, magic, 10F);
        handler.invoke(null, partial);
        require((Float) call(partial, "getAmount") == 10F, "Partial armor set grants magic resistance");
        System.out.println("CONVERGENCE_ARMOR_AUDIT_COMPLETE full_set=8 partial_set=10 physical=10 pvp_magic=10");
    }

    private static void auditForge(Object world, Object commands, Object source, Method command, String machineId, String frame, String cap) throws Exception {
        for (String placement : new String[]{
            "8 180 8 wombatcraft:" + machineId,
            "7 180 8 " + frame, "9 180 8 " + frame,
            "7 181 8 " + cap, "8 181 8 " + frame,
            "9 181 8 " + cap
        }) command.invoke(commands, source, "setblock " + placement);
        Object entity = world.getClass().getMethod("m_7702_", POSITION).invoke(world, position(8, 180, 8));
        Object machine = call(entity, "getMetaMachine");
        Class<?> direction = type("net.minecraft.core.Direction");
        machine.getClass().getMethod("setFrontFacing", direction).invoke(machine, direction.getField("NORTH").get(null));
        if (!(Boolean) call(machine, "checkPatternWithLock")) {
            Object state = call(machine, "getMultiblockState");
            Object error = state.getClass().getField("error").get(state);
            throw new IllegalStateException("The documented Forge pattern failed to form at " + call(state, "getPos")
                + " block=" + call(state, "getBlockState") + " reason=" + (error == null ? "missing match" : call(error, "getErrorInfo")));
        }
        call(machine, "onStructureFormed");
        require((Boolean) call(machine, "runRecipeLogic"), "The formed Forge cannot run recipes");
        Object inputs = storage(machine, "ingredients");
        Object products = storage(machine, "products");
        Object energy = storage(machine, "charge");
        Object mana = storage(machine, "mana");
        Method set = inputs.getClass().getMethod("setStackInSlot", int.class, STACK);
        Method get = products.getClass().getMethod("getStackInSlot", int.class);
        Method charge = energy.getClass().getMethod("receiveEnergy", int.class, boolean.class);
        Method fillMana = mana.getClass().getMethod("receiveMana", int.class);
        Object logic = call(machine, "getRecipeLogic");
        Object empty = STACK.getField("f_41583_").get(null);
        Object capability = type("net.minecraftforge.common.capabilities.ForgeCapabilities").getField("ITEM_HANDLER").get(null);
        Object lazy = entity.getClass().getMethod("getCapability", type("net.minecraftforge.common.capabilities.Capability"), direction)
            .invoke(entity, capability, direction.getField("UP").get(null));
        Object pipes = ((java.util.Optional<?>) call(lazy,"resolve")).orElseThrow();
        Class<?> inventoryApi=type("net.minecraftforge.items.IItemHandler");
        int slots=(Integer)inventoryApi.getMethod("getSlots").invoke(pipes), insertable=0, extractable=0;
        for (int i=0;i<slots;i++) {
            Object remainder=inventoryApi.getMethod("insertItem",int.class,STACK,boolean.class).invoke(pipes,i,stack("minecraft:cobblestone",1),true);
            if ((Boolean)call(remainder,"m_41619_")) insertable++;
        }
        for(int i=0;i<6;i++) set.invoke(inputs,i,stack("minecraft:cobblestone",1));
        for(int i=0;i<2;i++) products.getClass().getMethod("setStackInSlot",int.class,STACK).invoke(products,i,stack("minecraft:cobblestone",1));
        for(int i=0;i<slots;i++) {
            Object extracted=inventoryApi.getMethod("extractItem",int.class,int.class,boolean.class).invoke(pipes,i,1,true);
            if (!(Boolean)call(extracted,"m_41619_")) extractable++;
        }
        require(insertable==6 && extractable==2, "Machine pipe direction mismatch for "+machineId+": insert="+insertable+" extract="+extractable);
        for(int i=0;i<6;i++) set.invoke(inputs,i,empty);
        for(int i=0;i<2;i++) products.getClass().getMethod("setStackInSlot",int.class,STACK).invoke(products,i,empty);

        if (machineId.equals("convergence_forge")) {
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 200, 200000, 12000,
            "kubejs:convergence_alloy", 4, new String[]{"create:brass_ingot", "botania:terrasteel_ingot", "ad_astra:ostrum_ingot"}, new int[]{8,1,2});
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 280, 400000, 30000,
            "kubejs:convergence_lens", 1, new String[]{"kubejs:convergence_alloy", "aether:ambrosium_shard", "botania:mana_diamond"}, new int[]{2,8,2});
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 600, 1000000, 75000,
            "kubejs:convergence_keystone", 1, new String[]{"kubejs:convergence_lens","kubejs:trial_twilight_crest","kubejs:trial_aether_crest",
                "kubejs:trial_otherside_crest","ad_astra:calorite_ingot","botania:gaia_ingot"}, new int[]{1,1,1,1,2,1});
        }
        // BEGIN GENERATED PROGRESSION AUDIT
        // aetherium
        if (machineId.equals("convergence_forge"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 200, 200000, 12000,
            "kubejs:aetherium_ingot", 4, new String[]{"kubejs:resonant_alloy","kubejs:arcane_core","kubejs:temporal_core"}, new int[]{4,1,1});
        // astralium
        if (machineId.equals("convergence_forge"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 300, 600000, 30000,
            "kubejs:astralium_ingot", 4, new String[]{"kubejs:aetherium_ingot","kubejs:stellar_core","kubejs:mechanized_core"}, new int[]{4,1,1});
        // living_relic
        if (machineId.equals("arcane_infuser"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 240, 400000, 24000,
            "kubejs:living_relic", 1, new String[]{"kubejs:ancient_core","kubejs:primal_core"}, new int[]{1,1});
        // convergium
        if (machineId.equals("convergence_forge"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 600, 2000000, 120000,
            "kubejs:convergium_ingot", 4, new String[]{"kubejs:astralium_ingot","kubejs:living_relic","kubejs:mechanized_core","kubejs:arcane_core","kubejs:stellar_core","kubejs:temporal_core"}, new int[]{4,1,1,1,1,1});
        // ancient_core_industrial
        if (machineId.equals("dimensional_stabilizer"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 600, 1000000, 60000,
            "kubejs:ancient_core", 1, new String[]{"kubejs:aetherium_ingot","minecraft:nether_star","botania:terrasteel_ingot"}, new int[]{8,4,4});
        // temporal_core_industrial
        if (machineId.equals("dimensional_stabilizer"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 600, 1000000, 60000,
            "kubejs:temporal_core", 1, new String[]{"kubejs:aetherium_ingot","minecraft:ender_pearl","botania:mana_diamond"}, new int[]{8,16,8});
        // mechanized_processors
        if (machineId.equals("convergence_forge"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 200, 200000, 12000,
            "ae2:engineering_processor", 8, new String[]{"kubejs:mechanized_core","ae2:printed_silicon","minecraft:diamond","minecraft:redstone"}, new int[]{1,8,8,8});
        // arcane_industry
        if (machineId.equals("arcane_infuser"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 200, 200000, 12000,
            "botania:manasteel_ingot", 32, new String[]{"kubejs:arcane_core","minecraft:iron_ingot"}, new int[]{1,32});
        // stellar_industry
        if (machineId.equals("dimensional_stabilizer"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 400, 800000, 40000,
            "ad_astra:calorite_ingot", 8, new String[]{"kubejs:stellar_core","ad_astra:calorite_ingot","kubejs:aetherium_ingot"}, new int[]{1,4,8});
        // primal_harvest
        if (machineId.equals("arcane_infuser"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 200, 200000, 12000,
            "mysticalagriculture:supremium_farmland", 4, new String[]{"kubejs:primal_core","minecraft:dirt","minecraft:wheat"}, new int[]{1,4,32});
        // ancient_beacon
        if (machineId.equals("convergence_forge"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 200, 200000, 12000,
            "minecraft:beacon", 1, new String[]{"kubejs:ancient_core","minecraft:obsidian","minecraft:glass"}, new int[]{1,3,5});
        // temporal_travel
        if (machineId.equals("dimensional_stabilizer"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 200, 200000, 12000,
            "waystones:warp_stone", 1, new String[]{"kubejs:temporal_core","minecraft:ender_pearl","minecraft:amethyst_shard"}, new int[]{1,4,8});
        // aetherium_storage
        if (machineId.equals("arcane_infuser"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 200, 200000, 12000,
            "ae2:fluix_crystal", 32, new String[]{"kubejs:aetherium_ingot","ae2:certus_quartz_crystal","minecraft:redstone"}, new int[]{1,16,16});
        // astralium_travel
        if (machineId.equals("dimensional_stabilizer"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 600, 1000000, 60000,
            "minecraft:elytra", 1, new String[]{"kubejs:astralium_ingot","kubejs:stellar_core","minecraft:phantom_membrane","minecraft:feather"}, new int[]{4,1,16,32});
        // convergium_automation
        if (machineId.equals("convergence_forge"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 400, 1000000, 60000,
            "create:precision_mechanism", 16, new String[]{"kubejs:convergium_ingot","minecraft:gold_ingot","create:cogwheel"}, new int[]{1,16,16});
        // convergium_storage
        if (machineId.equals("convergence_forge"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 600, 2000000, 120000,
            "ae2:singularity", 1, new String[]{"kubejs:convergium_ingot","ae2:fluix_crystal"}, new int[]{4,64});
        // infused_catalyst
        if (machineId.equals("arcane_infuser"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 300, 400000, 24000,
            "kubejs:infused_catalyst", 8, new String[]{"kubejs:arcane_core","kubejs:primal_core","ars_nouveau:source_gem"}, new int[]{1,1,8});
        // stabilized_flux
        if (machineId.equals("dimensional_stabilizer"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 400, 800000, 48000,
            "kubejs:stabilized_flux", 8, new String[]{"kubejs:temporal_core","kubejs:stellar_core","minecraft:ender_pearl"}, new int[]{1,1,8});
        // field_charge
        if (machineId.equals("arcane_infuser"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 200, 200000, 12000,
            "kubejs:field_charge", 8, new String[]{"kubejs:infused_catalyst","minecraft:prismarine_crystals","minecraft:glowstone_dust"}, new int[]{1,8,8});
        // infused_circuits
        if (machineId.equals("arcane_infuser"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 300, 400000, 24000,
            "mekanism:advanced_control_circuit", 8, new String[]{"kubejs:mechanized_core","kubejs:infused_catalyst","minecraft:iron_ingot","minecraft:redstone"}, new int[]{1,1,8,16});
        // infused_terrasteel
        if (machineId.equals("arcane_infuser"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 600, 1200000, 500000,
            "botania:terrasteel_ingot", 4, new String[]{"kubejs:infused_catalyst","botania:manasteel_ingot","botania:mana_diamond","botania:mana_pearl"}, new int[]{2,4,4,4});
        // primal_renewal
        if (machineId.equals("arcane_infuser"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 400, 800000, 60000,
            "mysticalagriculture:supremium_essence", 4, new String[]{"kubejs:primal_core","mysticalagriculture:imperium_essence","kubejs:infused_catalyst"}, new int[]{1,8,1});
        // resonant_recrystallization
        if (machineId.equals("arcane_infuser"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 300, 300000, 18000,
            "wombatcraft_smp:resonant_crystal", 16, new String[]{"kubejs:infused_catalyst","minecraft:amethyst_shard","minecraft:redstone"}, new int[]{1,16,16});
        // desh_reconstitution
        if (machineId.equals("dimensional_stabilizer"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 400, 800000, 48000,
            "ad_astra:desh_ingot", 16, new String[]{"kubejs:stellar_core","kubejs:stabilized_flux","minecraft:iron_ingot"}, new int[]{1,1,16});
        // ostrum_reconstitution
        if (machineId.equals("dimensional_stabilizer"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 500, 1200000, 60000,
            "ad_astra:ostrum_ingot", 16, new String[]{"kubejs:stellar_core","kubejs:stabilized_flux","ad_astra:desh_ingot"}, new int[]{1,2,16});
        // overclocked_processors
        if (machineId.equals("arcane_infuser"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 50, 400000, 24000,
            "ae2:engineering_processor", 8, new String[]{"kubejs:mechanized_core","kubejs:stabilized_flux","ae2:printed_silicon","minecraft:diamond","minecraft:redstone"}, new int[]{1,1,8,8,8});
        // overclocked_astralium
        if (machineId.equals("dimensional_stabilizer"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 75, 1200000, 60000,
            "kubejs:astralium_ingot", 4, new String[]{"kubejs:aetherium_ingot","kubejs:stellar_core","kubejs:mechanized_core","kubejs:stabilized_flux"}, new int[]{4,1,1,1});
        // matrix_industry
        if (machineId.equals("convergence_matrix"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 800, 3000000, 180000,
            "mekanism:pellet_polonium", 4, new String[]{"kubejs:mechanized_core","kubejs:stabilized_flux","mekanism:ingot_uranium"}, new int[]{2,4,16});
        // matrix_arcane
        if (machineId.equals("convergence_matrix"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 800, 3000000, 300000,
            "botania:gaia_ingot", 4, new String[]{"kubejs:arcane_core","kubejs:ancient_core","kubejs:infused_catalyst","botania:terrasteel_ingot"}, new int[]{2,1,4,4});
        // matrix_nature
        if (machineId.equals("convergence_matrix"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 800, 3000000, 180000,
            "mysticalagriculture:awakened_supremium_essence", 4, new String[]{"kubejs:primal_core","kubejs:infused_catalyst","mysticalagriculture:supremium_essence"}, new int[]{2,4,16});
        // matrix_expedition
        if (machineId.equals("convergence_matrix"))
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 800, 3000000, 180000,
            "minecraft:nether_star", 4, new String[]{"kubejs:ancient_core","kubejs:stellar_core","kubejs:temporal_core","kubejs:stabilized_flux"}, new int[]{2,1,1,4});
        // END GENERATED PROGRESSION AUDIT
    }

    private static void forge(Object logic, Object inputs, Object products, Object energy, Object mana, Method set, Method get,
            Method charge, Method fillMana, Object empty, int ticks, int fe, int manaAmount, String output, int count,
            String[] items, int[] counts) throws Exception {
        for (int i = 0; i < items.length; i++) {
            Object input = stack(items[i], counts[i]);
            require(counts[i] <= (Integer) call(input, "m_41741_"), "Input exceeds stack limit: " + items[i]);
            set.invoke(inputs, i, input);
        }
        require(count <= (Integer) call(stack(output, count), "m_41741_"), "Output exceeds stack limit: " + output);
        Method putProduct = products.getClass().getMethod("setStackInSlot", int.class, STACK);
        putProduct.invoke(products, 0, stack("minecraft:cobblestone",64));
        putProduct.invoke(products, 1, stack("minecraft:cobblestone",64));
        call(logic, "findAndHandleRecipe");
        require((Boolean) call(logic, "isIdle"), output + " started without FE and mana");
        charge.invoke(energy, fe, false);
        fillMana.invoke(mana, manaAmount);
        call(logic, "findAndHandleRecipe");
        require((Boolean) call(logic, "isIdle"), output + " started with blocked output slots");
        require((Integer) call(energy, "getEnergyStored") == fe && (Integer) call(mana, "getCurrentMana") == manaAmount, "Blocked output consumed power for " + output);
        for (int i=0; i<items.length; i++) {
            Object present=inputs.getClass().getMethod("getStackInSlot",int.class).invoke(inputs,i);
            require((Integer) call(present,"m_41613_")==counts[i], "Blocked output consumed inputs for " + output);
        }
        putProduct.invoke(products, 0, empty); putProduct.invoke(products, 1, empty);
        call(logic, "findAndHandleRecipe");
        for (int attempt = 0; attempt < 200 && (Boolean) call(logic, "isIdle"); attempt++) {
            call(logic, "findAndHandleRecipe");
            Thread.sleep(2);
        }
        require(!(Boolean) call(logic, "isIdle"), "Forge could not start " + output
            + " with FE=" + call(energy, "getEnergyStored") + " and mana=" + call(mana, "getCurrentMana"));
        for (int i = 0; i < ticks + 2; i++) call(logic, "serverTick");
        Object result = get.invoke(products, 0);
        require(itemId(result).equals(output) && (Integer) call(result, "m_41613_") == count, "Unexpected Forge output for " + output);
        require((Integer) call(energy, "getEnergyStored") == 0 && (Integer) call(mana, "getCurrentMana") == 0, "Incorrect energy or mana debit for " + output);
        for (int i = 0; i < items.length; i++) {
            Object remaining = inputs.getClass().getMethod("getStackInSlot", int.class).invoke(inputs, i);
            require((Boolean) call(remaining, "m_41619_"), "An input was left behind for " + output);
        }
        products.getClass().getMethod("setStackInSlot", int.class, STACK).invoke(products, 0, empty);
        call(logic, "resetRecipeLogic");
    }

    private static void auditGate(Object server, Object world, String trial) throws Exception {
        String gatewayId = "wombatcraft:trial_" + trial;
        Object registry = type("dev.shadowsoffire.gateways.gate.GatewayRegistry").getField("INSTANCE").get(null);
        Object holder = registry.getClass().getMethod("holder", LOCATION).invoke(registry, location(gatewayId));
        require((Boolean) call(holder, "isBound"), "Missing gateway " + gatewayId);
        Object gateway = call(holder, "get");
        require((Integer) call(gateway, "getNumWaves") == 3, "Expected three waves in " + gatewayId);
        Class<?> profile = type("com.mojang.authlib.GameProfile");
        Object player = type("net.minecraft.server.level.ServerPlayer").getConstructor(
                type("net.minecraft.server.MinecraftServer"), type("net.minecraft.server.level.ServerLevel"), profile)
            .newInstance(server, world, profile.getConstructor(UUID.class, String.class).newInstance(UUID.randomUUID(), "TrialAudit"));
        ENTITY.getMethod("m_6034_", double.class, double.class, double.class).invoke(player, 12.5, 180.0, 12.5);
        Object gate = gateway.getClass().getMethod("createEntity", type("net.minecraft.world.level.Level"),
                type("net.minecraft.world.entity.player.Player")).invoke(gateway, world, player);
        ENTITY.getMethod("m_6034_", double.class, double.class, double.class).invoke(gate, 12.5, 180.0, 12.5);
        world.getClass().getMethod("m_7967_", ENTITY).invoke(world, gate);
        Class<?> base = type("dev.shadowsoffire.gateways.entity.GatewayEntity");
        Field field = base.getDeclaredField("currentWaveEntities");
        field.setAccessible(true);
        Set<?> enemies = (Set<?>) field.get(gate);
        Method tick = gate.getClass().getMethod("m_8119_");
        Method health = type("net.minecraft.world.entity.LivingEntity").getMethod("m_21153_", float.class);
        for (int wave = 0; wave < 3; wave++) {
            for (int i = 0; i < 160 && enemies.isEmpty(); i++) tick.invoke(gate);
            require(!enemies.isEmpty(), "No mobs spawned in " + gatewayId + " wave " + (wave + 1));
            for (Object mob : enemies) health.invoke(mob, 0f);
            tick.invoke(gate);
            require((Integer) call(gate, "getWave") == wave + 1, "Wave did not advance in " + gatewayId);
        }
        // Simulated test ticks increment the entity's tick count so queued wave rewards can drop.
        Field tickCount = ENTITY.getField("f_19797_");
        for (int i = 0; i < 40 && !(Boolean) call(gate, "m_213877_"); i++) {
            tickCount.setInt(gate, tickCount.getInt(gate) + 1);
            tick.invoke(gate);
        }
        Class<?> box = type("net.minecraft.world.phys.AABB");
        Object region = box.getConstructor(double.class,double.class,double.class,double.class,double.class,double.class)
            .newInstance(0,179,0,25,190,25);
        Class<?> itemEntity = type("net.minecraft.world.entity.item.ItemEntity");
        Method getEntities = world.getClass().getMethod("m_45976_", Class.class, box);
        Collection<?> drops = (Collection<?>) getEntities.invoke(world, itemEntity, region);
        boolean found = false;
        for (Object drop : drops) {
            if (itemId(call(drop, "m_32055_")).equals("kubejs:trial_" + trial + "_crest")) found = true;
            call(drop, "m_146870_");
        }
        require(found, "The final crest did not drop for " + gatewayId);
    }

    private static Object storage(Object machine, String name) throws Exception {
        Object trait = machine.getClass().getMethod("getTraitByName", String.class).invoke(machine, name);
        require(trait != null, "Missing Forge trait " + name);
        return trait.getClass().getField("storage").get(trait);
    }

    private static Object stack(String id, int count) throws Exception {
        return STACK.getConstructor(ITEM_LIKE, int.class).newInstance(lookupItem(id), count);
    }

    private static Object lookupItem(String id) throws Exception {
        Object items = type("net.minecraftforge.registries.ForgeRegistries").getField("ITEMS").get(null);
        return type("net.minecraftforge.registries.IForgeRegistry").getMethod("getValue", LOCATION).invoke(items, location(id));
    }

    private static String itemId(Object stack) throws Exception {
        Object registry = type("net.minecraftforge.registries.ForgeRegistries").getField("ITEMS").get(null);
        return "" + type("net.minecraftforge.registries.IForgeRegistry").getMethod("getKey", Object.class)
            .invoke(registry, call(stack, "m_41720_"));
    }

    private static Object position(int x, int y, int z) throws Exception {
        return POSITION.getConstructor(int.class, int.class, int.class).newInstance(x, y, z);
    }

    private static Object location(String name) throws Exception {
        return LOCATION.getConstructor(String.class).newInstance(name);
    }

    private static Object find(Object parent, String returnType) throws Exception {
        for (Method method : parent.getClass().getMethods()) {
            if (method.getParameterCount() == 0 && method.getReturnType().getName().equals(returnType)) return method.invoke(parent);
        }
        throw new IllegalStateException("Missing accessor for " + returnType);
    }

    private static Object call(Object parent, String method) throws Exception {
        return parent.getClass().getMethod(method).invoke(parent);
    }

    private static Class<?> type(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(name, e);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
