// JITL sends every knowledge category on every player tick. Keep the periodic
// refresh at 1 Hz; XP gains still use the original immediate sendPacket path.
function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var FieldNode = Java.type('org.objectweb.asm.tree.FieldNode');
    var Field = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var Insns = Java.type('org.objectweb.asm.tree.InsnList');
    var Insn = Java.type('org.objectweb.asm.tree.InsnNode');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var IntInsn = Java.type('org.objectweb.asm.tree.IntInsnNode');
    var Jump = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var Label = Java.type('org.objectweb.asm.tree.LabelNode');
    var Frame = Java.type('org.objectweb.asm.tree.FrameNode');
    return {wombatcraftJitlIdleSync: {
        target: {type: 'CLASS', name: 'net.jitl.common.capability.stats.PlayerStats'},
        transformer: function(node) {
            var field = 'wombatcraft$syncTicks', method = null;
            for (var i = 0; i < node.fields.size(); i++) if (node.fields.get(i).name === field) throw new Error('Duplicate JITL sync throttle');
            for (var j = 0; j < node.methods.size(); j++) {
                var m = node.methods.get(j);
                if (m.name === 'update' && m.desc === '(Lnet/minecraft/world/entity/player/Player;)V') method = m;
            }
            if (method === null) throw new Error('JITL periodic stats update changed');
            node.fields.add(new FieldNode(O.ACC_PRIVATE, field, 'I', null, null));
            var code = new Insns(), sync = new Label();
            code.add(new Var(O.ALOAD, 0)); code.add(new Insn(O.DUP));
            code.add(new Field(O.GETFIELD, node.name, field, 'I')); code.add(new Insn(O.DUP_X1));
            code.add(new Insn(O.ICONST_1)); code.add(new Insn(O.IADD));
            code.add(new Field(O.PUTFIELD, node.name, field, 'I'));
            code.add(new IntInsn(O.BIPUSH, 20)); code.add(new Insn(O.IREM));
            code.add(new Jump(O.IFEQ, sync)); code.add(new Insn(O.RETURN));
            code.add(sync); code.add(new Frame(O.F_SAME, 0, null, 0, null));
            method.instructions.insert(code); method.maxStack = Math.max(method.maxStack, 4);
            Java.type('net.minecraftforge.coremod.api.ASMAPI').log('INFO', 'Wombatcraft JITL idle synchronization applied');
            return node;
        }
    }};
}
