"""
Genera los sprites de OverZone (personajes, proyectiles, escudo y mapa).

Todo se dibuja pixel por pixel con Pillow, así los recursos se pueden volver
a generar o modificar cambiando colores y medidas en este archivo.

Uso (desde la carpeta del proyecto):
    python tools/generar_sprites.py

Requiere: pip install pillow
"""
import math
import os
import random

from PIL import Image, ImageDraw

RECURSOS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "assets")
CUADRO = 32
ABAJO, IZQUIERDA, DERECHA, ARRIBA = 0, 1, 2, 3
CONTORNO = (14, 16, 22, 255)


def rgba(color_hex, alfa=255):
    color_hex = color_hex.lstrip("#")
    return tuple(int(color_hex[i:i + 2], 16) for i in (0, 2, 4)) + (alfa,)


# ----------------------------------------------------------------------
# Personajes
# ----------------------------------------------------------------------

# Colores de cada facción, según la propuesta:
#   CyberCore (Volt):           azul, celeste y violeta
#   Titan Force (Brutus):       gris metálico, rojo y negro
#   Aegis Protocol (Iris):      blanco, verde y cian
#   Shadow Syndicate (Phantom): negro, rojo oscuro y detalles luminosos

VOLT = {
    "armadura": rgba("#3f7fe0"), "armadura_sombra": rgba("#24519c"), "armadura_clara": rgba("#8cc0ff"),
    "traje": rgba("#232a44"), "traje_claro": rgba("#36406a"),
    "acento": rgba("#3ff2ff"), "acento_oscuro": rgba("#8a5cff"),
    "visor": rgba("#6ff6ff"), "arma": rgba("#4a5563"), "arma_clara": rgba("#7d8a99"),
    "ancho_torso": 10, "alto_torso": 8, "ancho_cabeza": 8, "alto_cabeza": 7, "ancho_pierna": 3, "alto_pierna": 5,
    "hombro": 0, "ancho_brazo": 2, "ancho_arma": 2, "largo_arma": 7, "mochila": False,
}

BRUTUS = {
    "armadura": rgba("#6a737e"), "armadura_sombra": rgba("#3e444d"), "armadura_clara": rgba("#929ca8"),
    "traje": rgba("#1d1c1f"), "traje_claro": rgba("#302e33"),
    "acento": rgba("#ff3b30"), "acento_oscuro": rgba("#a3150f"),
    "visor": rgba("#ff6a4d"), "arma": rgba("#3f464f"), "arma_clara": rgba("#808a96"),
    "ancho_torso": 16, "alto_torso": 9, "ancho_cabeza": 10, "alto_cabeza": 6, "ancho_pierna": 4, "alto_pierna": 5,
    "hombro": 4, "ancho_brazo": 3, "ancho_arma": 4, "largo_arma": 9, "mochila": True,
}

IRIS = {
    "armadura": rgba("#eef3f1"), "armadura_sombra": rgba("#a9b8b3"), "armadura_clara": rgba("#ffffff"),
    "traje": rgba("#2c3b38"), "traje_claro": rgba("#3f5450"),
    "acento": rgba("#3dff8f"), "acento_oscuro": rgba("#17a85a"),
    "visor": rgba("#5ff7ff"), "arma": rgba("#c9d6d2"), "arma_clara": rgba("#ffffff"),
    "ancho_torso": 9, "alto_torso": 8, "ancho_cabeza": 8, "alto_cabeza": 7, "ancho_pierna": 3, "alto_pierna": 5,
    "hombro": 0, "ancho_brazo": 2, "ancho_arma": 2, "largo_arma": 5, "mochila": False,
    "holograma": True,              # dispositivos holográficos flotando a su lado
}

PHANTOM = {
    "armadura": rgba("#23242b"), "armadura_sombra": rgba("#121318"), "armadura_clara": rgba("#464955"),
    "traje": rgba("#15161a"), "traje_claro": rgba("#2a2b32"),
    "acento": rgba("#ff2a3d"), "acento_oscuro": rgba("#8a0f1c"),
    "visor": rgba("#ff3348"), "arma": rgba("#2c2e36"), "arma_clara": rgba("#5b5f6b"),
    "ancho_torso": 9, "alto_torso": 8, "ancho_cabeza": 8, "alto_cabeza": 7, "ancho_pierna": 3, "alto_pierna": 6,
    "hombro": 0, "ancho_brazo": 2, "ancho_arma": 2, "largo_arma": 6, "mochila": False,
    "ranura": True,              # casco cerrado: solo una ranura roja como visor
}


