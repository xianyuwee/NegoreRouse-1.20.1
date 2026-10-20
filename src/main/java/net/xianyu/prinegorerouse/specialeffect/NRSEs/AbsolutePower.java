package net.xianyu.prinegorerouse.specialeffect.NRSEs;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.entity.EntityAbsolutePhantom;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;
import net.xianyu.prinegorerouse.specialeffect.ChaosHandler;
import net.xianyu.prinegorerouse.specialeffect.NRSpecialEffect;

import java.util.List;
import java.util.Random;

/**
 * AbsolutePower（绝对神力）。
 * <p>两部分：</p>
 * <ul>
 *   <li><b>被动 buff（未激活也生效）</b>：持刀时按主/副手给予速度、夜视、增伤、回血。</li>
 *   <li><b>幻影刃（激活后）</b>：当手持（主/副手）激活的该 SE 刀<b>造成伤害</b>时，按<b>概率 = 伤害量/100</b>
 *       在玩家身边生成一把淡金色透明、复刻触发刀建模（仅刀刃）的悬浮幻影刃；命中存活生物后其传送追击、
 *       随机释放剑气并施加真实伤害，目标死亡后飞回环绕。玩家周身<b>最多同时环绕 3 把</b>；已满 3 把时，
 *       再次触发只<b>刷新其持续时间</b>而不新增。寿命 = min{40 + 5×力量, 300} tick。仅以存活生物为目标
 *       （自动过滤物品/经验球等，并把多段生物如末影龙归并到本体）。卡俄斯上的幻影刃模型改从被吞模型池随机取。</li>
 * </ul>
 */
@EventBusSubscriber
public class AbsolutePower extends NRSpecialEffect {

    public AbsolutePower() {
        super(50, false, false);
    }

    public static Random random = new Random();

    // ===== 微调旋钮 =====
    private static final int MAX_PHANTOMS = 3;     // 玩家周身同时存在的幻影刃上限
    private static final int LIFE_BASE = 40;       // 寿命基数（tick）
    private static final int LIFE_PER_LVL = 5;     // 每级力量增加寿命
    private static final int LIFE_CAP = 300;       // 寿命上限（tick）

