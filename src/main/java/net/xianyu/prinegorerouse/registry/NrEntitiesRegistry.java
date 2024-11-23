package net.xianyu.prinegorerouse.registry;

import com.google.common.base.CaseFormat;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.xianyu.prinegorerouse.entity.*;


public class NrEntitiesRegistry {
    public static final ResourceLocation BlisteringSwordsLoc = new ResourceLocation("prinegorerouse", classToString(EntityBlisteringSword.class));
    public static EntityType<EntityBlisteringSword> BlisteringSword;
    public static EntityType<EntityBlisteringSword> Zenith12th_Sword;
    public static EntityType<EntityBlisteringSword> Storm_Sword;
    public static EntityType<EntityDrive> DriveEx;
    public NrEntitiesRegistry() {
    }

    public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityBlisteringSword> entity = BlisteringSword = Builder
                    .of(EntityBlisteringSword::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .setTrackingRange(4)
                    .setUpdateInterval(20)
                    .setCustomClientFactory(EntityBlisteringSword::createInstance)
                    .build(BlisteringSwordsLoc.toString());
            helper.register(BlisteringSwordsLoc, entity);
        });

        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityBlisteringSword> entity = Zenith12th_Sword = Builder
                    .of(EntityBlisteringSword::new, MobCategory.MISC)
                    .sized(0.9F, 0.9F)
                    .setTrackingRange(4)
                    .setUpdateInterval(20)
                    .setCustomClientFactory(EntityZenith12thSword::createInstance)
                    .build("zenith12th_sword");
            helper.register(new ResourceLocation("prinegorerouse", "zenith12th_sword"), entity);
        });

        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityBlisteringSword> entity = Storm_Sword = Builder
                    .of(EntityBlisteringSword::new, MobCategory.MISC)
                    .sized(0.9F, 0.9F)
                    .setTrackingRange(4)
                    .setUpdateInterval(20)
                    .setCustomClientFactory(EntityStormSword::createInstance)
                    .build("explosion_sword");
            helper.register(new ResourceLocation("prinegorerouse", "explosion_sword"), entity);
        });

        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityDrive> entity = DriveEx = Builder
                    .of(EntityDrive::new, MobCategory.MISC)
                    .sized(10.0F, 3.0F)
                    .setTrackingRange(4)
                    .setUpdateInterval(20)
                    .setCustomClientFactory(EntityDriveEx::createInstance)
                    .build("drive_ex");
            helper.register(new ResourceLocation("prinegorerouse", "drive_ex"), entity);
        });

    }


    private static String classToString(Class<? extends Entity> entityClass) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entityClass.getSimpleName())
                .replace("entity_", "");
    }
}
