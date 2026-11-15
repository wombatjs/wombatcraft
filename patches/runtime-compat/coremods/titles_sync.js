// Titles 3.8.4 clears the outer map on client synchronization but then expects
// all five AwardType buckets to exist. Restore the constructor's empty buckets
// before applying the packet. This also keeps a subsequent integrated-server
// datapack reload from failing after returning to the title screen.
function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var List = Java.type('org.objectweb.asm.tree.InsnList');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Field = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var Type = Java.type('org.objectweb.asm.tree.TypeInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Insn = Java.type('org.objectweb.asm.tree.InsnNode');
    return {wombatcraftTitlesSync: {
        target: {type: 'METHOD', class: 'aurilux.titles.common.core.TitleRegistry',
            methodName: 'processServerData',
            methodDesc: '(Laurilux/titles/common/network/messages/PacketSyncDatapack;)V'},
        transformer: function(method) {
            var clear = null;
            for (var node = method.instructions.getFirst(); node !== null; node = node.getNext()) {
                if (node.getOpcode() === O.INVOKEINTERFACE && node.owner === 'java/util/Map' &&
                    node.name === 'clear' && node.desc === '()V') {
                    if (clear !== null) throw new Error('Unexpected extra Titles clear');
                    clear = node;
                }
            }
            if (clear === null) throw new Error('Titles sync clear not found');
            var code = new List();
            ['ADVANCEMENT', 'COMMAND', 'CONTRIBUTOR', 'LOOT', 'STARTING'].forEach(function(name) {
                code.add(new Var(O.ALOAD, 0));
                code.add(new Field(O.GETFIELD, 'aurilux/titles/common/core/TitleRegistry', 'titles', 'Ljava/util/Map;'));
                code.add(new Field(O.GETSTATIC, 'aurilux/titles/api/Title$AwardType', name, 'Laurilux/titles/api/Title$AwardType;'));
                code.add(new Type(O.NEW, 'java/util/HashMap'));
                code.add(new Insn(O.DUP));
                code.add(new Call(O.INVOKESPECIAL, 'java/util/HashMap', '<init>', '()V', false));
                code.add(new Call(O.INVOKEINTERFACE, 'java/util/Map', 'put', '(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;', true));
                code.add(new Insn(O.POP));
            });
            method.instructions.insert(clear, code);
            method.maxStack = Math.max(method.maxStack, 4);
            return method;
        }
    }};
}
