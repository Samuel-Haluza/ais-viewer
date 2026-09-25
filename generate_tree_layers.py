from __future__ import annotations

import os
from pathlib import Path

from PIL import Image, ImageDraw

SIZE = 1024
CX = SIZE // 2
CY = SIZE // 2

TREE_DIR = Path(__file__).resolve().parents[1] / "src/main/resources/sk/ukf/aisviewer/images/tree"


def new_layer():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def alpha_color(rgb, alpha=255):
    return (*rgb, alpha)


def add_base_tree(img):
    draw = ImageDraw.Draw(img)

    # Main trunk
    trunk_x = CX + 18
    trunk_top = 210
    trunk_bottom = 880
    trunk_w = 130
    trunk_h = 620
    draw.ellipse((trunk_x - 70, trunk_top - 30, trunk_x + 70, trunk_top + 70), fill=alpha_color((112, 76, 48,)))
    draw.rounded_rectangle((trunk_x - trunk_w // 2, trunk_top, trunk_x + trunk_w // 2, trunk_bottom), radius=55, fill=alpha_color((120, 82, 53)))

    # V-shaped roots
    root_color = (92, 60, 40)
    roots = [
        ((CX - 120, 900), (CX - 190, 980), (CX - 40, 980)),
        ((CX + 38, 900), (CX + 150, 980), (CX - 20, 980)),
        ((CX - 10, 900), (CX - 62, 980), (CX + 90, 980)),
    ]
    for a, b, c in roots:
        draw.polygon([a, b, c], fill=alpha_color(root_color))

    # Lower stump texture and bark stripes
    for offset in range(-35, 36, 18):
        draw.rounded_rectangle((trunk_x + offset, trunk_top + 20, trunk_x + offset + 12, trunk_bottom), radius=10, fill=alpha_color((136, 94, 65), 180))

    return img


def add_branches(img):
    draw = ImageDraw.Draw(img)
    base_x = CX + 18
    trunk_top = 210
    trunk_bottom = 880
    branch_color = (108, 74, 48)

    # branch start points near top of trunk
    branch_starts = [
        (base_x - 10, 260),
        (base_x - 22, 310),
        (base_x + 28, 340),
        (base_x - 6, 390),
        (base_x + 42, 430),
        (base_x - 48, 500),
    ]

    for x, y in branch_starts:
        length = 210
        angle = -0.85 if x < base_x else 0.8
        x2 = x + int(length * __import__("math").cos(angle))
        y2 = y + int(length * __import__("math").sin(angle))
        draw.line((x, y, x2, y2), fill=alpha_color(branch_color), width=24)
        draw.line((x, y, x + 80, y - 20), fill=alpha_color(branch_color), width=18)
        draw.line((x, y, x - 90, y + 30), fill=alpha_color(branch_color), width=16)

    # secondary branch lines
    for x, y, dx, dy, w in [
        (base_x + 35, 360, 90, -20, 15),
        (base_x - 15, 425, -110, 30, 13),
        (base_x + 28, 500, 125, -30, 14),
        (base_x - 55, 570, -95, 25, 12),
        (base_x + 12, 650, 140, -20, 12),
    ]:
        draw.line((x, y, x + dx, y + dy), fill=alpha_color(branch_color), width=w)

    return img


def add_leaves(img):
    draw = ImageDraw.Draw(img)
    leaf_green = (82, 144, 90)
    leaf_dark = (54, 104, 65)

    leaves = [
        (CX - 20, 200, 220, 120),
        (CX - 180, 240, 230, 120),
        (CX + 150, 240, 230, 120),
        (CX - 100, 300, 260, 130),
        (CX + 100, 300, 260, 130),
        (CX - 200, 360, 220, 140),
        (CX + 180, 360, 220, 140),
        (CX - 40, 360, 260, 150),
        (CX + 10, 420, 300, 160),
        (CX - 170, 500, 240, 140),
        (CX + 170, 500, 240, 140),
        (CX - 45, 560, 330, 180),
        (CX + 30, 620, 360, 180),
        (CX - 220, 600, 220, 120),
        (CX + 210, 610, 220, 120),
    ]

    for x, y, w, h in leaves:
        ellipse = (x - w // 2, y - h // 2, x + w // 2, y + h // 2)
        draw.ellipse(ellipse, fill=alpha_color(leaf_green), outline=alpha_color(leaf_dark), width=2)
        draw.ellipse((x - w // 3, y - h // 3, x + w // 3, y + h // 3), fill=alpha_color((111, 176, 109), 170))

    # Clustered canopy body for natural fullness
    for x, y, w, h in [
        (CX, 300, 360, 220),
        (CX - 110, 430, 380, 220),
        (CX + 120, 420, 340, 210),
        (CX, 530, 420, 220),
        (CX, 680, 300, 140),
    ]:
        draw.ellipse((x - w // 2, y - h // 2, x + w // 2, y + h // 2), fill=alpha_color((92, 160, 96), 200))

    return img


def add_flowers(img):
    draw = ImageDraw.Draw(img)
    flower_petals = (226, 150, 90)
    flower_center = (255, 87, 72)

    flower_positions = [
        (CX - 145, 260), (CX - 230, 345), (CX - 120, 430), (CX - 20, 260), (CX + 130, 275),
        (CX + 220, 340), (CX + 150, 430), (CX + 40, 520), (CX - 140, 515), (CX - 255, 620),
        (CX + 10, 610), (CX + 230, 560), (CX + 120, 680), (CX - 120, 700), (CX - 40, 760),
        (CX + 200, 720), (CX - 220, 760), (CX + 210, 440), (CX - 280, 470), (CX + 48, 350)
    ]

    for x, y in flower_positions:
        draw.ellipse((x - 12, y - 12, x + 12, y + 12), fill=alpha_color(flower_petals))
        draw.ellipse((x - 5, y - 5, x + 5, y + 5), fill=alpha_color(flower_center))

    return img


def add_effects(img):
    draw = ImageDraw.Draw(img)

    # Soft warm glow around canopy edges
    for i, radius in enumerate((220, 180, 140, 100), start=1):
        alpha = 18 if i == 1 else 12
        draw.ellipse((CX - radius, 180 - radius // 2, CX + radius, 760 + radius // 2), outline=alpha_color((255, 226, 140), alpha), width=4)

    # Fairytale sparkles
    sparkle_positions = [
        (CX - 90, 270), (CX + 140, 320), (CX - 210, 390), (CX + 230, 520),
        (CX - 140, 620), (CX + 170, 680), (CX + 40, 780), (CX - 240, 720)
    ]
    for x, y in sparkle_positions:
        draw.line((x, y - 10, x, y + 10), fill=alpha_color((255, 240, 180), 150), width=2)
        draw.line((x - 10, y, x + 10, y), fill=alpha_color((255, 240, 180), 150), width=2)

    # gentle glow at tree center
    draw.ellipse((CX - 110, 260, CX + 110, 720), outline=alpha_color((255, 250, 220), 36), width=18)

    return img


def generate_all_layers():
    TREE_DIR.mkdir(parents=True, exist_ok=True)

    layers = {
        "tree-base.png": add_base_tree(new_layer()),
        "tree-branches.png": add_branches(new_layer()),
        "tree-leaves.png": add_leaves(new_layer()),
        "tree-flowers.png": add_flowers(new_layer()),
        "tree-effects.png": add_effects(new_layer()),
    }

    for filename, image in layers.items():
        image.save(TREE_DIR / filename, format="PNG")

    print(f"Generated {len(layers)} layers into {TREE_DIR}")


if __name__ == "__main__":
    generate_all_layers()
