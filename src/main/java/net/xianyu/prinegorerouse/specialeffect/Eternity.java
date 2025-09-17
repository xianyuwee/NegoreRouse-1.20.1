package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.xianyu.prinegorerouse.entity.EntityNRJudgementCut;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;
import net.xianyu.prinegorerouse.utils.BlackHoleUtil;

@EventBusSubscriber
public class Eternity extends SpecialEffect {
  public Eternity() {
    super(200, false, false);
  }
  
  @SubscribeEvent
  public static void onSlashBladeHit(SlashBladeEvent.HitEvent event) {
    // 添加空值检查
    if (event.getUser() == null || event.getTarget() == null) {
      return;
    }
    
    // 检查用户是否为玩家
    if (!(event.getUser() instanceof Player)) {
      return;
    }
    
    ISlashBladeState state = event.getSlashBladeState();
    if (state != null && state.hasSpecialEffect(NrSpecialEffectsRegistry.Eternity.getId())) {
      Player player = (Player)event.getUser();
      int level = player.experienceLevel;
      
      // 检查特殊效果是否有效
      if (!SpecialEffect.isEffective(NrSpecialEffectsRegistry.Eternity.get(), level)) {
        if (event.getSlashBladeState() != null) {
          event.getSlashBladeState().setBroken(true);
        }
      } else {
        // 检查目标是否为生物实体
        if (!(event.getTarget() instanceof LivingEntity)) {
          return;
        }
        
        LivingEntity target = (LivingEntity) event.getTarget();
        float hp = target.getHealth();
        target.setHealth(0.5F * hp);
        
        // 只在服务器端生成审判斩
        if (target.level() instanceof net.minecraft.server.level.ServerLevel) {
          spawnJudgementCut(target); 
        }
      } 
    } 
  }
  
  private static void spawnJudgementCut(LivingEntity target) {
    if (!target.isAlive()) {
      return; 
    }
    
    int blackHoleCount = BlackHoleUtil.getBlackHoleCount(target);
    if (blackHoleCount <= 0) {
      return; 
    }
    
    // 确保实体类型已注册
    if (NrEntitiesRegistry.NRJudgementCut == null) {
      return;
    }
    
    EntityNRJudgementCut judgementCut = new EntityNRJudgementCut(NrEntitiesRegistry.NRJudgementCut, target.level());
    judgementCut.setLifetime(blackHoleCount * 20);
    judgementCut.setDamage(blackHoleCount * 5);
    judgementCut.setCycleHit(true);
    judgementCut.setShooter(target);
    judgementCut.setPos(target.position());
    judgementCut.setRenderScale(blackHoleCount * 0.1F);
    judgementCut.setColor(4915330);
    
    // 添加实体到世界
    if (target.level() != null) {
      target.level().addFreshEntity(judgementCut);
    }
    
    BlackHoleUtil.resetBlackHoleCount(target);
  }
}