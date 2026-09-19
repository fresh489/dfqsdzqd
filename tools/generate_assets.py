"""
Generateur des modeles 3D et des textures du mod (Pillow requis : pip install pillow).
Usage, depuis la racine du projet :  python tools/generate_assets.py

Produit :
- src/main/java/fr/missilemod/client/ModModels.java   (toutes les LayerDefinition)
- src/main/resources/assets/missilemod/textures/entity/*.png  (textures des modeles, une par variante)
- src/main/resources/assets/missilemod/textures/item/*.png    (icones)
- src/main/resources/assets/missilemod/textures/block/*.png   (textures des blocs)

Unites en pixels (1/16 de bloc). Missiles et obus : nez vers -Y, centres sur 0 (le renderer retourne le modele).
Artillerie : Y vers le haut, tube vers +Z, origine au centre du bas du bloc.
"""
import math
import os
import random

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources", "assets", "missilemod", "textures")
JAVA = os.path.join(HERE, "..", "src", "main", "java", "fr", "missilemod", "client", "ModModels.java")


# ============================================================== structures

class Box:
    def __init__(self, x, y, z, w, h, d, paint, mirrors=(), deform=0.0):
        self.x, self.y, self.z, self.w, self.h, self.d = x, y, z, int(w), int(h), int(d)
        self.paint, self.mirrors, self.deform = paint, list(mirrors), deform


class Part:
    def __init__(self, name, parent=None, pivot=(0, 0, 0), rot=(0, 0, 0), local=False, boxes=()):
        self.name, self.parent, self.pivot, self.rot, self.local = name, parent, pivot, rot, local
        self.boxes = list(boxes)


def cyl(y0, y1, dia, paint, **kw):
    """Boite de section carree centree sur l'axe Y (corps de missile)."""
    return Box(-dia / 2.0, y0, -dia / 2.0, dia, y1 - y0, dia, paint, **kw)


def fins4(y0, y1, root, span, paint, thick=1):
    """Quatre ailettes en croix autour de l'axe Y, collees a un corps de rayon root."""
    t = thick / 2.0
    return [
        Box(root, y0, -t, span, y1 - y0, thick, paint, mirrors=[(-root - span, -t)]),
        Box(-t, y0, root, thick, y1 - y0, span, paint, mirrors=[(-t, -root - span)]),
    ]


def fins2(y0, y1, root, span, paint, thick=1):
    t = thick / 2.0
    return [Box(root, y0, -t, span, y1 - y0, thick, paint, mirrors=[(-root - span, -t)])]


def stepped_fins4(steps, paint):
    """Ailettes en escalier (delta) : steps = [(y0, y1, root, span), ...]"""
    boxes = []
    for (y0, y1, root, span) in steps:
        boxes += fins4(y0, y1, root, span, paint)
    return boxes


# ============================================================== modeles

MODELS = {}

# --- AIM-120 (3 blocs) : identique a la v1.1 ---
MODELS["aim120"] = dict(child="missile", parts=[Part("missile", boxes=[
    cyl(-13, 20, 6, ("body", [(-9, -7, "yellow"), (6, 8, "brown"), (-1, 0, "seam"), (12, 13, "seam"), (13, 20, "tail")])),
    cyl(-16, -13, 5, ("solid", "radome1")),
    cyl(-19, -16, 4, ("solid", "radome2")),
    cyl(-21, -19, 3, ("solid", "radome3")),
    cyl(-23, -21, 2, ("solid", "radome4")),
    cyl(-24, -23, 1, ("solid", "tipc")),
    cyl(20, 22, 5, ("solid", "boattail")),
    cyl(22, 24, 4, ("nozzle",)),
    Box(-3, -13, -3, 6, 1, 6, ("solid", "seam"), mirrors=[("y", 9), ("y", 19)], deform=0.2),
] + stepped_fins4([(-6, 3, 3, 2), (-3, 3, 5, 2), (0, 3, 7, 1)], ("fin", "fin"))
  + stepped_fins4([(12, 20, 3, 3), (14, 20, 6, 2), (16, 20, 8, 1)], ("fin", "fin")))],
    palettes={"missile_aim120": dict(main=(232, 234, 236), yellow=(214, 176, 48), brown=(122, 86, 52),
                                     seam=(170, 175, 180), tail=(204, 208, 212), radome1=(214, 214, 204),
                                     radome2=(206, 207, 198), radome3=(194, 196, 190), radome4=(176, 179, 180),
                                     tipc=(130, 134, 140), boattail=(150, 154, 160), fin=(196, 200, 205))})

