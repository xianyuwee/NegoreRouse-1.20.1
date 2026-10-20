package net.xianyu.prinegorerouse.event;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.BladeRecipeReloadPacket;
import net.xianyu.prinegorerouse.NetworkPacketHandler;
import net.xianyu.prinegorerouse.data.DifficultySavedData;
import net.xianyu.prinegorerouse.data.NRBladeRuntimeRecipeRegistry;
import net.xianyu.prinegorerouse.data.WeaponSystem;
import net.xianyu.prinegorerouse.prinegorerouse;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
                // 根节点不设权限门槛，使 difficulty get 对所有权限组开放；受限判定下沉到 reload、blade 子节点。
                .then(Commands.literal("reload")
                        .requires(NRCommands::canUseCommand)
                        .then(Commands.literal("recipes")
                                .executes(NRCommands::executeReloadRecipes)
                        )
                )
                // ========== difficulty 子命令：get 全员开放；set 需作弊权限，改后自动热加载配方 ==========
                .then(Commands.literal("difficulty")
                        .then(Commands.literal("get")
                                .executes(NRCommands::executeDifficultyGet)
                        )
                        .then(Commands.literal("set")
                                .requires(NRCommands::canUseCommand)
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
                                        .executes(NRCommands::executeDifficultySet)
                                )
                        )
                )
                // ========== 新增 blade give 子命令：直接给出成型刀（带外观/属性/效果） ==========
                // 用法: /prinegorerouse blade give <玩家> <刀名> [数量]
                // 例: /prinegorerouse blade give @s aritemiss_blade
                .then(Commands.literal("blade")
                        .requires(NRCommands::canUseCommand)
                        .then(Commands.literal("give")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests(BLADE_SUGGESTIONS)
                                                .executes(NRCommands::executeGiveBlade)
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                        .executes(NRCommands::executeGiveBlade)
                                                )
                                        )
                                )
                        )
                )
        );
    }

    // 查询当前难度
    private static int executeDifficultyGet(CommandContext<CommandSourceStack> ctx) {
        int diff = WeaponSystem.getDifficulty();
        ctx.getSource().sendSuccess(() -> Component.literal("§e当前武器合成难度: §a" + diff), false);
        return 1;
    }

    // 设置难度：写入世界存档数据（不依赖配置文件）→ 应用到运行时 → 热加载配方并同步客户端。
    private static int executeDifficultySet(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        int value = Math.max(1, IntegerArgumentType.getInteger(ctx, "value"));

        // 持久化到世界存档
        DifficultySavedData.get(server.overworld()).setDifficulty(value);
        // 应用到运行时武器系统（重算各武器实时合成要求）
        WeaponSystem.setDifficulty(value);

        source.sendSuccess(() -> Component.literal("§a武器合成难度已设置为 §6" + value + "§a，正在热加载配方..."), true);

        server.execute(() -> {
            reloadAndSync(server);
            source.sendSuccess(() -> Component.literal("§a配方难度热加载完成！"), true);
        });
        return value;
    }

    // 补全本模组注册的所有刀定义（named_blades 动态注册表中命名空间为本模组的条目）
    private static final SuggestionProvider<CommandSourceStack> BLADE_SUGGESTIONS = (ctx, builder) -> {
        Registry<SlashBladeDefinition> registry =
                ctx.getSource().registryAccess().registryOrThrow(SlashBladeDefinition.REGISTRY_KEY);
        List<String> names = registry.keySet().stream()
                .filter(rl -> rl.getNamespace().equals(prinegorerouse.MOD_ID))
                .map(ResourceLocation::getPath)
                .collect(Collectors.toList());
        return SharedSuggestionProvider.suggest(names, builder);
    };

    // 给出成型刀：复用拔刀剑官方 SlashBladeDefinition#getBlade()，与合成产出完全一致
    private static int executeGiveBlade(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        String path = StringArgumentType.getString(ctx, "name");
        int count = 1;
        try {
            count = IntegerArgumentType.getInteger(ctx, "count");
        } catch (IllegalArgumentException ignored) {
            // 未提供 count 时保持 1
        }
        ResourceLocation key = new ResourceLocation(prinegorerouse.MOD_ID, path);

        Registry<SlashBladeDefinition> registry =
                source.registryAccess().registryOrThrow(SlashBladeDefinition.REGISTRY_KEY);
        SlashBladeDefinition def;
        try {
            def = registry.getOrThrow(ResourceKey.create(SlashBladeDefinition.REGISTRY_KEY, key));
        } catch (RuntimeException ex) {
            def = null;
        }
        if (def == null) {
            source.sendFailure(Component.literal("§c找不到刀定义: " + key));
            return 0;
        }

        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer player : targets) {
            for (int i = 0; i < count; i++) {
                // 每次都要新建一份，getBlade() 返回的是全新的完整配置 ItemStack
                ItemStack blade = def.getBlade();
                if (!player.getInventory().add(blade)) {
                    player.drop(blade, false);
                }
            }
        }

        // 用对应的中文翻译键替换英文名（path）：def.getTranslationKey() = "item.<namespace>.<path>"，
        // 与 lang 文件中的键一致，可随客户端语言本地化显示刀名。def 不是 effectively final，
        // 先在 lambda 外取出键字符串。
        final String bladeNameKey = def.getTranslationKey();
        // 把给予对象（玩家名）写进回复句；多个目标用“、”连接。
        final String targetNames = targets.stream()
                .map(ServerPlayer::getScoreboardName)
                .collect(Collectors.joining("、"));
        int total = count * targets.size();
        source.sendSuccess(() -> Component.literal("§a已给予 " + targetNames + " " + total + " 把 ")
                .append(Component.translatable(bladeNameKey)), true);
        return total;
    }

    // 权限判定：仅指定 ID 的玩家（xyu0721 / xianyuwee / Dev）、拥有作弊权限(OP) 或创造模式下的玩家可用；
    // 非玩家来源（服务器控制台 / 命令方块 / 函数）按 OP 权限处理。
    private static boolean canUseCommand(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return source.hasPermission(2);
        }
        String name = player.getGameProfile().getName();
        if (name.equals("xyu0721") || name.equals("xianyuwee") || name.equals("Dev")) {
            return true;
        }
        if (source.hasPermission(2)) {
            return true;
        }
        return player.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
    }

    // 重新注入动态配方并把更新包同步给所有在线客户端（须在服务器线程调用）。
    private static void reloadAndSync(MinecraftServer server) {
        NRBladeRuntimeRecipeRegistry.refreshRecipes(server.getRecipeManager());

        // 同步所有客户端：先发配方更新包（客户端据此更新原版合成与 RecipeManager），
        // 紧随其后发本模组的“重载”包，触发客户端强制刷新 JEI 配方索引（同一连接保序）。
        ClientboundUpdateRecipesPacket packet = new ClientboundUpdateRecipesPacket(server.getRecipeManager().getRecipes());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(packet);
            NetworkPacketHandler.sendToClientPlayer(new BladeRecipeReloadPacket(), player);
        }
    }

    // 执行配方重载
    private static int executeReloadRecipes(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();

        source.sendSuccess(() -> Component.literal("§a正在重新加载拔刀剑配方..."), true);

        server.execute(() -> {
            reloadAndSync(server);
            source.sendSuccess(() -> Component.literal("§a拔刀剑配方重载完成！"), true);
        });

        return 1;
    }
}
