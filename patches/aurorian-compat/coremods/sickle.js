function initializeCoreMod() {
    var ASMAPI=Java.type('net.minecraftforge.coremod.api.ASMAPI');
    var Opcodes=Java.type('org.objectweb.asm.Opcodes');
    var FieldInsnNode=Java.type('org.objectweb.asm.tree.FieldInsnNode');
    return {emptyCraftingRemainder: {
        target:{type:'METHOD',class:'com.shiroroku.theaurorian.item.SickleItem',methodName:'getCraftingRemainingItem',methodDesc:'(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;'},
        transformer:function(method) {
            var replaced=0;
            for(var insn=method.instructions.getFirst();insn!==null;){
                var next=insn.getNext();
                if(insn.getOpcode()===Opcodes.ACONST_NULL){
                    method.instructions.set(insn,new FieldInsnNode(Opcodes.GETSTATIC,'net/minecraft/world/item/ItemStack',ASMAPI.mapField('f_41583_'),'Lnet/minecraft/world/item/ItemStack;'));
                    replaced++;
                }
                insn=next;
            }
            if(replaced!==1)throw new Error('Unexpected Aurorian sickle remainder: '+replaced);
            ASMAPI.log('INFO','Wombatcraft Aurorian crafting remainder fix applied');
            return method;
        }
    }};
}
