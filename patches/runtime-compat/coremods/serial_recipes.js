// Some recipe serializers initialize shared, non-thread-safe enchantment maps.
// Keep KubeJS recipe construction sequential; filtering and gameplay are intact.
function initializeCoreMod() {
    var ASMAPI=Java.type('net.minecraftforge.coremod.api.ASMAPI');
    var Opcodes=Java.type('org.objectweb.asm.Opcodes');
    return {serialRecipeConstruction: {
        target:{type:'CLASS',name:'dev.latvian.mods.kubejs.recipe.RecipesEventJS'},
        transformer:function(cls) {
            var changed=0;
            for(var m=0;m<cls.methods.size();m++){
                var method=cls.methods.get(m);
                if(method.name!=='lambda$post$6' && method.name!=='lambda$post$7')continue;
                for(var insn=method.instructions.getFirst();insn!==null;insn=insn.getNext()){
                    if(insn.getOpcode()===Opcodes.INVOKEINTERFACE && insn.owner==='java/util/Collection' && insn.name==='parallelStream'){
                        insn.name='stream';changed++;
                    }
                }
            }
            if(changed!==2)throw new Error('Unexpected KubeJS recipe construction sites: '+changed);
            ASMAPI.log('INFO','Wombatcraft sequential recipe construction applied');
            return cls;
        }
    }};
}