# --- Leger : roquette type Grad 122 mm (2 blocs) ---
MODELS["grad"] = dict(child="missile", parts=[Part("missile", boxes=[
    cyl(-10, 13, 3, ("body", [(-10, -4, "warhead"), (-4, -3, "band"), (4, 5, "seam"), (11, 13, "tail")])),
    cyl(-13, -10, 2, ("solid", "warhead")),
    cyl(-16, -13, 1, ("solid", "fuze")),
    cyl(13, 15, 4, ("solid", "ring"), ),
    cyl(15, 16, 2, ("nozzle",)),
] + fins4(9, 15, 1.5, 2, ("fin", "fin")))],
    palettes={
        "missile_grad": dict(main=(92, 104, 70), warhead=(78, 88, 60), band=(214, 176, 48), seam=(66, 74, 50),
                             tail=(84, 94, 64), fuze=(150, 130, 70), ring=(70, 78, 54), fin=(80, 90, 62)),
        "missile_grad_frag": dict(main=(92, 104, 70), warhead=(60, 62, 64), band=(190, 40, 36), seam=(66, 74, 50),
                                  tail=(84, 94, 64), fuze=(150, 130, 70), ring=(70, 78, 54), fin=(80, 90, 62)),
    })

# --- Moyen : missile de croisiere type Tomahawk (3 blocs) ---
MODELS["tomahawk"] = dict(child="missile", parts=[Part("missile", boxes=[
    cyl(-20, 20, 5, ("body", [(-20, -14, "nose"), (-9, -8, "band"), (-2, -1, "seam"), (10, 11, "seam"), (14, 15, "black")])),
    cyl(-22, -20, 4, ("solid", "nose")),
    cyl(-23, -22, 3, ("solid", "nose")),
    cyl(-24, -23, 1, ("solid", "nose2")),
    cyl(20, 22, 4, ("solid", "tail")),
    cyl(22, 24, 3, ("nozzle",)),
    Box(-1.5, 6, 2.5, 3, 7, 2, ("solid", "intake")),                       # prise d'air ventrale
] + fins2(-3, 2, 2, 9, ("fin", "wing")) + fins4(17, 22, 2.5, 3, ("fin", "fin")))],
    palettes={
        "missile_tomahawk": dict(main=(196, 200, 204), nose=(96, 100, 106), nose2=(60, 62, 66), band=(214, 176, 48),
                                 seam=(160, 165, 170), black=(40, 42, 44), tail=(150, 154, 160),
                                 intake=(70, 72, 76), wing=(176, 180, 186), fin=(170, 174, 180)),
        "missile_tomahawk_cluster": dict(main=(196, 200, 204), nose=(96, 100, 106), nose2=(60, 62, 66),
                                         band=(230, 120, 30), seam=(160, 165, 170), black=(40, 42, 44),
                                         tail=(150, 154, 160), intake=(70, 72, 76), wing=(176, 180, 186),
                                         fin=(170, 174, 180)),
    })

# --- Lourd : missile balistique type Scud (4 blocs) ---
MODELS["scud"] = dict(child="missile", parts=[Part("missile", boxes=[
    cyl(-18, 30, 10, ("body", [(-18, -16, "seam"), (-6, -4, "band"), (8, 9, "seam"), (22, 23, "seam"), (26, 30, "tail")])),
    cyl(-22, -18, 8, ("solid", "nose1")),
    cyl(-26, -22, 6, ("solid", "nose2")),
    cyl(-29, -26, 4, ("solid", "nose3")),
    cyl(-31, -29, 2, ("solid", "nose3")),
    cyl(-32, -31, 1, ("solid", "tipc")),
    cyl(30, 32, 6, ("nozzle",)),
] + stepped_fins4([(18, 32, 5, 3), (22, 32, 8, 2), (26, 32, 10, 1)], ("fin", "fin")))],
    palettes={
        "missile_scud": dict(main=(96, 108, 72), seam=(70, 80, 52), band=(226, 226, 220), tail=(84, 94, 64),
                             nose1=(88, 98, 66), nose2=(80, 90, 60), nose3=(72, 80, 54), tipc=(50, 52, 50),
                             fin=(86, 96, 64)),
        "missile_scud_frag": dict(main=(96, 108, 72), seam=(70, 80, 52), band=(190, 40, 36), tail=(84, 94, 64),
                                  nose1=(70, 72, 74), nose2=(64, 66, 68), nose3=(58, 60, 62), tipc=(40, 40, 42),
                                  fin=(86, 96, 64)),
    })

