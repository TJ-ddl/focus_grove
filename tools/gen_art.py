#!/usr/bin/env python3
"""Generate original watercolor-style tree art for the app.

Output:
  app/src/main/res/drawable-nodpi/tree_<id>_<stage>.webp  (used by the app)
  art/preview/*.png                                       (review copies)
"""
import math
import os
import random

from PIL import Image, ImageChops, ImageDraw, ImageFilter

W, H = 900, 1200
BASE_Y = int(H * 0.93)

STAGES = {
    1: {"height": 0.15, "cw": 0.24, "ch": 0.17, "blobs": 5, "branches": 0, "trunk": 0.016},
    2: {"height": 0.42, "cw": 0.42, "ch": 0.33, "blobs": 10, "branches": 2, "trunk": 0.040},
    3: {"height": 0.76, "cw": 0.68, "ch": 0.54, "blobs": 18, "branches": 4, "trunk": 0.078},
}

SPECIES = {
    "sapling": {
        "trunk": (94, 74, 56),
        "leaves": [(159, 200, 143), (127, 174, 116), (185, 216, 166), (110, 158, 110)],
        "accent": None,
        "shape": (0.90, 1.05),
        "style": "plain",
    },
    "oak": {
        "trunk": (97, 71, 51),
        "leaves": [(111, 143, 94), (90, 122, 78), (135, 166, 110), (79, 112, 72)],
        "accent": (165, 112, 70),
        "shape": (1.05, 0.95),
        "style": "acorns",
    },
    "sakura": {
        "trunk": (92, 66, 61),
        "leaves": [(242, 182, 198), (231, 154, 176), (247, 206, 216), (217, 138, 164)],
        "accent": (247, 206, 216),
        "shape": (1.18, 0.82),
        "style": "petals",
    },
    "ginkgo": {
        "trunk": (102, 79, 56),
        "leaves": [(232, 199, 90), (212, 169, 60), (241, 219, 138), (192, 148, 50)],
        "accent": (241, 219, 138),
        "shape": (1.15, 0.82),
        "style": "leaves",
    },
    "maple": {
        "trunk": (84, 61, 51),
        "leaves": [(217, 122, 84), (192, 90, 62), (232, 154, 106), (169, 74, 54)],
        "accent": (232, 154, 106),
        "shape": (1.20, 0.95),
        "style": "leaves",
    },
    "moon": {
        "trunk": (64, 82, 82),
        "leaves": [(127, 185, 176), (94, 154, 148), (168, 212, 201), (76, 132, 128)],
        "accent": (234, 246, 240),
        "shape": (1.00, 1.00),
        "style": "sparkles",
    },
    "pine": {
        "trunk": (76, 62, 48),
        "leaves": [(64, 100, 76), (48, 82, 62), (88, 124, 94), (108, 140, 104)],
        "accent": None,
        "shape": (0.72, 1.18),
        "style": "pine",
    },
    "willow": {
        "trunk": (96, 78, 58),
        "leaves": [(178, 198, 132), (150, 176, 108), (204, 218, 158), (132, 160, 96)],
        "accent": None,
        "shape": (1.15, 1.00),
        "style": "willow",
    },
    "persimmon": {
        "trunk": (92, 68, 52),
        "leaves": [(104, 138, 88), (84, 116, 74), (130, 160, 108), (70, 100, 66)],
        "accent": (224, 132, 66),
        "shape": (1.05, 0.92),
        "style": "fruit",
    },
}


def rgba(color, alpha=255):
    return (color[0], color[1], color[2], int(alpha))


def blob(img, cx, cy, rx, ry, color, alpha, blur, rnd, wobble=0.16):
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    points = []
    for i in range(28):
        angle = 2 * math.pi * i / 28
        jitter = 1.0 + (rnd.random() - 0.5) * wobble
        points.append((cx + math.cos(angle) * rx * jitter, cy + math.sin(angle) * ry * jitter))
    draw.polygon(points, fill=rgba(color, alpha))
    layer = layer.filter(ImageFilter.GaussianBlur(blur))
    img.alpha_composite(layer)


def ground_shadow(img, cx, width, rnd):
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    draw.ellipse(
        [cx - width / 2, BASE_Y - width * 0.06, cx + width / 2, BASE_Y + width * 0.09],
        fill=(40, 50, 35, 55),
    )
    layer = layer.filter(ImageFilter.GaussianBlur(width * 0.05 + 8))
    img.alpha_composite(layer)


