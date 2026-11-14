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

    static void run(Object server) throws Exception {
        Object world = call(server, "m_129783_");
        for (int x : new int[]{0, 16}) {
            for (int z : new int[]{0, 16}) {
                world.getClass().getMethod("m_46745_", POSITION).invoke(world, position(x, 180, z));
            }
        }
        Object source = call(server, "m_129893_");
        Object commands = find(server, "net.minecraft.commands.Commands");
        Method command = commands.getClass().getMethod("m_230957_", type("net.minecraft.commands.CommandSourceStack"), String.class);
        command.invoke(commands, source, "fill 0 179 0 24 179 24 minecraft:stone");

        try {
            auditForge(world, commands, source, command);
            for (String trial : TRIALS) auditGate(server, world, trial);
        } finally {
            command.invoke(commands, source, "fill 0 179 0 24 184 24 minecraft:air");
        }
        System.out.println("FRONTIER_GAMEPLAY_AUDIT_COMPLETE forge_recipes=3 trials=3");
    }

    private static void auditForge(Object world, Object commands, Object source, Method command) throws Exception {
        for (String placement : new String[]{
            "8 180 8 wombatcraft:convergence_forge",
            "7 180 8 create:brass_block", "9 180 8 create:brass_block",
            "7 181 8 minecraft:crying_obsidian", "8 181 8 create:brass_block",
            "9 181 8 minecraft:crying_obsidian"
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

        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 200, 200000, 12000,
            "kubejs:convergence_alloy", 4, new String[]{"create:brass_ingot", "botania:terrasteel_ingot", "ad_astra:ostrum_ingot"}, new int[]{8,1,2});
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 280, 400000, 30000,
            "kubejs:convergence_lens", 1, new String[]{"kubejs:convergence_alloy", "aether:ambrosium_shard", "botania:mana_diamond"}, new int[]{2,8,2});
        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, 600, 1000000, 75000,
            "kubejs:convergence_keystone", 1, new String[]{"kubejs:convergence_lens","kubejs:trial_twilight_crest","kubejs:trial_aether_crest",
                "kubejs:trial_otherside_crest","ad_astra:calorite_ingot","botania:gaia_ingot"}, new int[]{1,1,1,1,2,1});
    }

    private static void forge(Object logic, Object inputs, Object products, Object energy, Object mana, Method set, Method get,
            Method charge, Method fillMana, Object empty, int ticks, int fe, int manaAmount, String output, int count,
            String[] items, int[] counts) throws Exception {
        for (int i = 0; i < items.length; i++) set.invoke(inputs, i, stack(items[i], counts[i]));
        call(logic, "findAndHandleRecipe");
        require((Boolean) call(logic, "isIdle"), output + " started without FE and mana");
        charge.invoke(energy, fe, false);
        fillMana.invoke(mana, manaAmount);
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
