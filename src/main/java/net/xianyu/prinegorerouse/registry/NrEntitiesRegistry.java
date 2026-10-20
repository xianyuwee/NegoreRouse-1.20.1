package net.xianyu.prinegorerouse.registry;

import com.google.common.base.CaseFormat;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.entity.EntityJudgementCut;
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
    public static EntityType<EntityNRBlisteringSword> Reverse_Sword;
    public static EntityType<EntityAbsolutePhantom> Absolute_Phantom;
    public static EntityType<EntityNRDrive> NRDrive;
    public static EntityType<EntityNRDrive> DriveEx;
    public static EntityType<EntityNRDrive> LineDrive;
    public static EntityType<EntityNRSDrive> NRSDrive;
    public static EntityType<EntityNRSDrive> FireDrive;
    public static EntityType<EntityNRSDrive> ShinyDrive;
    public static EntityType<EntityNRJudgementCut> NRJudgementCut;
    public static EntityType<EntityFateEye> Fate_Eye;
    public NrEntitiesRegistry() {
    }

    public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityNRBlisteringSword> entity = NRBlisteringSword = Builder
                    .of(EntityNRBlisteringSword::new, MobCategory.MISC)
                    .sized(0.5F,0.5F)
                    // setTrackingRange 单位是「区块」，服务端按 ×16 换算成方格：原来的 4 只有 64 格。
                    // 幻影剑常在数十格外的锁定目标身上生成，追踪外扩也会超过 64 格，
                    // 一旦越界服务端就下发移除包、客户端直接把它删掉（表现为“剑凭空消失/没飞回来”）。
                    // 提到 8（128 格）；实际仍会被服务端视距截断，不会多发无用的包。
                    .setTrackingRange(8)
                    .setUpdateInterval(20)
                    .setCustomClientFactory(EntityNRBlisteringSword::createInstance)
                    .build(NRBlisteringSwordsLoc.toString());
            helper.register(NRBlisteringSwordsLoc, entity);
        });

        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityNRBlisteringSword> entity = Storm_Sword = Builder
                    .of(EntityNRBlisteringSword::new, MobCategory.MISC)
                    .sized(0.9F, 0.9F)
                    .setTrackingRange(8) // 同 NRBlisteringSword：64 格太小，远处生成/外扩会看不见
                    .setUpdateInterval(20)
                    .setCustomClientFactory(EntityStormSword::createInstance)
                    .build("explosion_sword");
            helper.register(new ResourceLocation("prinegorerouse", "explosion_sword"), entity);
        });

        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityNRBlisteringSword> entity = Enchanted_Sword = Builder
                    .of(EntityNRBlisteringSword::new, MobCategory.MISC)
                    .sized(0.9F, 0.9F)
                    .setTrackingRange(8) // 同 NRBlisteringSword：十二连就在这类剑上生成于远处目标
                    .setUpdateInterval(20)
                    .setCustomClientFactory(EntityEnchantedSword::createInstance)
                    .build("enchanted_sword");
            helper.register(new ResourceLocation("prinegorerouse", "enchanted_sword"), entity);
        });

        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityNRBlisteringSword> entity = Countable_Sword = Builder
                    .of(EntityNRBlisteringSword::new, MobCategory.MISC)
                    .sized(0.9F, 0.9F)
                    .setTrackingRange(8) // 同 NRBlisteringSword：64 格太小
                    .setUpdateInterval(20)
                    .setCustomClientFactory(EntityCountableSwords::createInstance)
                    .build("countable_sword");
            helper.register(new ResourceLocation("prinegorerouse", "countable_sword"), entity);
        });

        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityNRBlisteringSword> entity = Reverse_Sword = Builder
                    .of(EntityNRBlisteringSword::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .setTrackingRange(8) // 同 NRBlisteringSword：64 格太小
                    .setUpdateInterval(20)
                    .setCustomClientFactory(EntityReverseSword::createInstance)
                    .build("reverse_sword");
            helper.register(new ResourceLocation("prinegorerouse", "reverse_sword"), entity);
        });

        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityAbsolutePhantom> entity = Absolute_Phantom = Builder
                    .of(EntityAbsolutePhantom::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .setTrackingRange(64)
                    .setUpdateInterval(1)
                    .setCustomClientFactory(EntityAbsolutePhantom::createInstance)
                    .build("absolute_phantom");
            helper.register(new ResourceLocation("prinegorerouse", "absolute_phantom"), entity);
        });

        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityNRDrive> entity = DriveEx = Builder
                    .of(EntityNRDrive::new, MobCategory.MISC)
                    .sized(15.0F, 15.0F)
                    .setTrackingRange(4)
                    .setUpdateInterval(20)
                    .setCustomClientFactory(EntityDriveEx::createInstance)
                    .build("drive_ex");
            helper.register(new ResourceLocation("prinegorerouse", "drive_ex"), entity);
        });

        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityNRSDrive> entity = FireDrive = Builder
                    .of(EntityNRSDrive::new, MobCategory.MISC)
                    .sized(3.0F, 5.0F)
                    .setTrackingRange(4)
                    .setUpdateInterval(20)
                    .setCustomClientFactory(EntityFireDrive::createInstance)
                    .build("fire_drive");
            helper.register(new ResourceLocation("prinegorerouse", "fire_drive"), entity);
        });

        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityNRDrive> entity = LineDrive = Builder
                    .of(EntityNRDrive::new, MobCategory.MISC)
                    .sized(3.0F,5.0F)
                    .setTrackingRange(5)
                    .setUpdateInterval(20)
                    .setCustomClientFactory(EntityLineDrive::createInstance)
                    .build("line_drive");
            helper.register(new ResourceLocation("prinegorerouse", "line_drive"), entity);
        });

        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityNRSDrive> entity = ShinyDrive = Builder
                    .of(EntityNRSDrive::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F)
                    .setTrackingRange(4)
                    .setUpdateInterval(20)
                    .setCustomClientFactory(EntityShinyDrive::createInstance)
                    .build("shiny_drive");
            helper.register(new ResourceLocation("prinegorerouse", "shiny_drive"), entity);
        });

        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityNRDrive> entityType = NRDrive = Builder
                    .of(EntityNRDrive::new, MobCategory.MISC)
                    .sized(10.0F, 5.0F)
                    .setTrackingRange(5)
                    .setCustomClientFactory(EntityNRDrive::createInstance)
                    .build("NR_drive");
            helper.register(new ResourceLocation("prinegorerouse", "nr_drive"), entityType);
        });

        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityNRSDrive> entityType = NRSDrive = Builder
                    .of(EntityNRSDrive::new, MobCategory.MISC)
                    .sized(3.0F, 5.0F)
                    .setTrackingRange(4)
                    .setCustomClientFactory(EntityNRSDrive::createInstance)
                    .build("NRS_drive");
            helper.register(new ResourceLocation("prinegorerouse", "nrs_drive"), entityType);
        });

        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityNRJudgementCut> entityType = NRJudgementCut = Builder
                    .of(EntityNRJudgementCut::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .setTrackingRange(4)
                    .setCustomClientFactory(EntityNRJudgementCut::createInstance)
                    .build("nr_judgement_cut");
            helper.register(new ResourceLocation("prinegorerouse", "nr_judgement_cut"), entityType);
        });

        event.register(ForgeRegistries.Keys.ENTITY_TYPES, (helper) -> {
            EntityType<EntityFateEye> entity = Fate_Eye = Builder
                    .of(EntityFateEye::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .setTrackingRange(64)
                    .setUpdateInterval(1)
                    .setCustomClientFactory(EntityFateEye::createInstance)
                    .build("fate_eye");
            helper.register(new ResourceLocation("prinegorerouse", "fate_eye"), entity);
        });

    }


    private static String classToString(Class<? extends Entity> entityClass) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entityClass.getSimpleName())
                .replace("entity_", "");
    }
}
