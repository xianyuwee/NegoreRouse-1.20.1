package net.xianyu.prinegorerouse.specialeffect.NRSEs;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.xianyu.prinegorerouse.compat.MaidCompat;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.prinegorerouse;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;
import net.xianyu.prinegorerouse.specialeffect.NRSpecialEffect;
import net.xianyu.prinegorerouse.utils.DamageTypeHolderUtils;

import java.util.Random;


/**
 * Back（回溯）。
 * <p>两部分：</p>
 * <ul>
 *   <li><b>被动 buff（不激活也生效）</b>：手持该刀蓄力时给予抗性提升 III、伤害提升 III（100 tick），等级与持续时间不变。</li>
 *   <li><b>回溯层数（激活后）</b>：世界每经过 {@link #LAYER_PERIOD_TICKS} 游戏刻累积一层，最多 {@link #MAX_LAYERS} 层；
 *       客户端在刀镡附近随机位置渲染金色“点 → 四芒星 → 点”的星闪（纯粒子、不做实体），同时最多渲染 {@link #MAX_RENDER} 个，
 *       一颗熄灭后歇一小段时间再换个位置重亮。</li>
 *   <li><b>时间素反转</b>：存在层数时，玩家<b>每次消耗时间素都被反转为增加等量时间素</b>，同时降一层并回复当前刀
 *       {@link #REPAIR_PER_LAYER} 点耐久。该逻辑<b>覆盖卡俄斯的反向吞噬扣减</b>：卡俄斯仍照常累计吞噬时间素
 *       （阶段成长与随机增益不变），但玩家的时间素改为增加。</li>
 * </ul>
 */
@EventBusSubscriber
public class Back extends NRSpecialEffect {

    public Back() {
        super(90, false, false);
    }

    // ===== 微调旋钮 =====
    /** 刀根 tag：当前累积的回溯层数（写在物品 NBT 上，随背包同步到客户端驱动星星渲染）。 */
    public static final String LAYERS_TAG = "BackLayers";
    /** 最多可存在的回溯层数。 */
    public static final int MAX_LAYERS = 20;
    /** 客户端最多同时渲染的星星数（层数可以更多，只是一屏只闪这么多）。 */
    public static final int MAX_RENDER = 10;
    /** 世界每经过这么多游戏刻累积一层。 */
    public static final int LAYER_PERIOD_TICKS = 100;
    /** 每降一层回复当前刀的耐久。 */
    public static final int REPAIR_PER_LAYER = 10;
    /** 单颗星星一次闪烁的时长（tick），客户端调度与粒子寿命共用。 */
    public static final int STAR_LIFETIME = 32;

