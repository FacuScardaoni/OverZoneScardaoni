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

RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "assets")
INTERFAZ = os.path.join(RAIZ, "ui")
FUENTE_TITULO = os.path.join(RAIZ, "fonts", "RussoOne-Regular.ttf")


def textura_brillo(tamanio=128):
    """Mancha de luz blanca, suave en los bordes. Se tiñe al dibujarla."""
    y, x = np.mgrid[0:tamanio, 0:tamanio]
    c = (tamanio - 1) / 2
    d = np.sqrt((x - c) ** 2 + (y - c) ** 2) / (tamanio / 2)
    a = np.clip(1 - d, 0, 1) ** 2.2
    imagen = np.zeros((tamanio, tamanio, 4), np.uint8)
    imagen[..., :3] = 255
    imagen[..., 3] = (a * 255).astype(np.uint8)
    return Image.fromarray(imagen)


def relleno_radial(tamanio, interior, exterior):
    """Círculo con degradé del centro (inner) al borde (outer)."""
    y, x = np.mgrid[0:tamanio, 0:tamanio]
    c = (tamanio - 1) / 2
    d = np.clip(np.sqrt((x - c) ** 2 + (y - c) ** 2) / (tamanio / 2), 0, 1)[..., None]
    columna = np.array(interior, float) * (1 - d) + np.array(exterior, float) * d
    imagen = np.zeros((tamanio, tamanio, 4), np.uint8)
    imagen[..., :3] = columna.astype(np.uint8)
    imagen[..., 3] = 255
    return Image.fromarray(imagen)


def boton_circular(tamanio, interior, exterior, borde, dibujar_icono):
    """Botón redondo con degradé, borde claro, brillo superior y un ícono."""
    ss = tamanio * 4                              # se dibuja grande y se achica: bordes suaves
    base = relleno_radial(ss, interior, exterior)
    mascara = Image.new("L", (ss, ss), 0)
    mascara_d = ImageDraw.Draw(mascara)
    mascara_d.ellipse([ss * 0.06, ss * 0.06, ss * 0.94, ss * 0.94], fill=255)
    imagen = Image.new("RGBA", (ss, ss), (0, 0, 0, 0))
    imagen.paste(base, (0, 0), mascara)
    d = ImageDraw.Draw(imagen)
    d.ellipse([ss * 0.06, ss * 0.06, ss * 0.94, ss * 0.94], outline=borde, width=int(ss * 0.035))
    d.ellipse([ss * 0.12, ss * 0.12, ss * 0.88, ss * 0.88], outline=borde[:3] + (90,), width=int(ss * 0.012))
    # Brillo en la parte de arriba (da volumen)
    brillo_agudo = Image.new("RGBA", (ss, ss), (0, 0, 0, 0))
    sombra_d = ImageDraw.Draw(brillo_agudo)
    sombra_d.ellipse([ss * 0.2, ss * 0.1, ss * 0.8, ss * 0.45], fill=(255, 255, 255, 70))
    brillo_agudo = brillo_agudo.filter(ImageFilter.GaussianBlur(ss * 0.03))
    imagen.alpha_composite(brillo_agudo)
    dibujar_icono(ImageDraw.Draw(imagen), ss)
    return imagen.resize((tamanio, tamanio), Image.LANCZOS)


def mira(d, ss):
    """Mira de francotirador."""
    c = ss / 2
    w = int(ss * 0.045)
    blanco = (255, 255, 255, 255)
    r = ss * 0.24
    d.ellipse([c - r, c - r, c + r, c + r], outline=blanco, width=w)
    r2 = ss * 0.07
    d.ellipse([c - r2, c - r2, c + r2, c + r2], fill=blanco)
    for ax, ay in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        d.line([c + ax * ss * 0.13, c + ay * ss * 0.13, c + ax * ss * 0.36, c + ay * ss * 0.36], fill=blanco, width=w)


def rayo(d, ss):
    """Rayo (súper de Volt)."""
    puntos = [(0.56, 0.18), (0.32, 0.54), (0.48, 0.54), (0.40, 0.84), (0.70, 0.42), (0.53, 0.42), (0.62, 0.18)]
    poligono = [(x * ss, y * ss) for x, y in puntos]
    d.polygon(poligono, fill=(255, 255, 255, 255), outline=(120, 60, 0, 255))


def icono_escudo(d, ss):
    """Escudo (súper de Brutus)."""
    puntos = [(0.50, 0.20), (0.74, 0.30), (0.71, 0.56), (0.50, 0.80), (0.29, 0.56), (0.26, 0.30)]
    poligono = [(x * ss, y * ss) for x, y in puntos]
    d.polygon(poligono, fill=(255, 255, 255, 255))
    interior = [(0.50, 0.30), (0.64, 0.36), (0.62, 0.54), (0.50, 0.68)]
    d.polygon([(x * ss, y * ss) for x, y in interior], fill=(255, 190, 60, 255))