def trunk(img, cx, base_y, top_y, base_w, top_w, color, sway, rnd):
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    steps = 48
    left, right = [], []
    for i in range(steps + 1):
        t = i / steps
        y = base_y + (top_y - base_y) * t
        x = cx + math.sin(t * math.pi) * sway
        flare = 1.0 + 0.35 * (1 - t) ** 4
        half = (base_w * (1 - t) + top_w * t) / 2 * flare
        left.append((x - half, y))
        right.append((x + half, y))
    draw.polygon(left + right[::-1], fill=rgba(color, 255))
    layer = layer.filter(ImageFilter.GaussianBlur(1.2))
    img.alpha_composite(layer)

    dark = tuple(int(c * 0.72) for c in color)
    bark = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(bark)
    for _ in range(7):
        t0 = rnd.uniform(0.08, 0.82)
        t1 = min(1.0, t0 + rnd.uniform(0.08, 0.24))
        offset = rnd.uniform(-0.35, 0.35) * base_w * 0.5
        x0 = cx + math.sin(t0 * math.pi) * sway + offset
        x1 = cx + math.sin(t1 * math.pi) * sway + offset
        y0 = base_y + (top_y - base_y) * t0
        y1 = base_y + (top_y - base_y) * t1
        draw.line([(x0, y0), (x1, y1)], fill=rgba(dark, 70), width=max(2, int(base_w * 0.06)))
    bark = bark.filter(ImageFilter.GaussianBlur(1.6))
    img.alpha_composite(bark)


def branch(img, x0, y0, x1, y1, width, color):
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    draw.line([(x0, y0), (x1, y1)], fill=rgba(color, 225), width=max(3, int(width)))
    r = width / 2
    draw.ellipse([x1 - r, y1 - r, x1 + r, y1 + r], fill=rgba(color, 225))
    layer = layer.filter(ImageFilter.GaussianBlur(1.1))
    img.alpha_composite(layer)


