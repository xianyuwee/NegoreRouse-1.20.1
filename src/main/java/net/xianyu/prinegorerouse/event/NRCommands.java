package net.xianyu.prinegorerouse.event;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.data.NRBladeRuntimeRecipeRegistry;
import net.xianyu.prinegorerouse.prinegorerouse;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = prinegorerouse.MOD_ID)
public class NRCommands {

    // 自动注册指令
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    // 指令注册核心
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("prinegorerouse")
                .requires(source -> {
                    // 1. 管理员(OP) 直接使用
                    if (source.hasPermission(2)) {
                        return true;
                    }
                    // 2. 非玩家无法使用
                    if (!source.isPlayer()) {
                        return false;
                    }
                    // 3. 单人/局域网：仅房主可用（修复参数错误）
                    ServerPlayer player = source.getPlayer();
                    MinecraftServer server = player.getServer();
                    return server != null && server.isSingleplayerOwner(player.getGameProfile());
                })
                .then(Commands.literal("reload")
                        .then(Commands.literal("recipes")
                                .executes(NRCommands::executeReloadRecipes)
                        )
                )
        );
    }

    // 执行配方重载
    private static int executeReloadRecipes(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();

        source.sendSuccess(() -> Component.literal("§a正在重新加载拔刀剑配方..."), true);

        server.execute(() -> {
            NRBladeRuntimeRecipeRegistry.refreshRecipes(server.getRecipeManager());

            // 同步所有客户端
            ClientboundUpdateRecipesPacket packet = new ClientboundUpdateRecipesPacket(server.getRecipeManager().getRecipes());
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.connection.send(packet);
            }

            source.sendSuccess(() -> Component.literal("§a拔刀剑配方重载完成！"), true);
        });

        return 1;
    }
}