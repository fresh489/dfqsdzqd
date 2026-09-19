# MissileMod 1.3.0 — Arsenal militaire (Forge 1.20.1)

## Compilation
Envoie tous les fichiers à la racine du dépôt GitHub `missile-mod` (en remplaçant les anciens),
puis GitHub Actions compile tout seul : le `.jar` se récupère dans l'onglet **Actions → build → Artifacts**.

## Contenu
| Étape | Élément | Utilisation |
|---|---|---|
| 1 | Procédure d'impact commune | `/missileimpact <profil>` pour tester chaque profil |
| 2 | Missiles léger (Grad), moyen (Tomahawk), lourd (Scud), stratégique | Poser au sol, clic droit = ciblage. 1 missile sur 50 est raté |
| 3 | Mine antipersonnel / antichar | Désamorçage : clic droit avec une cisaille. Cassée à la main = explosion |
| 3 | Grenades fumigènes (blanc, rouge, vert, violet) | Clic droit pour lancer |
| 4 | Missiles à fragmentation, obus 105/155 explosifs et à fragmentation | Les obus se chargent dans l'artillerie |
| 5 | Alerte anti-missile | 200 / 80 / 30 blocs, signal redstone |
| 6 | Mortier 81 mm | Clic droit : coordonnées + choix de l'obus |
| 7 | Obusiers M777 105 mm et M777ER 155 mm | Idem, animations de pointage, tir, recul |
| 8 | Sous-munitions (Tomahawk et obus 155) | 8 à 15 bombes, 1 ou 2 restent comme mines |
| + | Radio de frappe aérienne | Viser un point, clic droit (recharge 30 s) |

## Configuration (`config/missilemod-common.toml`)
Nouvelle section `[munitions]` : `dudChance` (ratés), `alarmRadius`, `artilleryRangeMultiplier`.
`destroyBlocks = false` désactive toute destruction de blocs (serveurs).

## Modèles et textures
Tous les modèles 3D et textures sont générés par `tools/generate_assets.py` (Python + Pillow).
Pour retoucher un modèle : modifier le script puis `python tools/generate_assets.py`.
Les sons synthétisés sont dans `src/main/resources/assets/missilemod/sounds/`.
