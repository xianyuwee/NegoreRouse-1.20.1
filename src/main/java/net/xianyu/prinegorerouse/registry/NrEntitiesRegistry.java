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
    public static final ResourceLocation NRBlisteringSwordsLoc = new ResourceLocation("prinegorerouse", classToString(EntityNRBlisteringSword.class));
    public static EntityType<EntityNRBlisteringSword> NRBlisteringSword;
    public static EntityType<EntityNRBlisteringSword> Storm_Sword;
    public static EntityType<EntityNRBlisteringSword> Enchanted_Sword;
    public static EntityType<EntityNRBlisteringSword> Countable_Sword;
    public static EntityType<EntityDrive> DriveEx;
    public static EntityType<EntityDrive> FireDrive;
    public static EntityType<EntityDrive> ShinyDrive;
    public NrEntitiesRegistry() {
    }

    public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityNRBlisteringSword> entity = NRBlisteringSword = Builder
                    .of(EntityNRBlisteringSword::new, MobCategory.MISC)
                    .sized(0.5F,0.5F)
                    .setTrackingRange(4)
                    .setUpdateInterval(20)
                    .setCustomClientFactory(EntityNRBlisteringSword::createInstance)
                    .build(NRBlisteringSwordsLoc.toString());
            helper.register(NRBlisteringSwordsLoc, entity);
        });

        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityNRBlisteringSword> entity = Storm_Sword = Builder
                    .of(EntityNRBlisteringSword::new, MobCategory.MISC)
                    .sized(0.9F, 0.9F)
                    .setTrackingRange(4)
                    .setUpdateInterval(20)
                    .setCustomClientFactory(EntityStormSword::createInstance)
                    .build("explosion_sword");
            helper.register(new ResourceLocation("prinegorerouse", "explosion_sword"), entity);
        });

        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityNRBlisteringSword> entity = Enchanted_Sword = Builder
                    .of(EntityNRBlisteringSword::new, MobCategory.MISC)
                    .sized(0.9F, 0.9F)
                    .setTrackingRange(4)
                    .setUpdateInterval(20)
                    .setCustomClientFactory(EntityEnchantedSword::createInstance)
                    .build("enchanted_sword");
            helper.register(new ResourceLocation("prinegorerouse", "enchanted_sword"), entity);
        });

        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityNRBlisteringSword> entity = Countable_Sword = Builder
                    .of(EntityNRBlisteringSword::new, MobCategory.MISC)
                    .sized(0.9F, 0.9F)
                    .setTrackingRange(4)
                    .setUpdateInterval(20)
                    .setCustomClientFactory(EntityCountableSwords::createInstance)
                    .build("countable_sword");
            helper.register(new ResourceLocation("prinegorerouse", "countable_sword"), entity);
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

        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityDrive> entity = FireDrive = Builder
                    .of(EntityDrive::new, MobCategory.MISC)
                    .sized(10.0F, 3.0F)
                    .setTrackingRange(4)
                    .setUpdateInterval(20)
                    .setCustomClientFactory(EntityFireDrive::createInstance)
                    .build("fire_drive");
            helper.register(new ResourceLocation("prinegorerouse", "fire_drive"), entity);
        });

        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityDrive> entity = ShinyDrive = Builder
                    .of(EntityDrive::new, MobCategory.MISC)
                    .sized(10.0F, 3.0F)
                    .setTrackingRange(4)
                    .setUpdateInterval(20)
                    .setCustomClientFactory(EntityShinyDrive::createInstance)
                    .build("shiny_drive");
            helper.register(new ResourceLocation("prinegorerouse", "shiny_drive"), entity);
        });

    }


    private static String classToString(Class<? extends Entity> entityClass) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entityClass.getSimpleName())
                .replace("entity_", "");
    }
}