# --- Strategique : gros missile blanc/kaki a bandes (5 blocs) ---
MODELS["strategic"] = dict(child="missile", parts=[Part("missile", boxes=[
    cyl(-24, 36, 12, ("body", [(-24, -22, "khaki"), (-10, -7, "black"), (-7, -4, "khaki"), (8, 9, "seam"),
                               (20, 23, "black"), (23, 26, "khaki")])),
    cyl(-28, -24, 11, ("solid", "main")),
    cyl(-32, -28, 9, ("solid", "main")),
    cyl(-35, -32, 7, ("solid", "nose")),
    cyl(-38, -35, 5, ("solid", "nose")),
    cyl(-39, -38, 3, ("solid", "khaki")),
    cyl(-40, -39, 1, ("solid", "black")),
    Box(-6, 34, -6, 12, 6, 12, ("solid", "khaki"), deform=0.5),            # jupe arriere
    cyl(38, 40, 8, ("nozzle",)),
] + fins4(30, 40, 6, 4, ("fin", "fin")))],
    palettes={"missile_strategic": dict(main=(234, 234, 228), khaki=(170, 150, 104), black=(44, 44, 42),
                                        seam=(200, 200, 194), nose=(222, 222, 214), fin=(160, 142, 100))})

# --- Obus (nez vers -Y) ---
MODELS["mortar_shell"] = dict(child="shell", parts=[Part("shell", boxes=[
    cyl(-3, 2, 3, ("body", [(-3, -2, "band")])),
    cyl(-5, -3, 2, ("solid", "main")),
    cyl(-6, -5, 1, ("solid", "fuze")),
    cyl(2, 6, 1, ("solid", "tail")),
] + fins4(4, 6, 0.5, 1, ("fin", "tail")))],
    palettes={"shell_mortar": dict(main=(92, 104, 70), band=(214, 176, 48), fuze=(170, 150, 80), tail=(70, 72, 70))})

MODELS["shell_105"] = dict(child="shell", parts=[Part("shell", boxes=[
    cyl(-3, 5, 4, ("body", [(-3, -2, "band"), (3, 4, "copper")])),
    cyl(-5, -3, 3, ("solid", "main")),
    cyl(-6, -5, 2, ("solid", "main")),
    cyl(-8, -6, 1, ("solid", "fuze")),
    cyl(5, 6, 3, ("solid", "base")),
])],
    palettes={
        "shell_105_he": dict(main=(92, 104, 70), band=(214, 176, 48), copper=(184, 110, 60), fuze=(170, 150, 80),
                             base=(80, 88, 62)),
        "shell_105_frag": dict(main=(62, 64, 66), band=(190, 40, 36), copper=(184, 110, 60), fuze=(170, 150, 80),
                               base=(54, 56, 58)),
    })

MODELS["shell_155"] = dict(child="shell", parts=[Part("shell", boxes=[
    cyl(-4, 7, 5, ("body", [(-4, -3, "band"), (-1, 0, "band"), (5, 6, "copper")])),
    cyl(-6, -4, 4, ("solid", "main")),
    cyl(-8, -6, 3, ("solid", "main")),
    cyl(-9, -8, 2, ("solid", "fuze")),
    cyl(-10, -9, 1, ("solid", "fuze")),
    cyl(7, 8, 4, ("solid", "base")),
])],
    palettes={
        "shell_155_he": dict(main=(92, 104, 70), band=(214, 176, 48), copper=(184, 110, 60), fuze=(170, 150, 80),
                             base=(80, 88, 62)),
        "shell_155_frag": dict(main=(62, 64, 66), band=(190, 40, 36), copper=(184, 110, 60), fuze=(170, 150, 80),
                               base=(54, 56, 58)),
        "shell_155_cluster": dict(main=(92, 104, 70), band=(230, 120, 30), copper=(184, 110, 60),
                                  fuze=(170, 150, 80), base=(80, 88, 62)),
    })


