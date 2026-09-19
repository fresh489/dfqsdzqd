package fr.missilemod.client;

import fr.missilemod.MissileMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;

/**
 * FICHIER GENERE par tools/generate_assets.py : ne pas modifier a la main, relancer le script.
 * Tous les modeles 3D du mod (missiles, obus, artillerie).
 */
@SuppressWarnings("unused")
public final class ModModels {

    public static final ModelLayerLocation AIM120 = layer("aim120");
    public static final ModelLayerLocation GRAD = layer("grad");
    public static final ModelLayerLocation TOMAHAWK = layer("tomahawk");
    public static final ModelLayerLocation SCUD = layer("scud");
    public static final ModelLayerLocation STRATEGIC = layer("strategic");
    public static final ModelLayerLocation MORTAR_SHELL = layer("mortar_shell");
    public static final ModelLayerLocation SHELL_105 = layer("shell_105");
    public static final ModelLayerLocation SHELL_155 = layer("shell_155");
    public static final ModelLayerLocation M777_105 = layer("m777_105");
    public static final ModelLayerLocation M777_155 = layer("m777_155");
    public static final ModelLayerLocation MORTAR = layer("mortar");

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(new ResourceLocation(MissileMod.MOD_ID, name), "main");
    }

    public static void register(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(AIM120, ModModels::aim120);
        event.registerLayerDefinition(GRAD, ModModels::grad);
        event.registerLayerDefinition(TOMAHAWK, ModModels::tomahawk);
        event.registerLayerDefinition(SCUD, ModModels::scud);
        event.registerLayerDefinition(STRATEGIC, ModModels::strategic);
        event.registerLayerDefinition(MORTAR_SHELL, ModModels::mortarShell);
        event.registerLayerDefinition(SHELL_105, ModModels::shell105);
        event.registerLayerDefinition(SHELL_155, ModModels::shell155);
        event.registerLayerDefinition(M777_105, ModModels::m777105);
        event.registerLayerDefinition(M777_155, ModModels::m777155);
        event.registerLayerDefinition(MORTAR, ModModels::mortar);
    }

    public static LayerDefinition aim120() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_missile = root.addOrReplaceChild("missile", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.00F, -13.00F, -3.00F, 6.0F, 33.0F, 6.0F)
                .texOffs(24, 0).addBox(-2.50F, -16.00F, -2.50F, 5.0F, 3.0F, 5.0F)
                .texOffs(44, 0).addBox(-2.00F, -19.00F, -2.00F, 4.0F, 3.0F, 4.0F)
                .texOffs(44, 7).addBox(-1.50F, -21.00F, -1.50F, 3.0F, 2.0F, 3.0F)
                .texOffs(56, 7).addBox(-1.00F, -23.00F, -1.00F, 2.0F, 2.0F, 2.0F)
                .texOffs(60, 0).addBox(-0.50F, -24.00F, -0.50F, 1.0F, 1.0F, 1.0F)
                .texOffs(24, 8).addBox(-2.50F, 20.00F, -2.50F, 5.0F, 2.0F, 5.0F)
                .texOffs(44, 12).addBox(-2.00F, 22.00F, -2.00F, 4.0F, 2.0F, 4.0F)
                .texOffs(24, 18).addBox(-3.00F, -13.00F, -3.00F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.20F))
                .texOffs(24, 18).addBox(-3.00F, 9.00F, -3.00F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.20F))
                .texOffs(24, 18).addBox(-3.00F, 19.00F, -3.00F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.20F))
                .texOffs(48, 18).addBox(3.00F, -6.00F, -0.50F, 2.0F, 9.0F, 1.0F)
                .texOffs(48, 18).addBox(-5.00F, -6.00F, -0.50F, 2.0F, 9.0F, 1.0F)
                .texOffs(54, 18).addBox(-0.50F, -6.00F, 3.00F, 1.0F, 9.0F, 2.0F)
                .texOffs(54, 18).addBox(-0.50F, -6.00F, -5.00F, 1.0F, 9.0F, 2.0F)
                .texOffs(24, 25).addBox(5.00F, -3.00F, -0.50F, 2.0F, 6.0F, 1.0F)
                .texOffs(24, 25).addBox(-7.00F, -3.00F, -0.50F, 2.0F, 6.0F, 1.0F)
                .texOffs(30, 25).addBox(-0.50F, -3.00F, 5.00F, 1.0F, 6.0F, 2.0F)
                .texOffs(30, 25).addBox(-0.50F, -3.00F, -7.00F, 1.0F, 6.0F, 2.0F)
                .texOffs(60, 2).addBox(7.00F, 0.00F, -0.50F, 1.0F, 3.0F, 1.0F)
                .texOffs(60, 2).addBox(-8.00F, 0.00F, -0.50F, 1.0F, 3.0F, 1.0F)
                .texOffs(60, 11).addBox(-0.50F, 0.00F, 7.00F, 1.0F, 3.0F, 1.0F)
                .texOffs(60, 11).addBox(-0.50F, 0.00F, -8.00F, 1.0F, 3.0F, 1.0F)
                .texOffs(36, 25).addBox(3.00F, 12.00F, -0.50F, 3.0F, 8.0F, 1.0F)
                .texOffs(36, 25).addBox(-6.00F, 12.00F, -0.50F, 3.0F, 8.0F, 1.0F)
                .texOffs(44, 28).addBox(-0.50F, 12.00F, 3.00F, 1.0F, 8.0F, 3.0F)
                .texOffs(44, 28).addBox(-0.50F, 12.00F, -6.00F, 1.0F, 8.0F, 3.0F)
                .texOffs(52, 29).addBox(6.00F, 14.00F, -0.50F, 2.0F, 6.0F, 1.0F)
                .texOffs(52, 29).addBox(-8.00F, 14.00F, -0.50F, 2.0F, 6.0F, 1.0F)
                .texOffs(58, 29).addBox(-0.50F, 14.00F, 6.00F, 1.0F, 6.0F, 2.0F)
                .texOffs(58, 29).addBox(-0.50F, 14.00F, -8.00F, 1.0F, 6.0F, 2.0F)
                .texOffs(60, 15).addBox(8.00F, 16.00F, -0.50F, 1.0F, 4.0F, 1.0F)
                .texOffs(60, 15).addBox(-9.00F, 16.00F, -0.50F, 1.0F, 4.0F, 1.0F)
                .texOffs(60, 20).addBox(-0.50F, 16.00F, 8.00F, 1.0F, 4.0F, 1.0F)
                .texOffs(60, 20).addBox(-0.50F, 16.00F, -9.00F, 1.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(0.00F, 0.00F, 0.00F, 0.00F, 0.00F, 0.00F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition grad() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_missile = root.addOrReplaceChild("missile", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.50F, -10.00F, -1.50F, 3.0F, 23.0F, 3.0F)
                .texOffs(12, 0).addBox(-1.00F, -13.00F, -1.00F, 2.0F, 3.0F, 2.0F)
                .texOffs(20, 0).addBox(-0.50F, -16.00F, -0.50F, 1.0F, 3.0F, 1.0F)
                .texOffs(24, 0).addBox(-2.00F, 13.00F, -2.00F, 4.0F, 2.0F, 4.0F)
                .texOffs(40, 0).addBox(-1.00F, 15.00F, -1.00F, 2.0F, 1.0F, 2.0F)
                .texOffs(48, 0).addBox(1.50F, 9.00F, -0.50F, 2.0F, 6.0F, 1.0F)
                .texOffs(48, 0).addBox(-3.50F, 9.00F, -0.50F, 2.0F, 6.0F, 1.0F)
                .texOffs(54, 0).addBox(-0.50F, 9.00F, 1.50F, 1.0F, 6.0F, 2.0F)
                .texOffs(54, 0).addBox(-0.50F, 9.00F, -3.50F, 1.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(0.00F, 0.00F, 0.00F, 0.00F, 0.00F, 0.00F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition tomahawk() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_missile = root.addOrReplaceChild("missile", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.50F, -20.00F, -2.50F, 5.0F, 40.0F, 5.0F)
                .texOffs(20, 0).addBox(-2.00F, -22.00F, -2.00F, 4.0F, 2.0F, 4.0F)
                .texOffs(36, 0).addBox(-1.50F, -23.00F, -1.50F, 3.0F, 1.0F, 3.0F)
                .texOffs(48, 0).addBox(-0.50F, -24.00F, -0.50F, 1.0F, 1.0F, 1.0F)
                .texOffs(48, 2).addBox(-2.00F, 20.00F, -2.00F, 4.0F, 2.0F, 4.0F)
                .texOffs(36, 4).addBox(-1.50F, 22.00F, -1.50F, 3.0F, 2.0F, 3.0F)
                .texOffs(20, 6).addBox(-1.50F, 6.00F, 2.50F, 3.0F, 7.0F, 2.0F)
                .texOffs(30, 9).addBox(2.00F, -3.00F, -0.50F, 9.0F, 5.0F, 1.0F)
                .texOffs(30, 9).addBox(-11.00F, -3.00F, -0.50F, 9.0F, 5.0F, 1.0F)
                .texOffs(50, 8).addBox(2.50F, 17.00F, -0.50F, 3.0F, 5.0F, 1.0F)
                .texOffs(50, 8).addBox(-5.50F, 17.00F, -0.50F, 3.0F, 5.0F, 1.0F)
                .texOffs(50, 14).addBox(-0.50F, 17.00F, 2.50F, 1.0F, 5.0F, 3.0F)
                .texOffs(50, 14).addBox(-0.50F, 17.00F, -5.50F, 1.0F, 5.0F, 3.0F),
                PartPose.offsetAndRotation(0.00F, 0.00F, 0.00F, 0.00F, 0.00F, 0.00F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition scud() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_missile = root.addOrReplaceChild("missile", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.00F, -18.00F, -5.00F, 10.0F, 48.0F, 10.0F)
                .texOffs(40, 0).addBox(-4.00F, -22.00F, -4.00F, 8.0F, 4.0F, 8.0F)
                .texOffs(72, 0).addBox(-3.00F, -26.00F, -3.00F, 6.0F, 4.0F, 6.0F)
                .texOffs(96, 0).addBox(-2.00F, -29.00F, -2.00F, 4.0F, 3.0F, 4.0F)
                .texOffs(112, 0).addBox(-1.00F, -31.00F, -1.00F, 2.0F, 2.0F, 2.0F)
                .texOffs(120, 0).addBox(-0.50F, -32.00F, -0.50F, 1.0F, 1.0F, 1.0F)
                .texOffs(96, 7).addBox(-3.00F, 30.00F, -3.00F, 6.0F, 2.0F, 6.0F)
                .texOffs(120, 2).addBox(5.00F, 18.00F, -0.50F, 3.0F, 14.0F, 1.0F)
                .texOffs(120, 2).addBox(-8.00F, 18.00F, -0.50F, 3.0F, 14.0F, 1.0F)
                .texOffs(72, 10).addBox(-0.50F, 18.00F, 5.00F, 1.0F, 14.0F, 3.0F)
                .texOffs(72, 10).addBox(-0.50F, 18.00F, -8.00F, 1.0F, 14.0F, 3.0F)
                .texOffs(80, 10).addBox(8.00F, 22.00F, -0.50F, 2.0F, 10.0F, 1.0F)
                .texOffs(80, 10).addBox(-10.00F, 22.00F, -0.50F, 2.0F, 10.0F, 1.0F)
                .texOffs(86, 10).addBox(-0.50F, 22.00F, 8.00F, 1.0F, 10.0F, 2.0F)
                .texOffs(86, 10).addBox(-0.50F, 22.00F, -10.00F, 1.0F, 10.0F, 2.0F)
                .texOffs(92, 10).addBox(10.00F, 26.00F, -0.50F, 1.0F, 6.0F, 1.0F)
                .texOffs(92, 10).addBox(-11.00F, 26.00F, -0.50F, 1.0F, 6.0F, 1.0F)
                .texOffs(40, 12).addBox(-0.50F, 26.00F, 10.00F, 1.0F, 6.0F, 1.0F)
                .texOffs(40, 12).addBox(-0.50F, 26.00F, -11.00F, 1.0F, 6.0F, 1.0F),
                PartPose.offsetAndRotation(0.00F, 0.00F, 0.00F, 0.00F, 0.00F, 0.00F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static LayerDefinition strategic() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_missile = root.addOrReplaceChild("missile", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6.00F, -24.00F, -6.00F, 12.0F, 60.0F, 12.0F)
                .texOffs(48, 0).addBox(-5.50F, -28.00F, -5.50F, 11.0F, 4.0F, 11.0F)
                .texOffs(92, 0).addBox(-4.50F, -32.00F, -4.50F, 9.0F, 4.0F, 9.0F)
                .texOffs(92, 13).addBox(-3.50F, -35.00F, -3.50F, 7.0F, 3.0F, 7.0F)
                .texOffs(48, 15).addBox(-2.50F, -38.00F, -2.50F, 5.0F, 3.0F, 5.0F)
                .texOffs(68, 15).addBox(-1.50F, -39.00F, -1.50F, 3.0F, 1.0F, 3.0F)
                .texOffs(120, 13).addBox(-0.50F, -40.00F, -0.50F, 1.0F, 1.0F, 1.0F)
                .texOffs(48, 23).addBox(-6.00F, 34.00F, -6.00F, 12.0F, 6.0F, 12.0F, new CubeDeformation(0.50F))
                .texOffs(96, 23).addBox(-4.00F, 38.00F, -4.00F, 8.0F, 2.0F, 8.0F)
                .texOffs(96, 33).addBox(6.00F, 30.00F, -0.50F, 4.0F, 10.0F, 1.0F)
                .texOffs(96, 33).addBox(-10.00F, 30.00F, -0.50F, 4.0F, 10.0F, 1.0F)
                .texOffs(106, 33).addBox(-0.50F, 30.00F, 6.00F, 1.0F, 10.0F, 4.0F)
                .texOffs(106, 33).addBox(-0.50F, 30.00F, -10.00F, 1.0F, 10.0F, 4.0F),
                PartPose.offsetAndRotation(0.00F, 0.00F, 0.00F, 0.00F, 0.00F, 0.00F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition mortarShell() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_shell = root.addOrReplaceChild("shell", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.50F, -3.00F, -1.50F, 3.0F, 5.0F, 3.0F)
                .texOffs(12, 0).addBox(-1.00F, -5.00F, -1.00F, 2.0F, 2.0F, 2.0F)
                .texOffs(20, 0).addBox(-0.50F, -6.00F, -0.50F, 1.0F, 1.0F, 1.0F)
                .texOffs(24, 0).addBox(-0.50F, 2.00F, -0.50F, 1.0F, 4.0F, 1.0F)
                .texOffs(28, 0).addBox(0.50F, 4.00F, -0.50F, 1.0F, 2.0F, 1.0F)
                .texOffs(28, 0).addBox(-1.50F, 4.00F, -0.50F, 1.0F, 2.0F, 1.0F)
                .texOffs(32, 0).addBox(-0.50F, 4.00F, 0.50F, 1.0F, 2.0F, 1.0F)
                .texOffs(32, 0).addBox(-0.50F, 4.00F, -1.50F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.00F, 0.00F, 0.00F, 0.00F, 0.00F, 0.00F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition shell105() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_shell = root.addOrReplaceChild("shell", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.00F, -3.00F, -2.00F, 4.0F, 8.0F, 4.0F)
                .texOffs(16, 0).addBox(-1.50F, -5.00F, -1.50F, 3.0F, 2.0F, 3.0F)
                .texOffs(28, 0).addBox(-1.00F, -6.00F, -1.00F, 2.0F, 1.0F, 2.0F)
                .texOffs(36, 0).addBox(-0.50F, -8.00F, -0.50F, 1.0F, 2.0F, 1.0F)
                .texOffs(40, 0).addBox(-1.50F, 5.00F, -1.50F, 3.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(0.00F, 0.00F, 0.00F, 0.00F, 0.00F, 0.00F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition shell155() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_shell = root.addOrReplaceChild("shell", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.50F, -4.00F, -2.50F, 5.0F, 11.0F, 5.0F)
                .texOffs(20, 0).addBox(-2.00F, -6.00F, -2.00F, 4.0F, 2.0F, 4.0F)
                .texOffs(36, 0).addBox(-1.50F, -8.00F, -1.50F, 3.0F, 2.0F, 3.0F)
                .texOffs(48, 0).addBox(-1.00F, -9.00F, -1.00F, 2.0F, 1.0F, 2.0F)
                .texOffs(56, 0).addBox(-0.50F, -10.00F, -0.50F, 1.0F, 1.0F, 1.0F)
                .texOffs(48, 3).addBox(-2.00F, 7.00F, -2.00F, 4.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(0.00F, 0.00F, 0.00F, 0.00F, 0.00F, 0.00F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition m777105() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_base = root.addOrReplaceChild("base", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-7.00F, 8.00F, -7.00F, 14.0F, 6.0F, 14.0F)
                .texOffs(56, 0).addBox(-6.00F, 14.00F, -5.00F, 12.0F, 3.0F, 10.0F)
                .texOffs(100, 0).addBox(9.00F, 0.00F, -5.00F, 3.0F, 10.0F, 10.0F)
                .texOffs(100, 0).addBox(-12.00F, 0.00F, -5.00F, 3.0F, 10.0F, 10.0F)
                .texOffs(126, 0).addBox(8.00F, 4.00F, -1.50F, 1.0F, 3.0F, 3.0F)
                .texOffs(126, 0).addBox(-9.00F, 4.00F, -1.50F, 1.0F, 3.0F, 3.0F)
                .texOffs(134, 0).addBox(-9.00F, 5.00F, -1.00F, 18.0F, 2.0F, 2.0F)
                .texOffs(174, 0).addBox(-5.50F, 17.00F, -3.00F, 1.0F, 9.0F, 6.0F)
                .texOffs(174, 0).addBox(4.50F, 17.00F, -3.00F, 1.0F, 9.0F, 6.0F),
                PartPose.offsetAndRotation(0.00F, 0.00F, 0.00F, 0.00F, 0.00F, 0.00F));
        PartDefinition p_trail_l = p_base.addOrReplaceChild("trail_l", CubeListBuilder.create()
                .texOffs(126, 15).addBox(-1.50F, -1.50F, -46.00F, 3.0F, 3.0F, 46.0F)
                .texOffs(188, 0).addBox(-3.50F, -5.00F, -48.00F, 7.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(-5.00F, 7.00F, -5.00F, 0.12F, 0.38F, 0.00F));
        PartDefinition p_trail_r = p_base.addOrReplaceChild("trail_r", CubeListBuilder.create()
                .texOffs(0, 20).addBox(-1.50F, -1.50F, -46.00F, 3.0F, 3.0F, 46.0F)
                .texOffs(206, 0).addBox(-3.50F, -5.00F, -48.00F, 7.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(5.00F, 7.00F, -5.00F, 0.12F, -0.38F, 0.00F));
        PartDefinition p_outrigger_l = p_base.addOrReplaceChild("outrigger_l", CubeListBuilder.create()
                .texOffs(224, 0).addBox(-1.00F, -1.00F, 0.00F, 2.0F, 2.0F, 12.0F)
                .texOffs(134, 4).addBox(-2.50F, -1.50F, 11.00F, 5.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(-6.00F, 8.00F, 6.00F, 0.35F, -0.55F, 0.00F));
        PartDefinition p_outrigger_r = p_base.addOrReplaceChild("outrigger_r", CubeListBuilder.create()
                .texOffs(224, 14).addBox(-1.00F, -1.00F, 0.00F, 2.0F, 2.0F, 12.0F)
                .texOffs(154, 4).addBox(-2.50F, -1.50F, 11.00F, 5.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(6.00F, 8.00F, 6.00F, 0.35F, 0.55F, 0.00F));
        PartDefinition p_cradle = root.addOrReplaceChild("cradle", CubeListBuilder.create()
                .texOffs(98, 64).addBox(-4.00F, -4.00F, -12.00F, 8.0F, 7.0F, 28.0F)
                .texOffs(170, 64).addBox(-2.50F, 3.00F, -10.00F, 2.0F, 2.0F, 26.0F)
                .texOffs(170, 64).addBox(0.50F, 3.00F, -10.00F, 2.0F, 2.0F, 26.0F)
                .texOffs(0, 69).addBox(-5.50F, -3.00F, -8.00F, 1.0F, 3.0F, 20.0F)
                .texOffs(0, 69).addBox(4.50F, -3.00F, -8.00F, 1.0F, 3.0F, 20.0F),
                PartPose.offsetAndRotation(0.00F, 24.00F, -2.00F, 0.00F, 0.00F, 0.00F));
        PartDefinition p_barrel = p_cradle.addOrReplaceChild("barrel", CubeListBuilder.create()
                .texOffs(0, 99).addBox(-2.00F, -2.00F, -12.00F, 4.0F, 4.0F, 78.0F)
                .texOffs(224, 28).addBox(-3.50F, -3.50F, -20.00F, 7.0F, 7.0F, 8.0F)
                .texOffs(98, 20).addBox(-2.50F, -2.50F, 30.80F, 5.0F, 5.0F, 5.0F)
                .texOffs(224, 43).addBox(-4.00F, -3.00F, 66.00F, 8.0F, 6.0F, 7.0F),
                PartPose.offsetAndRotation(0.00F, 0.00F, 0.00F, 0.00F, 0.00F, 0.00F));
        return LayerDefinition.create(mesh, 256, 256);
    }

    public static LayerDefinition m777155() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_base = root.addOrReplaceChild("base", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-7.00F, 8.00F, -7.00F, 14.0F, 6.0F, 14.0F)
                .texOffs(56, 0).addBox(-6.00F, 14.00F, -5.00F, 12.0F, 3.0F, 10.0F)
                .texOffs(100, 0).addBox(9.00F, 0.00F, -5.00F, 3.0F, 10.0F, 10.0F)
                .texOffs(100, 0).addBox(-12.00F, 0.00F, -5.00F, 3.0F, 10.0F, 10.0F)
                .texOffs(126, 0).addBox(8.00F, 4.00F, -1.50F, 1.0F, 3.0F, 3.0F)
                .texOffs(126, 0).addBox(-9.00F, 4.00F, -1.50F, 1.0F, 3.0F, 3.0F)
                .texOffs(134, 0).addBox(-9.00F, 5.00F, -1.00F, 18.0F, 2.0F, 2.0F)
                .texOffs(174, 0).addBox(-5.50F, 17.00F, -3.00F, 1.0F, 9.0F, 6.0F)
                .texOffs(174, 0).addBox(4.50F, 17.00F, -3.00F, 1.0F, 9.0F, 6.0F),
                PartPose.offsetAndRotation(0.00F, 0.00F, 0.00F, 0.00F, 0.00F, 0.00F));
        PartDefinition p_trail_l = p_base.addOrReplaceChild("trail_l", CubeListBuilder.create()
                .texOffs(126, 15).addBox(-1.50F, -1.50F, -46.00F, 3.0F, 3.0F, 46.0F)
                .texOffs(188, 0).addBox(-3.50F, -5.00F, -48.00F, 7.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(-5.00F, 7.00F, -5.00F, 0.12F, 0.38F, 0.00F));
        PartDefinition p_trail_r = p_base.addOrReplaceChild("trail_r", CubeListBuilder.create()
                .texOffs(0, 20).addBox(-1.50F, -1.50F, -46.00F, 3.0F, 3.0F, 46.0F)
                .texOffs(206, 0).addBox(-3.50F, -5.00F, -48.00F, 7.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(5.00F, 7.00F, -5.00F, 0.12F, -0.38F, 0.00F));
        PartDefinition p_outrigger_l = p_base.addOrReplaceChild("outrigger_l", CubeListBuilder.create()
                .texOffs(224, 0).addBox(-1.00F, -1.00F, 0.00F, 2.0F, 2.0F, 12.0F)
                .texOffs(134, 4).addBox(-2.50F, -1.50F, 11.00F, 5.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(-6.00F, 8.00F, 6.00F, 0.35F, -0.55F, 0.00F));
        PartDefinition p_outrigger_r = p_base.addOrReplaceChild("outrigger_r", CubeListBuilder.create()
                .texOffs(224, 14).addBox(-1.00F, -1.00F, 0.00F, 2.0F, 2.0F, 12.0F)
                .texOffs(154, 4).addBox(-2.50F, -1.50F, 11.00F, 5.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(6.00F, 8.00F, 6.00F, 0.35F, 0.55F, 0.00F));
        PartDefinition p_cradle = root.addOrReplaceChild("cradle", CubeListBuilder.create()
                .texOffs(98, 64).addBox(-4.00F, -4.00F, -12.00F, 8.0F, 7.0F, 28.0F)
                .texOffs(170, 64).addBox(-2.50F, 3.00F, -10.00F, 2.0F, 2.0F, 26.0F)
                .texOffs(170, 64).addBox(0.50F, 3.00F, -10.00F, 2.0F, 2.0F, 26.0F)
                .texOffs(0, 69).addBox(-5.50F, -3.00F, -8.00F, 1.0F, 3.0F, 20.0F)
                .texOffs(0, 69).addBox(4.50F, -3.00F, -8.00F, 1.0F, 3.0F, 20.0F),
                PartPose.offsetAndRotation(0.00F, 24.00F, -2.00F, 0.00F, 0.00F, 0.00F));
        PartDefinition p_barrel = p_cradle.addOrReplaceChild("barrel", CubeListBuilder.create()
                .texOffs(0, 99).addBox(-2.50F, -2.50F, -12.00F, 5.0F, 5.0F, 106.0F)
                .texOffs(224, 28).addBox(-3.50F, -3.50F, -20.00F, 7.0F, 7.0F, 8.0F)
                .texOffs(98, 20).addBox(-3.00F, -3.00F, 43.40F, 6.0F, 6.0F, 5.0F)
                .texOffs(224, 43).addBox(-4.50F, -3.50F, 94.00F, 9.0F, 7.0F, 7.0F),
                PartPose.offsetAndRotation(0.00F, 0.00F, 0.00F, 0.00F, 0.00F, 0.00F));
        return LayerDefinition.create(mesh, 256, 256);
    }

    public static LayerDefinition mortar() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_base = root.addOrReplaceChild("base", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-7.00F, 0.00F, -7.00F, 14.0F, 1.0F, 14.0F)
                .texOffs(0, 15).addBox(-3.00F, 1.00F, -3.00F, 6.0F, 2.0F, 6.0F),
                PartPose.offsetAndRotation(0.00F, 0.00F, 0.00F, 0.00F, 0.00F, 0.00F));
        PartDefinition p_bipod_l = p_base.addOrReplaceChild("bipod_l", CubeListBuilder.create()
                .texOffs(56, 0).addBox(-0.50F, -14.00F, -0.50F, 1.0F, 14.0F, 1.0F),
                PartPose.offsetAndRotation(-1.00F, 14.00F, 5.00F, 0.00F, 0.00F, 0.30F));
        PartDefinition p_bipod_r = p_base.addOrReplaceChild("bipod_r", CubeListBuilder.create()
                .texOffs(60, 0).addBox(-0.50F, -14.00F, -0.50F, 1.0F, 14.0F, 1.0F),
                PartPose.offsetAndRotation(1.00F, 14.00F, 5.00F, 0.00F, 0.00F, -0.30F));
        PartDefinition p_cradle = root.addOrReplaceChild("cradle", CubeListBuilder.create()
                .texOffs(24, 15).addBox(-1.00F, -1.00F, -1.00F, 2.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.00F, 3.00F, 0.00F, 0.00F, 0.00F, 0.00F));
        PartDefinition p_barrel = p_cradle.addOrReplaceChild("barrel", CubeListBuilder.create()
                .texOffs(0, 23).addBox(-1.50F, -1.50F, 0.00F, 3.0F, 3.0F, 26.0F)
                .texOffs(32, 15).addBox(-2.00F, -2.00F, 24.00F, 4.0F, 4.0F, 2.0F)
                .texOffs(44, 15).addBox(-2.50F, -2.50F, 13.00F, 5.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(0.00F, 0.00F, 0.00F, 0.00F, 0.00F, 0.00F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private ModModels() {
    }
}
