// Allocate each mob's variant on that mob, never on vanilla Mob. Allocating
// subclass fields on Mob shifts unrelated entities according to class-load
// order and causes different client/server layouts (including EvilCraft spirits).
function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var Type = Java.type('org.objectweb.asm.Type');
    var result = {};
    ['divinerpg.entities.vanilla.overworld.EntityAequorea', 'divinerpg.entities.boss.EntityAyeraco'].forEach(function(name) {
        result[name] = {
            target: {type: 'CLASS', name: name},
            transformer: function(node) {
                var changed = 0;
                for (var i = 0; i < node.methods.size(); i++) {
                    var m = node.methods.get(i);
                    if (m.name !== '<clinit>') continue;
                    for (var it = m.instructions.getFirst(); it !== null; it = it.getNext()) {
                        if (it.getOpcode() !== O.LDC || String(it.cst) !== 'Lnet/minecraft/world/entity/Mob;') continue;
                        var serializer = it.getNext(), call = serializer === null ? null : serializer.getNext();
                        if (serializer === null || serializer.getOpcode() !== O.GETSTATIC || call === null || call.getOpcode() !== O.INVOKESTATIC || call.owner !== 'net/minecraft/network/syncher/SynchedEntityData') throw new Error('DivineRPG variant registration shape changed');
                        it.cst = Type.getObjectType(node.name); changed++;
                    }
                }
                if (changed !== 1) throw new Error('Expected one DivineRPG base-Mob variant registration: ' + name + ', got ' + changed);
                Java.type('net.minecraftforge.coremod.api.ASMAPI').log('INFO', 'Wombatcraft DivineRPG entity data fixed: ' + name);
                return node;
            }
        };
    });
    return result;
}
