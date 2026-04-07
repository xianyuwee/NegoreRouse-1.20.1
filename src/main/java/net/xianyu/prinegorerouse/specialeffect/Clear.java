package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;

import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber
public class Clear extends SpecialEffect {
    public Clear() {
        super(1, false, false);
    }

    static Random random = new Random();

    @SubscribeEvent
    public static void OnSlashBladeUpdate(SlashBladeEvent.UpdateEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(NrSpecialEffectsRegistry.Clear.getId())) {
            if (!(event.getEntity() instanceof Player))
                return;
            Player player = (Player) event.getEntity();
            if (!(event.isSelected())) {
                return;
            }
            int level = player.experienceLevel;
            if (!SpecialEffect.isEffective((SpecialEffect) NrSpecialEffectsRegistry.Clear.get(), level)) {
                if(player.isUsingItem()) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20, 2));
                }
            } else {
                if(player.isUsingItem() && player.getMainHandItem().getHoverName().equals(event.getBlade().getHoverName())) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100, 2));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1));
                }
            }
        }
    }

    @SubscribeEvent
    public static void OnSlashBladeHit(SlashBladeEvent.HitEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(NrSpecialEffectsRegistry.Clear.getId())) {
            // 前置类型检查，非Player直接返回
            if (!(event.getUser() instanceof Player)) {
                return;
            }
            Player player = (Player) event.getUser();
            int level = player.experienceLevel;
            if (SpecialEffect.isEffective((SpecialEffect) NrSpecialEffectsRegistry.Clear.get(), level)) {
                Level world = event.getUser().level();
                double decimal = random.nextDouble();
                if (world.isDay()) {
                    int blade_damage = state.getDamage();
                    state.setDamage(blade_damage + 1);
                    if (decimal <= 0.2d) { // 优化随机数判断写法
                        player.giveExperiencePoints(200);
                    }
                } else {
                    if (decimal <= 0.5d) { // 优化随机数判断写法
                        int blade_damage = state.getDamage();
                        state.setDamage(blade_damage - 2);
                        Entity target = event.getTarget();

                        // --- 核心修改：爆炸逻辑开始 ---
                        double x = target.getX();
                        double y = target.getY();
                        double z = target.getZ();
                        float explosionRadius = 3.0F; // 爆炸影响范围
                        float fixedDamage = (float) (0.1 * player.getAttributeValue(Attributes.ATTACK_DAMAGE));     // 自定义固定伤害

                        // 1. 触发爆炸效果（不破坏方块 + 不生成火焰 + 制造击退）
                        // 注意：explode() 方法本身就会执行爆炸，无需再对返回值调用 explode()
                        world.explode(
                                player,                 // 爆炸来源实体（决定击退归属）
                                x, y, z,                // 爆炸坐标
                                explosionRadius,        // 爆炸半径
                                false ,                   // 是否生成火焰
                                Level.ExplosionInteraction.NONE // 不破坏方块
                        );

                        // 2. 手动处理固定伤害
                        AABB damageRange = new AABB(
                                x - explosionRadius, y - explosionRadius, z - explosionRadius,
                                x + explosionRadius, y + explosionRadius, z + explosionRadius
                        );
                        List<Entity> entities = world.getEntities(player, damageRange); // 排除玩家自己

                        DamageSource damageSource = player.damageSources().playerAttack(player); // 伤害来源为玩家

                        for (Entity entity : entities) {
                            if (entity.isAlive() && !entity.isSpectator()) {
                                // 造成伤害
                                entity.hurt(damageSource, fixedDamage);
                            }
                        }
                    }
                }
            }
        }
    }
}