    // ===== 被动 buff：未激活也生效 =====
    @SubscribeEvent
    public static void onSlashBladeUpdate(SlashBladeEvent.UpdateEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(NrSpecialEffectsRegistry.AbsolutePower.getId())) {
            if (!(event.getEntity() instanceof Player))
                return;
            Player player = (Player) event.getEntity();
            if (!(event.isSelected())) {
                return;
            }
            // 被动 buff 不再受“激活”门槛限制：只要携带该 SE 且为持刀选中状态即生效
            float decimal = random.nextFloat();
            if (player.isUsingItem() && player.getMainHandItem().getHoverName().equals(event.getBlade().getHoverName())) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 5));
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100, 3));
            } else if (player.isUsingItem() && player.getOffhandItem().getHoverName().equals(event.getBlade().getHoverName()) && NRConfig.OFFHAND_CAN_ACTIVE.get().equals(true)) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20, 3));
                if (decimal - 0.3F <= 0.00001F && player.isAlive()) {
                    player.heal(player.getMaxHealth() * 0.1F);
                }
            }
        }
    }

    // ===== 幻影刃：造成伤害时按概率生成（概率 = 伤害量/100），周身最多 MAX_PHANTOMS 把，满则只刷新持续时间 =====
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        Level level = event.getEntity().level();
        if (level.isClientSide()) {
            return;
        }
        DamageSource src = event.getSource();
        // 幻影刃自身的真实伤害会归因玩家，需排除，避免无限自我触发
        if (src.getDirectEntity() instanceof EntityAbsolutePhantom) {
            return;
        }
        if (!(src.getEntity() instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        ResourceLocation id = NrSpecialEffectsRegistry.AbsolutePower.getId();
        // 判定手持（主手优先，其次副手）携带且已激活的刀
        ItemStack blade = player.getMainHandItem();
        if (!(NRSpecialEffect.carriesSE(blade, id) && NRSpecialEffect.isAquaActive(blade, id))) {
            ItemStack off = player.getOffhandItem();
            if (NRSpecialEffect.carriesSE(off, id) && NRSpecialEffect.isAquaActive(off, id)) {
                blade = off;
            } else {
                return;
            }
        }
        float amount = event.getAmount();
        if (amount <= 0.0F) {
            return;
        }
        // 生成概率 = 伤害量 / 100（如 50 点伤害 → 50%）
        if (random.nextFloat() >= amount / 100.0F) {
            return;
        }

        int power = blade.getEnchantmentLevel(Enchantments.POWER_ARROWS);
        int life = Math.min(LIFE_BASE + LIFE_PER_LVL * power, LIFE_CAP);

        // 统计玩家周身已有的幻影刃
        List<EntityAbsolutePhantom> owned = level.getEntitiesOfClass(
                EntityAbsolutePhantom.class, player.getBoundingBox().inflate(48),
                e -> e.getOwner() == player);

        if (owned.size() >= MAX_PHANTOMS) {
            // 已满：只刷新持续时间（不缩短更长者）
            for (EntityAbsolutePhantom p : owned) {
                p.refreshLife(life);
            }
            return;
        }

        // 未满：生成一把，并把本次伤害的目标下发给它
        // 模型/贴图：卡俄斯从被吞池随机取，否则用触发刀自身
        Entity victim = event.getEntity();
        ResourceLocation model = null;
        ResourceLocation tex = null;
        if (NRSpecialEffect.isChaosBlade(blade)) {
            ResourceLocation[] pool = ChaosHandler.randomDevouredModel(blade, player.level().registryAccess());
            if (pool != null) {
                model = pool[0];
                tex = pool[1];
            }
        }
        if (model == null) {
            ISlashBladeState st = blade.getCapability(ItemSlashBlade.BLADESTATE).orElse(null);
            if (st != null) {
                model = st.getModel().orElse(null);
                tex = st.getTexture().orElse(null);
            }
        }
        EntityAbsolutePhantom spawned = spawnOne(player, owned.size(), life, model, tex);
        if (spawned != null) {
            spawned.setCommandedTarget(victim);
        }
    }

    /** 生成单把幻影刃；{@code slot} 用于均匀分布环绕相位。 */
    private static EntityAbsolutePhantom spawnOne(ServerPlayer player, int slot, int life,
                                                  ResourceLocation model, ResourceLocation tex) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 base = player.getEyePosition().add(0, -0.2, 0);
        EntityAbsolutePhantom p = new EntityAbsolutePhantom(NrEntitiesRegistry.Absolute_Phantom, level);
        p.setOwner(player);
        p.setupModel(model, tex);
        double radius = 2.0 + random.nextDouble() * 2.0;   // 2–4 格环
        double phase = (Math.PI * 2.0 * slot / MAX_PHANTOMS) + random.nextDouble() * 0.5;
        p.setupOrbit(slot, radius, phase, life);
        p.setColor(0xFFD8AF4A);
        Vec3 sp = base.add(Math.cos(phase) * radius, 0, Math.sin(phase) * radius);
        p.setPos(sp.x, sp.y, sp.z);
        level.addFreshEntity(p);
        return p;
    }

    // ===== 命中获取目标：把存活目标下发给玩家周围的幻影刃 =====
    @SubscribeEvent
    public static void onHit(SlashBladeEvent.HitEvent event) {
        if (!(event.getUser() instanceof ServerPlayer player)) {
            return;
        }
        Entity target = event.getTarget();
        if (target == null || !target.isAlive() || player.level().isClientSide()) {
            return;
        }
        for (EntityAbsolutePhantom p : player.level().getEntitiesOfClass(
                EntityAbsolutePhantom.class, player.getBoundingBox().inflate(48),
                e -> e.getOwner() == player)) {
            p.setCommandedTarget(target);
        }
    }
}
