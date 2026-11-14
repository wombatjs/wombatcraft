// Keep the Forge project definitions in the managed pack config directory.
function initializeCoreMod() {
    var Opcodes = Java.type('org.objectweb.asm.Opcodes');
    var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
    return {wombatcraftMachineProjects: {
        target: {type: 'METHOD', class: 'com.lowdragmc.mbd2.MBD2', methodName: 'createDir', methodDesc: '()Ljava/io/File;'},
        transformer: function(method) {
            for (var insn = method.instructions.getFirst(); insn !== null; insn = insn.getNext()) {
                if (insn.getOpcode() !== Opcodes.INVOKESTATIC || insn.owner !== 'com/lowdragmc/lowdraglib/LDLib' || insn.name !== 'getLDLibDir') continue;
                var path = insn.getNext();
                if (path.getOpcode() !== Opcodes.LDC || path.cst !== 'assets/mbd2') break;
                method.instructions.set(insn, new InsnNode(Opcodes.ACONST_NULL));
                path.cst = 'config/wombatcraft-mbd2';
                return method;
            }
            throw new Error('Unexpected Multiblocked2 project location');
        }
    }};
}
