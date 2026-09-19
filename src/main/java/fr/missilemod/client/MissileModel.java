package fr.missilemod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;

/**
 * Modele generique d'un objet rigide (missile, obus) : affiche une partie nommee d'un modele de {@link ModModels}.
 * L'orientation est entierement geree par le renderer.
 */
public class MissileModel<T extends Entity> extends EntityModel<T> {

    private final ModelPart part;

    public MissileModel(ModelPart root, String child) {
        super(RenderType::entityCutoutNoCull);
        this.part = root.getChild(child);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.part.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