def rect(d, x0, y0, w, h, color):
    if w > 0 and h > 0:
        d.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], fill=color)


def caja_sombreada(d, x0, y0, w, h, base, claro, sombra):
    """Caja con luz arriba a la izquierda y sombra abajo a la derecha."""
    rect(d, x0, y0, w, h, base)
    rect(d, x0, y0, w, 1, claro)
    rect(d, x0, y0, 1, h, claro)
    rect(d, x0, y0 + h - 1, w, 1, sombra)
    rect(d, x0 + w - 1, y0, 1, h, sombra)


def redondear_esquinas(d, x0, y0, w, h):
    """Borra las 4 esquinas de una caja para que se vea redondeada."""
    for x, y in ((x0, y0), (x0 + w - 1, y0), (x0, y0 + h - 1), (x0 + w - 1, y0 + h - 1)):
        d.point((x, y), fill=(0, 0, 0, 0))


def desplazamientos_caminata(cuadro):
    """Desplazamiento de cada pierna y del cuerpo según el cuadro de caminata."""
    # cuadro 0 y 2: piernas juntas; 1 y 3: una adelante y otra atrás
    pierna = {0: (0, 0), 1: (1, -1), 2: (0, 0), 3: (-1, 1)}[cuadro]
    vaiven = -1 if cuadro in (1, 3) else 0
    return pierna, vaiven


