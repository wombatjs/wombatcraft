// JITL 2.2.4 applies these client capabilities directly on Netty's thread.
// Queue them through Forge's context so respawn packets and capability updates
// run in order on the client thread. Ignore updates for an invalid old player;
// the server's periodic synchronization supplies the replacement player's data.
function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var Method = Java.type('org.objectweb.asm.tree.MethodNode');
    var Insns = Java.type('org.objectweb.asm.tree.InsnList');
    var Insn = Java.type('org.objectweb.asm.tree.InsnNode');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Field = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var TypeInsn = Java.type('org.objectweb.asm.tree.TypeInsnNode');
    var Jump = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var Context = 'net/minecraftforge/network/NetworkEvent$Context';
    var result = {};
    [
        ['Stats', 'net.jitl.client.stats.PacketPlayerStats', 'stats/PlayerStatsProvider', 'PLAYER_STATS'],
        ['Essence', 'net.jitl.core.network.PacketEssenceBar', 'essence/PlayerEssenceProvider', 'PLAYER_ESSENCE']
    ].forEach(function(spec) {
        result['wombatcraftJitl' + spec[0]] = {
            target: {type: 'CLASS', name: spec[1]},
            transformer: function(node) {
                if (node.interfaces.contains('java/lang/Runnable')) throw new Error('JITL Runnable shape changed');
                var handler = null;
                for (var i = 0; i < node.methods.size(); i++) {
                    var m = node.methods.get(i);
                    if (m.name === 'handle' && m.desc === '(Ljava/util/function/Supplier;)V') handler = m;
                    if (m.name === 'run' && m.desc === '()V') throw new Error('JITL run method already exists');
                }
                if (handler === null) throw new Error('Missing JITL packet handler');
                var mc = null, player = null, end = null, handled = null;
                for (var it = handler.instructions.getFirst(); it !== null; it = it.getNext()) {
                    if (it.getOpcode() === O.INVOKESTATIC && it.owner === 'net/minecraft/client/Minecraft') mc = it;
                    if (it.getOpcode() === O.GETFIELD && it.owner === 'net/minecraft/client/Minecraft') player = it;
                    if (it.getOpcode() === O.IFNULL) end = it.label;
                    if (it.getOpcode() === O.INVOKEVIRTUAL && it.owner === Context && it.name === 'setPacketHandled') handled = it;
                }
                if (mc === null || player === null || end === null || handled === null) throw new Error('JITL handler shape changed');
                var cursor = handled;
                var expected = [O.INVOKEVIRTUAL, O.ICONST_1, O.CHECKCAST, O.INVOKEINTERFACE, O.ALOAD];
                for (var k = 0; k < expected.length; k++) {
                    if (cursor === null || cursor.getOpcode() !== expected[k]) throw new Error('JITL handled sequence changed');
                    var previous = cursor.getPrevious(); handler.instructions.remove(cursor); cursor = previous;
                }
                handler.name = 'run'; handler.desc = '()V'; handler.signature = null;
                handler.access = O.ACC_PUBLIC; handler.maxLocals = 1;
                if (handler.localVariables !== null) handler.localVariables.clear();
                handler.parameters = null;
                var guard = new Insns();
                function currentPlayer() {
                    guard.add(new Call(mc.getOpcode(), mc.owner, mc.name, mc.desc, mc.itf));
                    guard.add(new Field(player.getOpcode(), player.owner, player.name, player.desc));
                }
                currentPlayer(); guard.add(new Jump(O.IFNULL, end));
                currentPlayer();
                guard.add(new Field(O.GETSTATIC, 'net/jitl/common/capability/' + spec[2], spec[3], 'Lnet/minecraftforge/common/capabilities/Capability;'));
                guard.add(new Call(O.INVOKEVIRTUAL, 'net/minecraft/client/player/LocalPlayer', 'getCapability', '(Lnet/minecraftforge/common/capabilities/Capability;)Lnet/minecraftforge/common/util/LazyOptional;', false));
                guard.add(new Call(O.INVOKEVIRTUAL, 'net/minecraftforge/common/util/LazyOptional', 'isPresent', '()Z', false));
                guard.add(new Jump(O.IFEQ, end));
                handler.instructions.insert(guard);
                node.interfaces.add('java/lang/Runnable');
                var queued = new Method(O.ACC_PUBLIC, 'handle', '(Ljava/util/function/Supplier;)V', null, null);
                queued.instructions.add(new Var(O.ALOAD, 1));
                queued.instructions.add(new Call(O.INVOKEINTERFACE, 'java/util/function/Supplier', 'get', '()Ljava/lang/Object;', true));
                queued.instructions.add(new TypeInsn(O.CHECKCAST, Context));
                queued.instructions.add(new Var(O.ASTORE, 2));
                queued.instructions.add(new Var(O.ALOAD, 2)); queued.instructions.add(new Var(O.ALOAD, 0));
                queued.instructions.add(new Call(O.INVOKEVIRTUAL, Context, 'enqueueWork', '(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;', false));
                queued.instructions.add(new Insn(O.POP));
                queued.instructions.add(new Var(O.ALOAD, 2)); queued.instructions.add(new Insn(O.ICONST_1));
                queued.instructions.add(new Call(O.INVOKEVIRTUAL, Context, 'setPacketHandled', '(Z)V', false));
                queued.instructions.add(new Insn(O.RETURN));
                queued.maxStack = 2; queued.maxLocals = 3; node.methods.add(queued);
                Java.type('net.minecraftforge.coremod.api.ASMAPI').log('INFO', 'Wombatcraft JITL ' + spec[0] + ' packet threading applied');
                return node;
            }
        };
    });
    return result;
}