# --- Obusier type M777 (Y vers le haut, tube vers +Z) ---
def m777(barrel_len, dia):
    brake_w = dia + 4
    base = Part("base", boxes=[
        Box(-7, 8, -7, 14, 6, 14, ("solid", "olive")),                                   # affut
        Box(-6, 14, -5, 12, 3, 10, ("solid", "olive2")),                                 # sellette
        Box(9, 0, -5, 3, 10, 10, ("solid", "tire"), mirrors=[(-12, -5)]),                # roues
        Box(8, 4, -1.5, 1, 3, 3, ("solid", "dark"), mirrors=[(-9, -1.5)]),               # moyeux
        Box(-9, 5, -1, 18, 2, 2, ("solid", "dark")),                                     # essieu
        Box(-5.5, 17, -3, 1, 9, 6, ("solid", "olive2"), mirrors=[(4.5, -3)]),            # flasques
    ])
    trail_l = Part("trail_l", parent="base", pivot=(-5, 7, -5), rot=(0.12, 0.38, 0), local=True, boxes=[
        Box(-1.5, -1.5, -46, 3, 3, 46, ("solid", "olive")),
        Box(-3.5, -5, -48, 7, 6, 2, ("solid", "dark")),                                  # beche
    ])
    trail_r = Part("trail_r", parent="base", pivot=(5, 7, -5), rot=(0.12, -0.38, 0), local=True, boxes=[
        Box(-1.5, -1.5, -46, 3, 3, 46, ("solid", "olive")),
        Box(-3.5, -5, -48, 7, 6, 2, ("solid", "dark")),
    ])
    out_l = Part("outrigger_l", parent="base", pivot=(-6, 8, 6), rot=(0.35, -0.55, 0), local=True, boxes=[
        Box(-1, -1, 0, 2, 2, 12, ("solid", "olive")),
        Box(-2.5, -1.5, 11, 5, 1, 5, ("solid", "dark")),
    ])
    out_r = Part("outrigger_r", parent="base", pivot=(6, 8, 6), rot=(0.35, 0.55, 0), local=True, boxes=[
        Box(-1, -1, 0, 2, 2, 12, ("solid", "olive")),
        Box(-2.5, -1.5, 11, 5, 1, 5, ("solid", "dark")),
    ])
    cradle = Part("cradle", pivot=(0, 24, -2), boxes=[
        Box(-4, 20, -14, 8, 7, 28, ("solid", "olive2")),                                 # berceau
        Box(-2.5, 27, -12, 2, 2, 26, ("solid", "dark"), mirrors=[(0.5, -12)]),           # freins de recul
        Box(-5.5, 21, -10, 1, 3, 20, ("solid", "olive"), mirrors=[(4.5, -10)]),          # equilibreurs
    ])
    barrel = Part("barrel", parent="cradle", pivot=(0, 24, -2), boxes=[
        Box(-dia / 2.0, 24 - dia / 2.0, -14, dia, dia, barrel_len + 14, ("solid", "barrel")),
        Box(-3.5, 20.5, -22, 7, 7, 8, ("solid", "dark")),                                # culasse
        Box(-dia / 2.0 - 0.5, 24 - dia / 2.0 - 0.5, barrel_len * 0.45, dia + 1, dia + 1, 5, ("solid", "barrel2")),
        Box(-brake_w / 2.0, 24 - (dia + 2) / 2.0, barrel_len, brake_w, dia + 2, 7, ("solid", "barrel2")),  # frein de bouche
    ])
    return dict(child=None, parts=[base, trail_l, trail_r, out_l, out_r, cradle, barrel])


ART_PALETTE = dict(olive=(74, 78, 52), olive2=(64, 68, 46), dark=(40, 42, 36), tire=(28, 28, 28),
                   barrel=(46, 48, 40), barrel2=(38, 40, 34))
MODELS["m777_105"] = dict(m777(64, 4), palettes={"artillery_m777_105": ART_PALETTE})
MODELS["m777_155"] = dict(m777(92, 5), palettes={"artillery_m777_155": dict(ART_PALETTE, olive=(150, 132, 92),
                                                                                olive2=(138, 120, 84))})

