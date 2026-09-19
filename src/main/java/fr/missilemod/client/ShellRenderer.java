package fr.missilemod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.missilemod.MissileMod;
import fr.missilemod.entity.ShellEntity;
import fr.missilemod.entity.ShellType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

/** Rendu des obus, orientes selon leur vitesse. */
public class ShellRenderer extends EntityRenderer<ShellEntity> {

    private final MissileModel<ShellEntity> mortar;
    private final MissileModel<ShellEntity> shell105;
    private final MissileModel<ShellEntity> shell155;
    private final Map<ShellType, ResourceLocation> textures = new EnumMap<>(ShellType.class);

    public ShellRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.mortar = new MissileModel<>(context.bakeLayer(ModModels.MORTAR_SHELL), "shell");
        this.shell105 = new MissileModel<>(context.bakeLayer(ModModels.SHELL_105), "shell");
        this.shell155 = new MissileModel<>(context.bakeLayer(ModModels.SHELL_155), "shell");
        for (ShellType type : ShellType.values()) {
            this.textures.put(type, new ResourceLocation(MissileMod.MOD_ID, "textures/entity/" + type.id + ".png"));
        }
    }

    @Override
    public void render(ShellEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        ShellType type = entity.getShellType();
        MissileModel<ShellEntity> model = switch (type.caliber) {
            case MORTAR_81 -> this.mortar;
            case C105 -> this.shell105;
            case C155 -> this.shell155;
        };
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.2D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYawDegrees(partialTick)));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F - entity.getPitchDegrees(partialTick)));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        VertexConsumer consumer = buffer.getBuffer(model.renderType(getTextureLocation(entity)));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ShellEntity entity) {
        return this.textures.get(entity.getShellType());
    }
}