    public static boolean hasSpecialEffects(ItemStack stack, String effect) {
        CompoundTag tag = stack.getOrCreateTag();

        if (tag.contains("bladeState")) {
            CompoundTag forgeCaps = tag.getCompound("bladeState");

            if (forgeCaps.contains("SpecialEffects")) {
                ListTag specialEffects = forgeCaps.getList("SpecialEffects", 8);
                for (int i = 0; i < specialEffects.size(); i++){
                    String currentEffect = specialEffects.getString(i);
                    if (effect.equals(currentEffect)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onSlashBladeUpdate(SlashBladeEvent.UpdateEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(NrSpecialEffectsRegistry.Back.getId())) {
            if (!(event.getEntity() instanceof Player player))
                return;
            ItemStack blade = event.getBlade();
            if (player.level().isClientSide()) {
                return;
            }
            if (!(event.isSelected()))
                return;
            // 被动 buff：不再要求激活，持刀选中即生效（等级与持续时间不变）
            if (player.isUsingItem() && player.getMainHandItem().getHoverName().equals(blade.getHoverName())) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 2));
            }
            // 激活后：按世界游戏刻累积回溯层数（层数写在刀 tag 上，客户端据此渲染星星）
            if (player instanceof ServerPlayer sp
                    && NRSpecialEffect.isAquaActive(blade, NrSpecialEffectsRegistry.Back.getId())
                    && sp.level().getGameTime() % LAYER_PERIOD_TICKS == 0L) {
                int layers = layers(blade);
                if (layers < MAX_LAYERS) {
                    blade.getOrCreateTag().putInt(LAYERS_TAG, layers + 1);
                    // 服务端改手持刀 NBT 必须广播，否则客户端读到的层数永远是旧值
                    sp.inventoryMenu.broadcastChanges();
                }
            }
        }
    }

    // ===== 回溯层数：读写与消耗 =====

    /** 该刀当前累积的回溯层数。 */
    public static int layers(ItemStack blade) {
        return blade == null || blade.isEmpty() ? 0 : blade.getOrCreateTag().getInt(LAYERS_TAG);
    }

    /** 该玩家当前生效的回溯层数（主手优先，其次副手）。 */
    public static int layers(Player player) {
        return layers(activeBlade(player));
    }

    /** 当前生效的“回溯”刀：主手优先，其次副手（需携带 back 且已激活）；都没有则空栈。 */
    public static ItemStack activeBlade(Player player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        ResourceLocation id = NrSpecialEffectsRegistry.Back.getId();
        ItemStack main = player.getMainHandItem();
        if (isBackActive(main, id)) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        return isBackActive(off, id) ? off : ItemStack.EMPTY;
    }

    /** 生效的“回溯”刀是否在副手（客户端据此决定星星出现在哪一侧）。 */
    public static boolean isActiveInOffhand(Player player) {
        if (player == null) {
            return false;
        }
        ResourceLocation id = NrSpecialEffectsRegistry.Back.getId();
        return !isBackActive(player.getMainHandItem(), id) && isBackActive(player.getOffhandItem(), id);
    }

    private static boolean isBackActive(ItemStack blade, ResourceLocation id) {
        return carriesSE(blade, id) && isAquaActive(blade, id);
    }

    /**
     * 反转一笔时间素消耗：降一层回溯，并回复当前刀耐久。
     *
     * @return true 表示已反转（调用方应把这笔扣减改为等量增加）；无生效刀或层数为 0 返回 false。
     */
    public static boolean consumeLayer(Player player) {
        ItemStack blade = activeBlade(player);
        int layers = layers(blade);
        if (layers <= 0) {
            return false;
        }
        blade.getOrCreateTag().putInt(LAYERS_TAG, layers - 1);
        blade.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(st -> {
            int damage = st.getDamage();
            if (damage > 0) {
                st.setDamage(Math.max(0, damage - REPAIR_PER_LAYER));
            }
        });
        if (player instanceof ServerPlayer sp) {
            sp.inventoryMenu.broadcastChanges();
        }
        return true;
    }

    static Random random = new Random();


    @SubscribeEvent
    public static void onSlashBladeHit(SlashBladeEvent.HitEvent event) {
        ISlashBladeState state = event.getSlashBladeState();

        // 第一步：先检查类型，非Player直接返回
        if (!(event.getUser() instanceof Player)) {
            return;
        }
        // 第二步：安全强转
        Player player = (Player) event.getUser();
        ItemStack offhandItem = player.getOffhandItem();

        if(state.hasSpecialEffect(NrSpecialEffectsRegistry.Back.getId())) {
            Level level1 = player.level();
            int level = player.experienceLevel;
            if(NRSpecialEffect.isAquaActive(event.getBlade(), NrSpecialEffectsRegistry.Back.getId()) && player.getMainHandItem().getHoverName().equals(event.getBlade().getHoverName())) {
                double decimal = random.nextDouble();
                if ( decimal-0.4d <= 0.00001) {
                    Entity entity = event.getTarget();
                    // 女仆/宠物保护：主人自己的驯服生物免疫回溯剑气伤害
                    if (!MaidCompat.isOwnedBy(entity, player)) {
                        entity.hurt(new DamageSource(DamageTypeHolderUtils.getHolder(level1, "out_of_world"),player,player), state.getBaseAttackModifier() * 0.5F);
                    }
                    int blade_damage = state.getDamage();
                    state.setDamage(blade_damage - 1);
                }
            }
        } else if(NRConfig.OFFHAND_CAN_ACTIVE.get().equals(true) && offhandItem.getItem() instanceof ItemSlashBlade) {
            Level level = player.level();
            if (!hasSpecialEffects(player.getOffhandItem(), prinegorerouse.MOD_ID+":back"))return;
            double decimal = random.nextDouble();
            if ( decimal-0.5d <= 0.00001) {
                Entity entity = event.getTarget();
                // 女仆/宠物保护：主人自己的驯服生物免疫副手剑气伤害
                if (!MaidCompat.isOwnedBy(entity, player)) {
                    entity.hurt(new DamageSource(DamageTypeHolderUtils.getHolder(level, "out_of_world"),player,player), state.getBaseAttackModifier() * 0.2F);
                }
                int blade_damage = state.getDamage();
                state.setDamage(blade_damage - 1);
            }
        }
    }
}