def dibujar_frente_espalda(d, s, cuadro, mira_abajo):
    (pierna_a, pierna_b), vaiven = desplazamientos_caminata(cuadro)
    cx = CUADRO // 2
    pies_y = 28
    ancho_torso, grosor, ancho_linea, alto_linea = s["ancho_torso"], s["alto_torso"], s["ancho_pierna"], s["alto_pierna"]

    # Piernas (una se alarga y la otra se acorta al caminar)
    for costado, desplazamiento in ((-1, pierna_a), (1, pierna_b)):
        x = cx - ancho_linea - 1 if costado < 0 else cx + 1
        h = alto_linea + desplazamiento
        arriba = pies_y - alto_linea - 1
        caja_sombreada(d, x, arriba, ancho_linea, h + 1, s["traje"], s["traje_claro"], s["traje"])
        rect(d, x, arriba + h, ancho_linea, 1, s["armadura_sombra"])          # bota

    torso_arriba = pies_y - alto_linea - grosor + vaiven
    tx = cx - ancho_torso // 2

    # Mochila (se ve de espaldas) o detalle trasero
    if not mira_abajo and s["mochila"]:
        caja_sombreada(d, tx + 2, torso_arriba - 1, ancho_torso - 4, grosor, s["armadura_sombra"], s["armadura"], s["traje"])

    # Torso
    caja_sombreada(d, tx, torso_arriba, ancho_torso, grosor, s["armadura"], s["armadura_clara"], s["armadura_sombra"])
    rect(d, tx, torso_arriba + grosor - 2, ancho_torso, 1, s["traje"])           # cinturón
    if mira_abajo:
        rect(d, cx - 1, torso_arriba + 2, 2, 3, s["acento"])       # núcleo de energía
        rect(d, cx - 1, torso_arriba + 4, 2, 1, s["acento_oscuro"])
    else:
        rect(d, tx + 1, torso_arriba + 2, ancho_torso - 2, 1, s["acento_oscuro"])

    # Hombreras
    alto_hombro = s["hombro"]
    if alto_hombro:
        for x in (tx - alto_hombro + 1, tx + ancho_torso - 1):
            caja_sombreada(d, x, torso_arriba - 1, alto_hombro, alto_hombro + 1, s["armadura"], s["armadura_clara"], s["armadura_sombra"])
            rect(d, x, torso_arriba + alto_hombro - 1, alto_hombro, 1, s["acento"])

    # Brazos
    ancho_brazo = s["ancho_brazo"]
    brazo_arriba = torso_arriba + 1 + (alto_hombro // 2)
    for x in (tx - ancho_brazo, tx + ancho_torso):
        caja_sombreada(d, x, brazo_arriba, ancho_brazo, grosor - 2, s["traje"], s["traje_claro"], s["traje"])
        rect(d, x, brazo_arriba + grosor - 3, ancho_brazo, 1, s["armadura_sombra"])   # guante

    # Arma: de frente, sale hacia abajo desde la mano derecha
    if mira_abajo:
        # En vista frontal el arma se ve "en escorzo": más corta.
        largo_arma2 = max(4, s["largo_arma"] - 3)
        arma_x = tx + ancho_torso - 1
        gy = brazo_arriba + grosor - 4
        caja_sombreada(d, arma_x, gy, s["ancho_arma"], largo_arma2, s["arma"], s["arma_clara"], s["arma"])
        rect(d, arma_x, gy + largo_arma2 - 1, s["ancho_arma"], 1, s["acento"])

    # Cuello (separa la cabeza del torso)
    ancho_cabeza, alto_cabeza = s["ancho_cabeza"], s["alto_cabeza"]
    rect(d, cx - ancho_cabeza // 2 + 1, torso_arriba, ancho_cabeza - 2, 1, s["traje"])
    # Cabeza / casco
    hx = cx - ancho_cabeza // 2
    hy = torso_arriba - alto_cabeza
    caja_sombreada(d, hx, hy, ancho_cabeza, alto_cabeza, s["armadura"], s["armadura_clara"], s["armadura_sombra"])
    redondear_esquinas(d, hx, hy, ancho_cabeza, alto_cabeza)
    if mira_abajo:
        if s.get("ranura"):
            rect(d, hx + 1, hy + 3, ancho_cabeza - 2, 1, s["visor"])     # ranura del casco cerrado
            rect(d, cx - 1, hy + 4, 2, 1, s["acento_oscuro"])
        else:
            rect(d, hx + 1, hy + 3, ancho_cabeza - 2, 2, s["visor"])     # visor
            rect(d, hx + 1, hy + 3, 2, 1, rgba("#ffffff"))
    else:
        rect(d, hx + 1, hy + alto_cabeza - 3, ancho_cabeza - 2, 1, s["armadura_sombra"])
        rect(d, cx - 1, hy + 1, 2, alto_cabeza - 3, s["acento_oscuro"])   # cresta

    if s.get("holograma"):
        dibujar_holograma(d, s, hx, hy, ancho_cabeza, cuadro)


def dibujar_costado(d, s, cuadro, mira_derecha):
    pierna, vaiven = desplazamientos_caminata(cuadro)
    paso = pierna[0] * 2           # -2, 0 o 2 píxeles
    cx = CUADRO // 2
    pies_y = 28
    ancho_torso = max(6, s["ancho_torso"] - 4)
    grosor, ancho_linea, alto_linea = s["alto_torso"], s["ancho_pierna"], s["alto_pierna"]
    signo = 1 if mira_derecha else -1

    # Piernas: una adelante y otra atrás
    for desplazamiento, color in ((-paso, s["traje"]), (paso, s["traje_claro"])):
        x = cx - ancho_linea // 2 + desplazamiento * signo
        caja_sombreada(d, x, pies_y - alto_linea - 1, ancho_linea, alto_linea + 1, color, s["traje_claro"], s["traje"])
        rect(d, x + (1 if mira_derecha else -1), pies_y - 1, ancho_linea, 1, s["armadura_sombra"])

    torso_arriba = pies_y - alto_linea - grosor + vaiven
    tx = cx - ancho_torso // 2

    if s["mochila"]:
        px = tx - 4 if mira_derecha else tx + ancho_torso
        caja_sombreada(d, px, torso_arriba + 1, 4, grosor - 2, s["armadura_sombra"], s["armadura"], s["traje"])
        rect(d, px + 1, torso_arriba + 2, 2, 1, s["acento"])

    caja_sombreada(d, tx, torso_arriba, ancho_torso, grosor, s["armadura"], s["armadura_clara"], s["armadura_sombra"])
    rect(d, tx, torso_arriba + grosor - 2, ancho_torso, 1, s["traje"])
    rect(d, tx + (ancho_torso - 3 if mira_derecha else 1), torso_arriba + 2, 2, 2, s["acento"])

    alto_hombro = s["hombro"]
    if alto_hombro:
        caja_sombreada(d, cx - alto_hombro // 2 - 1, torso_arriba - 1, alto_hombro + 2, alto_hombro, s["armadura"], s["armadura_clara"], s["armadura_sombra"])
        rect(d, cx - alto_hombro // 2 - 1, torso_arriba + alto_hombro - 2, alto_hombro + 2, 1, s["acento"])

    # Arma apuntando hacia adelante
    arma_y = torso_arriba + grosor // 2 - 1
    largo_arma = s["largo_arma"]
    arma_x = cx + 1 if mira_derecha else cx - largo_arma - 1
    caja_sombreada(d, arma_x, arma_y, largo_arma, s["ancho_arma"], s["arma"], s["arma_clara"], s["arma"])
    rect(d, arma_x + (largo_arma - 1 if mira_derecha else 0), arma_y, 1, s["ancho_arma"], s["acento"])
    # Brazo sobre el arma
    ancho_brazo = s["ancho_brazo"]
    rect(d, cx - ancho_brazo // 2, arma_y - 1, ancho_brazo + 1, 3, s["traje_claro"])

    # Cabeza de perfil con visor hacia adelante
    ancho_cabeza, alto_cabeza = s["ancho_cabeza"] - 1, s["alto_cabeza"]
    hx = cx - ancho_cabeza // 2 + signo
    rect(d, cx - ancho_cabeza // 2 + 1, torso_arriba, ancho_cabeza - 2, 1, s["traje"])        # cuello
    hy = torso_arriba - alto_cabeza
    caja_sombreada(d, hx, hy, ancho_cabeza, alto_cabeza, s["armadura"], s["armadura_clara"], s["armadura_sombra"])
    redondear_esquinas(d, hx, hy, ancho_cabeza, alto_cabeza)
    vx = hx + ancho_cabeza - 3 if mira_derecha else hx
    if s.get("ranura"):
        rect(d, vx, hy + 3, 3, 1, s["visor"])
    else:
        rect(d, vx, hy + 3, 3, 2, s["visor"])

    if s.get("holograma"):
        dibujar_holograma(d, s, hx, hy, ancho_cabeza, cuadro)


def dibujar_holograma(d, s, hx, hy, ancho_cabeza, cuadro):
    """Dispositivos holográficos de Iris: un aro sobre la cabeza y un dron chico que flota."""
    vaiven = (0, -1, 0, 1)[cuadro]
    # Aro holográfico
    rect(d, hx + 1, hy - 2, ancho_cabeza - 2, 1, s["acento"][:3] + (200,))
    # Dron flotando arriba a la derecha
    dx, dy = hx + ancho_cabeza + 2, hy + 1 + vaiven
    rect(d, dx, dy, 3, 3, s["acento_oscuro"])
    d.point((dx + 1, dy + 1), fill=s["visor"])
    rect(d, dx - 1, dy + 1, 1, 1, s["acento"][:3] + (160,))


def agregar_contorno(imagen):
    """Agrega un contorno oscuro de 1 píxel alrededor de la figura."""
    w, h = imagen.size
    origen = imagen.load()
    salida = imagen.copy()
    destino = salida.load()
    for y in range(h):
        for x in range(w):
            if origen[x, y][3] != 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and origen[nx, ny][3] > 0:
                    destino[x, y] = CONTORNO
                    break
    return salida


def cuadro_personaje(especificacion, direccion, cuadro):
    cuerpo = Image.new("RGBA", (CUADRO, CUADRO), (0, 0, 0, 0))
    d = ImageDraw.Draw(cuerpo)
    if direccion in (ABAJO, ARRIBA):
        dibujar_frente_espalda(d, especificacion, cuadro, direccion == ABAJO)
    else:
        dibujar_costado(d, especificacion, cuadro, direccion == DERECHA)
    cuerpo = agregar_contorno(cuerpo)

    # Sombra en el piso, debajo de todo
    resultado = Image.new("RGBA", (CUADRO, CUADRO), (0, 0, 0, 0))
    sombra_d = ImageDraw.Draw(resultado)
    sombra_ancho = especificacion["ancho_torso"] + 6
    sombra_d.ellipse([16 - sombra_ancho // 2, 26, 16 + sombra_ancho // 2 - 1, 31], fill=(0, 0, 0, 90))
    resultado.alpha_composite(cuerpo)
    return resultado


def hoja_personaje(especificacion):
    """Spritesheet de 4 filas (abajo, izquierda, derecha, arriba) x 4 cuadros."""
    hoja = Image.new("RGBA", (CUADRO * 4, CUADRO * 4), (0, 0, 0, 0))
    for direccion in (ABAJO, IZQUIERDA, DERECHA, ARRIBA):
        for cuadro in range(4):
            hoja.alpha_composite(cuadro_personaje(especificacion, direccion, cuadro), (cuadro * CUADRO, direccion * CUADRO))
    return hoja


# ----------------------------------------------------------------------
# Efectos: proyectiles, escudo, anillo de equipo
# ----------------------------------------------------------------------

def bola_brillo(tamanio, radio_centro):
    """Bola de energía blanca con halo. Se tiñe con el color del equipo al dibujarla."""
    imagen = Image.new("RGBA", (tamanio, tamanio), (0, 0, 0, 0))
    px = imagen.load()
    c = (tamanio - 1) / 2
    for y in range(tamanio):
        for x in range(tamanio):
            distancia = math.hypot(x - c, y - c)
            if distancia <= radio_centro:
                px[x, y] = (255, 255, 255, 255)
            elif distancia <= tamanio / 2:
                t = (distancia - radio_centro) / (tamanio / 2 - radio_centro)
                px[x, y] = (230, 240, 255, int(200 * (1 - t) ** 1.5))
    return imagen


def proyectil_pesado(tamanio):
    """Proyectil pesado de Brutus: núcleo sólido con borde marcado."""
    imagen = bola_brillo(tamanio, tamanio * 0.30)
    d = ImageDraw.Draw(imagen)
    r = tamanio * 0.36
    c = (tamanio - 1) / 2
    d.ellipse([c - r, c - r, c + r, c + r], outline=(255, 255, 255, 255), width=1)
    return imagen


def arco_escudo():
    """Arco del Escudo Cinético, apuntando hacia la derecha (se rota al dibujarlo)."""
    tamanio = 48
    imagen = Image.new("RGBA", (tamanio, tamanio), (0, 0, 0, 0))
    px = imagen.load()
    c = (tamanio - 1) / 2
    for y in range(tamanio):
        for x in range(tamanio):
            dx, dy = x - c, y - c
            distancia = math.hypot(dx, dy)
            angulo = abs(math.degrees(math.atan2(dy, dx)))
            if 16 <= distancia <= 22.5 and angulo <= 66:
                desvanecido = 1 - (angulo / 66) ** 4
                if distancia >= 21 or distancia <= 17:
                    px[x, y] = (255, 236, 190, int(255 * desvanecido))          # bordes brillantes
                else:
                    px[x, y] = (255, 150, 40, int(150 * desvanecido))           # interior traslúcido
    return imagen


def bola_pulso(tamanio):
    """Pulso de energía de Iris: anillo con centro brillante (se tiñe al dibujarlo)."""
    imagen = bola_brillo(tamanio, tamanio * 0.16)
    d = ImageDraw.Draw(imagen)
    r = tamanio * 0.32
    c = (tamanio - 1) / 2
    d.ellipse([c - r, c - r, c + r, c + r], outline=(255, 255, 255, 230), width=1)
    return imagen


def dardo(tamanio):
    """Dardo de Phantom: un rombo fino y muy brillante."""
    imagen = bola_brillo(tamanio, 0.8)
    d = ImageDraw.Draw(imagen)
    c = (tamanio - 1) / 2
    d.polygon([(c, 0), (c + tamanio * 0.18, c), (c, tamanio - 1), (c - tamanio * 0.18, c)], fill=(255, 255, 255, 255))
    d.polygon([(0, c), (c, c - tamanio * 0.12), (tamanio - 1, c), (c, c + tamanio * 0.12)], fill=(255, 255, 255, 200))
    return imagen


def zona_regeneracion():
    """Campo de Regeneración de Iris (blanco: se tiñe de verde al dibujarlo)."""
    tamanio = 128
    imagen = Image.new("RGBA", (tamanio, tamanio), (0, 0, 0, 0))
    px = imagen.load()
    c = (tamanio - 1) / 2
    R = tamanio / 2 - 1
    for y in range(tamanio):
        for x in range(tamanio):
            distancia = math.hypot(x - c, y - c)
            if distancia > R:
                continue
            t = distancia / R
            a = 40 + 70 * t ** 3                       # más intenso hacia el borde
            if distancia >= R - 2.5:
                a = 230                                 # borde
            elif abs(distancia - R * 0.62) < 0.8:
                a = 110                                 # anillo interior
            px[x, y] = (255, 255, 255, int(a))
    d = ImageDraw.Draw(imagen)
    # Cruces de curación repartidas en el círculo
    for angulo in range(0, 360, 60):
        a = math.radians(angulo + 30)
        x, y = c + math.cos(a) * R * 0.8, c + math.sin(a) * R * 0.8
        d.rectangle([x - 1, y - 4, x + 1, y + 4], fill=(255, 255, 255, 200))
        d.rectangle([x - 4, y - 1, x + 4, y + 1], fill=(255, 255, 255, 200))
    d.rectangle([c - 3, c - 10, c + 3, c + 10], fill=(255, 255, 255, 150))
    d.rectangle([c - 10, c - 3, c + 10, c + 3], fill=(255, 255, 255, 150))
    return imagen


def anillo_equipo():
    """Anillo blanco que se dibuja bajo los pies, teñido con el color del equipo."""
    imagen = Image.new("RGBA", (32, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(imagen)
    d.ellipse([1, 2, 30, 13], outline=(255, 255, 255, 230), width=2)
    return imagen


# ----------------------------------------------------------------------
# Mapa
# ----------------------------------------------------------------------

def baldosa_piso():
    """Baldosa metálica de 64x64 que se repite sin cortes."""
    azar = random.Random(7)
    tamanio = 64
    imagen = Image.new("RGBA", (tamanio, tamanio), rgba("#1b2129"))
    d = ImageDraw.Draw(imagen)
    # Cuatro placas de 32x32 con juntas
    for py in (0, 32):
        for px in (0, 32):
            base = rgba("#222932") if (px + py) % 64 == 0 else rgba("#1f262e")
            d.rectangle([px + 1, py + 1, px + 30, py + 30], fill=base)
            d.line([px + 1, py + 1, px + 30, py + 1], fill=rgba("#2b333d"))
            d.line([px + 1, py + 1, px + 1, py + 30], fill=rgba("#2b333d"))
            d.line([px + 1, py + 30, px + 30, py + 30], fill=rgba("#161b21"))
            d.line([px + 30, py + 1, px + 30, py + 30], fill=rgba("#161b21"))
            for bx, by in ((3, 3), (28, 3), (3, 28), (28, 28)):   # remaches
                imagen.putpixel((px + bx, py + by), rgba("#39424d"))
    # Desgaste leve
    for _ in range(140):
        x, y = azar.randrange(tamanio), azar.randrange(tamanio)
        r, g, b, a = imagen.getpixel((x, y))
        k = azar.choice((-6, -4, 4, 6))
        imagen.putpixel((x, y), (max(0, r + k), max(0, g + k), max(0, b + k), a))
    # Línea de energía tenue en una de las juntas
    d.line([0, 32, tamanio - 1, 32], fill=rgba("#163a48"))
    return imagen


def baldosa_pared():
    """Pared perimetral de 20x20 que se repite a lo largo y a lo alto."""
    tamanio = 20
    imagen = Image.new("RGBA", (tamanio, tamanio), rgba("#3a424d"))
    d = ImageDraw.Draw(imagen)
    d.rectangle([0, 0, tamanio - 1, 2], fill=rgba("#55606d"))
    d.rectangle([0, tamanio - 3, tamanio - 1, tamanio - 1], fill=rgba("#262c34"))
    d.rectangle([0, 8, tamanio - 1, 11], fill=rgba("#2c333c"))
    for x in range(0, tamanio, 5):                        # franja de advertencia
        d.polygon([(x, 8), (x + 2, 8), (x + 4, 11), (x + 2, 11)], fill=rgba("#c9a227"))
    d.line([0, 0, 0, tamanio - 1], fill=rgba("#2a3038"))
    return imagen


def bloque_cobertura():
    """Bloque de cobertura de 48x48: contenedor reforzado con un Núcleo Over."""
    tamanio = 48
    imagen = Image.new("RGBA", (tamanio, tamanio), (0, 0, 0, 0))
    d = ImageDraw.Draw(imagen)
    d.rectangle([0, 0, tamanio - 1, tamanio - 1], fill=rgba("#11151a"))
    d.rectangle([1, 1, tamanio - 2, tamanio - 2], fill=rgba("#4a535e"))
    d.rectangle([1, 1, tamanio - 2, 3], fill=rgba("#6a7581"))
    d.rectangle([1, 1, 3, tamanio - 2], fill=rgba("#5d6773"))
    d.rectangle([1, tamanio - 4, tamanio - 2, tamanio - 2], fill=rgba("#323941"))
    d.rectangle([tamanio - 4, 1, tamanio - 2, tamanio - 2], fill=rgba("#373e47"))
    d.rectangle([8, 8, tamanio - 9, tamanio - 9], fill=rgba("#3b434d"), outline=rgba("#2a3038"))
    for c in ((5, 5), (tamanio - 6, 5), (5, tamanio - 6), (tamanio - 6, tamanio - 6)):
        d.rectangle([c[0] - 1, c[1] - 1, c[0] + 1, c[1] + 1], fill=rgba("#7c8794"))
    # Núcleo Over en el centro
    cx = cy = tamanio // 2
    d.ellipse([cx - 7, cy - 7, cx + 6, cy + 6], fill=rgba("#1a2e38"), outline=rgba("#0f1b22"))
    d.ellipse([cx - 4, cy - 4, cx + 3, cy + 3], fill=rgba("#28c8e8"))
    d.ellipse([cx - 2, cy - 2, cx + 1, cy + 1], fill=rgba("#bff6ff"))
    return imagen


def decorado_centro():
    """Marca del Núcleo Over que se pinta en el centro del piso."""
    tamanio = 96
    imagen = Image.new("RGBA", (tamanio, tamanio), (0, 0, 0, 0))
    d = ImageDraw.Draw(imagen)
    c = tamanio // 2
    d.ellipse([4, 4, tamanio - 5, tamanio - 5], outline=rgba("#1d5566", 200), width=2)
    d.ellipse([16, 16, tamanio - 17, tamanio - 17], outline=rgba("#1a4756", 160), width=1)
    for angulo in range(0, 360, 45):
        r1, r2 = 20, 44
        a = math.radians(angulo)
        d.line([c + r1 * math.cos(a), c + r1 * math.sin(a), c + r2 * math.cos(a), c + r2 * math.sin(a)],
               fill=rgba("#1a4756", 150), width=1)
    d.ellipse([c - 10, c - 10, c + 9, c + 9], fill=rgba("#123642", 220), outline=rgba("#2ab7d6", 220))
    d.ellipse([c - 4, c - 4, c + 3, c + 3], fill=rgba("#7ee9ff", 230))
    return imagen


def zona_aparicion(color):
    """Zona de aparición de un equipo (se pinta en el piso)."""
    w, h = 72, 168
    imagen = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(imagen)
    d.rectangle([0, 0, w - 1, h - 1], fill=color[:3] + (28,), outline=color[:3] + (120,))
    for y in range(6, h - 6, 12):
        d.line([4, y, 10, y + 6], fill=color[:3] + (70,))
        d.line([w - 11, y, w - 5, y + 6], fill=color[:3] + (70,))
    return imagen


def principal():
    os.makedirs(RECURSOS, exist_ok=True)
    salida = {
        "volt_walk.png": hoja_personaje(VOLT),
        "brutus_walk.png": hoja_personaje(BRUTUS),
        "iris_walk.png": hoja_personaje(IRIS),
        "phantom_walk.png": hoja_personaje(PHANTOM),
        "projectile_volt.png": bola_brillo(8, 1.8),
        "projectile_brutus.png": proyectil_pesado(12),
        "projectile_iris.png": bola_pulso(9),
        "projectile_phantom.png": dardo(9),
        "regen_zone.png": zona_regeneracion(),
        "shield.png": arco_escudo(),
        "team_ring.png": anillo_equipo(),
        "floor_tile.png": baldosa_piso(),
        "wall_tile.png": baldosa_pared(),
        "wall_tile_v.png": baldosa_pared().rotate(90),       # para las paredes verticales
        "cover_block.png": bloque_cobertura(),
        "core_decal.png": decorado_centro(),
        "spawn_blue.png": zona_aparicion(rgba("#3d8bff")),
        "spawn_red.png": zona_aparicion(rgba("#ff4d4d")),
    }
    for nombre, imagen in salida.items():
        imagen.save(os.path.join(RECURSOS, nombre))
        print("generado:", nombre, imagen.size)


if __name__ == "__main__":
    principal()
