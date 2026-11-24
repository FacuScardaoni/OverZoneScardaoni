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

ASSETS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "assets")
FRAME = 32
DOWN, LEFT, RIGHT, UP = 0, 1, 2, 3
OUTLINE = (14, 16, 22, 255)


def rgba(hex_color, alpha=255):
    hex_color = hex_color.lstrip("#")
    return tuple(int(hex_color[i:i + 2], 16) for i in (0, 2, 4)) + (alpha,)


# ----------------------------------------------------------------------
# Personajes
# ----------------------------------------------------------------------

# Colores de cada facción, según la propuesta:
#   CyberCore (Volt):           azul, celeste y violeta
#   Titan Force (Brutus):       gris metálico, rojo y negro
#   Aegis Protocol (Iris):      blanco, verde y cian
#   Shadow Syndicate (Phantom): negro, rojo oscuro y detalles luminosos

VOLT = {
    "armor": rgba("#3f7fe0"), "armor_shade": rgba("#24519c"), "armor_light": rgba("#8cc0ff"),
    "suit": rgba("#232a44"), "suit_light": rgba("#36406a"),
    "accent": rgba("#3ff2ff"), "accent_dark": rgba("#8a5cff"),
    "visor": rgba("#6ff6ff"), "gun": rgba("#4a5563"), "gun_light": rgba("#7d8a99"),
    "torso_w": 10, "torso_h": 8, "head_w": 8, "head_h": 7, "leg_w": 3, "leg_h": 5,
    "shoulder": 0, "arm_w": 2, "gun_w": 2, "gun_len": 7, "pack": False,
}

BRUTUS = {
    "armor": rgba("#6a737e"), "armor_shade": rgba("#3e444d"), "armor_light": rgba("#929ca8"),
    "suit": rgba("#1d1c1f"), "suit_light": rgba("#302e33"),
    "accent": rgba("#ff3b30"), "accent_dark": rgba("#a3150f"),
    "visor": rgba("#ff6a4d"), "gun": rgba("#3f464f"), "gun_light": rgba("#808a96"),
    "torso_w": 16, "torso_h": 9, "head_w": 10, "head_h": 6, "leg_w": 4, "leg_h": 5,
    "shoulder": 4, "arm_w": 3, "gun_w": 4, "gun_len": 9, "pack": True,
}

IRIS = {
    "armor": rgba("#eef3f1"), "armor_shade": rgba("#a9b8b3"), "armor_light": rgba("#ffffff"),
    "suit": rgba("#2c3b38"), "suit_light": rgba("#3f5450"),
    "accent": rgba("#3dff8f"), "accent_dark": rgba("#17a85a"),
    "visor": rgba("#5ff7ff"), "gun": rgba("#c9d6d2"), "gun_light": rgba("#ffffff"),
    "torso_w": 9, "torso_h": 8, "head_w": 8, "head_h": 7, "leg_w": 3, "leg_h": 5,
    "shoulder": 0, "arm_w": 2, "gun_w": 2, "gun_len": 5, "pack": False,
    "holo": True,              # dispositivos holográficos flotando a su lado
}

PHANTOM = {
    "armor": rgba("#23242b"), "armor_shade": rgba("#121318"), "armor_light": rgba("#464955"),
    "suit": rgba("#15161a"), "suit_light": rgba("#2a2b32"),
    "accent": rgba("#ff2a3d"), "accent_dark": rgba("#8a0f1c"),
    "visor": rgba("#ff3348"), "gun": rgba("#2c2e36"), "gun_light": rgba("#5b5f6b"),
    "torso_w": 9, "torso_h": 8, "head_w": 8, "head_h": 7, "leg_w": 3, "leg_h": 6,
    "shoulder": 0, "arm_w": 2, "gun_w": 2, "gun_len": 6, "pack": False,
    "slit": True,              # casco cerrado: solo una ranura roja como visor
}


def rect(d, x0, y0, w, h, color):
    if w > 0 and h > 0:
        d.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], fill=color)