# --- Mortier 81 mm ---
MODELS["mortar"] = dict(child=None, parts=[
    Part("base", boxes=[
        Box(-7, 0, -7, 14, 1, 14, ("solid", "olive")),                                   # plaque de base
        Box(-3, 1, -3, 6, 2, 6, ("solid", "dark")),                                      # rotule
    ]),
    Part("bipod_l", parent="base", pivot=(-1, 14, 5), rot=(0, 0, 0.3), local=True, boxes=[
        Box(-0.5, -14, -0.5, 1, 14, 1, ("solid", "olive2"))]),
    Part("bipod_r", parent="base", pivot=(1, 14, 5), rot=(0, 0, -0.3), local=True, boxes=[
        Box(-0.5, -14, -0.5, 1, 14, 1, ("solid", "olive2"))]),
    Part("cradle", pivot=(0, 3, 0), boxes=[Box(-1, 2, -1, 2, 2, 2, ("solid", "dark"))]),
    Part("barrel", parent="cradle", pivot=(0, 3, 0), boxes=[
        Box(-1.5, 1.5, 0, 3, 3, 26, ("solid", "barrel")),
        Box(-2, 1, 24, 4, 4, 2, ("solid", "barrel2")),
        Box(-2.5, 0.5, 13, 5, 5, 2, ("solid", "dark")),                                  # collier du bipied
    ]),
], palettes={"artillery_mortar": ART_PALETTE})


# ============================================================== peinture

random.seed(120)


def jitter(color, amount=4):
    d = random.randint(-amount, amount)
    return tuple(max(0, min(255, c + d)) for c in color[:3]) + (255,)


def darker(c, f=0.75):
    return tuple(int(v * f) for v in c[:3])


def fill(img, x, y, w, h, color, noise=4):
    for i in range(x, x + w):
        for j in range(y, y + h):
            img.putpixel((i, j), jitter(color, noise))


def uv_size(b):
    return 2 * (b.d + b.w), b.d + b.h


def paint_box(img, u, v, b, pal):
    w, h, d = b.w, b.h, b.d
    kind = b.paint[0]
    if kind == "body":
        bands = b.paint[1]
        main = pal["main"]
        fill(img, u + d, v, w, d, main, 3)
        fill(img, u + d + w, v, w, d, darker(main, 0.85), 3)
        for row in range(h):
            y = b.y + row
            color = main
            for (y0, y1, key) in bands:
                if y0 <= y < y1:
                    color = pal[key]
            fill(img, u, v + d + row, 2 * (w + d), 1, color, 3)
    elif kind == "solid":
        c = pal[b.paint[1]]
        fill(img, u, v, 2 * (w + d), d + h, c, 3)
        fill(img, u + d, v, w, d, tuple(min(255, int(x * 1.06)) for x in c), 2)
    elif kind == "fin":
        c = pal[b.paint[1]]
        fill(img, u, v, 2 * (w + d), d + h, c, 3)
        edge = darker(c, 0.72)
        for i in range(2 * (w + d)):
            img.putpixel((u + i, v + d), edge + (255,))
            img.putpixel((u + i, v + d + h - 1), edge + (255,))
        fill(img, u + d, v, w, d, edge, 2)
        fill(img, u + d + w, v, w, d, edge, 2)
    elif kind == "nozzle":
        fill(img, u, v, 2 * (w + d), d + h, (46, 48, 52), 3)
        fill(img, u + d + w, v, w, d, (22, 22, 24), 2)


def pack(boxes, tex_w, tex_h):
    used = [[False] * tex_w for _ in range(tex_h)]
    places = []
    for b in boxes:
        uw, uh = uv_size(b)
        found = None
        for v in range(tex_h - uh + 1):
            for u in range(tex_w - uw + 1):
                if all(not used[v + j][u + i] for j in range(uh) for i in range(uw)):
                    found = (u, v)
                    break
            if found:
                break
        if not found:
            return None
        for j in range(uh):
            for i in range(uw):
                used[found[1] + j][found[0] + i] = True
        places.append(found)
    return places


def fmt(n):
    return "%.2fF" % n


