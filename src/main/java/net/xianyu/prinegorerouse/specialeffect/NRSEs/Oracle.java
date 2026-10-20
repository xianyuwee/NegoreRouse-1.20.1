package net.xianyu.prinegorerouse.specialeffect.NRSEs;

import mods.flammpfeil.slashblade.SlashBladeConfig;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.xianyu.prinegorerouse.aquatime.AquaTimeData;
import net.xianyu.prinegorerouse.compat.MaidCompat;
import net.xianyu.prinegorerouse.entity.EntityShinyDrive;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;
import net.xianyu.prinegorerouse.specialeffect.NRSpecialEffect;

import java.util.Random;

@EventBusSubscriber
public class Oracle extends NRSpecialEffect {

    public Oracle() {
        super(1, false, false);
    }

    public static Random random = new Random();

    @SubscribeEvent
    public static void onSlashBladeUpdate(SlashBladeEvent.UpdateEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(NrSpecialEffectsRegistry.Oracle.getId())) {
            if (!(event.getEntity() instanceof Player))
                return;
            Player player = (Player) event.getEntity();
            if (!(event.isSelected())) {
                return;
            }
            if (NRSpecialEffect.isAquaActive(event.getBlade(), NrSpecialEffectsRegistry.Oracle.getId())) {
                if (player.isUsingItem() && player.getMainHandItem().getHoverName().equals(event.getBlade().getHoverName())) {
                    player.addEffect(new MobEffectInstance(MobEffects.LUCK, 100, 2));
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 4));
                }
            }
        }
    }

    /**
     * 挥刀剑气：已激活神谕的刀在挥刀时（约 80% 概率）沿<b>挥刀方向</b>射出剑气。
     * <p>去除原有的副手限制——直接判定正在挥出的这把刀；方向取实体自身的朝向向量
     * （与拔刀剑官方 {@code slasharts.Drive} 一致），使剑气与挥刀方向一致；
     * 伤害为刀面板攻击力的 10%。</p>
     */
    @SubscribeEvent
    public static void onDoingSlash(SlashBladeEvent.DoSlashEvent event) {
        // 女仆适配：女仆持用本 mod 拔刀剑挥砍时，神谕剑气（SE）不起效果
        if (MaidCompat.isTameable(event.getUser())) {
            return;
        }
        Level level = event.getUser().level();
        if (level.isClientSide()) {
            return;
        }
        // 判定正在挥出的这把刀：携带且已激活神谕
        if (!event.getSlashBladeState().hasSpecialEffect(NrSpecialEffectsRegistry.Oracle.getId())) {
            return;
        }
        if (!NRSpecialEffect.isAquaActive(event.getBlade(), NrSpecialEffectsRegistry.Oracle.getId())) {
            return;
        }
        // 保留原有约 80% 的触发概率
        if (random.nextFloat() > 0.8F) {
            return;
        }

        LivingEntity user = event.getUser();
        // 方向取实体朝向（含俯仰与水平角）。注意：DoSlashEvent.getYRot() 在该版本被写死为 0，
        // 不能用它当挥刀 yaw，否则方向恒落在 +z 半平面（x 分量恒为 0）。
        Vec3 dir = user.getLookAngle();
        Vec3 pos = user.getEyePosition().add(dir.scale(0.3D));

        EntityShinyDrive drive = new EntityShinyDrive(NrEntitiesRegistry.ShinyDrive, level);
        drive.setOwner(user);
        // 剑气最终伤害 = 玩家攻击属性的 10%。
        // EntityNRSDrive.onHitEntity 会把 setDamage 的值再乘一次「玩家攻击属性 × 伤害缩放倍率」，
        // 所以这里把 setDamage 设为「比例 ÷ 缩放倍率」来抵消那次多余乘法，得到真实面板的 10%。
        float scaleMult = AttackManager.getSlashBladeDamageScale(user)
                * SlashBladeConfig.SLASHBLADE_DAMAGE_MULTIPLIER.get().floatValue();
        drive.setDamage(scaleMult > 0.0F ? 0.1F / scaleMult : 0.1F);
        drive.setSpeed(3.0F);
        drive.setColor(111111111);
        drive.setPos(pos.x, pos.y, pos.z);
        drive.shoot(dir.x, dir.y, dir.z, drive.getSpeed(), 0);
        drive.setDelay(10);
        drive.setDelayTick(50);
        drive.setDelaySpeed(3.0F);
        drive.setLifetime(50);
        drive.setNoClip(true);          // 物理上穿过方块
        drive.setPierce(true);          // 命中方块时不消失（仅本特效，黑洞等其它使用者不受影响）
        // 滚转与挥刀一致
        drive.setRotationRoll(event.getRoll());

        level.addFreshEntity(drive);
    }

    /**
     * 击杀奖励：持有并激活神谕的刀击杀生物后，直接为玩家增加时间素，
     * 数量 = 本次击杀获得的经验值 ×10；该增长不参与卡俄斯的反向吞噬。
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Level level = event.getEntity().level();
        if (level.isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof ServerPlayer sp) || sp.isSpectator()) {
            return;
        }
        // 击杀所用的刀（主手）需携带并激活神谕
        ItemStack main = sp.getMainHandItem();
        if (!NRSpecialEffect.carriesSE(main, NrSpecialEffectsRegistry.Oracle.getId())
                || !NRSpecialEffect.isAquaActive(main, NrSpecialEffectsRegistry.Oracle.getId())) {
            return;
        }
        int xp = mob.getExperienceReward();
        if (xp <= 0) {
            return;
        }
        // 经验 ×10，直接加到玩家身上；chaosAware=false 使该增长不参与卡俄斯减少
        AquaTimeData.add(sp, (long) xp * 10L, false);
    }
}
