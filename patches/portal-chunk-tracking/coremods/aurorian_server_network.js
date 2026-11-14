// Dedicated-server compatibility for Aurorian Reborn 1.0.1-fix.
// The upstream channel registers a method stripped by @OnlyIn(CLIENT).
// Keep packet IDs/codecs unchanged; the server never renders client boss bars.
function initializeCoreMod() {
    var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
    var Opcodes = Java.type('org.objectweb.asm.Opcodes');
    var Handle = Java.type('org.objectweb.asm.Handle');
    var MethodNode = Java.type('org.objectweb.asm.tree.MethodNode');
    var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var TypeInsnNode = Java.type('org.objectweb.asm.tree.TypeInsnNode');
    var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
    return {serverBossBarConsumer: {
        target: {type:'CLASS', name:'com.shiroroku.theaurorian.network.NetworkHandler'},
        transformer: function(cls) {
            var owner='com/shiroroku/theaurorian/network/NetworkHandler';
            var packet='com/shiroroku/theaurorian/network/client/SUpdateBossBar';
            var descriptor='(L'+packet+';Ljava/util/function/Supplier;)V';
            var replaced=0;
            for(var m=0;m<cls.methods.size();m++) {
                var method=cls.methods.get(m);
                if(method.name!=='register')continue;
                for(var insn=method.instructions.getFirst();insn!==null;insn=insn.getNext()) {
                    if(insn.getOpcode()!==Opcodes.INVOKEDYNAMIC)continue;
                    for(var a=0;a<insn.bsmArgs.length;a++) {
                        var arg=insn.bsmArgs[a];
                        if(arg instanceof Handle && arg.getOwner()===packet && arg.getName()==='handle') {
                            insn.bsmArgs[a]=new Handle(Opcodes.H_INVOKESTATIC,owner,'wombatcraft$serverBossBar',descriptor,false);
                            replaced++;
                        }
                    }
                }
            }
            if(replaced!==1)throw new Error('Unexpected Aurorian packet registration: '+replaced);
            var stub=new MethodNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,'wombatcraft$serverBossBar',descriptor,null,null);
            stub.instructions.add(new VarInsnNode(Opcodes.ALOAD,1));
            stub.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,'java/util/function/Supplier','get','()Ljava/lang/Object;',true));
            stub.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST,'net/minecraftforge/network/NetworkEvent$Context'));
            stub.instructions.add(new InsnNode(Opcodes.ICONST_1));
            stub.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,'net/minecraftforge/network/NetworkEvent$Context','setPacketHandled','(Z)V',false));
            stub.instructions.add(new InsnNode(Opcodes.RETURN));
            stub.maxStack=2;stub.maxLocals=2;cls.methods.add(stub);
            ASMAPI.log('INFO','Wombatcraft Aurorian dedicated-server packet guard applied');
            return cls;
        }
    }};
}
