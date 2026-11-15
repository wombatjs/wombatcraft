// JEI's generated anvil/disenchantment examples multiply every enchantment,
// level and item into millions of capability-bearing ItemStacks in this pack.
// Keep vanilla equipment examples and all normal recipes, repairs and mod
// categories. This changes only generated JEI previews, never enchanting rules.
// Advanced opt-out: -Dwombatcraft.fullEnchantmentPreviews=true
function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var API = Java.type('net.minecraftforge.coremod.api.ASMAPI');
    var Type = Java.type('org.objectweb.asm.Type');
    var Handle = Java.type('org.objectweb.asm.Handle');
    var Method = Java.type('org.objectweb.asm.tree.MethodNode');
    var Insns = Java.type('org.objectweb.asm.tree.InsnList');
    var Insn = Java.type('org.objectweb.asm.tree.InsnNode');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Field = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var Ldc = Java.type('org.objectweb.asm.tree.LdcInsnNode');
    var Label = Java.type('org.objectweb.asm.tree.LabelNode');
    var Jump = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var Frame = Java.type('org.objectweb.asm.tree.FrameNode');
    var Dynamic = Java.type('org.objectweb.asm.tree.InvokeDynamicInsnNode');
    var bootstrap = new Handle(O.H_INVOKESTATIC, 'java/lang/invoke/LambdaMetafactory', 'metafactory',
        '(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;', false);
    var result = {};
    [
        ['Anvil', 'mezz.jei.library.plugins.vanilla.anvil.AnvilRecipeMaker', 'getBookEnchantmentRecipes', '()Ljava/util/stream/Stream;', 'net/minecraft/world/item/ItemStack', 1],
        ['Grindstone', 'mezz.jei.library.plugins.vanilla.grindstone.GrindstoneRecipeMaker', 'getDisenchantRecipes', '(Lmezz/jei/common/platform/IPlatformRecipeHelper;Lnet/minecraft/world/inventory/GrindstoneMenu;)Ljava/util/stream/Stream;', 'net/minecraft/world/item/Item', 2]
    ].forEach(function(spec) {
        result['wombatcraftJeiPreview' + spec[0]] = {
            target: {type: 'CLASS', name: spec[1]},
            transformer: function(node) {
                var target = null, name = 'wombatcraft$previewEquipment';
                for (var i=0;i<node.methods.size();i++) {
                    var m=node.methods.get(i);
                    if (m.name===name) throw new Error('JEI preview filter already applied');
                    if (m.name===spec[2] && m.desc===spec[3]) target=m;
                }
                if (target===null) throw new Error('JEI preview method changed: '+spec[1]);
                var streams=0, anchor=null;
                for(var it=target.instructions.getFirst();it!==null;it=it.getNext()) {
                    if ((it.getOpcode()===O.INVOKEINTERFACE || it.getOpcode()===O.INVOKEVIRTUAL) && it.desc==='()Ljava/util/stream/Stream;' && (spec[0]==='Anvil' ? it.owner==='java/util/Collection' : it.owner==='net/minecraft/core/Registry')) {
                        streams++; if(streams===spec[5]) anchor=it;
                    }
                }
                if(streams!==spec[5] || anchor===null) throw new Error('JEI stream shape changed: '+spec[1]);
                var desc='(L'+spec[4]+';)Z';
                var filter=new Insns();
                filter.add(new Dynamic('test','()Ljava/util/function/Predicate;',bootstrap,
                    Type.getMethodType('(Ljava/lang/Object;)Z'),
                    new Handle(O.H_INVOKESTATIC,node.name,name,desc,false),Type.getMethodType(desc)
                ));
                filter.add(new Call(O.INVOKEINTERFACE,'java/util/stream/Stream','filter','(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;',true));
                target.instructions.insert(anchor,filter);
                target.maxStack=Math.max(target.maxStack,2);
                var helper=new Method(O.ACC_PRIVATE|O.ACC_STATIC|O.ACC_SYNTHETIC,name,desc,null,null);
                var normal=new Label();
                helper.instructions.add(new Ldc('wombatcraft.fullEnchantmentPreviews'));
                helper.instructions.add(new Call(O.INVOKESTATIC,'java/lang/Boolean','getBoolean','(Ljava/lang/String;)Z',false));
                helper.instructions.add(new Jump(O.IFEQ,normal));
                helper.instructions.add(new Insn(O.ICONST_1));helper.instructions.add(new Insn(O.IRETURN));
                helper.instructions.add(normal);helper.instructions.add(new Frame(O.F_SAME,0,null,0,null));
                helper.instructions.add(new Ldc('minecraft'));
                helper.instructions.add(new Field(O.GETSTATIC,'net/minecraftforge/registries/ForgeRegistries','ITEMS','Lnet/minecraftforge/registries/IForgeRegistry;'));
                helper.instructions.add(new Var(O.ALOAD,0));
                if(spec[0]==='Anvil') helper.instructions.add(new Call(O.INVOKEVIRTUAL,spec[4],API.mapMethod('m_41720_'),'()Lnet/minecraft/world/item/Item;',false));
                helper.instructions.add(new Call(O.INVOKEINTERFACE,'net/minecraftforge/registries/IForgeRegistry','getKey','(Ljava/lang/Object;)Lnet/minecraft/resources/ResourceLocation;',true));
                helper.instructions.add(new Call(O.INVOKEVIRTUAL,'net/minecraft/resources/ResourceLocation',API.mapMethod('m_135827_'),'()Ljava/lang/String;',false));
                helper.instructions.add(new Call(O.INVOKEVIRTUAL,'java/lang/String','equals','(Ljava/lang/Object;)Z',false));
                helper.instructions.add(new Insn(O.IRETURN));helper.maxLocals=1;helper.maxStack=3;
                node.methods.add(helper);
                API.log('INFO','Wombatcraft compact JEI '+spec[0]+' previews applied');
                return node;
            }
        };
    });
    return result;
}
