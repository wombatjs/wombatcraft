package net.wombatcraft.smp;

import com.mojang.brigadier.context.ParsedArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.fml.ModList;
import net.wombatcraft.smp.rules.TpaReceipts;
import java.lang.reflect.*;
import java.util.*;

/** Pinned adapter for installed FTB Essentials 2001.2.4; retains its teleport and warmup implementation. */
final class TpaGuard {
    private final TpaReceipts receipts=new TpaReceipts();
    private Field requests;
    private Method accept,deny,target,uuid;
    private boolean initialized;
    private void initialize() throws ReflectiveOperationException {
        if(initialized)return;
        String version=ModList.get().getModContainerById("ftbessentials").orElseThrow().getModInfo().getVersion().toString();
        if(!version.equals("2001.2.4"))throw new IllegalStateException("Unverified FTB Essentials version: "+version);
        Class<?> commands=Class.forName("dev.ftb.mods.ftbessentials.command.TPACommands");
        Class<?> request=Class.forName("dev.ftb.mods.ftbessentials.command.TPACommands$TPARequest");
        requests=commands.getDeclaredField("REQUESTS");requests.setAccessible(true);
        accept=commands.getMethod("tpaccept",ServerPlayer.class,String.class);
        deny=commands.getMethod("tpdeny",ServerPlayer.class,String.class);
        target=request.getMethod("target");
        uuid=target.getReturnType().getMethod("getUuid");initialized=true;
    }
    void command(CommandEvent event) {
        var context=event.getParseResults().getContext();
        if(context.getNodes().isEmpty() || !event.getParseResults().getExceptions().isEmpty())return;
        String root=context.getNodes().get(0).getNode().getName();
        if(!root.equals("tpaccept")&&!root.equals("tpdeny"))return;
        ServerPlayer player=context.getSource().getPlayer();if(player==null)return;
        ParsedArgument<CommandSourceStack,?> arg=context.getArguments().get("id");
        if(arg==null || !(arg.getResult() instanceof String id))return;
        event.setCanceled(true);
        try {
            initialize();
            Map<?,?> active=(Map<?,?>)requests.get(null);Object request=active.get(id);
            long now=System.nanoTime()/1_000_000;
            if(request==null) {
                player.sendSystemMessage(Component.literal(receipts.wasAccepted(id,player.getUUID(),now)
                        ? "That request was already accepted. A pending teleport will continue unless interrupted."
                        : "That teleport request has expired or is no longer available."));return;
            }
            if(!player.getUUID().equals(uuid.invoke(target.invoke(request)))) {
                player.sendSystemMessage(Component.literal("Only the requested player can answer this teleport request."));return;
            }
            (root.equals("tpaccept")?accept:deny).invoke(null,player,id);
            if(root.equals("tpaccept")&&!active.containsKey(id))receipts.accepted(id,player.getUUID(),now);
        } catch (ReflectiveOperationException|RuntimeException e) {
            WombatcraftSmp.LOGGER.error("TPA compatibility guard failed; request not retried",e);
            player.sendSystemMessage(Component.literal("Teleport request could not be processed. Please contact an administrator."));
        }
    }
    void clear(){receipts.clear();}
}
