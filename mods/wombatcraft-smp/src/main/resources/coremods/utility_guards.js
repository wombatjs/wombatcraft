// Adapters for the exact pinned utility jars. Fail closed on unexpected upstream signatures.
function initializeCoreMod(){
    var O=Java.type('org.objectweb.asm.Opcodes'),List=Java.type('org.objectweb.asm.tree.InsnList');
    var Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Jump=Java.type('org.objectweb.asm.tree.JumpInsnNode'),Label=Java.type('org.objectweb.asm.tree.LabelNode'),Insn=Java.type('org.objectweb.asm.tree.InsnNode');
    var guard='net/wombatcraft/smp/UtilityGuard',pos='Lnet/minecraft/core/BlockPos;',player='Lnet/minecraft/world/entity/player/Player;',sp='Lnet/minecraft/server/level/ServerPlayer;';
    function call(code,name,desc){code.add(new Call(O.INVOKESTATIC,guard,name,desc,false));}
    return {
        gravePlacement:{target:{type:'METHOD',class:'de.maxhenkel.gravestone.events.DeathEvents',methodName:'playerDeath',methodDesc:'(Lde/maxhenkel/gravestone/corelib/death/PlayerDeathEvent;)V'},transformer:function(m){
            var event='de/maxhenkel/gravestone/corelib/death/PlayerDeathEvent',entry=new List(),ok=new Label();
            entry.add(new Var(O.ALOAD,1));entry.add(new Call(O.INVOKEVIRTUAL,event,'getPlayer','()'+sp,false));call(entry,'allowGrave','('+sp+')Z');
            entry.add(new Jump(O.IFNE,ok));entry.add(new Insn(O.RETURN));entry.add(ok);m.instructions.insert(entry);
            var found=0;
            for(var i=0;i<m.instructions.size();i++){var n=m.instructions.get(i);if(n.getOpcode()===O.INVOKESTATIC&&n.owner==='de/maxhenkel/gravestone/GraveUtils'&&n.name==='getGraveStoneLocation'){
                var c=new List();c.add(new Var(O.ALOAD,1));c.add(new Call(O.INVOKEVIRTUAL,event,'getPlayer','()'+sp,false));call(c,'gravePosition','('+pos+sp+')'+pos);m.instructions.insert(n,c);found++;
            }}
            if(found!==1)throw new Error('Unexpected GraveStone placement implementation');m.maxStack+=2;return m;
        }},
        elevatorAccess:{target:{type:'METHOD',class:'xyz.vsngamer.elevatorid.network.TeleportHandler',methodName:'isBadTeleportPacket',methodDesc:'(Lxyz/vsngamer/elevatorid/network/TeleportRequest;'+sp+')Z'},transformer:function(m){
            var c=new List(),ok=new Label(),req='xyz/vsngamer/elevatorid/network/TeleportRequest';c.add(new Var(O.ALOAD,1));
            c.add(new Var(O.ALOAD,0));c.add(new Call(O.INVOKEVIRTUAL,req,'getFrom','()'+pos,false));c.add(new Var(O.ALOAD,0));c.add(new Call(O.INVOKEVIRTUAL,req,'getTo','()'+pos,false));
            call(c,'elevator','('+sp+pos+pos+')Z');c.add(new Jump(O.IFNE,ok));c.add(new Insn(O.ICONST_1));c.add(new Insn(O.IRETURN));c.add(ok);m.instructions.insert(c);m.maxStack+=3;return m;
        }},
        miningPlacement:{target:{type:'CLASS',name:'com.direwolf20.mininggadgets.common.items.MiningGadget'},transformer:function(node){
            var found=0;
            for(var j=0;j<node.methods.size();j++){var m=node.methods.get(j);if(m.desc!=='(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;I)V')continue;
                for(var i=0;i<m.instructions.size();i++){var n=m.instructions.get(i);if(n.getOpcode()===O.INVOKEVIRTUAL&&n.owner==='net/minecraft/world/level/Level'&&n.desc==='('+pos+'Lnet/minecraft/world/level/block/state/BlockState;)Z'){
                    m.instructions.insertBefore(n,new Var(O.ALOAD,2));m.instructions.set(n,new Call(O.INVOKESTATIC,guard,'setMiningBlock','(Lnet/minecraft/world/level/Level;'+pos+'Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/LivingEntity;)Z',false));found++;
                }}m.maxStack+=1;
            }if(found!==2)throw new Error('Unexpected Mining Gadgets placement implementation: '+found);return node;
        }},
        miningFreeze:{target:{type:'METHOD',class:'com.direwolf20.mininggadgets.common.tiles.RenderBlockTileEntity',methodName:'replaceBlockWithAlternative',methodDesc:'(Lnet/minecraft/world/level/Level;'+pos+'Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/item/ItemStack;II)I'},transformer:function(m){
            var c=new List(),ok=new Label();c.add(new Var(O.ALOAD,0));c.add(new Call(O.INVOKEVIRTUAL,'com/direwolf20/mininggadgets/common/tiles/RenderBlockTileEntity','getPlayer','()'+player,false));c.add(new Var(O.ALOAD,2));call(c,'edit','('+player+pos+')Z');c.add(new Jump(O.IFNE,ok));c.add(new Insn(O.ICONST_0));c.add(new Insn(O.IRETURN));c.add(ok);m.instructions.insert(c);m.maxStack+=2;return m;
        }}
    };
}
