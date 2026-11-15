function initializeCoreMod() {
    var ASMAPI = Java.type("net.minecraftforge.coremod.api.ASMAPI");
    var Opcodes = Java.type("org.objectweb.asm.Opcodes");
    var FrameNode = Java.type("org.objectweb.asm.tree.FrameNode");
    var InsnList = Java.type("org.objectweb.asm.tree.InsnList");
    var InsnNode = Java.type("org.objectweb.asm.tree.InsnNode");
    var JumpInsnNode = Java.type("org.objectweb.asm.tree.JumpInsnNode");
    var LabelNode = Java.type("org.objectweb.asm.tree.LabelNode");
    var MethodInsnNode = Java.type("org.objectweb.asm.tree.MethodInsnNode");
    var VarInsnNode = Java.type("org.objectweb.asm.tree.VarInsnNode");

    return {
        skipRemovedPlayers: {
            target: {
                type: "METHOD",
                class: "qouteall.imm_ptl.core.chunk_loading.NewChunkTrackingGraph",
                methodName: "lambda$tick$12",
                methodDesc: "(JLnet/minecraft/server/level/ServerPlayer;)V"
            },
            transformer: function (method) {
                // Forge 3.0.7 purges removed players' watch records but still
                // iterates them in the player list until the respawn request.
                var trackPlayer = new LabelNode();
                var guard = new InsnList();
                guard.add(new VarInsnNode(Opcodes.ALOAD, 2));
                guard.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL, "net/minecraft/server/level/ServerPlayer",
                    ASMAPI.mapMethod("m_213877_"), "()Z", false
                ));
                guard.add(new JumpInsnNode(Opcodes.IFEQ, trackPlayer));
                guard.add(new InsnNode(Opcodes.RETURN));
                guard.add(trackPlayer);
                guard.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
                method.instructions.insert(guard);
                ASMAPI.log("INFO", "Wombatcraft portal chunk guard applied");
                return method;
            }
        }
    };
}
