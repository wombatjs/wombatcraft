// These pinned plugin versions retain the preceding JEI runtime in static fields.
// Deferred recipe loading can then keep both complete recipe indexes alive until
// the new load finishes, exhausting the heap before that handoff can happen.
// Use JEI's normal lifecycle callback; leave registration and recipes unchanged.
function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var Method = Java.type('org.objectweb.asm.tree.MethodNode');
    var Field = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var Insn = Java.type('org.objectweb.asm.tree.InsnNode');
    var Insns = Java.type('org.objectweb.asm.tree.InsnList');
    var desc = 'Lmezz/jei/api/runtime/IJeiRuntime;';
    var result = {};
    [
        ['Create', 'com.simibubi.create.compat.jei.CreateJEI', 'runtime'],
        ['Ars', 'com.hollingsworth.arsnouveau.client.jei.JEIArsNouveauPlugin', 'jeiRuntime'],
        ['Occultism', 'com.klikli_dev.occultism.integration.jei.JeiPlugin', 'runtime'],
        ['AE2', 'appeng.integration.modules.jei.JEIPlugin', 'jeiRuntime', true]
    ].forEach(function(spec) {
        result['wombatcraftJeiCleanup' + spec[0]] = {
            target: {type: 'CLASS', name: spec[1]},
            transformer: function(node) {
                var found = false;
                for (var i = 0; i < node.fields.size(); i++) {
                    var f = node.fields.get(i);
                    if (f.name === spec[2] && f.desc === desc && (f.access & O.ACC_STATIC) !== 0) found = true;
                }
                if (!found) throw new Error('JEI runtime field changed: ' + spec[1]);
                for (var j = 0; j < node.methods.size(); j++) {
                    var existing = node.methods.get(j);
                    if (existing.name === 'onRuntimeUnavailable' && existing.desc === '()V') {
                        if (spec[3]) {
                            var end = null, returns = 0;
                            for (var it = existing.instructions.getFirst(); it !== null; it = it.getNext()) {
                                if (it.getOpcode() === O.PUTSTATIC && it.owner === node.name && it.name === spec[2])
                                    throw new Error('AE2 already clears JEI runtime; review patch');
                                if (it.getOpcode() === O.RETURN) { end = it; returns++; }
                            }
                            if (returns !== 1) throw new Error('AE2 cleanup shape changed');
                            var clear = new Insns();
                            clear.add(new Insn(O.ACONST_NULL));
                            clear.add(new Field(O.PUTSTATIC, node.name, spec[2], desc));
                            existing.instructions.insertBefore(end, clear);
                            return node;
                        }
                        throw new Error('Plugin now supplies JEI cleanup; review compatibility patch: ' + spec[1]);
                    }
                }
                if (spec[3]) throw new Error('Missing AE2 cleanup method');
                var method = new Method(O.ACC_PUBLIC, 'onRuntimeUnavailable', '()V', null, null);
                method.instructions.add(new Insn(O.ACONST_NULL));
                method.instructions.add(new Field(O.PUTSTATIC, node.name, spec[2], desc));
                method.instructions.add(new Insn(O.RETURN));
                method.maxStack = 1;
                method.maxLocals = 1;
                node.methods.add(method);
                return node;
            }
        };
    });
    return result;
}