def shaded_box(d, x0, y0, w, h, base, light, shade):
    """Caja con luz arriba a la izquierda y sombra abajo a la derecha."""
    rect(d, x0, y0, w, h, base)
    rect(d, x0, y0, w, 1, light)
    rect(d, x0, y0, 1, h, light)
    rect(d, x0, y0 + h - 1, w, 1, shade)
    rect(d, x0 + w - 1, y0, 1, h, shade)


def round_corners(d, x0, y0, w, h):
    """Borra las 4 esquinas de una caja para que se vea redondeada."""
    for x, y in ((x0, y0), (x0 + w - 1, y0), (x0, y0 + h - 1), (x0 + w - 1, y0 + h - 1)):
        d.point((x, y), fill=(0, 0, 0, 0))


def walk_offsets(frame):
    """Desplazamiento de cada pierna y del cuerpo según el cuadro de caminata."""
    # cuadro 0 y 2: piernas juntas; 1 y 3: una adelante y otra atrás
    leg = {0: (0, 0), 1: (1, -1), 2: (0, 0), 3: (-1, 1)}[frame]
    bob = -1 if frame in (1, 3) else 0
    return leg, bob


def draw_front_back(d, s, frame, facing_down):
    (leg_a, leg_b), bob = walk_offsets(frame)
    cx = FRAME // 2
    feet_y = 28
    tw, th, lw, lh = s["torso_w"], s["torso_h"], s["leg_w"], s["leg_h"]

    # Piernas (una se alarga y la otra se acorta al caminar)
    for side, off in ((-1, leg_a), (1, leg_b)):
        x = cx - lw - 1 if side < 0 else cx + 1
        h = lh + off
        top = feet_y - lh - 1
        shaded_box(d, x, top, lw, h + 1, s["suit"], s["suit_light"], s["suit"])
        rect(d, x, top + h, lw, 1, s["armor_shade"])          # bota

    torso_top = feet_y - lh - th + bob
    tx = cx - tw // 2

    # Mochila (se ve de espaldas) o detalle trasero
    if not facing_down and s["pack"]:
        shaded_box(d, tx + 2, torso_top - 1, tw - 4, th, s["armor_shade"], s["armor"], s["suit"])

    # Torso
    shaded_box(d, tx, torso_top, tw, th, s["armor"], s["armor_light"], s["armor_shade"])
    rect(d, tx, torso_top + th - 2, tw, 1, s["suit"])           # cinturón
    if facing_down:
        rect(d, cx - 1, torso_top + 2, 2, 3, s["accent"])       # núcleo de energía
        rect(d, cx - 1, torso_top + 4, 2, 1, s["accent_dark"])
    else:
        rect(d, tx + 1, torso_top + 2, tw - 2, 1, s["accent_dark"])

    # Hombreras
    sh = s["shoulder"]
    if sh:
        for x in (tx - sh + 1, tx + tw - 1):
            shaded_box(d, x, torso_top - 1, sh, sh + 1, s["armor"], s["armor_light"], s["armor_shade"])
            rect(d, x, torso_top + sh - 1, sh, 1, s["accent"])

    # Brazos
    aw = s["arm_w"]
    arm_top = torso_top + 1 + (sh // 2)
    for x in (tx - aw, tx + tw):
        shaded_box(d, x, arm_top, aw, th - 2, s["suit"], s["suit_light"], s["suit"])
        rect(d, x, arm_top + th - 3, aw, 1, s["armor_shade"])   # guante

    # Arma: de frente, sale hacia abajo desde la mano derecha
    if facing_down:
        # En vista frontal el arma se ve "en escorzo": más corta.
        glen = max(4, s["gun_len"] - 3)
        gx = tx + tw - 1
        gy = arm_top + th - 4
        shaded_box(d, gx, gy, s["gun_w"], glen, s["gun"], s["gun_light"], s["gun"])
        rect(d, gx, gy + glen - 1, s["gun_w"], 1, s["accent"])

    # Cuello (separa la cabeza del torso)
    hw, hh = s["head_w"], s["head_h"]
    rect(d, cx - hw // 2 + 1, torso_top, hw - 2, 1, s["suit"])
    # Cabeza / casco
    hx = cx - hw // 2
    hy = torso_top - hh
    shaded_box(d, hx, hy, hw, hh, s["armor"], s["armor_light"], s["armor_shade"])
    round_corners(d, hx, hy, hw, hh)
    if facing_down:
        if s.get("slit"):
            rect(d, hx + 1, hy + 3, hw - 2, 1, s["visor"])     # ranura del casco cerrado
            rect(d, cx - 1, hy + 4, 2, 1, s["accent_dark"])
        else:
            rect(d, hx + 1, hy + 3, hw - 2, 2, s["visor"])     # visor
            rect(d, hx + 1, hy + 3, 2, 1, rgba("#ffffff"))
    else:
        rect(d, hx + 1, hy + hh - 3, hw - 2, 1, s["armor_shade"])
        rect(d, cx - 1, hy + 1, 2, hh - 3, s["accent_dark"])   # cresta

    if s.get("holo"):
        draw_holo(d, s, hx, hy, hw, frame)


def draw_side(d, s, frame, facing_right):
    leg, bob = walk_offsets(frame)
    step = leg[0] * 2           # -2, 0 o 2 píxeles
    cx = FRAME // 2
    feet_y = 28
    tw = max(6, s["torso_w"] - 4)
    th, lw, lh = s["torso_h"], s["leg_w"], s["leg_h"]
    sign = 1 if facing_right else -1

    # Piernas: una adelante y otra atrás
    for off, color in ((-step, s["suit"]), (step, s["suit_light"])):
        x = cx - lw // 2 + off * sign
        shaded_box(d, x, feet_y - lh - 1, lw, lh + 1, color, s["suit_light"], s["suit"])
        rect(d, x + (1 if facing_right else -1), feet_y - 1, lw, 1, s["armor_shade"])

    torso_top = feet_y - lh - th + bob
    tx = cx - tw // 2

    if s["pack"]:
        px = tx - 4 if facing_right else tx + tw
        shaded_box(d, px, torso_top + 1, 4, th - 2, s["armor_shade"], s["armor"], s["suit"])
        rect(d, px + 1, torso_top + 2, 2, 1, s["accent"])

    shaded_box(d, tx, torso_top, tw, th, s["armor"], s["armor_light"], s["armor_shade"])
    rect(d, tx, torso_top + th - 2, tw, 1, s["suit"])
    rect(d, tx + (tw - 3 if facing_right else 1), torso_top + 2, 2, 2, s["accent"])

    sh = s["shoulder"]
    if sh:
        shaded_box(d, cx - sh // 2 - 1, torso_top - 1, sh + 2, sh, s["armor"], s["armor_light"], s["armor_shade"])
        rect(d, cx - sh // 2 - 1, torso_top + sh - 2, sh + 2, 1, s["accent"])

    # Arma apuntando hacia adelante
    gun_y = torso_top + th // 2 - 1
    gl = s["gun_len"]
    gx = cx + 1 if facing_right else cx - gl - 1
    shaded_box(d, gx, gun_y, gl, s["gun_w"], s["gun"], s["gun_light"], s["gun"])
    rect(d, gx + (gl - 1 if facing_right else 0), gun_y, 1, s["gun_w"], s["accent"])
    # Brazo sobre el arma
    aw = s["arm_w"]
    rect(d, cx - aw // 2, gun_y - 1, aw + 1, 3, s["suit_light"])

    # Cabeza de perfil con visor hacia adelante
    hw, hh = s["head_w"] - 1, s["head_h"]
    hx = cx - hw // 2 + sign
    rect(d, cx - hw // 2 + 1, torso_top, hw - 2, 1, s["suit"])        # cuello
    hy = torso_top - hh
    shaded_box(d, hx, hy, hw, hh, s["armor"], s["armor_light"], s["armor_shade"])
    round_corners(d, hx, hy, hw, hh)
    vx = hx + hw - 3 if facing_right else hx
    if s.get("slit"):
        rect(d, vx, hy + 3, 3, 1, s["visor"])
    else:
        rect(d, vx, hy + 3, 3, 2, s["visor"])

    if s.get("holo"):
        draw_holo(d, s, hx, hy, hw, frame)


def draw_holo(d, s, hx, hy, hw, frame):
    """Dispositivos holográficos de Iris: un aro sobre la cabeza y un dron chico que flota."""
    bob = (0, -1, 0, 1)[frame]
    # Aro holográfico
    rect(d, hx + 1, hy - 2, hw - 2, 1, s["accent"][:3] + (200,))
    # Dron flotando arriba a la derecha
    dx, dy = hx + hw + 2, hy + 1 + bob
    rect(d, dx, dy, 3, 3, s["accent_dark"])
    d.point((dx + 1, dy + 1), fill=s["visor"])
    rect(d, dx - 1, dy + 1, 1, 1, s["accent"][:3] + (160,))


def add_outline(img):
    """Agrega un contorno oscuro de 1 píxel alrededor de la figura."""
    w, h = img.size
    src = img.load()
    out = img.copy()
    dst = out.load()
    for y in range(h):
        for x in range(w):
            if src[x, y][3] != 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and src[nx, ny][3] > 0:
                    dst[x, y] = OUTLINE
                    break
    return out


def character_frame(spec, direction, frame):
    body = Image.new("RGBA", (FRAME, FRAME), (0, 0, 0, 0))
    d = ImageDraw.Draw(body)
    if direction in (DOWN, UP):
        draw_front_back(d, spec, frame, direction == DOWN)
    else:
        draw_side(d, spec, frame, direction == RIGHT)
    body = add_outline(body)

    # Sombra en el piso, debajo de todo
    result = Image.new("RGBA", (FRAME, FRAME), (0, 0, 0, 0))
    sd = ImageDraw.Draw(result)
    sw = spec["torso_w"] + 6
    sd.ellipse([16 - sw // 2, 26, 16 + sw // 2 - 1, 31], fill=(0, 0, 0, 90))
    result.alpha_composite(body)
    return result


def character_sheet(spec):
    """Spritesheet de 4 filas (abajo, izquierda, derecha, arriba) x 4 cuadros."""
    sheet = Image.new("RGBA", (FRAME * 4, FRAME * 4), (0, 0, 0, 0))
    for direction in (DOWN, LEFT, RIGHT, UP):
        for frame in range(4):
            sheet.alpha_composite(character_frame(spec, direction, frame), (frame * FRAME, direction * FRAME))
    return sheet


# ----------------------------------------------------------------------
# Efectos: proyectiles, escudo, anillo de equipo
# ----------------------------------------------------------------------

def glow_ball(size, core_radius):
    """Bola de energía blanca con halo. Se tiñe con el color del equipo al dibujarla."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            dist = math.hypot(x - c, y - c)
            if dist <= core_radius:
                px[x, y] = (255, 255, 255, 255)
            elif dist <= size / 2:
                t = (dist - core_radius) / (size / 2 - core_radius)
                px[x, y] = (230, 240, 255, int(200 * (1 - t) ** 1.5))
    return img


def heavy_shell(size):
    """Proyectil pesado de Brutus: núcleo sólido con borde marcado."""
    img = glow_ball(size, size * 0.30)
    d = ImageDraw.Draw(img)
    r = size * 0.36
    c = (size - 1) / 2
    d.ellipse([c - r, c - r, c + r, c + r], outline=(255, 255, 255, 255), width=1)
    return img


def shield_arc():
    """Arco del Escudo Cinético, apuntando hacia la derecha (se rota al dibujarlo)."""
    size = 48
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            dx, dy = x - c, y - c
            dist = math.hypot(dx, dy)
            ang = abs(math.degrees(math.atan2(dy, dx)))
            if 16 <= dist <= 22.5 and ang <= 66:
                fade = 1 - (ang / 66) ** 4
                if dist >= 21 or dist <= 17:
                    px[x, y] = (255, 236, 190, int(255 * fade))          # bordes brillantes
                else:
                    px[x, y] = (255, 150, 40, int(150 * fade))           # interior traslúcido
    return img


def pulse_ball(size):
    """Pulso de energía de Iris: anillo con centro brillante (se tiñe al dibujarlo)."""
    img = glow_ball(size, size * 0.16)
    d = ImageDraw.Draw(img)
    r = size * 0.32
    c = (size - 1) / 2
    d.ellipse([c - r, c - r, c + r, c + r], outline=(255, 255, 255, 230), width=1)
    return img


def dart(size):
    """Dardo de Phantom: un rombo fino y muy brillante."""
    img = glow_ball(size, 0.8)
    d = ImageDraw.Draw(img)
    c = (size - 1) / 2
    d.polygon([(c, 0), (c + size * 0.18, c), (c, size - 1), (c - size * 0.18, c)], fill=(255, 255, 255, 255))
    d.polygon([(0, c), (c, c - size * 0.12), (size - 1, c), (c, c + size * 0.12)], fill=(255, 255, 255, 200))
    return img


def regen_zone():
    """Campo de Regeneración de Iris (blanco: se tiñe de verde al dibujarlo)."""
    size = 128
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    c = (size - 1) / 2
    R = size / 2 - 1
    for y in range(size):
        for x in range(size):
            dist = math.hypot(x - c, y - c)
            if dist > R:
                continue
            t = dist / R
            a = 40 + 70 * t ** 3                       # más intenso hacia el borde
            if dist >= R - 2.5:
                a = 230                                 # borde
            elif abs(dist - R * 0.62) < 0.8:
                a = 110                                 # anillo interior
            px[x, y] = (255, 255, 255, int(a))
    d = ImageDraw.Draw(img)
    # Cruces de curación repartidas en el círculo
    for ang in range(0, 360, 60):
        a = math.radians(ang + 30)
        x, y = c + math.cos(a) * R * 0.8, c + math.sin(a) * R * 0.8
        d.rectangle([x - 1, y - 4, x + 1, y + 4], fill=(255, 255, 255, 200))
        d.rectangle([x - 4, y - 1, x + 4, y + 1], fill=(255, 255, 255, 200))
    d.rectangle([c - 3, c - 10, c + 3, c + 10], fill=(255, 255, 255, 150))
    d.rectangle([c - 10, c - 3, c + 10, c + 3], fill=(255, 255, 255, 150))
    return img


def team_ring():
    """Anillo blanco que se dibuja bajo los pies, teñido con el color del equipo."""
    img = Image.new("RGBA", (32, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([1, 2, 30, 13], outline=(255, 255, 255, 230), width=2)
    return img


# ----------------------------------------------------------------------
# Mapa
# ----------------------------------------------------------------------

def floor_tile():
    """Baldosa metálica de 64x64 que se repite sin cortes."""
    rnd = random.Random(7)
    size = 64
    img = Image.new("RGBA", (size, size), rgba("#1b2129"))
    d = ImageDraw.Draw(img)
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
                img.putpixel((px + bx, py + by), rgba("#39424d"))
    # Desgaste leve
    for _ in range(140):
        x, y = rnd.randrange(size), rnd.randrange(size)
        r, g, b, a = img.getpixel((x, y))
        k = rnd.choice((-6, -4, 4, 6))
        img.putpixel((x, y), (max(0, r + k), max(0, g + k), max(0, b + k), a))
    # Línea de energía tenue en una de las juntas
    d.line([0, 32, size - 1, 32], fill=rgba("#163a48"))
    return img


def wall_tile():
    """Pared perimetral de 20x20 que se repite a lo largo y a lo alto."""
    size = 20
    img = Image.new("RGBA", (size, size), rgba("#3a424d"))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, size - 1, 2], fill=rgba("#55606d"))
    d.rectangle([0, size - 3, size - 1, size - 1], fill=rgba("#262c34"))
    d.rectangle([0, 8, size - 1, 11], fill=rgba("#2c333c"))
    for x in range(0, size, 5):                        # franja de advertencia
        d.polygon([(x, 8), (x + 2, 8), (x + 4, 11), (x + 2, 11)], fill=rgba("#c9a227"))
    d.line([0, 0, 0, size - 1], fill=rgba("#2a3038"))
    return img


def cover_block():
    """Bloque de cobertura de 48x48: contenedor reforzado con un Núcleo Over."""
    size = 48
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, size - 1, size - 1], fill=rgba("#11151a"))
    d.rectangle([1, 1, size - 2, size - 2], fill=rgba("#4a535e"))
    d.rectangle([1, 1, size - 2, 3], fill=rgba("#6a7581"))
    d.rectangle([1, 1, 3, size - 2], fill=rgba("#5d6773"))
    d.rectangle([1, size - 4, size - 2, size - 2], fill=rgba("#323941"))
    d.rectangle([size - 4, 1, size - 2, size - 2], fill=rgba("#373e47"))
    d.rectangle([8, 8, size - 9, size - 9], fill=rgba("#3b434d"), outline=rgba("#2a3038"))
    for c in ((5, 5), (size - 6, 5), (5, size - 6), (size - 6, size - 6)):
        d.rectangle([c[0] - 1, c[1] - 1, c[0] + 1, c[1] + 1], fill=rgba("#7c8794"))
    # Núcleo Over en el centro
    cx = cy = size // 2
    d.ellipse([cx - 7, cy - 7, cx + 6, cy + 6], fill=rgba("#1a2e38"), outline=rgba("#0f1b22"))
    d.ellipse([cx - 4, cy - 4, cx + 3, cy + 3], fill=rgba("#28c8e8"))
    d.ellipse([cx - 2, cy - 2, cx + 1, cy + 1], fill=rgba("#bff6ff"))
    return img


def core_decal():
    """Marca del Núcleo Over que se pinta en el centro del piso."""
    size = 96
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = size // 2
    d.ellipse([4, 4, size - 5, size - 5], outline=rgba("#1d5566", 200), width=2)
    d.ellipse([16, 16, size - 17, size - 17], outline=rgba("#1a4756", 160), width=1)
    for ang in range(0, 360, 45):
        r1, r2 = 20, 44
        a = math.radians(ang)
        d.line([c + r1 * math.cos(a), c + r1 * math.sin(a), c + r2 * math.cos(a), c + r2 * math.sin(a)],
               fill=rgba("#1a4756", 150), width=1)
    d.ellipse([c - 10, c - 10, c + 9, c + 9], fill=rgba("#123642", 220), outline=rgba("#2ab7d6", 220))
    d.ellipse([c - 4, c - 4, c + 3, c + 3], fill=rgba("#7ee9ff", 230))
    return img


def spawn_zone(color):
    """Zona de aparición de un equipo (se pinta en el piso)."""
    w, h = 72, 168
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, w - 1, h - 1], fill=color[:3] + (28,), outline=color[:3] + (120,))
    for y in range(6, h - 6, 12):
        d.line([4, y, 10, y + 6], fill=color[:3] + (70,))
        d.line([w - 11, y, w - 5, y + 6], fill=color[:3] + (70,))
    return img


def main():
    os.makedirs(ASSETS, exist_ok=True)
    out = {
        "volt_walk.png": character_sheet(VOLT),
        "brutus_walk.png": character_sheet(BRUTUS),
        "iris_walk.png": character_sheet(IRIS),
        "phantom_walk.png": character_sheet(PHANTOM),
        "projectile_volt.png": glow_ball(8, 1.8),
        "projectile_brutus.png": heavy_shell(12),
        "projectile_iris.png": pulse_ball(9),
        "projectile_phantom.png": dart(9),
        "regen_zone.png": regen_zone(),
        "shield.png": shield_arc(),
        "team_ring.png": team_ring(),
        "floor_tile.png": floor_tile(),
        "wall_tile.png": wall_tile(),
        "wall_tile_v.png": wall_tile().rotate(90),       # para las paredes verticales
        "cover_block.png": cover_block(),
        "core_decal.png": core_decal(),
        "spawn_blue.png": spawn_zone(rgba("#3d8bff")),
        "spawn_red.png": spawn_zone(rgba("#ff4d4d")),
    }
    for name, img in out.items():
        img.save(os.path.join(ASSETS, name))
        print("generado:", name, img.size)


if __name__ == "__main__":
    main()
