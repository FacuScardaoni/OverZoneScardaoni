"""
Genera las imágenes y el sonido de los Power-Ups:
  assets/powerup_health.png    Botiquín (cruz verde)
  assets/powerup_turbo.png     Turbo (flechas amarillas)
  assets/powerup_overload.png  Sobrecarga (rayo rojo)
  assets/powerup_armor.png     Blindaje (escudo celeste)
  assets/powerup_pad.png       base en el piso donde aparecen (blanca, se tiñe)
  assets/powerup.wav           sonido al agarrar uno
Uso: python tools/generar_powerups.py      Requiere: pip install pillow numpy
"""
import math
import os
import wave

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ASSETS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "assets")
SS = 4          # se dibuja 4 veces más grande y se achica: bordes suaves
SIZE = 32

COLORS = {
    "health": (90, 255, 140),
    "turbo": (255, 215, 60),
    "overload": (255, 80, 50),
    "armor": (80, 210, 255),
}


def hexagon(cx, cy, r):
    return [(cx + r * math.cos(math.radians(60 * i + 30)), cy + r * math.sin(math.radians(60 * i + 30)))
            for i in range(6)]


def capsule(color, icon):
    s = SIZE * SS
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    # Brillo exterior
    glow = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    ImageDraw.Draw(glow).polygon(hexagon(s / 2, s / 2, s * 0.44), fill=color[:3] + (170,))
    img.alpha_composite(glow.filter(ImageFilter.GaussianBlur(s * 0.06)))
    d = ImageDraw.Draw(img)
    # Hexágono oscuro con borde del color
    d.polygon(hexagon(s / 2, s / 2, s * 0.40), fill=color)
    d.polygon(hexagon(s / 2, s / 2, s * 0.34), fill=(18, 22, 34, 255))
    # Reflejo arriba
    d.polygon(hexagon(s / 2, s * 0.40, s * 0.22), fill=(255, 255, 255, 22))
    icon(d, s, color)
    return img.resize((SIZE, SIZE), Image.LANCZOS)


def icon_health(d, s, c):
    w, h = s * 0.09, s * 0.22
    d.rectangle([s / 2 - w, s / 2 - h, s / 2 + w, s / 2 + h], fill=c)
    d.rectangle([s / 2 - h, s / 2 - w, s / 2 + h, s / 2 + w], fill=c)
    d.rectangle([s / 2 - w * 0.4, s / 2 - h * 0.8, s / 2 + w * 0.4, s / 2 + h * 0.8], fill=(255, 255, 255, 200))


def icon_turbo(d, s, c):
    for k in (-0.10, 0.06):
        x = s / 2 + s * k
        d.polygon([(x - s * 0.08, s * 0.30), (x + s * 0.10, s / 2), (x - s * 0.08, s * 0.70),
                   (x - s * 0.02, s / 2)], fill=c)
    d.line([(s * 0.30, s / 2), (s * 0.40, s / 2)], fill=(255, 255, 255, 200), width=int(s * 0.03))


def icon_overload(d, s, c):
    pts = [(s * 0.56, s * 0.24), (s * 0.36, s * 0.53), (s * 0.49, s * 0.53),
           (s * 0.42, s * 0.77), (s * 0.66, s * 0.45), (s * 0.52, s * 0.45)]
    d.polygon(pts, fill=c)
    d.line([pts[0], pts[1]], fill=(255, 255, 255, 220), width=int(s * 0.025))


def icon_armor(d, s, c):
    pts = [(s * 0.5, s * 0.25), (s * 0.70, s * 0.33), (s * 0.67, s * 0.56),
           (s * 0.5, s * 0.75), (s * 0.33, s * 0.56), (s * 0.30, s * 0.33)]
    d.polygon(pts, fill=c)
    d.polygon([(s * 0.5, s * 0.33), (s * 0.62, s * 0.38), (s * 0.60, s * 0.54), (s * 0.5, s * 0.66)],
              fill=(255, 255, 255, 120))


def pad():
    """Base circular en el piso (blanca, el juego la tiñe con el color del Power-Up)."""
    size = 48
    s = size * SS
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = s / 2
    d.ellipse([c - s * 0.46, c - s * 0.46, c + s * 0.46, c + s * 0.46], fill=(255, 255, 255, 40),
              outline=(255, 255, 255, 170), width=int(s * 0.03))
    d.ellipse([c - s * 0.30, c - s * 0.30, c + s * 0.30, c + s * 0.30], outline=(255, 255, 255, 110),
              width=int(s * 0.02))
    for ang in range(0, 360, 90):  # marcas en cruz
        a = math.radians(ang + 45)
        x1, y1 = c + math.cos(a) * s * 0.36, c + math.sin(a) * s * 0.36
        x2, y2 = c + math.cos(a) * s * 0.46, c + math.sin(a) * s * 0.46
        d.line([(x1, y1), (x2, y2)], fill=(255, 255, 255, 200), width=int(s * 0.035))
    return img.resize((size, size), Image.LANCZOS)


def pickup_sound():
    rate = 44100
    t = np.linspace(0, 0.35, int(rate * 0.35), endpoint=False)
    freq = 520 + 900 * (t / 0.35)                       # sube
    phase = 2 * np.pi * np.cumsum(freq) / rate
    env = np.minimum(1, t / 0.01) * np.exp(-t * 7)
    sig = env * (np.sin(phase) + 0.35 * np.sin(2 * phase))
    sig += 0.25 * np.sin(2 * np.pi * 1760 * t) * np.exp(-np.maximum(0, t - 0.12) * 18) * (t > 0.12)
    sig = sig / np.max(np.abs(sig)) * 0.7
    with wave.open(os.path.join(ASSETS, "powerup.wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(rate)
        w.writeframes((sig * 32767).astype(np.int16).tobytes())
    print("generado: powerup.wav")


if __name__ == "__main__":
    icons = {"health": icon_health, "turbo": icon_turbo, "overload": icon_overload, "armor": icon_armor}
    for name, fn in icons.items():
        capsule(COLORS[name] + (255,), fn).save(os.path.join(ASSETS, "powerup_" + name + ".png"))
        print("generado: powerup_" + name + ".png")
    pad().save(os.path.join(ASSETS, "powerup_pad.png"))
    print("generado: powerup_pad.png")
    pickup_sound()
