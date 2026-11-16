package net.wombatcraft.smp;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

final class StagingAudit {
    static int run(CommandSourceStack source){
        if(!WombatcraftSmp.ENABLED.get())throw new IllegalStateException("Staging is disabled");
        var dimension=ResourceKey.create(Registries.DIMENSION,new ResourceLocation(ArenaGuard.DIMENSION));
        var level=source.getServer().getLevel(dimension);
        if(level==null)throw new IllegalStateException("Arena dimension did not load");
        BlockPos pos=new BlockPos(0,64,0);var original=level.getBlockState(pos);
        try {
            ArenaGuard.build(()->level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState()));
            if(!level.getBlockState(pos).is(Blocks.STONE))throw new IllegalStateException("Scoped build failed");
            boolean changed=level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            if(changed||!level.getBlockState(pos).is(Blocks.STONE))throw new IllegalStateException("World block guard bypassed");
            level.getChunkAt(pos).setBlockState(pos,Blocks.AIR.defaultBlockState(),false);
            if(!level.getBlockState(pos).is(Blocks.STONE))throw new IllegalStateException("Direct chunk guard bypassed");
            try{ArenaGuard.build(()->{throw new IllegalStateException("intentional scope test");});}catch(IllegalStateException expected){}
            if(ArenaGuard.allowLevel(level))throw new IllegalStateException("Build permission leaked after exception");
            if(!ArenaGuard.allowLevel(source.getServer().overworld()))throw new IllegalStateException("Overworld edits affected");
            source.sendSuccess(()->Component.literal("SMP_AUDIT_PASS: world/chunk edits blocked; scoped build and exception recovery passed; overworld unaffected."),false);
            return 1;
        }finally{ArenaGuard.build(()->level.setBlockAndUpdate(pos,original));}
    }
}