def icono_curacion(d, ss):
    """Cruz de curación (súper de Iris)."""
    blanco = (255, 255, 255, 255)
    d.rounded_rectangle([ss * 0.42, ss * 0.22, ss * 0.58, ss * 0.78], radius=ss * 0.03, fill=blanco)
    d.rounded_rectangle([ss * 0.22, ss * 0.42, ss * 0.78, ss * 0.58], radius=ss * 0.03, fill=blanco)
    d.rounded_rectangle([ss * 0.46, ss * 0.30, ss * 0.54, ss * 0.70], radius=ss * 0.02, fill=(80, 220, 130, 255))


def icono_ojo(d, ss):
    """Ojo tachado (súper de Phantom: camuflaje)."""
    blanco = (255, 255, 255, 255)
    c = ss / 2
    d.chord([ss * 0.20, ss * 0.34, ss * 0.80, ss * 0.70], 180, 360, fill=blanco)
    d.chord([ss * 0.20, ss * 0.30, ss * 0.80, ss * 0.66], 0, 180, fill=blanco)
    r = ss * 0.10
    d.ellipse([c - r, c - r, c + r, c + r], fill=(120, 30, 40, 255))
    d.line([ss * 0.24, ss * 0.76, ss * 0.76, ss * 0.24], fill=(120, 30, 40, 255), width=int(ss * 0.09))
    d.line([ss * 0.24, ss * 0.76, ss * 0.76, ss * 0.24], fill=blanco, width=int(ss * 0.04))


def logo():
    """Logo "OVERZONE": OVER en blanco/cian y ZONE en naranja/amarillo, con brillo."""
    fuente = ImageFont.truetype(FUENTE_TITULO, 150)
    w, h = 1100, 260
    texto = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    texto_d = ImageDraw.Draw(texto)
    ancho_over = texto_d.textlength("OVER", font=fuente)
    ancho_zone = texto_d.textlength("ZONE", font=fuente)
    x0 = (w - ancho_over - ancho_zone - 10) / 2
    y0 = 40

    def palabra_degradada(palabra, x, arriba, abajo):
        mascara = Image.new("L", (w, h), 0)
        ImageDraw.Draw(mascara).text((x, y0), palabra, font=fuente, fill=255)
        degradado = np.zeros((h, w, 4), np.uint8)
        for yy in range(h):
            t = min(1, max(0, (yy - y0 - 20) / 150))
            degradado[yy, :, :3] = np.array(arriba) * (1 - t) + np.array(abajo) * t
        degradado[..., 3] = np.array(mascara)
        return Image.fromarray(degradado)

    over = palabra_degradada("OVER", x0, (255, 255, 255), (120, 225, 255))
    zone = palabra_degradada("ZONE", x0 + ancho_over + 10, (255, 236, 120), (255, 96, 24))
    texto.alpha_composite(over)
    texto.alpha_composite(zone)

    # Inclinación hacia adelante: sensación de velocidad
    texto = texto.transform((w, h), Image.AFFINE, (1, 0.18, -30, 0, 1, 0), Image.BICUBIC)

    # Contorno oscuro y brillo de color detrás
    alfa = texto.split()[3]
    contorno = Image.new("RGBA", (w, h), (8, 10, 20, 0))
    contorno.putalpha(alfa.filter(ImageFilter.MaxFilter(9)))
    brillo_cian = Image.new("RGBA", (w, h), (34, 224, 255, 0))
    brillo_cian.putalpha(alfa.filter(ImageFilter.GaussianBlur(18)).point(lambda v: int(v * 0.9)))
    brillo_naranja = Image.new("RGBA", (w, h), (255, 110, 20, 0))
    brillo_naranja.putalpha(alfa.filter(ImageFilter.GaussianBlur(30)).point(lambda v: int(v * 0.7)))

    salida = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    salida.alpha_composite(brillo_naranja)
    salida.alpha_composite(brillo_cian)
    salida.alpha_composite(contorno)
    salida.alpha_composite(texto)

    # Corte diagonal de energía que atraviesa el logo
    barra = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(barra).polygon([(120, 208), (980, 172), (990, 180), (130, 216)], fill=(255, 230, 160, 200))
    salida.alpha_composite(barra.filter(ImageFilter.GaussianBlur(1.2)))
    return salida


def principal():
    os.makedirs(INTERFAZ, exist_ok=True)
    imagenes = {
        "glow.png": textura_brillo(),
        "btn_attack.png": boton_circular(256, (255, 92, 92), (170, 16, 28), (255, 210, 210, 255), mira),
        "btn_super_volt.png": boton_circular(256, (255, 246, 140), (240, 160, 10), (255, 252, 220, 255), rayo),
        "btn_super_brutus.png": boton_circular(256, (255, 246, 140), (240, 160, 10), (255, 252, 220, 255), icono_escudo),
        "btn_super_iris.png": boton_circular(256, (255, 246, 140), (240, 160, 10), (255, 252, 220, 255), icono_curacion),
        "btn_super_phantom.png": boton_circular(256, (255, 246, 140), (240, 160, 10), (255, 252, 220, 255), icono_ojo),
        "logo.png": logo(),
    }
    for nombre, imagen in imagenes.items():
        imagen.save(os.path.join(INTERFAZ, nombre))
        print("generado: ui/" + nombre, imagen.size)


if __name__ == "__main__":
    principal()
