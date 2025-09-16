package net.xianyu.prinegorerouse.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xianyu.prinegorerouse.config.NRConfig;
import net.xianyu.prinegorerouse.entity.EntityEnchantedSword;
import net.xianyu.prinegorerouse.entity.EntityNRBlisteringSword;
import net.xianyu.prinegorerouse.prinegorerouse;
import net.xianyu.prinegorerouse.registry.NrEntitiesRegistry;
import net.xianyu.prinegorerouse.registry.NrSpecialEffectsRegistry;

import java.util.Random;


@Mod.EventBusSubscriber
public class Fate extends SpecialEffect {
    public Fate() {
        super(30, false, false);
    }

    static Random random = new Random();

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
    public static void onSlashBladeHit(SlashBladeEvent.HitEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        // 已正确添加Player类型前置判断，避免ClassCastException
        if (!(event.getUser() instanceof Player)) {
            return; 
        }
        
        Player player = (Player) event.getUser();
        int level = player.experienceLevel; // 仅定义一次，避免重复变量编译错误
        ItemStack offhandItem = player.getOffhandItem();
        
        if (state.hasSpecialEffect(NrSpecialEffectsRegistry.Fate.getId())) {
            // 移除重复的int level定义，复用上方已定义的level变量
            if (SpecialEffect.isEffective((SpecialEffect) NrSpecialEffectsRegistry.Fate.get(), level) && player.getMainHandItem().getHoverName().equals(event.getBlade().getHoverName())) {
                double decimal = random.nextDouble();
                if (decimal - 0.5d <= 0.000001) {
                    player.addEffect(new MobEffectInstance(MobEffects.LUCK, 100, 10));
                    player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 10, 1));
                    player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 10, 1));
                    event.getTarget().addEffect(new MobEffectInstance(MobEffects.GLOWING, 10, 1));
                }
                if (decimal - 0.8d <= 0.0000001D) {
                    Level worldIn = player.level();
                    EntityEnchantedSword ss = new EntityEnchantedSword(NrEntitiesRegistry.Enchanted_Sword, worldIn);
                    worldIn.addFreshEntity(ss);
                    ss.setSpeed(10.0F);
                    ss.setNoClip(false);
                    ss.setDamage(state.getBaseAttackModifier() * 0.2F);
                    ss.startRiding(player, true);
                    ss.setShooter(player);
                    ss.setDelay(5);
                    ss.setColor(16766720);
                    RandomSource random = worldIn.random;
                    double xOffset = random.nextDouble() * 2;
                    double zOffset = (double) random.nextFloat() * 2;
                    double yOffset = random.nextFloat();
                    ss.setCenterPosition(player.getEyePosition().add(xOffset, yOffset, zOffset));
                    Vec3 offset = new Vec3(player.getEyePosition().x, player.getEyePosition().y, player.getEyePosition().z);
                    EntityEnchantedSword.spawnSwords(player,worldIn,offset, EntityNRBlisteringSword.SpawnMode.RANDOM,1,
                            true,0,0,0,5,ss.getDamage(),ss.getColor(), false, ss.getDelay());
                    player.playSound(SoundEvents.ENDER_DRAGON_FLAP, 0.2F,1.45F);
                }
            }
        }else if (NRConfig.OFFHAND_CAN_ACTIVE.get().equals(true) && offhandItem.getItem() instanceof ItemSlashBlade) {
            CompoundTag nbt = offhandItem.getTag();
            if (nbt == null) return;
            if (!hasSpecialEffects(player.getOffhandItem(), prinegorerouse.MOD_ID+":fate"))return;
            double decimal1 = random.nextDouble();
            if (decimal1 - 0.5d <= 0.000001D) {
                Level worldIn = player.level();
                EntityEnchantedSword ss = new EntityEnchantedSword(NrEntitiesRegistry.Enchanted_Sword, worldIn);
                ss.setSpeed(10.0F);
                ss.setNoClip(false);
                ss.setDamage(state.getBaseAttackModifier() * 0.1F);
                ss.setDelay(5);
                ss.setColor(16766720);
                RandomSource random = worldIn.random;
                double xOffset = random.nextDouble() * 2;
                double zOffset = (double) random.nextFloat() * 2;
                double yOffset = random.nextFloat();
                ss.setCenterPosition(player.getEyePosition().add(xOffset, yOffset, zOffset));
                Vec3 offset = new Vec3(player.getEyePosition().x, player.getEyePosition().y, player.getEyePosition().z);
                EntityEnchantedSword.spawnSwords(player,worldIn,offset, EntityNRBlisteringSword.SpawnMode.RANDOM,1,
                        true,0,0,0,5,ss.getDamage(),ss.getColor(), false, ss.getDelay());
                player.playSound(SoundEvents.ENDER_DRAGON_FLAP, 0.2F,1.45F);
            }
        }
    }
}
