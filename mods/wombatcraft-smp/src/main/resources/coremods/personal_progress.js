// Pinned FTB Quests 2001.4.22: isolate quests without changing FTB land ownership.
function initializeCoreMod() {
    var O=Java.type('org.objectweb.asm.Opcodes');
    var I=Java.type('org.objectweb.asm.tree.InsnNode');
    var V=Java.type('org.objectweb.asm.tree.VarInsnNode');
    var M=Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var T=Java.type('org.objectweb.asm.tree.TypeInsnNode');
    var helper='net/wombatcraft/smp/PersonalProgress';
    var result={};
    function replace(key,cls,name,desc,body) {
        result[key]={target:{type:'METHOD',class:cls,methodName:name,methodDesc:desc},transformer:function(m){
            m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!==null)m.localVariables.clear();
            body(m.instructions);return m;
        }};
    }
    var base='dev.ftb.mods.ftbquests.quest.BaseQuestFile';
    var server='dev.ftb.mods.ftbquests.quest.ServerQuestFile';
    var client='dev.ftb.mods.ftbquests.client.ClientQuestFile';
    var data='dev/ftb/mods/ftbquests/quest/TeamData';
    [base].forEach(function(cls,index){
        replace('personalData'+index,cls,'getOrCreateTeamData','(Lnet/minecraft/world/entity/Entity;)L'+data+';',function(i){
            i.add(new V(O.ALOAD,0));i.add(new V(O.ALOAD,1));
            i.add(new M(O.INVOKESTATIC,helper,'data','(Ljava/lang/Object;Lnet/minecraft/world/entity/Entity;)Ljava/lang/Object;',false));
            i.add(new T(O.CHECKCAST,data));i.add(new I(O.ARETURN));
        });
    });
    [server,client].forEach(function(cls,index){
        replace('personalOwner'+index,cls,'isPlayerOnTeam','(Lnet/minecraft/world/entity/player/Player;L'+data+';)Z',function(i){
            i.add(new V(O.ALOAD,1));i.add(new V(O.ALOAD,2));
            i.add(new M(O.INVOKESTATIC,helper,'owns','(Lnet/minecraft/world/entity/player/Player;Ljava/lang/Object;)Z',false));i.add(new I(O.IRETURN));
        });
    });
    ['playerChangedTeam','teamCreated'].forEach(function(name){
        var event=name==='teamCreated'?'TeamCreatedEvent':'PlayerChangedTeamEvent';
        replace('noProgressTransfer'+name,server,name,'(Ldev/ftb/mods/ftbteams/api/event/'+event+';)V',function(i){i.add(new I(O.RETURN));});
    });
    replace('personalRecipients','dev.ftb.mods.ftbquests.quest.TeamData','getOnlineMembers','()Ljava/util/Collection;',function(i){
        i.add(new V(O.ALOAD,0));i.add(new M(O.INVOKESTATIC,helper,'online','(Ljava/lang/Object;)Ljava/util/Collection;',false));i.add(new I(O.ARETURN));
    });
    result.personalClientLookup={target:{type:'METHOD',class:client,methodName:'getOrCreateTeamData',methodDesc:'(Lnet/minecraft/world/entity/Entity;)L'+data+';'},transformer:function(m){
        for(var i=m.instructions.getFirst();i!==null;i=i.getNext())if(i.getOpcode()===O.INVOKEVIRTUAL&&i.owner==='dev/ftb/mods/ftbteams/api/client/KnownClientPlayer'&&i.name==='teamId'){i.name='id';return m;}
        throw new Error('Unexpected FTB client player lookup');
    }};
    result.personalLogin={target:{type:'METHOD',class:server,methodName:'playerLoggedIn',methodDesc:'(Ldev/ftb/mods/ftbteams/api/event/PlayerLoggedInAfterTeamEvent;)V'},transformer:function(m){
        for(var i=m.instructions.getFirst();i!==null;i=i.getNext())if(i.getOpcode()===O.INVOKEVIRTUAL&&i.name==='getTeam'){
            m.instructions.set(i,new M(O.INVOKESTATIC,helper,'loginTeam','(Ldev/ftb/mods/ftbteams/api/event/PlayerLoggedInAfterTeamEvent;)Ldev/ftb/mods/ftbteams/api/Team;',false));return m;
        }
        throw new Error('Unexpected FTB Quests login structure');
    }};
    ['dev.ftb.mods.ftbquests.util.FTBQuestsInventoryListener','dev.ftb.mods.ftbquests.quest.reward.CommandReward'].forEach(function(cls,index){
        result['personalLookup'+index]={target:{type:'CLASS',name:cls},transformer:function(node){
            var changes=0;
            for(var n=0;n<node.methods.size();n++){
                var m=node.methods.get(n);
                for(var i=m.instructions.getFirst();i!==null;i=i.getNext())if(i.getOpcode()===O.INVOKEINTERFACE&&i.owner==='dev/ftb/mods/ftbteams/api/TeamManager'&&i.name==='getTeamForPlayer'){
                    m.instructions.set(i,new M(O.INVOKESTATIC,helper,'lookup','(Ldev/ftb/mods/ftbteams/api/TeamManager;Lnet/minecraft/server/level/ServerPlayer;)Ljava/util/Optional;',false));changes++;break;
                }
            }
            if(changes===0)throw new Error('Unexpected FTB personal lookup: '+cls);
            return node;
        }};
    });
    return result;
}