def canopy(img, cx, cy, rw, rh, palette, count, rnd):
    base = max(rw, rh)
    blob(img, cx, cy, rw * 0.95, rh * 0.92, palette[1], 62, base * 0.10, rnd)
    blob(img, cx - rw * 0.28, cy - rh * 0.22, rw * 0.60, rh * 0.55, palette[2], 30, base * 0.08, rnd)
    blob(img, cx + rw * 0.30, cy + rh * 0.24, rw * 0.55, rh * 0.50, palette[3], 28, base * 0.08, rnd)

    for _ in range(count):
        angle = rnd.random() * 2 * math.pi
        dist = (rnd.random() ** 0.55) * 0.66
        bx = cx + math.cos(angle) * rw * dist
        by = cy + math.sin(angle) * rh * dist
        br = min(rw, rh) * (0.20 + rnd.random() * 0.22)
        color = palette[rnd.randrange(len(palette))]
        blob(img, bx, by, br * 1.10, br * 0.95, color, rnd.randint(34, 58), rnd.uniform(5, 12), rnd)

    edge_count = max(5, count // 2)
    for i in range(edge_count):
        angle = 2 * math.pi * i / edge_count + rnd.uniform(-0.2, 0.2)
        dist = rnd.uniform(0.80, 0.98)
        bx = cx + math.cos(angle) * rw * dist
        by = cy + math.sin(angle) * rh * dist
        br = min(rw, rh) * rnd.uniform(0.13, 0.22)
        blob(img, bx, by, br, br * 0.9, palette[rnd.randrange(len(palette))], rnd.randint(28, 48), rnd.uniform(4, 9), rnd)

    for _ in range(max(2, count // 3)):
        bx = cx - rw * 0.35 + rnd.uniform(-0.10, 0.10) * rw
        by = cy - rh * 0.35 + rnd.uniform(-0.10, 0.10) * rh
        br = min(rw, rh) * (0.14 + rnd.random() * 0.10)
        blob(img, bx, by, br, br * 0.9, palette[2], 44, 8, rnd)

    speckle = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(speckle)
    for _ in range(count * 2):
        angle = rnd.random() * 2 * math.pi
        dist = (rnd.random() ** 0.5) * 0.90
        x = cx + math.cos(angle) * rw * dist
        y = cy + math.sin(angle) * rh * dist
        r = rnd.uniform(3, max(5.0, min(rw, rh) * 0.045))
        draw.ellipse([x - r, y - r * 0.8, x + r, y + r * 0.8], fill=rgba(rnd.choice(palette), rnd.randint(40, 80)))
    img.alpha_composite(speckle.filter(ImageFilter.GaussianBlur(0.8)))


def conifer(img, cx, cy, rw, rh, palette, count, rnd):
    """Layered boughs give a fir silhouette while keeping the watercolor blobs."""
    layers = max(3, min(7, count // 2))
    for i in range(layers):
        t = i / (layers - 1)
        y = cy + rh * (0.62 - 1.18 * t)
        w = rw * (1.02 - 0.72 * t)
        h = rh * (0.24 - 0.05 * t)
        blob(img, cx + rnd.uniform(-0.04, 0.04) * rw, y, w, h,
             palette[i % len(palette)], 78, max(4, h * 0.16), rnd, wobble=0.24)
    blob(img, cx - rw * 0.22, cy - rh * 0.05, rw * 0.50, rh * 0.42,
         palette[2], 38, 8, rnd)


def willow(img, cx, cy, rw, rh, palette, count, rnd):
    canopy(img, cx, cy - rh * 0.10, rw * 0.92, rh * 0.80, palette, count, rnd)
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    for _ in range(max(10, count * 2)):
        theta = rnd.uniform(0.12, 0.88) * math.pi
        x = cx + math.cos(theta) * rw * 0.88
        top = cy + math.sin(theta) * rh * 0.70
        length = rnd.uniform(0.45, 0.95) * rh
        sway = rnd.uniform(-0.16, 0.16) * rw
        draw.line(
            [
                (x, top),
                (x + sway * 0.35, top + length * 0.45),
                (x + sway, top + length),
                (x + sway * 0.80, top + length * 1.12),
            ],
            fill=rgba(palette[rnd.randrange(len(palette))], rnd.randint(90, 150)),
            width=max(2, int(rw * 0.012)),
            joint="curve",
        )
    layer = layer.filter(ImageFilter.GaussianBlur(1.2))
    img.alpha_composite(layer)


def falling(img, cx, cy, rw, rh, color, count, rnd, size):
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    for _ in range(count):
        x = cx + rnd.uniform(-1.25, 1.25) * rw
        y = cy + rnd.uniform(-0.8, 1.6) * rh
        r = rnd.uniform(size * 0.5, size * 1.4)
        draw.ellipse([x - r, y - r * 0.6, x + r, y + r * 0.6], fill=rgba(color, rnd.randint(90, 170)))
    layer = layer.filter(ImageFilter.GaussianBlur(1.2))
    img.alpha_composite(layer)


def ground_scatter(img, cx, color, count, rnd):
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    for _ in range(count):
        x = cx + rnd.uniform(-0.32, 0.32) * W
        y = BASE_Y + rnd.uniform(-6, 14)
        r = rnd.uniform(3, 6)
        draw.ellipse([x - r, y - r * 0.5, x + r, y + r * 0.5], fill=rgba(color, rnd.randint(120, 190)))
    layer = layer.filter(ImageFilter.GaussianBlur(0.8))
    img.alpha_composite(layer)


def sparkles(img, cx, cy, rw, rh, color, count, rnd):
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    for _ in range(count):
        x = cx + rnd.uniform(-0.9, 0.9) * rw
        y = cy + rnd.uniform(-0.9, 0.9) * rh
        s = rnd.uniform(4, 11)
        draw.line([(x - s, y), (x + s, y)], fill=rgba(color, 200), width=2)
        draw.line([(x, y - s), (x, y + s)], fill=rgba(color, 200), width=2)
    layer = layer.filter(ImageFilter.GaussianBlur(0.8))
    img.alpha_composite(layer)


def grain(img):
    noise = Image.effect_noise((W, H), 16).convert("L")
    mask = ImageChops.multiply(img.getchannel("A"), noise).point(lambda v: int(v * 0.10))
    tint = Image.new("RGBA", (W, H), (62, 52, 38, 255))
    tint.putalpha(mask)
    img.alpha_composite(tint)


def render(species_id, stage):
    cfg = STAGES[stage]
    sp = SPECIES[species_id]
    rnd = random.Random(f"{species_id}-{stage}-v1")
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))

    base_x = W * 0.5 + rnd.uniform(-0.02, 0.02) * W
    height = H * cfg["height"]
    top_y = BASE_Y - height
    sway = rnd.uniform(-0.02, 0.02) * W * (0.4 if stage == 1 else 1.0)
    base_w = W * cfg["trunk"]
    top_w = base_w * 0.45

    shape_w, shape_h = sp["shape"]
    rw = W * cfg["cw"] * 0.5 * shape_w
    rh = H * cfg["ch"] * 0.5 * shape_h
    canopy_cy = top_y + rh * 0.45

    if sp["style"] == "sparkles":
        blob(img, base_x, canopy_cy, rw * 1.35, rh * 1.25, (255, 255, 255), 26, 60, rnd)

    ground_shadow(img, base_x, max(rw * 1.6, base_w * 6), rnd)
    trunk(img, base_x, BASE_Y, top_y, base_w, top_w, sp["trunk"], sway, rnd)

    for i in range(cfg["branches"]):
        t = 0.55 + i * 0.11
        x0 = base_x + math.sin(t * math.pi) * sway
        y0 = BASE_Y + (top_y - BASE_Y) * t
        angle = rnd.uniform(-0.9, 0.9)
        length = height * rnd.uniform(0.14, 0.24)
        x1 = x0 + math.sin(angle) * length * rnd.choice([-1, 1])
        y1 = y0 - math.cos(angle) * length * 0.55
        branch(img, x0, y0, x1, y1, base_w * 0.28, sp["trunk"])

    if sp["style"] == "pine":
        conifer(img, base_x, canopy_cy, rw, rh, sp["leaves"], cfg["blobs"], rnd)
    elif sp["style"] == "willow":
        willow(img, base_x, canopy_cy, rw, rh, sp["leaves"], cfg["blobs"], rnd)
    else:
        canopy(img, base_x, canopy_cy, rw, rh, sp["leaves"], cfg["blobs"], rnd)

    accent = sp["accent"]
    style = sp["style"]
    if accent is not None:
        if style == "acorns":
            layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
            draw = ImageDraw.Draw(layer)
            for _ in range(7):
                x = base_x + rnd.uniform(-0.7, 0.7) * rw
                y = canopy_cy + rnd.uniform(-0.1, 0.75) * rh
                r = rnd.uniform(4, 7)
                draw.ellipse([x - r * 0.8, y - r, x + r * 0.8, y + r], fill=rgba(accent, 210))
                draw.line([(x, y - r), (x, y - r * 1.7)], fill=rgba((90, 70, 50), 180), width=2)
            img.alpha_composite(layer.filter(ImageFilter.GaussianBlur(0.6)))
        elif style == "fruit":
            layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
            draw = ImageDraw.Draw(layer)
            for _ in range(9):
                x = base_x + rnd.uniform(-0.65, 0.65) * rw
                y = canopy_cy + rnd.uniform(-0.15, 0.75) * rh
                r = rnd.uniform(7, 11)
                draw.ellipse([x - r * 0.9, y - r, x + r * 0.9, y + r],
                             fill=rgba(accent, 220))
                draw.line([(x, y - r), (x, y - r * 1.6)],
                          fill=rgba((90, 70, 50), 170), width=2)
            img.alpha_composite(layer.filter(ImageFilter.GaussianBlur(0.6)))
        elif style == "petals":
            falling(img, base_x, canopy_cy, rw, rh, accent, 26, rnd, 8)
            ground_scatter(img, base_x, accent, 10, rnd)
        elif style == "leaves":
            falling(img, base_x, canopy_cy, rw, rh, accent, 16, rnd, 6)
            ground_scatter(img, base_x, accent, 12, rnd)
        elif style == "sparkles":
            sparkles(img, base_x, canopy_cy, rw, rh, accent, 14, rnd)

    grain(img)

    os.makedirs("art/preview", exist_ok=True)
    os.makedirs("app/src/main/res/drawable-nodpi", exist_ok=True)
    out = f"app/src/main/res/drawable-nodpi/tree_{species_id}_{stage}.webp"
    img.save(out, "WEBP", quality=92, method=4)
    img.save(f"art/preview/tree_{species_id}_{stage}.png", "PNG")
    return out


def contact_sheet():
    thumb_w, thumb_h = 300, 400
    cols = 3
    ids = list(SPECIES)
    rows = (len(ids) + cols - 1) // cols
    sheet = Image.new("RGB", (thumb_w * cols, thumb_h * rows), (247, 242, 231))
    for index, sid in enumerate(ids):
        art = Image.open(f"art/preview/tree_{sid}_3.png").convert("RGBA").resize((thumb_w, thumb_h))
        sheet.paste(art, ((index % cols) * thumb_w, (index // cols) * thumb_h), art)
    sheet.save("art/preview/contact_stage3.png")

    stages = Image.new("RGB", (thumb_w * 3, thumb_h), (247, 242, 231))
    for stage in (1, 2, 3):
        art = Image.open(f"art/preview/tree_sakura_{stage}.png").convert("RGBA").resize((thumb_w, thumb_h))
        stages.paste(art, ((stage - 1) * thumb_w, 0), art)
    stages.save("art/preview/contact_sakura_stages.png")


def notification_icon():
    size = 96
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    white = (255, 255, 255, 255)
    draw.polygon(
        [(48, 8), (66, 36), (54, 36), (72, 60), (60, 60), (78, 84),
         (18, 84), (36, 60), (24, 60), (42, 36), (30, 36)],
        fill=white,
    )
    draw.rectangle([42, 84, 54, 92], fill=white)
    img.save("app/src/main/res/drawable-nodpi/ic_stat_tree.png")


if __name__ == "__main__":
    for species in SPECIES:
        for stage in (1, 2, 3):
            print(render(species, stage))
    notification_icon()
    contact_sheet()
    print("done")
