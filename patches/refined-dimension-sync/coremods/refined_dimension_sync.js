function initializeCoreMod() {
    var ASMAPI = Java.type("net.minecraftforge.coremod.api.ASMAPI");
    var Opcodes = Java.type("org.objectweb.asm.Opcodes");
    var InsnList = Java.type("org.objectweb.asm.tree.InsnList");
    var MethodInsnNode = Java.type("org.objectweb.asm.tree.MethodInsnNode");
    var VarInsnNode = Java.type("org.objectweb.asm.tree.VarInsnNode");

    var owner = "qouteall/q_misc_util/forge/networking/Dim_Sync";
    var context = "net/minecraftforge/network/NetworkEvent$Context";

    function expect(node, opcode) {
        if (node === null || node.getOpcode() !== opcode) {
            throw new Error("Unexpected Immersive Portals 3.0.7 Dim_Sync handler");
        }
        return node;
    }

    return {
        refinedDimensionSync: {
            target: {
                type: "METHOD",
                class: owner.replace(/\//g, "."),
                methodName: "handle",
                methodDesc: "(Ljava/util/function/Supplier;)Z"
            },
            transformer: function (method) {
                var queued = ASMAPI.findFirstMethodCall(
                    method, ASMAPI.MethodType.VIRTUAL, context, "enqueueWork",
                    "(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;"
                );
                expect(queued, Opcodes.INVOKEVIRTUAL);
                var callback = expect(queued.getPrevious(), Opcodes.INVOKEDYNAMIC);
                var contextArg = expect(callback.getPrevious(), Opcodes.ALOAD);
                var packet = expect(contextArg.getPrevious(), Opcodes.ALOAD);
                var contextReceiver = expect(packet.getPrevious(), Opcodes.ALOAD);
                var pop = expect(queued.getNext(), Opcodes.POP);
                if (contextArg.var !== 2 || packet.var !== 0 || contextReceiver.var !== 2) {
                    throw new Error("Unexpected Immersive Portals packet or context slot");
                }

                // The handler already queues the dimension-type and world updates
                // on the render thread. Publish the numeric lookup during packet
                // handling so Netty can decode the next packet in arrival order.
                var publish = new InsnList();
                publish.add(new VarInsnNode(Opcodes.ALOAD, 0));
                publish.add(new VarInsnNode(Opcodes.ALOAD, 2));
                publish.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL, owner, "lambda$handle$1",
                    "(L" + context + ";)V", false
                ));
                method.instructions.insertBefore(contextReceiver, publish);
                [contextReceiver, packet, contextArg, callback, queued, pop].forEach(function (node) {
                    method.instructions.remove(node);
                });
                ASMAPI.log("INFO", "Wombatcraft Refined dimension sync ordering applied");
                return method;
            }
        }
    };
}
