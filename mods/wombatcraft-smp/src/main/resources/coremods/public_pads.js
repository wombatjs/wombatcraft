function initializeCoreMod() {
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),F=Java.type('org.objectweb.asm.tree.FieldInsnNode'),J=Java.type('org.objectweb.asm.tree.JumpInsnNode'),B=Java.type('org.objectweb.asm.tree.LabelNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),M=Java.type('org.objectweb.asm.tree.MethodNode'),API=Java.type('net.minecraftforge.coremod.api.ASMAPI');
 var hook='net/wombatcraft/smp/PublicPads',operator='whocraft/tardis_refined/common/capability/tardis/TardisLevelOperator',pilot='whocraft/tardis_refined/common/tardis/manager/TardisPilotingManager',nav='Lwhocraft/tardis_refined/common/tardis/TardisNavLocation;',op='L'+operator+';';
 function operatorArg(c){c.add(new V(O.ALOAD,0));c.add(new F(O.GETFIELD,pilot,'operator',op));}
 return {
  publicPadFlight:{target:{type:'CLASS',name:pilot.replace(/\//g,'.')},transformer:function(node){
   var count=0;
   for(var j=0;j<node.methods.size();j++){var m=node.methods.get(j),c=new L(),ok=new B();
    if((m.name==='beginFlight'&&m.desc==='(Z)Z')||(m.name==='endFlight'&&m.desc==='(ZZ)Z')){
     operatorArg(c);c.add(new I(m.name==='endFlight'?O.ICONST_1:O.ICONST_0));c.add(new C(O.INVOKESTATIC,hook,'flight','('+op+'Z)Z',false));c.add(new J(O.IFNE,ok));c.add(new I(O.ICONST_0));c.add(new I(O.IRETURN));c.add(ok);m.instructions.insert(c);m.maxStack+=2;count++;
    }
    if(m.name==='findClosestValidPosition'&&m.desc==='('+nav+'I)Ljava/util/Optional;'){
     operatorArg(c);c.add(new V(O.ALOAD,1));c.add(new C(O.INVOKESTATIC,hook,'exactLanding','('+op+nav+')Ljava/util/Optional;',false));c.add(new I(O.DUP));c.add(new J(O.IFNULL,ok));c.add(new I(O.ARETURN));c.add(ok);c.add(new I(O.POP));m.instructions.insert(c);m.maxStack+=2;count++;
    }
   }if(count!==3)throw new Error('Refined 2.1.7 pad flight hooks changed: '+count);print('Wombatcraft public pad flight guards applied');return node;
  }},
  publicPadPlacement:{target:{type:'METHOD',class:operator.replace(/\//g,'.'),methodName:'setOrUpdateExteriorBlock',methodDesc:'('+nav+'Ljava/util/Optional;ZLwhocraft/tardis_refined/api/event/ShellChangeSource;)V'},transformer:function(m){
   var c=new L();c.add(new V(O.ALOAD,0));c.add(new V(O.ALOAD,1));c.add(new C(O.INVOKESTATIC,hook,'requirePlacement','('+op+nav+')V',false));m.instructions.insert(c);m.maxStack+=2;print('Wombatcraft public pad placement guard applied');return m;
  }},
  publicPadRemoval:{target:{type:'CLASS',name:'whocraft.tardis_refined.common.block.device.LandingPadBlock'},transformer:function(node){
   var state='Lnet/minecraft/world/level/block/state/BlockState;',level='Lnet/minecraft/world/level/Level;',pos='Lnet/minecraft/core/BlockPos;',desc='('+state+level+pos+state+'Z)V',name=API.mapMethod('m_6810_');
   for(var i=0;i<node.methods.size();i++)if(node.methods.get(i).name===name&&node.methods.get(i).desc===desc)throw new Error('Refined pad removal now overrides onRemove; update integration');
   var m=new M(O.ACC_PUBLIC,name,desc,null,null),c=m.instructions;
   [2,3,1,4].forEach(function(n){c.add(new V(O.ALOAD,n));});c.add(new C(O.INVOKESTATIC,hook,'removed','('+level+pos+state+state+')V',false));
   for(var n=0;n<=4;n++)c.add(new V(O.ALOAD,n));c.add(new V(O.ILOAD,5));c.add(new C(O.INVOKESPECIAL,'net/minecraft/world/level/block/Block',name,desc,false));c.add(new I(O.RETURN));m.maxStack=6;m.maxLocals=6;node.methods.add(m);print('Wombatcraft public pad removal hook applied');return node;
  }},
  publicPadMonitor:{target:{type:'METHOD',class:'whocraft.tardis_refined.client.screen.main.MonitorScreen',methodName:'createSelectionList',methodDesc:'()Lwhocraft/tardis_refined/client/screen/components/GenericMonitorSelectionList;'},transformer:function(m){
   var count=0;for(var i=m.instructions.size()-1;i>=0;i--){var n=m.instructions.get(i);if(n.getOpcode()===O.ARETURN){var c=new L(),skip=new B();c.add(new I(O.DUP));c.add(new J(O.IFNULL,skip));c.add(new I(O.DUP));c.add(new C(O.INVOKESTATIC,'net/wombatcraft/smp/client/PublicPadsScreen','extend','(Lwhocraft/tardis_refined/client/screen/components/GenericMonitorSelectionList;)V',false));c.add(skip);m.instructions.insertBefore(n,c);count++;}}
   if(count!==2)throw new Error('Refined monitor returns changed: '+count);m.maxStack+=2;print('Wombatcraft public pad monitor entry applied');return m;
  }},
  publicPadFreeFlight:{target:{type:'METHOD',class:'dev.jeryn.mc.rwf.common.entity.FlightTracker',methodName:'finishFlight',methodDesc:'(Lnet/minecraft/server/level/ServerPlayer;Z)V'},transformer:function(m){
   var c=new L(),ok=new B();c.add(new V(O.ALOAD,0));c.add(new V(O.ILOAD,1));c.add(new C(O.INVOKESTATIC,hook,'freeFlight','(Lnet/minecraft/server/level/ServerPlayer;Z)Z',false));c.add(new J(O.IFNE,ok));c.add(new I(O.RETURN));c.add(ok);m.instructions.insert(c);m.maxStack+=2;print('Wombatcraft public pad exterior flight guard applied');return m;
  }}
 };
}
