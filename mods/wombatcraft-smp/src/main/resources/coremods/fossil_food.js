// Fossils 9.3.4.0 still references End's Delight's pre-2.x registry names.
// Remap only this optional adapter; retain every original food mapping.
function initializeCoreMod() {
    return { wombatcraft_fossil_ends_delight: {
        target: { type: 'CLASS', name: 'com.github.teamfossilsarcheology.fossil.forge.compat.farmers.addon.EndDelightCompat' },
        transformer: function(node) {
            for (var m = 0; m < node.methods.size(); m++) {
                var instructions = node.methods.get(m).instructions;
                for (var i = 0; i < instructions.size(); i++) {
                    var insn = instructions.get(i);
                    if (insn.getOpcode() !== 178) continue;
                    if (insn.owner === 'cn/foggyhillside/ends_delight/registry/ItemRegistry' ||
                        insn.owner === 'cn/foggyhillside/ends_delight/registry/BlockRegistry') {
                        insn.owner = insn.owner.replace('ItemRegistry', 'ModItems').replace('BlockRegistry', 'ModBlocks');
                        insn.name = String(insn.name).replace(/([a-z0-9])([A-Z])/g, '$1_$2').toUpperCase();
                    }
                }
            }
            return node;
        }
    }};
}
