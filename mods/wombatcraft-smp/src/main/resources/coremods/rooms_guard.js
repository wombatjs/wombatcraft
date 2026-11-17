function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),J=Java.type('org.objectweb.asm.tree.JumpInsnNode'),B=Java.type('org.objectweb.asm.tree.LabelNode'),I=Java.type('org.objectweb.asm.tree.InsnNode');
 var guard='net/wombatcraft/smp/RoomsGuard',p='Lnet/minecraft/server/level/ServerPlayer;',pos='Lnet/minecraft/core/BlockPos;',id='Lnet/minecraft/resources/ResourceLocation;';
 function entry(m,name,desc,count){var c=new L(),ok=new B();for(var n=0;n<count;n++)c.add(new V(O.ALOAD,n));c.add(new C(O.INVOKESTATIC,guard,name,desc,false));c.add(new J(O.IFNE,ok));c.add(new I(O.RETURN));c.add(ok);m.instructions.insert(c);m.maxStack+=count;}
 var out={
 flightStart:{target:{type:'METHOD',class:'dev.jeryn.mc.rwf.common.entity.FlightTracker',methodName:'startFlight',methodDesc:'(Lnet/minecraft/server/level/ServerLevel;'+p+')V'},transformer:function(m){var c=new L(),ok=new B();c.add(new V(O.ALOAD,1));c.add(new C(O.INVOKESTATIC,'net/wombatcraft/smp/UtilityGuard','travel','('+p+')Z',false));c.add(new J(O.IFNE,ok));c.add(new I(O.RETURN));c.add(ok);m.instructions.insert(c);m.maxStack++;return m;}},
 flightHop:{target:{type:'METHOD',class:'dev.jeryn.mc.rwf.util.RWFTeleport',methodName:'canTeleportTo',methodDesc:'('+pos+'Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;)Z'},transformer:function(m){var c=new L(),ok=new B();for(var i=0;i<3;i++)c.add(new V(O.ALOAD,i));c.add(new C(O.INVOKESTATIC,guard,'hop','('+pos+'Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;)Z',false));c.add(new J(O.IFNE,ok));c.add(new I(O.ICONST_0));c.add(new I(O.IRETURN));c.add(ok);m.instructions.insert(c);m.maxStack+=3;return m;}}
 };
 ['leonus.tardis_rooms.room.RoomGrowth','leonus.tardis_rooms.network.InterfacePayload','leonus.tardis_rooms.network.ManagePayloads$Rename','leonus.tardis_rooms.iface.InterfaceTools'].forEach(function(cls,index){out['rooms'+index]={target:{type:'CLASS',name:cls},transformer:function(node){
  var calls=0,grow=0,remove=0;
  for(var j=0;j<node.methods.size();j++){var m=node.methods.get(j);
   for(var k=0;k<m.instructions.size();k++){var n=m.instructions.get(k);if(n.getOpcode()===O.INVOKEVIRTUAL&&(n.owner==='net/minecraft/world/level/Level'||n.owner==='net/minecraft/server/level/ServerLevel')&&n.desc==='(Lnet/minecraft/world/entity/player/Player;'+pos+')Z'){
    m.instructions.set(n,new C(O.INVOKESTATIC,guard,'interact','(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;'+pos+')Z',false));calls++;
   }}
   if(m.name==='tryGrow'&&m.desc==='('+p+pos+id+pos+')V'){entry(m,'grow','('+p+pos+id+pos+')Z',4);grow++;}
   if(m.name==='tryRemove'&&m.desc==='('+p+pos+'Z)V'){entry(m,'remove','('+p+pos+')Z',2);remove++;}
  }
  if(calls!==(index===0?2:1)||(index===0&&(grow!==1||remove!==1)))throw new Error('Unexpected TARDIS Rooms 1.2.0 permission hooks: '+cls);
  print('Wombatcraft Rooms claim guard applied: '+cls);return node;
 }};});return out;
}
