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

RECURSOS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "assets")
SUPERMUESTREO = 4          # se dibuja 4 veces más grande y se achica: bordes suaves
TAMANIO = 32

COLORES = {
    "botiquin": (90, 255, 140),
    "turbo": (255, 215, 60),
    "sobrecarga": (255, 80, 50),
    "blindaje": (80, 210, 255),
}


def hexagono(cx, cy, r):
    return [(cx + r * math.cos(math.radians(60 * i + 30)), cy + r * math.sin(math.radians(60 * i + 30)))
            for i in range(6)]


def capsula(color, icono):
    s = TAMANIO * SUPERMUESTREO
    imagen = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    # Brillo exterior
    brillo = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    ImageDraw.Draw(brillo).polygon(hexagono(s / 2, s / 2, s * 0.44), fill=color[:3] + (170,))
    imagen.alpha_composite(brillo.filter(ImageFilter.GaussianBlur(s * 0.06)))
    d = ImageDraw.Draw(imagen)
    # Hexágono oscuro con borde del color
    d.polygon(hexagono(s / 2, s / 2, s * 0.40), fill=color)
    d.polygon(hexagono(s / 2, s / 2, s * 0.34), fill=(18, 22, 34, 255))
    # Reflejo arriba
    d.polygon(hexagono(s / 2, s * 0.40, s * 0.22), fill=(255, 255, 255, 22))
    icono(d, s, color)
    return imagen.resize((TAMANIO, TAMANIO), Image.LANCZOS)


def icono_botiquin(d, s, c):
    w, h = s * 0.09, s * 0.22
    d.rectangle([s / 2 - w, s / 2 - h, s / 2 + w, s / 2 + h], fill=c)
    d.rectangle([s / 2 - h, s / 2 - w, s / 2 + h, s / 2 + w], fill=c)
    d.rectangle([s / 2 - w * 0.4, s / 2 - h * 0.8, s / 2 + w * 0.4, s / 2 + h * 0.8], fill=(255, 255, 255, 200))


def icono_turbo(d, s, c):
    for k in (-0.10, 0.06):
        x = s / 2 + s * k
        d.polygon([(x - s * 0.08, s * 0.30), (x + s * 0.10, s / 2), (x - s * 0.08, s * 0.70),
                   (x - s * 0.02, s / 2)], fill=c)
    d.line([(s * 0.30, s / 2), (s * 0.40, s / 2)], fill=(255, 255, 255, 200), width=int(s * 0.03))


def icono_sobrecarga(d, s, c):
    puntos = [(s * 0.56, s * 0.24), (s * 0.36, s * 0.53), (s * 0.49, s * 0.53),
           (s * 0.42, s * 0.77), (s * 0.66, s * 0.45), (s * 0.52, s * 0.45)]
    d.polygon(puntos, fill=c)
    d.line([puntos[0], puntos[1]], fill=(255, 255, 255, 220), width=int(s * 0.025))


def icono_blindaje(d, s, c):
    puntos = [(s * 0.5, s * 0.25), (s * 0.70, s * 0.33), (s * 0.67, s * 0.56),
           (s * 0.5, s * 0.75), (s * 0.33, s * 0.56), (s * 0.30, s * 0.33)]
    d.polygon(puntos, fill=c)
    d.polygon([(s * 0.5, s * 0.33), (s * 0.62, s * 0.38), (s * 0.60, s * 0.54), (s * 0.5, s * 0.66)],
              fill=(255, 255, 255, 120))


def base_piso():
    """Base circular en el piso (blanca, el juego la tiñe con el color del Power-Up)."""
    tamanio = 48
    s = tamanio * SUPERMUESTREO
    imagen = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(imagen)
    c = s / 2
    d.ellipse([c - s * 0.46, c - s * 0.46, c + s * 0.46, c + s * 0.46], fill=(255, 255, 255, 40),
              outline=(255, 255, 255, 170), width=int(s * 0.03))
    d.ellipse([c - s * 0.30, c - s * 0.30, c + s * 0.30, c + s * 0.30], outline=(255, 255, 255, 110),
              width=int(s * 0.02))
    for angulo in range(0, 360, 90):  # marcas en cruz
        a = math.radians(angulo + 45)
        x1, y1 = c + math.cos(a) * s * 0.36, c + math.sin(a) * s * 0.36
        x2, y2 = c + math.cos(a) * s * 0.46, c + math.sin(a) * s * 0.46
        d.line([(x1, y1), (x2, y2)], fill=(255, 255, 255, 200), width=int(s * 0.035))
    return imagen.resize((tamanio, tamanio), Image.LANCZOS)


def sonido_recogida():
    frecuencia_muestreo = 44100
    t = np.linspace(0, 0.35, int(frecuencia_muestreo * 0.35), endpoint=False)
    frecuencia = 520 + 900 * (t / 0.35)                       # sube
    fase = 2 * np.pi * np.cumsum(frecuencia) / frecuencia_muestreo
    envolvente = np.minimum(1, t / 0.01) * np.exp(-t * 7)
    senal = envolvente * (np.sin(fase) + 0.35 * np.sin(2 * fase))
    senal += 0.25 * np.sin(2 * np.pi * 1760 * t) * np.exp(-np.maximum(0, t - 0.12) * 18) * (t > 0.12)
    senal = senal / np.max(np.abs(senal)) * 0.7
    with wave.open(os.path.join(RECURSOS, "powerup.wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(frecuencia_muestreo)
        w.writeframes((senal * 32767).astype(np.int16).tobytes())
    print("generado: powerup.wav")


if __name__ == "__main__":
    iconos = {"botiquin": icono_botiquin, "turbo": icono_turbo, "sobrecarga": icono_sobrecarga, "blindaje": icono_blindaje}
    for nombre, funcion in iconos.items():
        capsula(COLORES[nombre] + (255,), funcion).save(os.path.join(RECURSOS, "powerup_" + nombre + ".png"))
        print("generado: powerup_" + nombre + ".png")
    base_piso().save(os.path.join(RECURSOS, "powerup_pad.png"))
    print("generado: powerup_pad.png")
    sonido_recogida()
