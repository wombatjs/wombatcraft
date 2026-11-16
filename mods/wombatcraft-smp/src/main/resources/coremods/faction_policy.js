function initializeCoreMod() {
    var O=Java.type('org.objectweb.asm.Opcodes');
    var List=Java.type('org.objectweb.asm.tree.InsnList');
    var Var=Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Call=Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Insn=Java.type('org.objectweb.asm.tree.InsnNode');
    var Jump=Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var Label=Java.type('org.objectweb.asm.tree.LabelNode');
    var desc='(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/core/BlockPos;Ldev/ftb/mods/ftbchunks/api/Protection;Lnet/minecraft/world/entity/Entity;)';
    return {wombatcraftFactionPolicy:{
        target:{type:'METHOD',class:'dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl',methodName:'shouldPreventInteraction',methodDesc:desc+'Z'},
        transformer:function(method){
            var code=new List(),fallback=new Label();
            for(var i=1;i<=5;i++)code.add(new Var(O.ALOAD,i));
            code.add(new Call(O.INVOKESTATIC,'net/wombatcraft/smp/ClaimHooks','decide',desc+'I',false));
            code.add(new Insn(O.DUP));code.add(new Jump(O.IFLT,fallback));code.add(new Insn(O.IRETURN));
            code.add(fallback);code.add(new Insn(O.POP));method.instructions.insert(code);
            method.maxStack=Math.max(method.maxStack,5);return method;
        }
    }};
}
