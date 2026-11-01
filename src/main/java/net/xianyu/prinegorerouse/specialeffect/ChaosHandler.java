package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.slashblade.RenderDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.xianyu.prinegorerouse.aquatime.AquaTimeData;
import net.xianyu.prinegorerouse.compat.MaidCompat;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.prinegorerouse;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;
import net.xianyu.prinegorerouse.utils.DamageTypeHolderUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * CHAOS（卡俄斯吞噬特效）的服务端逻辑集合。仅当刀为本 mod 的卡俄斯（{@link NRSpecialEffect#isChaosBlade}）时生效。
 * <p>功能：</p>
 * <ul>
 *   <li>吞噬其它拔刀剑：每种刀只吞一次；累加斩杀数(双倍)、耀魂(双倍)、锻造数、耐久、初始面板伤害、附魔(等级累加)，
 *       并复制其 SE 与 SA（均初始未激活）。</li>
 *   <li>获得的 SE 解锁费用改为 {@code i * k * 10000 * (n-1)}（线性；i=原经验需求，k=配置常数，n=该刀已激活获得 SE 数 + 1）。</li>
 *   <li>手持卡俄斯时时间素的“增加”改为“反向吞噬”：扣等量并记录，按权重 2:2:1 随机给 耀魂/杀敌/面板增益；池不足则仍增长。</li>
 *   <li>累计吞噬量每达一个数量级（1000,1e4,...）提升“阶段”，每阶段 +{@code dmg_per_stage} 最终伤害与 {@code void_per_stage} 虚空伤害。</li>
 *   <li>SA 可点选装备；随机模式在每次挥刀前从存储 SA 中随机选一并设为当前 SA。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = prinegorerouse.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChaosHandler {

    /** 卡俄斯专用子标签（挂在刀根 tag 下）。 */
    public static final String CHAOS_TAG = "Chaos";
    private static final String KEY_DEVOURED = "Devoured";
    private static final String KEY_MODELS = "DevouredModels";
    private static final String KEY_AQUA = "Aqua";
    private static final String KEY_SA = "SA";
    private static final String KEY_RANDOM = "Random";
    private static final String KEY_SA_DISABLED = "SaDisabled";

    /** 禁用/启用一个 SA 的时间素收支：禁用得 {@value}，重新启用花 {@value}。 */
    public static final long SA_TOGGLE_COST = 10000L;

    private static final Random RANDOM = new Random();

    private ChaosHandler() {
    }

    // ===== 内部工具 =====

    private static ISlashBladeState state(ItemStack stack) {
        return stack.getCapability(ItemSlashBlade.BLADESTATE).orElse(null);
    }

    /** 把状态变更序列化回刀 tag，以便持久化与同步到客户端。 */
    private static void persist(ItemStack stack, ISlashBladeState st) {
        if (stack.isEmpty() || st == null) {
            return;
        }
        stack.getOrCreateTag().put("bladeState", st.serializeNBT());
    }

    /** 读取卡俄斯子标签（只读；缺失时返回空标签）。 */
    private static CompoundTag chaosTag(ItemStack stack) {
        if (stack.isEmpty()) {
            return new CompoundTag();
        }
        return stack.getOrCreateTag().getCompound(CHAOS_TAG);
    }

    private static void saveChaos(ItemStack stack, CompoundTag c) {
        stack.getOrCreateTag().put(CHAOS_TAG, c);
    }

    private static String itemId(ItemStack stack) {
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return rl == null ? stack.getItem().toString() : rl.toString();
    }

    /**
     * 吞噬判重标识。普通刀按物理物品 id 即可区分；但同一把“无铭/无名换皮刀”共享同一物理物品 id，
     * 仅靠 bladeState 里的翻译键（决定建模/皮肤的 SlashBladeDefinition）区分不同皮肤，故此处再并入翻译键，
     * 使每种皮肤各自可吞一次，而非吞一把即屏蔽其余同物品皮肤。
     * 无翻译键时回退为纯物品 id，保证普通刀行为不变。
     */
    private static String identityKey(ItemStack target, ISlashBladeState ts) {
        String item = itemId(target);
        String tk = ts == null ? "" : ts.getTranslationKey();
        return (tk == null || tk.isEmpty()) ? item : item + "#" + tk;
    }

    private static String identityKey(ItemStack target) {
        return identityKey(target, state(target));
    }

    /** 字符串 ListTag 是否包含某值。 */
    private static boolean listContains(ListTag list, String s) {
        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(s)) {
                return true;
            }
        }
        return false;
    }

    // ===== 查询 =====

    /** 是否为可被卡俄斯交互的激活态卡俄斯（即本 mod 的卡俄斯）。 */
    public static boolean isChaos(ItemStack stack) {
        return NRSpecialEffect.isChaosBlade(stack);
    }

    public static boolean isDevoured(ItemStack chaos, ItemStack target) {
        if (target == null || target.isEmpty()) {
            return false;
        }
        return listContains(chaosTag(chaos).getList(KEY_DEVOURED, Tag.TAG_STRING), identityKey(target));
    }

    /** 该刀（若为卡俄斯）已吞噬的刀数量，供背包叠加统计；非卡俄斯返回 0。 */
    public static int devouredCount(ItemStack stack) {
        if (!NRSpecialEffect.isChaosBlade(stack)) {
            return 0;
        }
        return chaosTag(stack).getList(KEY_DEVOURED, Tag.TAG_STRING).size();
    }

    public static long devouredAqua(ItemStack stack) {
        return chaosTag(stack).getLong(KEY_AQUA);
    }

    /** 直接累加卡俄斯已吞时间素（不走反向吞噬钩子）；供 Empty 陷阱惩罚结算。仅服务端调用。 */
    public static void addDevouredAqua(ItemStack chaos, long delta) {
        if (delta <= 0L || !NRSpecialEffect.isChaosBlade(chaos)) {
            return;
        }
        CompoundTag c = chaosTag(chaos);
        c.putLong(KEY_AQUA, c.getLong(KEY_AQUA) + delta);
        saveChaos(chaos, c);
    }

    /**
     * 从卡俄斯被吞模型池中随机取一对建模/贴图（供 AbsolutePower 幻影刃选择外观）。
     * <p>池空时先按已吞列表（物品 id）从定义注册表惰性重建并回写，兼容旧存档吞入时未记录模型的情况。
     * 卡俄斯自身的模型也作为候选惰性补入池（{@code devour} 只记录被吞者，早期版本从不记录吞人者）。</p>
     *
     * @return 长度 2 的数组 {model, tex（可为 null）}；池仍空或解析失败返回 null。
     */
    public static ResourceLocation[] randomDevouredModel(ItemStack chaos, HolderLookup.Provider registries) {
        CompoundTag c = chaosTag(chaos);
        ListTag models = c.getList(KEY_MODELS, Tag.TAG_STRING);
        if (models.isEmpty()) {
            models = rebuildModelPool(chaos, registries);
        }
        // 把卡俄斯自身外观补进池：devour 只写被吞刀的模型，本体从未入池；旧存档也靠这里补齐
        ResourceLocation[] self = selfRender(chaos, registries);
        if (self != null && !listContains(models, modelEntry(self))) {
            models.add(StringTag.valueOf(modelEntry(self)));
            c.put(KEY_MODELS, models);
            saveChaos(chaos, c);
        }
        if (models.isEmpty()) {
            return null;
        }
        String s = models.getString(RANDOM.nextInt(models.size()));
        String[] parts = s.split("\\|", 2);
        ResourceLocation model = ResourceLocation.tryParse(parts[0]);
        if (model == null) {
            return null;
        }
        ResourceLocation tex = (parts.length > 1 && !parts[1].isEmpty()) ? ResourceLocation.tryParse(parts[1]) : null;
        return new ResourceLocation[]{model, tex};
    }

    /** 卡俄斯自身的建模/贴图：优先运行时 state 字段，为空则按翻译键/物品 id 回退定义注册表。 */
    private static ResourceLocation[] selfRender(ItemStack chaos, HolderLookup.Provider registries) {
        ISlashBladeState st = state(chaos);
        ResourceLocation model = st == null ? null : st.getModel().orElse(null);
        if (model != null) {
            return new ResourceLocation[]{model, st.getTexture().orElse(null)};
        }
        String tk = st == null ? "" : st.getTranslationKey();
        return renderOf(findDefinition(chaos, tk, registries));
    }

    /** 按已吞物品 id 列表从定义注册表重建模型池并回写；返回重建后的池。 */
    private static ListTag rebuildModelPool(ItemStack chaos, HolderLookup.Provider registries) {
        ListTag devoured = chaosTag(chaos).getList(KEY_DEVOURED, Tag.TAG_STRING);
        ListTag models = new ListTag();
        for (int i = 0; i < devoured.size(); i++) {
            // 已吞记录新格式为 "物品id#翻译键"（翻译键用于区分换皮刀），旧格式为纯 "物品id"；'#' 不是合法
            // ResourceLocation 字符，须先拆出物品 id 部分再解析，否则 tryParse 返回 null 会丢失该条模型。
            String raw = devoured.getString(i);
            String idPart = raw;
            String tk = "";
            int hash = raw.indexOf('#');
            if (hash >= 0) {
                idPart = raw.substring(0, hash);
                tk = raw.substring(hash + 1);
            }
            ResourceLocation itemId = ResourceLocation.tryParse(idPart);
            if (itemId == null) {
                continue;
            }
            var item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) {
                continue;
            }
            SlashBladeDefinition def = findDefinition(new ItemStack(item), tk, registries);
            ResourceLocation[] render = renderOf(def);
            if (render != null && !listContains(models, modelEntry(render))) {
                models.add(StringTag.valueOf(modelEntry(render)));
            }
        }
        CompoundTag c = chaosTag(chaos);
        c.put(KEY_MODELS, models);
        saveChaos(chaos, c);
        return models;
    }

    /** 从定义提取建模/贴图；无定义返回 null。 */
    private static ResourceLocation[] renderOf(SlashBladeDefinition def) {
        if (def == null) {
            return null;
        }
        RenderDefinition rd = def.getRenderDefinition();
        ResourceLocation model = rd.getModelName();
        return model == null ? null : new ResourceLocation[]{model, rd.getTextureName()};
    }

    private static String modelEntry(ResourceLocation[] render) {
        return render[0].toString() + "|" + (render[1] == null ? "" : render[1].toString());
    }

    /**
     * 供客户端幻影刃模型预热：返回被吞模型池中所有建模 RL 的字符串（entry 的 '|' 前段，跳过空项）。
     * <p>纯读卡俄斯物品 NBT，不依赖服务端/客户端上下文，因此可安全在客户端调用。</p>
     */
    public static List<String> poolModelStrings(ItemStack chaos) {
        List<String> out = new ArrayList<>();
        ListTag models = chaosTag(chaos).getList(KEY_MODELS, Tag.TAG_STRING);
        for (int i = 0; i < models.size(); i++) {
            String m = models.getString(i).split("\\|", 2)[0];
            if (!m.isEmpty()) {
                out.add(m);
            }
        }
        return out;
    }

    /**
     * 为目标刀匹配定义：优先 translationKey 精确匹配（可区分同物品多定义），其次物理物品 id 匹配。
     * <p>用于 state 运行时 model 字段为空（未经 {@code getBlade()} 成型或经存档反序列化丢失）时的兑底。</p>
     */
    /**
     * 定义注册表运行期不变；缓存其快照，避免每次卡俄斯攻击都全量遍历并分配一个新 List（GC churn）。
     */
    private static volatile List<SlashBladeDefinition> DEFINITIONS;

    public static void clearDefinitionsCache() {
        DEFINITIONS = null;
    }

    private static List<SlashBladeDefinition> definitionsOf(HolderLookup.Provider registries) {
        List<SlashBladeDefinition> d = DEFINITIONS;
        if (d == null) {
            d = SlashBlade.getSlashBladeDefinitionRegistry(registries)
                    .listElements().map(h -> h.value()).toList();
            DEFINITIONS = d;
        }
        return d;
    }

    private static SlashBladeDefinition findDefinition(ItemStack target, String translationKey,
                                                       HolderLookup.Provider registries) {
        String item = itemId(target);
        SlashBladeDefinition byItem = null;
        List<SlashBladeDefinition> defs = definitionsOf(registries);
        for (SlashBladeDefinition def : defs) {
            if (!translationKey.isEmpty() && translationKey.equals(def.getTranslationKey())) {
                return def;
            }
            if (byItem == null && item.equals(def.getItemName().toString())) {
                byItem = def;
            }
        }
        return byItem;
    }

    /** 当前阶段：以累计吞噬量的数量级计，1000=阶段 1，上限 {@link NRConfig#chaosMaxStage()}。 */
    public static int stage(ItemStack stack) {
        long a = devouredAqua(stack);
        if (a < 1000L) {
            return 0;
        }
        int s = (int) Math.floor(Math.log10((double) a)) - 2;
        return Math.max(0, Math.min(s, NRConfig.chaosMaxStage()));
    }

    public static ListTag saList(ItemStack stack) {
        return chaosTag(stack).getList(KEY_SA, Tag.TAG_STRING);
    }

    public static boolean isRandomMode(ItemStack stack) {
        return chaosTag(stack).getByte(KEY_RANDOM) != 0;
    }

    /** 该 SA 是否在此卡俄斯上被禁用。 */
    public static boolean isSaDisabled(ItemStack chaos, ResourceLocation saKey) {
        if (saKey == null) {
            return false;
        }
        return listContains(chaosTag(chaos).getList(KEY_SA_DISABLED, Tag.TAG_STRING), saKey.toString());
    }

    private static boolean inSaList(ItemStack chaos, ResourceLocation saKey) {
        return listContains(saList(chaos), saKey.toString());
    }

    // ===== 吞噬 =====

    /**
     * 服务端把 {@code target} 吞噬进 {@code chaos}：累加属性、复制 SE/SA、记录已吞类型。
     *
     * @return 成功返回 true；非法目标 / 非卡俄斯 / 已吞过该类型返回 false。
     */
    public static boolean devour(ServerPlayer player, ItemStack chaos, ItemStack target) {
        if (!NRSpecialEffect.isChaosBlade(chaos)) {
            return false;
        }
        if (target == null || target.isEmpty()) {
            return false;
        }
        // 拥有 Empty（虚无）特效的刀无法被吞噬
        if (NRSpecialEffect.carriesSE(target, NrSpecialEffectsRegistry.Empty.getId())) {
            return false;
        }
        ISlashBladeState ts = state(target);
        if (ts == null) {
            return false; // 目标必须是拔刀剑
        }
        if (isDevoured(chaos, target)) {
            return false; // 每种刀只吞一次
        }
        ISlashBladeState cs = state(chaos);
        if (cs == null) {
            return false;
        }

        cs.setKillCount(cs.getKillCount() + 2 * ts.getKillCount());
        cs.setProudSoulCount(cs.getProudSoulCount() + 2 * ts.getProudSoulCount());
        cs.setRefine(cs.getRefine() + ts.getRefine());
        cs.setMaxDamage(cs.getMaxDamage() + ts.getMaxDamage());
        cs.setBaseAttackModifier(cs.getBaseAttackModifier() + ts.getBaseAttackModifier());

        mergeEnchantments(chaos, target);

        for (ResourceLocation se : ts.getSpecialEffects()) {
            // 跳过 CHAOS 本体，且已拥有该 SE 时不重复添加（否则吞多把同 SE 刀会在列表里重复出现）
            if (!NRSpecialEffect.isChaosSE(se) && !cs.hasSpecialEffect(se)) {
                cs.addSpecialEffect(se);
            }
        }

        ResourceLocation saKey = ts.getSlashArtsKey();
        CompoundTag c = chaosTag(chaos);
        if (saKey != null) {
            ListTag sa = c.getList(KEY_SA, Tag.TAG_STRING);
            String s = saKey.toString();
            if (!listContains(sa, s)) {
                sa.add(StringTag.valueOf(s));
            }
            c.put(KEY_SA, sa);
        }

        ListTag devoured = c.getList(KEY_DEVOURED, Tag.TAG_STRING);
        devoured.add(StringTag.valueOf(identityKey(target, ts)));
        c.put(KEY_DEVOURED, devoured);

        // 记录被吞刀的建模/贴图（供卡俄斯上的幻影刃随机选择外观）。
        // state 的 model/texture 是运行时字段（getBlade() 才设置、不随 NBT 序列化），存档加载/掉落/give
        // 的刀这里往往为空；此时回退到定义注册表按翻译键/物品 id 匹配取 RenderDefinition。
        ResourceLocation tModel = ts.getModel().orElse(null);
        ResourceLocation tTex = ts.getTexture().orElse(null);
        if (tModel == null) {
            ResourceLocation[] render = renderOf(findDefinition(target, ts.getTranslationKey(),
                    player.level().registryAccess()));
            if (render != null) {
                tModel = render[0];
                tTex = render[1];
            }
        }
        if (tModel != null) {
            String entry = tModel.toString() + "|" + (tTex == null ? "" : tTex.toString());
            ListTag models = c.getList(KEY_MODELS, Tag.TAG_STRING);
            if (!listContains(models, entry)) {
                models.add(StringTag.valueOf(entry));
            }
            c.put(KEY_MODELS, models);
        }
        saveChaos(chaos, c);

        persist(chaos, cs);
        return true;
    }

    /** 附魔等级累加：把 target 的 Enchantments 合并进 chaos（同附魔相加）。直接操作根 tag 的 "Enchantments"。 */
    private static void mergeEnchantments(ItemStack chaos, ItemStack target) {
        CompoundTag ct = chaos.getOrCreateTag();
        Map<String, Integer> map = new LinkedHashMap<>();
        collect(ct.getList("Enchantments", Tag.TAG_COMPOUND), map);
        collect(target.getOrCreateTag().getList("Enchantments", Tag.TAG_COMPOUND), map);
        ListTag out = new ListTag();
        for (Map.Entry<String, Integer> en : map.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.putString("id", en.getKey());
            e.putInt("lvl", en.getValue());
            out.add(e);
        }
        ct.put("Enchantments", out);
    }

    private static void collect(ListTag list, Map<String, Integer> map) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            String id = e.getString("id");
            int lvl = e.getInt("lvl");
            if (!id.isEmpty()) {
                map.merge(id, lvl, Integer::sum);
            }
        }
    }

    // ===== 反向吞噬时间素 =====

    /**
     * 若玩家主手持激活的卡俄斯且池足够，把一次“增加 delta”改为“扣减 delta”并记录吞噬量、随机施加增益。
     *
     * @return 已吞噬（调用方应放弃本次增长）返回 true；非卡俄斯或池不足返回 false（调用方按原样增长）。
     */
    public static boolean eatAquaIfHolding(ServerPlayer player, long delta) {
        ItemStack chaos = player.getMainHandItem();
        if (!NRSpecialEffect.isChaosBlade(chaos)) {
            return false;
        }
        ISlashBladeState cs = state(chaos);
        if (cs == null) {
            return false;
        }
        long cur = AquaTimeData.get(player);
        if (cur < delta) {
            return false; // 不够：仍增长
        }
        AquaTimeData.set(player, cur - delta);

        CompoundTag c = chaosTag(chaos);
        c.putLong(KEY_AQUA, c.getLong(KEY_AQUA) + delta);
        saveChaos(chaos, c);

        applyRollBuff(cs);
        persist(chaos, cs);
        return true;
    }

    /**
     * 被“回溯”覆盖时的记账：吞噬量照旧累加（阶段成长不变）、随机增益照旧发，
     * 但<b>不扣</b>玩家时间素（那笔消耗已被回溯反转为增加）。仅服务端调用。
     */
    public static void countDevouredOnly(ServerPlayer player, long delta) {
        if (delta <= 0L) {
            return;
        }
        ItemStack chaos = player.getMainHandItem();
        if (!NRSpecialEffect.isChaosBlade(chaos)) {
            return;
        }
        ISlashBladeState cs = state(chaos);
        if (cs == null) {
            return;
        }
        CompoundTag c = chaosTag(chaos);
        c.putLong(KEY_AQUA, c.getLong(KEY_AQUA) + delta);
        saveChaos(chaos, c);
        applyRollBuff(cs);
        persist(chaos, cs);
    }

    /** 权重 2:2:1 随机增益：耀魂+roll_soul / 杀敌+roll_kill / 面板+roll_dmg。 */
    private static void applyRollBuff(ISlashBladeState cs) {
        int r = RANDOM.nextInt(5);
        if (r < 2) {
            cs.setProudSoulCount(cs.getProudSoulCount() + NRConfig.chaosRollSoul());
        } else if (r < 4) {
            cs.setKillCount(cs.getKillCount() + NRConfig.chaosRollKill());
        } else {
            cs.setBaseAttackModifier(cs.getBaseAttackModifier() + NRConfig.chaosRollDmg());
        }
    }

    // ===== 获得 SE 的费用与激活 =====

    /** 该卡俄斯上“已获得且已激活”的（非 CHAOS 本体）SE 数量，用于计算激活次序 n。 */
    public static int activeGainedSECount(ItemStack chaos) {
        ISlashBladeState cs = state(chaos);
        if (cs == null) {
            return 0;
        }
        int n = 0;
        for (ResourceLocation se : cs.getSpecialEffects()) {
            if (prinegorerouse.MOD_ID.equals(se.getNamespace())
                    && !NRSpecialEffect.isChaosSE(se)
                    && NRSpecialEffect.isAquaActive(chaos, se)) {
                n++;
            }
        }
        return n;
    }

    /** CHAOS 公式费用：线性 i * k * 10000 * (n-1)，第一次解锁不收费。 */
    public static long chaosSECost(ItemStack chaos, ResourceLocation seId) {
        int n = activeGainedSECount(chaos) + 1;
        long i = SpecialEffect.getRequestLevel(seId);
        long k = NRConfig.chaosK();
        if (i <= 0L) {
            return 0L;
        }
        // 线性：乘子取 (n)=已激活获得SE数+1
        double cost = (double) i * (double) k * 10000.0 * (double) (n-1);
        if (Double.isNaN(cost) || cost >= (double) Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return (long) cost;
    }

    /** 该 seId 是否为“卡俄斯上通过吞噬获得的 SE”（区别于卡俄斯本体自带的固定 SE）。 */
    public static boolean isGainedSE(ItemStack chaos, ResourceLocation seId) {
        if (!NRSpecialEffect.isChaosBlade(chaos) || seId == null) {
            return false;
        }
        if (!prinegorerouse.MOD_ID.equals(seId.getNamespace()) || NRSpecialEffect.isChaosSE(seId)) {
            return false;
        }
        ISlashBladeState cs = state(chaos);
        return cs != null && cs.hasSpecialEffect(seId);
    }

    /** 用 CHAOS 公式费用激活一个获得的 SE。 */
    public static boolean tryActivateGainedSE(ServerPlayer player, ItemStack chaos, ResourceLocation seId) {
        long cost = chaosSECost(chaos, seId);
        return NRSpecialEffect.activateWithCost(player, chaos, seId, cost);
    }

    // ===== SA 装备与随机 =====

    /** 从存储的 SA 中点选一个装备（设为当前 slashArtsKey）。 */
    public static void selectSA(ServerPlayer player, ItemStack chaos, ResourceLocation saKey) {
        if (!NRSpecialEffect.isChaosBlade(chaos) || saKey == null) {
            return;
        }
        if (isSaDisabled(chaos, saKey)) {
            return; // 禁用的 SA 无法装备
        }
        ISlashBladeState cs = state(chaos);
        if (cs == null) {
            return;
        }
        if (!inSaList(chaos, saKey)) {
            return;
        }
        cs.setSlashArtsKey(saKey);
        persist(chaos, cs);
        // 同步主手刀到客户端，否则界面重开时读到的是旧值
        player.inventoryMenu.broadcastChanges();
    }

    /** 切换随机 SA 模式。 */
    public static void toggleRandom(ServerPlayer player, ItemStack chaos) {
        if (!NRSpecialEffect.isChaosBlade(chaos)) {
            return;
        }
        CompoundTag c = chaosTag(chaos);
        c.putByte(KEY_RANDOM, (byte) (c.getByte(KEY_RANDOM) == 0 ? 1 : 0));
        saveChaos(chaos, c);
        // 同步主手刀到客户端，否则界面重开时读到的是旧值
        player.inventoryMenu.broadcastChanges();
    }

    /** 禁用一个已存储的 SA：给予 {@link #SA_TOGGLE_COST} 时间素；禁用的 SA 不能装备也不参与随机。 */
    public static void disableSA(ServerPlayer player, ItemStack chaos, ResourceLocation saKey) {
        if (!NRSpecialEffect.isChaosBlade(chaos) || saKey == null) {
            return;
        }
        if (!inSaList(chaos, saKey) || isSaDisabled(chaos, saKey)) {
            return; // 只能禁用已存储且未禁用的 SA
        }
        ISlashBladeState cs = state(chaos);
        if (cs != null && saKey.equals(cs.getSlashArtsKey())) {
            return; // 不允许禁用当前装备的 SA
        }
        CompoundTag c = chaosTag(chaos);
        ListTag list = c.getList(KEY_SA_DISABLED, Tag.TAG_STRING);
        list.add(StringTag.valueOf(saKey.toString()));
        c.put(KEY_SA_DISABLED, list);
        saveChaos(chaos, c);
        // 直接给予，绕过卡俄斯反向吞噬（否则会被立即吞掉）
        AquaTimeData.add(player, SA_TOGGLE_COST, false);
        player.inventoryMenu.broadcastChanges();
    }

    /** 重新启用一个已禁用的 SA：扣除 {@link #SA_TOGGLE_COST} 时间素（不足则失败）。 */
    public static void enableSA(ServerPlayer player, ItemStack chaos, ResourceLocation saKey) {
        if (!NRSpecialEffect.isChaosBlade(chaos) || saKey == null) {
            return;
        }
        if (!isSaDisabled(chaos, saKey)) {
            return;
        }
        if (AquaTimeData.get(player) < SA_TOGGLE_COST) {
            player.displayClientMessage(
                    Component.translatable("screen.prinegorerouse.chaos.sa_insufficient", SA_TOGGLE_COST), true);
            return;
        }
        CompoundTag c = chaosTag(chaos);
        ListTag list = c.getList(KEY_SA_DISABLED, Tag.TAG_STRING);
        ListTag out = new ListTag();
        String s = saKey.toString();
        for (int i = 0; i < list.size(); i++) {
            String v = list.getString(i);
            if (!v.equals(s)) {
                out.add(StringTag.valueOf(v));
            }
        }
        c.put(KEY_SA_DISABLED, out);
        saveChaos(chaos, c);
        AquaTimeData.add(player, -SA_TOGGLE_COST, false); // 扣除
        player.inventoryMenu.broadcastChanges();
    }

    // ===== 事件 =====

    /**
     * 当前伤害管线正在处理的攻击者（服务端单线程，事件嵌套时序保证）：
     * slashblade 的 UpdateAttackEvent 没有实体上下文，只能靠外层
     * {@link LivingHurtEvent} 的 HIGHEST/LOWEST 夹心记录“这一刀是谁挥的”，
     * 用于女仆持用卡俄斯时屏蔽 SE 伤害倍率。
     */
    private static final ThreadLocal<ArrayDeque<LivingEntity>> CURRENT_ATTACKER_STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * 判定本次 {@link LivingHurtEvent} 是否需要记录攻击者：
     * 仅记录非玩家的生物攻击者（女仆等）；玩家本体与环境伤害（摔落/虚空等，
     * {@code getSource().getEntity()} 为 null）都返回 null 表示不入栈。
     * push 与 pop 复用此判定，保证收支平衡。
     */
    private static LivingEntity recordableAttacker(LivingHurtEvent event) {
        Entity src = event.getSource().getEntity();
        // ArrayDeque 不允许 null 元素，玩家/环境伤害一律返回 null 由调用方跳过入栈
        return (!(src instanceof ServerPlayer) && src instanceof LivingEntity l) ? l : null;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingAttackStart(LivingHurtEvent event) {
        LivingEntity attacker = recordableAttacker(event);
        if (attacker != null) {
            CURRENT_ATTACKER_STACK.get().push(attacker);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onIncomingAttackEnd(LivingHurtEvent event) {
        // 与 push 用同一判定：仅当本事件确实入过栈时才弹出，避免收支失衡
        if (recordableAttacker(event) == null) {
            return;
        }
        ArrayDeque<LivingEntity> stack = CURRENT_ATTACKER_STACK.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
        if (stack.isEmpty()) {
            CURRENT_ATTACKER_STACK.remove();
        }
    }

    public static void clearThreadLocals() {
        CURRENT_ATTACKER_STACK.remove();
    }

    /** 阶段最终伤害加成。 */
    @SubscribeEvent
    public static void onUpdateAttack(SlashBladeEvent.UpdateAttackEvent event) {
        // 女仆适配：女仆持用本 mod 拔刀剑时，卡俄斯的 SE 增伤不生效
        ArrayDeque<LivingEntity> stack = CURRENT_ATTACKER_STACK.get();
        LivingEntity currentAttacker = stack.isEmpty() ? null : stack.peek();
        if (currentAttacker instanceof TamableAnimal) {
            return;
        }
        ItemStack blade = event.getBlade();
        if (!NRSpecialEffect.isChaosBlade(blade)) {
            return;
        }
        int s = stage(blade);
        if (s <= 0) {
            return;
        }
        double mult = 1.0 + NRConfig.chaosDmgPerStage() * s;
        event.setNewDamage(event.getNewDamage() * mult);
    }

    /** 阶段虚空伤害：命中后按攻击者当前攻击力的一定比例追加 out_of_world 伤害。 */
    @SubscribeEvent
    public static void onHit(SlashBladeEvent.HitEvent event) {
        ItemStack blade = event.getBlade();
        if (!NRSpecialEffect.isChaosBlade(blade)) {
            return;
        }
        int s = stage(blade);
        if (s <= 0) {
            return;
        }
        LivingEntity user = event.getUser();
        LivingEntity target = event.getTarget();
        if (user == null || target == null || target.level().isClientSide()) {
            return;
        }
        // 女仆适配：女仆持用卡俄斯挥刀不触发虚空伤害；也不对主人自己的女仆/宠物造成伤害
        if (MaidCompat.isTameable(user) || MaidCompat.isOwnedBy(target, user)) {
            return;
        }
        double voidDamage = user.getAttributeValue(Attributes.ATTACK_DAMAGE) * NRConfig.chaosVoidPerStage() * s;
        if (voidDamage <= 0.0) {
            return;
        }
        target.hurt(new DamageSource(
                DamageTypeHolderUtils.getHolder(target.level(), "out_of_world"), user, user), (float) voidDamage);
    }

    /** 随机 SA 模式：每次挥刀前从存储 SA 中随机选一并设为当前 SA。 */
    @SubscribeEvent
    public static void onDoSlash(SlashBladeEvent.DoSlashEvent event) {
        if (!(event.getUser() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack chaos = event.getBlade();
        if (!NRSpecialEffect.isChaosBlade(chaos) || !isRandomMode(chaos)) {
            return;
        }
        ISlashBladeState st = event.getSlashBladeState();
        ListTag sa = saList(chaos);
        if (st == null || sa.isEmpty()) {
            return;
        }
        // 只在未禁用的 SA 中随机
        ListTag enabled = new ListTag();
        for (int i = 0; i < sa.size(); i++) {
            String v = sa.getString(i);
            ResourceLocation rl0 = ResourceLocation.tryParse(v);
            if (rl0 != null && !isSaDisabled(chaos, rl0)) {
                enabled.add(StringTag.valueOf(v));
            }
        }
        if (enabled.isEmpty()) {
            return;
        }
        String pick = enabled.getString(RANDOM.nextInt(enabled.size()));
        ResourceLocation rl = ResourceLocation.tryParse(pick);
        if (rl != null) {
            st.setSlashArtsKey(rl);
            persist(chaos, st);
        }
    }
}
