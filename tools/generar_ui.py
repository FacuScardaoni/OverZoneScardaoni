"""
Genera las imágenes de la interfaz de OverZone: logo, botones de ataque y
súper del HUD, y la textura de brillo que se usa para efectos.

Uso (desde la carpeta del proyecto):  python tools/generar_ui.py
Requiere: pip install pillow numpy
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "assets")
UI = os.path.join(ROOT, "ui")
TITLE_FONT = os.path.join(ROOT, "fonts", "RussoOne-Regular.ttf")


def glow_texture(size=128):
    """Mancha de luz blanca, suave en los bordes. Se tiñe al dibujarla."""
    y, x = np.mgrid[0:size, 0:size]
    c = (size - 1) / 2
    d = np.sqrt((x - c) ** 2 + (y - c) ** 2) / (size / 2)
    a = np.clip(1 - d, 0, 1) ** 2.2
    img = np.zeros((size, size, 4), np.uint8)
    img[..., :3] = 255
    img[..., 3] = (a * 255).astype(np.uint8)
    return Image.fromarray(img)


def radial_fill(size, inner, outer):
    """Círculo con degradé del centro (inner) al borde (outer)."""
    y, x = np.mgrid[0:size, 0:size]
    c = (size - 1) / 2
    d = np.clip(np.sqrt((x - c) ** 2 + (y - c) ** 2) / (size / 2), 0, 1)[..., None]
    col = np.array(inner, float) * (1 - d) + np.array(outer, float) * d
    img = np.zeros((size, size, 4), np.uint8)
    img[..., :3] = col.astype(np.uint8)
    img[..., 3] = 255
    return Image.fromarray(img)


def circle_button(size, inner, outer, rim, icon_drawer):
    """Botón redondo con degradé, borde claro, brillo superior y un ícono."""
    ss = size * 4                              # se dibuja grande y se achica: bordes suaves
    base = radial_fill(ss, inner, outer)
    mask = Image.new("L", (ss, ss), 0)
    md = ImageDraw.Draw(mask)
    md.ellipse([ss * 0.06, ss * 0.06, ss * 0.94, ss * 0.94], fill=255)
    img = Image.new("RGBA", (ss, ss), (0, 0, 0, 0))
    img.paste(base, (0, 0), mask)
    d = ImageDraw.Draw(img)
    d.ellipse([ss * 0.06, ss * 0.06, ss * 0.94, ss * 0.94], outline=rim, width=int(ss * 0.035))
    d.ellipse([ss * 0.12, ss * 0.12, ss * 0.88, ss * 0.88], outline=rim[:3] + (90,), width=int(ss * 0.012))
    # Brillo en la parte de arriba (da volumen)
    shine = Image.new("RGBA", (ss, ss), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shine)
    sd.ellipse([ss * 0.2, ss * 0.1, ss * 0.8, ss * 0.45], fill=(255, 255, 255, 70))
    shine = shine.filter(ImageFilter.GaussianBlur(ss * 0.03))
    img.alpha_composite(shine)
    icon_drawer(ImageDraw.Draw(img), ss)
    return img.resize((size, size), Image.LANCZOS)


def crosshair(d, ss):
    """Mira de francotirador."""
    c = ss / 2
    w = int(ss * 0.045)
    white = (255, 255, 255, 255)
    r = ss * 0.24
    d.ellipse([c - r, c - r, c + r, c + r], outline=white, width=w)
    r2 = ss * 0.07
    d.ellipse([c - r2, c - r2, c + r2, c + r2], fill=white)
    for ax, ay in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        d.line([c + ax * ss * 0.13, c + ay * ss * 0.13, c + ax * ss * 0.36, c + ay * ss * 0.36], fill=white, width=w)


def lightning(d, ss):
    """Rayo (súper de Volt)."""
    pts = [(0.56, 0.18), (0.32, 0.54), (0.48, 0.54), (0.40, 0.84), (0.70, 0.42), (0.53, 0.42), (0.62, 0.18)]
    poly = [(x * ss, y * ss) for x, y in pts]
    d.polygon(poly, fill=(255, 255, 255, 255), outline=(120, 60, 0, 255))


def shield_icon(d, ss):
    """Escudo (súper de Brutus)."""
    pts = [(0.50, 0.20), (0.74, 0.30), (0.71, 0.56), (0.50, 0.80), (0.29, 0.56), (0.26, 0.30)]
    poly = [(x * ss, y * ss) for x, y in pts]
    d.polygon(poly, fill=(255, 255, 255, 255))
    inner = [(0.50, 0.30), (0.64, 0.36), (0.62, 0.54), (0.50, 0.68)]
    d.polygon([(x * ss, y * ss) for x, y in inner], fill=(255, 190, 60, 255))


def heal_icon(d, ss):
    """Cruz de curación (súper de Iris)."""
    white = (255, 255, 255, 255)
    d.rounded_rectangle([ss * 0.42, ss * 0.22, ss * 0.58, ss * 0.78], radius=ss * 0.03, fill=white)
    d.rounded_rectangle([ss * 0.22, ss * 0.42, ss * 0.78, ss * 0.58], radius=ss * 0.03, fill=white)
    d.rounded_rectangle([ss * 0.46, ss * 0.30, ss * 0.54, ss * 0.70], radius=ss * 0.02, fill=(80, 220, 130, 255))


def eye_icon(d, ss):
    """Ojo tachado (súper de Phantom: camuflaje)."""
    white = (255, 255, 255, 255)
    c = ss / 2
    d.chord([ss * 0.20, ss * 0.34, ss * 0.80, ss * 0.70], 180, 360, fill=white)
    d.chord([ss * 0.20, ss * 0.30, ss * 0.80, ss * 0.66], 0, 180, fill=white)
    r = ss * 0.10
    d.ellipse([c - r, c - r, c + r, c + r], fill=(120, 30, 40, 255))
    d.line([ss * 0.24, ss * 0.76, ss * 0.76, ss * 0.24], fill=(120, 30, 40, 255), width=int(ss * 0.09))
    d.line([ss * 0.24, ss * 0.76, ss * 0.76, ss * 0.24], fill=white, width=int(ss * 0.04))


def logo():
    """Logo "OVERZONE": OVER en blanco/cian y ZONE en naranja/amarillo, con brillo."""
    font = ImageFont.truetype(TITLE_FONT, 150)
    w, h = 1100, 260
    text = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    td = ImageDraw.Draw(text)
    over_w = td.textlength("OVER", font=font)
    zone_w = td.textlength("ZONE", font=font)
    x0 = (w - over_w - zone_w - 10) / 2
    y0 = 40

    def gradient_word(word, x, top, bottom):
        mask = Image.new("L", (w, h), 0)
        ImageDraw.Draw(mask).text((x, y0), word, font=font, fill=255)
        grad = np.zeros((h, w, 4), np.uint8)
        for yy in range(h):
            t = min(1, max(0, (yy - y0 - 20) / 150))
            grad[yy, :, :3] = np.array(top) * (1 - t) + np.array(bottom) * t
        grad[..., 3] = np.array(mask)
        return Image.fromarray(grad)

    over = gradient_word("OVER", x0, (255, 255, 255), (120, 225, 255))
    zone = gradient_word("ZONE", x0 + over_w + 10, (255, 236, 120), (255, 96, 24))
    text.alpha_composite(over)
    text.alpha_composite(zone)

    # Inclinación hacia adelante: sensación de velocidad
    text = text.transform((w, h), Image.AFFINE, (1, 0.18, -30, 0, 1, 0), Image.BICUBIC)

    # Contorno oscuro y brillo de color detrás
    alpha = text.split()[3]
    outline = Image.new("RGBA", (w, h), (8, 10, 20, 0))
    outline.putalpha(alpha.filter(ImageFilter.MaxFilter(9)))
    glow_cyan = Image.new("RGBA", (w, h), (34, 224, 255, 0))
    glow_cyan.putalpha(alpha.filter(ImageFilter.GaussianBlur(18)).point(lambda v: int(v * 0.9)))
    glow_orange = Image.new("RGBA", (w, h), (255, 110, 20, 0))
    glow_orange.putalpha(alpha.filter(ImageFilter.GaussianBlur(30)).point(lambda v: int(v * 0.7)))

    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    out.alpha_composite(glow_orange)
    out.alpha_composite(glow_cyan)
    out.alpha_composite(outline)
    out.alpha_composite(text)

    # Corte diagonal de energía que atraviesa el logo
    slash = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(slash).polygon([(120, 208), (980, 172), (990, 180), (130, 216)], fill=(255, 230, 160, 200))
    out.alpha_composite(slash.filter(ImageFilter.GaussianBlur(1.2)))
    return out


def main():
    os.makedirs(UI, exist_ok=True)
    images = {
        "glow.png": glow_texture(),
        "btn_attack.png": circle_button(256, (255, 92, 92), (170, 16, 28), (255, 210, 210, 255), crosshair),
        "btn_super_volt.png": circle_button(256, (255, 246, 140), (240, 160, 10), (255, 252, 220, 255), lightning),
        "btn_super_brutus.png": circle_button(256, (255, 246, 140), (240, 160, 10), (255, 252, 220, 255), shield_icon),
        "btn_super_iris.png": circle_button(256, (255, 246, 140), (240, 160, 10), (255, 252, 220, 255), heal_icon),
        "btn_super_phantom.png": circle_button(256, (255, 246, 140), (240, 160, 10), (255, 252, 220, 255), eye_icon),
        "logo.png": logo(),
    }
    for name, img in images.items():
        img.save(os.path.join(UI, name))
        print("generado: ui/" + name, img.size)


if __name__ == "__main__":
    main()
