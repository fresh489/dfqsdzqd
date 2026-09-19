package fr.missilemod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.missilemod.MissileMod;
import fr.missilemod.entity.MissileEntity;
import fr.missilemod.entity.MissileType;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class MissileRenderer extends EntityRenderer<MissileEntity> {

    private final MissileModel<MissileEntity> model;
    private final ResourceLocation texture;

    public MissileRenderer(EntityRendererProvider.Context context, MissileType type) {
        super(context);
        this.model = new MissileModel<>(context.bakeLayer(layerFor(type)), "missile");
        this.texture = new ResourceLocation(MissileMod.MOD_ID, "textures/entity/" + type.texture + ".png");
        this.shadowRadius = 0.25F + type.width * 0.1F;
    }

    private static ModelLayerLocation layerFor(MissileType type) {
        return switch (type.model) {
            case "grad" -> ModModels.GRAD;
            case "tomahawk" -> ModModels.TOMAHAWK;
            case "scud" -> ModModels.SCUD;
            case "strategic" -> ModModels.STRATEGIC;
            default -> ModModels.AIM120;
        };
    }

    @Override
    public void render(MissileEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        // Pivot au centre du missile (moitie de sa hauteur au-dessus de la base de la hitbox)
        poseStack.translate(0.0D, entity.centerOffset(), 0.0D);
        // Yaw autour de Y, puis inclinaison : pitch 90 = vertical, 0 = horizontal, -90 = pique
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getVisualYaw(partialTick)));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F - entity.getVisualPitch(partialTick)));
        // Les modeles d'entite ont l'axe Y vers le bas : on retourne pour avoir le nez vers +Y
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        VertexConsumer consumer = buffer.getBuffer(this.model.renderType(getTextureLocation(entity)));
        this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MissileEntity entity) {
        return this.texture;
    }
}