def java_model(name, model):
    parts = model["parts"]
    all_boxes = [b for p in parts for b in p.boxes]
    for size in ((64, 64), (128, 64), (128, 128), (256, 128), (256, 256)):
        places = pack(all_boxes, *size)
        if places:
            break
    tex_w, tex_h = size
    uv = {id(b): places[i] for i, b in enumerate(all_boxes)}

    for variant, pal in model["palettes"].items():
        img = Image.new("RGBA", (tex_w, tex_h), (0, 0, 0, 0))
        random.seed(hash(variant) & 0xFFFF)
        for b in all_boxes:
            paint_box(img, *uv[id(b)], b, pal)
        path = os.path.join(RES, "entity", variant + ".png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)

    abs_pivot = {p.name: p.pivot for p in parts}
    lines = ["    public static LayerDefinition %s() {" % camel(name),
             "        MeshDefinition mesh = new MeshDefinition();",
             "        PartDefinition root = mesh.getRoot();"]
    for p in parts:
        parent = "root" if p.parent is None else "p_" + p.parent
        ppiv = (0, 0, 0) if p.parent is None else abs_pivot[p.parent]
        off = tuple(p.pivot[i] - ppiv[i] for i in range(3))
        builder = ["CubeListBuilder.create()"]
        for b in p.boxes:
            u, v = uv[id(b)]
            if p.local:
                ox, oy, oz = 0, 0, 0
            else:
                ox, oy, oz = p.pivot
            deform = ", new CubeDeformation(%s)" % fmt(b.deform) if b.deform else ""
            coords = [(b.x, b.y, b.z)]
            for m in b.mirrors:
                if m[0] == "y":
                    coords.append((b.x, m[1], b.z))
                else:
                    coords.append((m[0], b.y, m[1]))
            for (x, y, z) in coords:
                builder.append("                .texOffs(%d, %d).addBox(%s, %s, %s, %d.0F, %d.0F, %d.0F%s)"
                               % (u, v, fmt(x - ox), fmt(y - oy), fmt(z - oz), b.w, b.h, b.d, deform))
        pose = "PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s)" % (
            fmt(off[0]), fmt(off[1]), fmt(off[2]), fmt(p.rot[0]), fmt(p.rot[1]), fmt(p.rot[2]))
        lines.append("        PartDefinition p_%s = %s.addOrReplaceChild(\"%s\", %s,\n                %s);"
                     % (p.name, parent, p.name, "\n".join(builder), pose))
    lines.append("        return LayerDefinition.create(mesh, %d, %d);" % (tex_w, tex_h))
    lines.append("    }")
    return "\n".join(lines)


def camel(name):
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def write_java():
    consts, registers, methods = [], [], []
    for name, model in MODELS.items():
        const = name.upper()
        consts.append("    public static final ModelLayerLocation %s = layer(\"%s\");" % (const, name))
        registers.append("        event.registerLayerDefinition(%s, ModModels::%s);" % (const, camel(name)))
        methods.append(java_model(name, model))
    src = """package fr.missilemod.client;

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

%s

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(new ResourceLocation(MissileMod.MOD_ID, name), "main");
    }

    public static void register(EntityRenderersEvent.RegisterLayerDefinitions event) {
%s
    }

%s

    private ModModels() {
    }
}
""" % ("\n".join(consts), "\n".join(registers), "\n\n".join(methods))
    with open(JAVA, "w") as f:
        f.write(src)


# ============================================================== icones et blocs

def save(img, *path):
    p = os.path.join(RES, *path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


OUT = (40, 42, 44, 255)


def icon_missile(name, body, nose, band, fin, length=12, thick=1, flame=True):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    start = 14 - length
    for i in range(start, 14):
        x, y = i, 15 - i
        for t in range(-thick, thick + 1):
            for (ox, oy) in ((x + t, y), (x, y - t)):
                if 0 <= ox < 16 and 0 <= oy < 16:
                    img.putpixel((ox, oy), body + (255,))
    for i in range(start, 14):
        x, y = i, 15 - i
        for (ox, oy) in ((x - thick - 1, y), (x, y + thick + 1), (x + thick + 1, y), (x, y - thick - 1)):
            if 0 <= ox < 16 and 0 <= oy < 16 and img.getpixel((ox, oy))[3] == 0:
                img.putpixel((ox, oy), OUT)
    for i in range(11, 14):
        img.putpixel((i, 15 - i), nose + (255,))
    img.putpixel((14, 1), nose + (255,))
    img.putpixel((15, 0), OUT)
    bx = start + length // 2 + 1
    for t in range(-thick, thick + 1):
        if 0 <= bx + t < 16:
            img.putpixel((bx + t, 15 - bx), band + (255,))
    for p in ((start, 12), (start - 1, 12), (start + 2, 15), (start + 2, 14)):
        if 0 <= p[0] < 16 and 0 <= p[1] < 16:
            img.putpixel(p, fin + (255,))
    if flame:
        img.putpixel((max(0, start - 2), 15), (255, 180, 60, 255))
    save(img, "item", name + ".png")


def icon_shell(name, body, band, tip, scale=1.0):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    w = int(5 * scale)
    x0 = 8 - w // 2
    d.rectangle([x0, 6, x0 + w - 1, 14], fill=body + (255,), outline=OUT)
    d.polygon([(x0, 6), (x0 + w - 1, 6), (8, 1)], fill=tip + (255,), outline=OUT)
    d.line([(x0 + 1, 8), (x0 + w - 2, 8)], fill=band + (255,))
    d.line([(x0 + 1, 13), (x0 + w - 2, 13)], fill=(184, 110, 60, 255))
    save(img, "item", name + ".png")


def icon_grenade(name, color):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([5, 4, 10, 14], fill=(80, 88, 64, 255), outline=OUT)
    d.rectangle([6, 7, 9, 10], fill=color + (255,))
    d.rectangle([6, 2, 9, 3], fill=(120, 124, 130, 255), outline=OUT)
    d.line([(10, 3), (12, 1)], fill=(170, 174, 180, 255))
    save(img, "item", name + ".png")


def icon_simple(name, draw):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw(ImageDraw.Draw(img))
    save(img, "item", name + ".png")


def block_textures():
    random.seed(7)
    # Mine antipersonnel : kaki, detonateur au centre
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    for x in range(16):
        for y in range(16):
            img.putpixel((x, y), jitter((118, 112, 72), 6))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(88, 84, 54, 255))
    d.ellipse([5, 5, 10, 10], fill=(60, 62, 48, 255), outline=(40, 40, 32, 255))
    save(img, "block", "land_mine.png")
    # Mine antichar : vert olive, plus sombre, rainures
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    for x in range(16):
        for y in range(16):
            img.putpixel((x, y), jitter((78, 88, 58), 6))
    d = ImageDraw.Draw(img)
    for r in (2, 5):
        d.rectangle([r, r, 15 - r, 15 - r], outline=(56, 64, 42, 255))
    d.rectangle([6, 6, 9, 9], fill=(46, 50, 38, 255))
    save(img, "block", "anti_tank_mine.png")
    # Alerte anti-missile
    for state, lamp in (("off", (110, 30, 30)), ("on", (255, 60, 40))):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
        for x in range(16):
            for y in range(16):
                img.putpixel((x, y), jitter((92, 96, 100), 4))
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, 15, 15], outline=(60, 62, 66, 255))
        d.ellipse([3, 3, 12, 12], fill=lamp + (255,), outline=(50, 50, 54, 255))
        if state == "on":
            d.ellipse([5, 5, 7, 7], fill=(255, 200, 180, 255))
        save(img, "block", "missile_alarm_top_" + state + ".png")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    for x in range(16):
        for y in range(16):
            img.putpixel((x, y), jitter((92, 96, 100), 4))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(60, 62, 66, 255))
    for y in (4, 7, 10):
        d.line([(3, y), (12, y)], fill=(50, 52, 56, 255))
    d.rectangle([3, 12, 5, 13], fill=(214, 176, 48, 255))
    d.rectangle([10, 12, 12, 13], fill=(214, 176, 48, 255))
    save(img, "block", "missile_alarm_side.png")


