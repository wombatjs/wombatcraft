package net.wombatcraft.smp;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(WombatcraftSmp.ID)
public final class WombatcraftSmp {
    public static final String ID="wombatcraft_smp";
    public static final Logger LOGGER=LogUtils.getLogger();
    private static final ForgeConfigSpec SPEC;
    static final ForgeConfigSpec.BooleanValue ENABLED, TPA_GUARD, CHUNK_POLICIES, ELECTIONS, CONTRACTS;
    static final ForgeConfigSpec.ConfigValue<String> SPAWN_TEAM;
    static {
        var b=new ForgeConfigSpec.Builder();
        ENABLED=b.comment("Enable community menus, civic residents and supported integrations.").define("enabled",false);
        TPA_GUARD=b.comment("Pinned FTB Essentials 2001.2.4 acceptance guard.").define("tpaGuard",true);
        CHUNK_POLICIES=b.comment("Staging-only per-chunk faction policy integration.").define("chunkPolicies",false);
        ELECTIONS=b.comment("Enable two-stage elections and current mayor policies.").define("elections",false);
        CONTRACTS=b.comment("Spawn-only contract deliveries and currency collection.").define("contracts",false);
        SPAWN_TEAM=b.comment("FTB server-team UUID owning the town claim.").define("spawnTeam", "2de87e97-dce4-41d0-b02d-c24a395997a1",v->v instanceof String && ((String)v).matches("[0-9a-fA-F-]{36}"));
        SPEC=b.build();
    }
    private final TpaGuard tpa=new TpaGuard();
    public static boolean clientEnabled(){return ENABLED.get();}
    public WombatcraftSmp() {
        ConvergenceContent.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        ConvergenceResearch.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,SPEC);
        SmpNetwork.register();
        Factions.register();
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.server.ServerStartedEvent e)->PersonalProgress.initialize(e.getServer()));
        CivicResidents.register();
        MayorPolicies.register();
        ExpansionGuard.register();
        TutorialSpawn.register();
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(this::command);
        MinecraftForge.EVENT_BUS.addListener(this::stopped);
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e)->SmpNetwork.leave(e.getEntity().getUUID()));
    }
    private void command(CommandEvent event) {if(ENABLED.get()&&TPA_GUARD.get())tpa.command(event);}
    private void stopped(ServerStoppedEvent event){tpa.clear();SmpNetwork.clear();ContractService.clear();JourneyService.clear();}
    private void commands(RegisterCommandsEvent event) {
        JourneyService.register(event);
        event.getDispatcher().register(Commands.literal("contracts").requires(s->ENABLED.get()&&CONTRACTS.get()).executes(c->{
            var player=c.getSource().getPlayerOrException();
            SmpNetwork.view(player,0,"",0,4);return 1;
        }));
        event.getDispatcher().register(Commands.literal("townhall").requires(s->ENABLED.get()&&ELECTIONS.get()).executes(c->{SmpNetwork.view(c.getSource().getPlayerOrException(),0,"",0,3);return 1;}));
        event.getDispatcher().register(Commands.literal("faction").requires(s->ENABLED.get()).executes(c->{
            SmpNetwork.view(c.getSource().getPlayerOrException(),0,"");return 1;
        }));
        event.getDispatcher().register(Commands.literal("wcsmp").requires(s->s.hasPermission(2))
                .then(Commands.literal("audit").executes(c->StagingAudit.run(c.getSource())))
                .then(Commands.literal("status").executes(c->{
                    c.getSource().sendSuccess(()->Component.literal("Wombatcraft Communities: "
                            +(ENABLED.get()?"enabled":"disabled")+". Elections: "+ELECTIONS.get()+"; contracts: "+CONTRACTS.get()+". Arena duels and logout inventory handoff remain unavailable."),false);
                    return 1;
                })));
    }
}
