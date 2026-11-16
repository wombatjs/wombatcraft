function initializeCoreMod(){
    var O=Java.type('org.objectweb.asm.Opcodes'),List=Java.type('org.objectweb.asm.tree.InsnList');
    var Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Jump=Java.type('org.objectweb.asm.tree.JumpInsnNode'),Label=Java.type('org.objectweb.asm.tree.LabelNode'),Insn=Java.type('org.objectweb.asm.tree.InsnNode');
    function transformer(className,desc,guard,nullResult){return {
        target:{type:'CLASS',name:className},
        transformer:function(node){
            var found=0;
            for(var i=0;i<node.methods.size();i++){
                var method=node.methods.get(i);if(method.desc!==desc)continue;found++;
                var code=new List(),allow=new Label();code.add(new Var(O.ALOAD,0));
                code.add(new Call(O.INVOKESTATIC,'net/wombatcraft/smp/ArenaGuard',guard,'(L'+className.replace(/\./g,'/')+';)Z',false));
                code.add(new Jump(O.IFNE,allow));code.add(new Insn(nullResult?O.ACONST_NULL:O.ICONST_0));
                code.add(new Insn(nullResult?O.ARETURN:O.IRETURN));code.add(allow);method.instructions.insert(code);
                method.maxStack=Math.max(method.maxStack,1);
            }
            if(found!==1)throw new Error('Arena terrain guard expected one matching method in '+className+', found '+found);
            return node;
        }
    };}
    return {
        arenaLevel:transformer('net.minecraft.world.level.Level','(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z','allowLevel',false),
        arenaChunk:transformer('net.minecraft.world.level.chunk.LevelChunk','(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;','allowChunk',true)
    };
}
