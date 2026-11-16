package net.wombatcraft.smp;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.ForgeRegistries;

final class MayorPolicies {
    static void register(){MinecraftForge.EVENT_BUS.addListener(MayorPolicies::tick);}
    private static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!WombatcraftSmp.ENABLED.get()||!WombatcraftSmp.ELECTIONS.get()||event.getServer().getTickCount()%40!=0)return;
        var state=CivicState.get(event.getServer());state.advance();var policy=CandidateCatalog.ALL.get(state.election.mayor());
        if(!policy.has("boon_item"))return;
        String match=policy.get("boon_item").getAsString();var effect=ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(policy.get("boon_effect").getAsString()));
        if(effect==null)return;
        // Two hands per online player, every two seconds. No block, inventory or machine scans.
        for(var player:event.getServer().getPlayerList().getPlayers()){
            if(player.isSpectator()||!player.isAlive())continue;
            boolean eligible=false;
            for(var stack:player.getHandSlots()){
                var id=ForgeRegistries.ITEMS.getKey(stack.getItem());
                if(id!=null&&(match.endsWith(":")?id.toString().startsWith(match):id.toString().equals(match)))eligible=true;
            }
            if(!eligible)continue;
            var current=player.getEffect(effect);
            // Night vision flashes below ten seconds; keep its renewal above that threshold.
            int duration=effect==net.minecraft.world.effect.MobEffects.NIGHT_VISION?260:80;
            if(current==null||current.getAmplifier()==0&&current.getDuration()<duration-20)
                player.addEffect(new MobEffectInstance(effect,duration,0,true,false,true));
        }
    }
}
