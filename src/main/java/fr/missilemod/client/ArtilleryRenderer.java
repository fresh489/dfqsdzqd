package fr.missilemod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.missilemod.MissileMod;
import fr.missilemod.block.ArtilleryBlockEntity;
import fr.missilemod.block.ArtilleryType;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

/**
 * Rendu anime du mortier et des obusiers : rotation de toute la piece vers la cible (yaw), elevation du berceau
 * (pitch) et recul du tube, tous interpoles image par image.
 */
public class ArtilleryRenderer implements BlockEntityRenderer<ArtilleryBlockEntity> {

    private record Rig(ModelPart root, ModelPart cradle, ModelPart barrel, float barrelZ, ResourceLocation texture) {
    }

    private final Map<ArtilleryType, Rig> rigs = new EnumMap<>(ArtilleryType.class);

    public ArtilleryRenderer(BlockEntityRendererProvider.Context context) {
        this.rigs.put(ArtilleryType.MORTAR, rig(context.bakeLayer(ModModels.MORTAR), "artillery_mortar"));
        this.rigs.put(ArtilleryType.M777_105, rig(context.bakeLayer(ModModels.M777_105), "artillery_m777_105"));
        this.rigs.put(ArtilleryType.M777_155, rig(context.bakeLayer(ModModels.M777_155), "artillery_m777_155"));
    }

    private static Rig rig(ModelPart root, String texture) {
        ModelPart cradle = root.getChild("cradle");
        ModelPart barrel = cradle.getChild("barrel");
        return new Rig(root, cradle, barrel, barrel.z,
                new ResourceLocation(MissileMod.MOD_ID, "textures/entity/" + texture + ".png"));
    }

    @Override
    public void render(ArtilleryBlockEntity gun, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        Rig rig = this.rigs.get(gun.getArtilleryType());
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        // Modeles d'artillerie : Y vers le haut, tube vers +Z (pas de retournement comme pour les entites).
        poseStack.mulPose(Axis.YP.rotationDegrees(gun.getYaw(partialTick)));
        rig.cradle().xRot = (float) Math.toRadians(-gun.getPitch(partialTick));
        rig.barrel().z = rig.barrelZ() - gun.getRecoil(partialTick);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(rig.texture()));
        rig.root().render(poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(ArtilleryBlockEntity gun) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
