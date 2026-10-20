package net.xianyu.prinegorerouse.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import mods.flammpfeil.slashblade.client.renderer.util.MSAutoCloser;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.xianyu.prinegorerouse.entity.EntityAbsolutePhantom;

/**
 * 幻影刃渲染器：以复刻自触发刀（或卡俄斯被吞池）的建模 + 贴图，只渲染刀刃组 {@code "blade"}（不含 {@code sheath}），
 * 呈淡金色、半透明，并按实体同步的 yaw/pitch/roll 做朝向变换。
 * <p>仿 {@code SummonedSwordRenderer}（组名/渲染调用）与 {@code NRJudgementCutRenderer}（setCol alpha 透明度）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class AbsolutePhantomRenderer extends EntityRenderer<EntityAbsolutePhantom> {

    /** 刀刃组名（slashblade 刀模 OBJ 中刀刃为 {@code blade}，刀鞘为 {@code sheath}）。 */
    private static final String BLADE_GROUP = "blade";
    /** 淡金基色的 RGB 部分。 */
    private static final int BASE_RGB = 0xD8AF4A;
    /** 尺寸缩放（刀模顶点单位较大，取近原生召唤剑的缩放）。 */
    private static final float RENDER_SCALE = 0.0075F;

    private static final ResourceLocation FALLBACK_MODEL =
            ResourceLocation.tryParse("slashblade:model/util/ss.obj");
    private static final ResourceLocation FALLBACK_TEX =
            ResourceLocation.tryParse("slashblade:model/util/ss.png");

    public AbsolutePhantomRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityAbsolutePhantom entity) {
        ResourceLocation tex = parse(entity.getTextureLocStr());
        return tex != null ? tex : FALLBACK_TEX;
    }

    @Override
    public void render(EntityAbsolutePhantom entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ResourceLocation modelLoc = parse(entity.getModelLocStr());
        if (modelLoc == null) {
            modelLoc = entity.getModelLoc(); // 父类默认模型
        }
        WavefrontObject model = BladeModelManager.getInstance().getModel(modelLoc);
        if (model == null) {
            model = BladeModelManager.getInstance().getModel(FALLBACK_MODEL);
        }
        if (model == null) {
            return;
        }

        ResourceLocation tex = getTextureLocation(entity);

        // 淡入淡出透明度
        int remain = entity.getRemainingLife();
        float fadeIn = Mth.clamp(entity.tickCount / 5.0F, 0.0F, 1.0F);
        float fadeOut = Mth.clamp(remain / 8.0F, 0.0F, 1.0F);
        float life = fadeIn * fadeOut;
        // 半透明上限约 0xAA，配合发光呈现“淡金透明”
        int alpha = (int) (0xAA * (0.3F + 0.7F * life));
        alpha = Mth.clamp(alpha, 0, 0xFF);
        int color = (alpha << 24) | (BASE_RGB & 0xFFFFFF);

        try (MSAutoCloser msac = MSAutoCloser.pushMatrix(poseStack)) {
            // 朝向：本渲染器画的是手持刀模的 "blade" 组，其长轴（刀尖方向）沿模型局部 X（实测 dX≈333）。
            // 故：pitch→ZP 才能把 X 轴抬成竖直（ZP(90) 使刀身立起）；roll→XP 沿刀身自旋；yaw→YP 水平朝向。
            // 原生 SummonedSwordRenderer 末尾那个基准 YP(90) 是为 Z 轴对齐的 ss.obj 服务的，对 X 轴对齐的
            // "blade" 组会把刀身转到 Z、再被 ZP 绕自身旋转→永远平躺，故此处不加。
            poseStack.mulPose(Axis.YP.rotationDegrees(
                    Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(
                    Mth.rotLerp(partialTicks, entity.xRotO, entity.getXRot())));
            poseStack.mulPose(Axis.XP.rotationDegrees(entity.getRoll()));
            poseStack.scale(RENDER_SCALE, RENDER_SCALE, RENDER_SCALE);

            // 挥刀动画：仅攻击相位的刀会挥砍（环绕/归位保持静止姿态，不跟随玩家挥刀）。
            // 驱动源：玩家挥刀中→用其 getAttackAnim（与玩家动画同源同节奏）；否则→用幻影刃自挥进度。
            // 招式逐挥轮换（模拟玩家 combo 每段动作不同），曲线两端均为 0，挥完跳回无视觉跳变。
            if (entity.isAttackPhase()) {
                float t = 0.0F;
                int motionIdx = 0;
                float ownerT = 0.0F;
                if (entity.getOwner() instanceof LivingEntity livingOwner) {
                    ownerT = livingOwner.getAttackAnim(partialTicks);
                }
                float selfT = entity.getSelfSwingAnim(partialTicks);
                if (ownerT > 0.0F && ownerT <= 1.0F) {
                    t = ownerT;
                    motionIdx = entity.getPlayerSwingCount() % SWING_MOTIONS.length;
                } else if (selfT > 0.0F && selfT <= 1.0F) {
                    t = selfT;
                    motionIdx = entity.getSwingSeq() % SWING_MOTIONS.length;
                }
                if (t > 0.0F) {
                    float[] m = SWING_MOTIONS[motionIdx];
                    float swing = swingCurve(t, m[0], m[1]);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(swing));          // 刀面内扫掠（主挥砍弧）
                    poseStack.mulPose(Axis.XP.rotationDegrees(swing * m[2]));   // 面外偏转（斜斩/横扫感）
                }
            }

            BladeRenderState.setCol(color, false);
            BladeRenderState.renderOverridedLuminous(
                    ItemStack.EMPTY, model, BLADE_GROUP, tex, poseStack, buffer, BladeRenderState.MAX_LIGHT);
        }
    }

    /**
     * 挥砍招式库（模拟 combo 不同段落）：每行为｛蓄力角，挥砍角，面外权重｝。
     * 下劈／逆刃上挑／大幅横扫三式，由渲染层按挥刀次数轮换。
     */
    private static final float[][] SWING_MOTIONS = {
            {-65.0F, 115.0F, 0.30F},   // 一式：高举下劈
            {70.0F, -120.0F, -0.45F},  // 二式：反手持刀上挑（方向相反）
            {-50.0F, 95.0F, 0.95F},    // 三式：斜向大幅横扫（面外分量重）
    };

    /** 三段挥砍曲线：蓄力 0→windup → 挥砍 windup→slash → 收刀 slash→0。 */
    private static float swingCurve(float t, float windup, float slash) {
        if (t < 0.30F) {
            return Mth.lerp(t / 0.30F, 0.0F, windup);
        } else if (t < 0.55F) {
            return Mth.lerp((t - 0.30F) / 0.25F, windup, slash);
        } else {
            return Mth.lerp((t - 0.55F) / 0.45F, slash, 0.0F);
        }
    }

    private static ResourceLocation parse(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        return ResourceLocation.tryParse(s);
    }
}