def icons():
    icon_missile("missile_grad", (92, 104, 70), (150, 130, 70), (214, 176, 48), (80, 90, 62), length=12, thick=0)
    icon_missile("missile_grad_frag", (92, 104, 70), (60, 62, 64), (190, 40, 36), (80, 90, 62), length=12, thick=0)
    icon_missile("missile_tomahawk", (196, 200, 204), (96, 100, 106), (214, 176, 48), (150, 154, 160), length=12)
    icon_missile("missile_tomahawk_cluster", (196, 200, 204), (96, 100, 106), (230, 120, 30), (150, 154, 160), length=12)
    icon_missile("missile_scud", (96, 108, 72), (72, 80, 54), (226, 226, 220), (86, 96, 64), length=13)
    icon_missile("missile_scud_frag", (96, 108, 72), (58, 60, 62), (190, 40, 36), (86, 96, 64), length=13)
    icon_missile("missile_strategic", (234, 234, 228), (170, 150, 104), (44, 44, 42), (160, 142, 100), length=14)
    icon_shell("shell_mortar", (92, 104, 70), (214, 176, 48), (170, 150, 80), 0.8)
    icon_shell("shell_105_he", (92, 104, 70), (214, 176, 48), (92, 104, 70), 1.0)
    icon_shell("shell_105_frag", (62, 64, 66), (190, 40, 36), (62, 64, 66), 1.0)
    icon_shell("shell_155_he", (92, 104, 70), (214, 176, 48), (92, 104, 70), 1.3)
    icon_shell("shell_155_frag", (62, 64, 66), (190, 40, 36), (62, 64, 66), 1.3)
    icon_shell("shell_155_cluster", (92, 104, 70), (230, 120, 30), (92, 104, 70), 1.3)
    for color, rgb in (("white", (236, 236, 236)), ("red", (200, 40, 36)), ("green", (60, 170, 60)),
                       ("purple", (140, 60, 190))):
        icon_grenade("smoke_grenade_" + color, rgb)

    def bomblet(d):
        d.ellipse([4, 5, 11, 12], fill=(200, 170, 60, 255), outline=OUT)
        d.rectangle([6, 2, 9, 5], fill=(90, 92, 94, 255), outline=OUT)
    icon_simple("bomblet", bomblet)

    def aerial_bomb(d):
        d.ellipse([2, 5, 12, 10], fill=(86, 96, 64, 255), outline=OUT)
        d.polygon([(12, 7), (15, 4), (15, 11)], fill=(70, 78, 54, 255), outline=OUT)
        d.line([(5, 6), (5, 9)], fill=(214, 176, 48, 255))
    icon_simple("aerial_bomb", aerial_bomb)

    def radio(d):
        d.rectangle([4, 4, 11, 15], fill=(70, 78, 54, 255), outline=OUT)
        d.line([(10, 4), (10, 0)], fill=OUT)
        d.rectangle([5, 6, 10, 9], fill=(40, 44, 40, 255))
        d.point([(6, 11), (8, 11), (6, 13), (8, 13)], fill=(214, 176, 48, 255))
        d.point([(7, 7)], fill=(120, 220, 120, 255))
    icon_simple("strike_radio", radio)

    def casing(d):
        d.rectangle([6, 3, 9, 14], fill=(196, 150, 70, 255), outline=(110, 80, 30, 255))
        d.line([(7, 4), (7, 13)], fill=(230, 196, 110, 255))
    icon_simple("shell_casing", casing)

    def cannon(color):
        def draw(d):
            d.line([(2, 13), (14, 3)], fill=OUT, width=3)
            d.line([(2, 13), (14, 3)], fill=(46, 48, 40, 255), width=1)
            d.ellipse([3, 10, 8, 15], fill=(28, 28, 28, 255), outline=OUT)
            d.rectangle([6, 9, 11, 12], fill=color + (255,), outline=OUT)
            d.line([(8, 12), (1, 15)], fill=color + (255,), width=1)
        return draw
    icon_simple("m777_105", cannon((74, 78, 52)))
    icon_simple("m777_155", cannon((150, 132, 92)))

    def mortar(d):
        d.rectangle([2, 13, 13, 14], fill=(74, 78, 52, 255), outline=OUT)
        d.line([(7, 13), (11, 2)], fill=OUT, width=3)
        d.line([(7, 13), (11, 2)], fill=(46, 48, 40, 255), width=1)
        d.line([(10, 7), (13, 13)], fill=(64, 68, 46, 255))
        d.line([(10, 7), (7, 13)], fill=(64, 68, 46, 255))
    icon_simple("mortar", mortar)


if __name__ == "__main__":
    write_java()
    icons()
    block_textures()
    print("OK :", JAVA)
