#!/usr/bin/env python3
"""
Generatore di Sprite RETRO PIXEL ART 16-Bit / 32-Bit per Ferro e Rimbalzi.
Disegna su matrici a bassa risoluzione (griglia pixel nativa) e scala con NEAREST NEIGHBOR.
ZERO grafica vettoriale: solo pixel puri, palette controllata a 32 colori, contorni a blocchi e dithering.
"""
import pathlib
import math
from PIL import Image, ImageDraw

OUT_DIR = pathlib.Path(__file__).resolve().parent.parent / 'android/app/src/main/res/drawable'
OUT_DIR.mkdir(parents=True, exist_ok=True)

# Palette Fantasy 32-Colori (Stile 16-bit Dawnbringer / Endesga)
PAL = {
    'void': (0, 0, 0, 0),
    'black': (18, 14, 16, 255),
    'white': (255, 255, 255, 255),
    'stone_shadow': (32, 26, 42, 255),
    'stone_dark': (52, 45, 68, 255),
    'stone_mid': (82, 75, 102, 255),
    'stone_light': (132, 124, 153, 255),
    'stone_high': (186, 178, 204, 255),
    'iron_shadow': (24, 30, 44, 255),
    'iron_dark': (48, 58, 82, 255),
    'iron_mid': (88, 106, 142, 255),
    'iron_light': (148, 170, 210, 255),
    'iron_white': (224, 236, 255, 255),
    'gold_shadow': (115, 60, 10, 255),
    'gold_dark': (168, 92, 16, 255),
    'gold_mid': (228, 150, 30, 255),
    'gold_light': (252, 210, 60, 255),
    'gold_high': (255, 246, 150, 255),
    'wood_shadow': (36, 18, 14, 255),
    'wood_dark': (68, 36, 26, 255),
    'wood_mid': (112, 60, 42, 255),
    'wood_light': (164, 94, 66, 255),
    'fire_shadow': (120, 20, 30, 255),
    'fire_dark': (180, 35, 30, 255),
    'fire_mid': (235, 75, 30, 255),
    'fire_orange': (250, 130, 35, 255),
    'fire_yellow': (255, 215, 65, 255),
    'fire_white': (255, 252, 210, 255),
    'gem_cyan_dark': (20, 70, 110, 255),
    'gem_cyan_mid': (30, 135, 175, 255),
    'gem_cyan_light': (65, 205, 220, 255),
    'gem_cyan_white': (185, 252, 255, 255),
    'goblin_shadow': (18, 48, 25, 255),
    'goblin_dark': (34, 76, 42, 255),
    'goblin_mid': (64, 136, 58, 255),
    'goblin_light': (115, 196, 82, 255),
    'goblin_high': (182, 236, 120, 255),
    'ruby_dark': (110, 16, 36, 255),
    'ruby_mid': (180, 28, 54, 255),
    'ruby_light': (238, 70, 88, 255),
    'moss_dark': (20, 52, 28, 255),
    'moss_mid': (38, 92, 46, 255),
    'moss_light': (68, 148, 72, 255),
    'moss_high': (124, 204, 110, 255),
    'leaf_emerald': (40, 180, 100, 255),
    'wood_bark': (48, 26, 18, 255),
    'shroom_dark': (80, 22, 68, 255),
    'shroom_mid': (150, 38, 105, 255),
    'shroom_light': (215, 65, 150, 255),
    'shroom_spore': (90, 230, 200, 255),
    'spider_body': (28, 20, 36, 255),
    'spider_leg': (54, 38, 68, 255),
    'spider_eye': (235, 32, 60, 255),
    'amethyst_dark': (45, 18, 65, 255),
    'amethyst_mid': (95, 38, 130, 255),
    'amethyst_light': (165, 80, 215, 255),
    'amethyst_high': (225, 160, 255, 255),
    'crystal_dark': (14, 52, 78, 255),
    'crystal_mid': (24, 112, 158, 255),
    'crystal_light': (60, 195, 235, 255),
    'crystal_high': (190, 245, 255, 255),
    'obsidian_black': (12, 10, 18, 255),
    'obsidian_dark': (26, 20, 36, 255),
    'obsidian_mid': (46, 38, 62, 255),
    'obsidian_light': (78, 68, 98, 255),
    'laser_red': (255, 45, 80, 255),
    'laser_cyan': (60, 240, 255, 255),
}


def create_pixel_canvas(w, h):
    return Image.new('RGBA', (w, h), PAL['void'])


def scale_nearest(im, scale_factor=4):
    """Ingrandimento perfetto pixel-per-pixel (Nearest Neighbor), zero sfocature."""
    w, h = im.size
    return im.resize((w * scale_factor, h * scale_factor), Image.Resampling.NEAREST)


def gen_pixel_miniera():
    """Giacimento minerario 16-bit: roccia frastagliata, venature di ferro grezzo, cristalli azzurri e picconi incrociati."""
    w, h = 48, 36
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # 1. Sagoma rocciosa di granito frastagliato
    for y in range(4, 34):
        # Profilo roccioso non lineare
        left = 4 + int((34 - y) * 0.35) + (y % 3)
        right = 44 - int((34 - y) * 0.30) - ((y + 1) % 3)
        for x in range(left, right):
            # Dithering e ombreggiatura da sinistra a destra
            if x == left or x == right - 1 or y == 4 or y == 33:
                pix[x, y] = PAL['black']
            elif x < left + 8:
                pix[x, y] = PAL['stone_shadow'] if (x + y) % 2 == 0 else PAL['stone_dark']
            elif x < 26:
                pix[x, y] = PAL['stone_dark'] if (x + y) % 3 == 0 else PAL['stone_mid']
            elif x < 38:
                pix[x, y] = PAL['stone_mid'] if (x + y) % 2 == 0 else PAL['stone_light']
            else:
                pix[x, y] = PAL['stone_light'] if (x + y) % 2 == 0 else PAL['stone_high']

    # 2. Venature metalliche di ferro grezzo argentato
    iron_coords = [
        (12, 12), (13, 12), (14, 13), (15, 13), (16, 14), (17, 13),
        (28, 10), (29, 10), (30, 11), (31, 11), (32, 12),
        (10, 24), (11, 24), (12, 25), (13, 25), (14, 26)
    ]
    for x, y in iron_coords:
        if 0 <= x < w and 0 <= y < h:
            pix[x, y] = PAL['iron_white']
            pix[x, y + 1] = PAL['iron_light']
            pix[x + 1, y] = PAL['iron_mid']

    # 3. Cristalli azzurri luminosi (gemme esagonali 16-bit)
    gems = [(20, 10), (34, 18), (14, 18)]
    for gx, gy in gems:
        pix[gx, gy - 2] = PAL['gem_cyan_white']
        pix[gx - 1, gy - 1] = PAL['gem_cyan_light']
        pix[gx, gy - 1] = PAL['gem_cyan_white']
        pix[gx + 1, gy - 1] = PAL['gem_cyan_mid']
        pix[gx - 1, gy] = PAL['gem_cyan_mid']
        pix[gx, gy] = PAL['gem_cyan_dark']
        pix[gx + 1, gy] = PAL['black']
        pix[gx, gy + 1] = PAL['black']

    # 4. Picconi nanici pixel incrociati al centro
    # Piccone 1 (da basso sx ad alto dx)
    for i in range(16):
        px = 16 + i
        py = 28 - i
        pix[px, py] = PAL['wood_mid']
        pix[px + 1, py] = PAL['wood_light']
    # Testa piccone 1
    for k in range(-4, 5):
        pix[31 - k, 12 - k] = PAL['iron_white'] if k > 0 else PAL['iron_mid']
        pix[32 - k, 13 - k] = PAL['black']

    # Piccone 2 (da basso dx ad alto sx)
    for i in range(16):
        px = 31 - i
        py = 28 - i
        pix[px, py] = PAL['wood_dark']
        pix[px - 1, py] = PAL['wood_mid']
    # Testa piccone 2
    for k in range(-4, 5):
        pix[16 + k, 12 - k] = PAL['iron_light'] if k > 0 else PAL['iron_dark']
        pix[15 + k, 13 - k] = PAL['black']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_miniera.png')
    print('Pixel art generata: sprite_miniera.png (192x144)')


def gen_pixel_forgia_aperta():
    """Fucina nanica 16-bit: mattoni refrattari, camini, bocca fusa con gradiente fuoco/lava e incudine nera."""
    w, h = 48, 32
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Struttura esterna della fornace in mattoni
    for y in range(4, 30):
        for x in range(4, 44):
            if x == 4 or x == 43 or y == 4 or y == 29:
                pix[x, y] = PAL['black']
            else:
                # Trama mattoni 16-bit
                is_mortar_y = (y % 5 == 4)
                row = y // 5
                shift = 4 if row % 2 == 1 else 0
                is_mortar_x = ((x + shift) % 8 == 0)
                if is_mortar_y or is_mortar_x:
                    pix[x, y] = PAL['stone_shadow']
                else:
                    pix[x, y] = PAL['stone_mid'] if (x + y) % 3 == 0 else PAL['stone_dark']

    # Camini superiori
    for cx in [8, 34]:
        for y in range(1, 5):
            for x in range(cx, cx + 6):
                if y == 1 or x == cx or x == cx + 5:
                    pix[x, y] = PAL['black']
                else:
                    pix[x, y] = PAL['stone_mid']

    # Bocca fusa della fornace (Arco di fuoco ardente)
    for y in range(10, 27):
        for x in range(12, 36):
            # Forma ad arco
            if y < 14 and (x < 15 or x > 32):
                continue
            if x == 12 or x == 35 or y == 26:
                pix[x, y] = PAL['black']
            elif y < 14:
                pix[x, y] = PAL['fire_dark']
            elif y < 18:
                pix[x, y] = PAL['fire_mid'] if (x + y) % 2 == 0 else PAL['fire_orange']
            elif y < 22:
                pix[x, y] = PAL['fire_orange'] if (x + y) % 2 == 0 else PAL['fire_yellow']
            else:
                pix[x, y] = PAL['fire_yellow'] if (x + y) % 2 == 0 else PAL['fire_white']

    # Incudine d'acciaio scuro al centro del focolare
    anvil_pts = [
        # Base
        (20, 24), (21, 24), (22, 24), (23, 24), (24, 24), (25, 24), (26, 24), (27, 24),
        # Fusto
        (22, 22), (23, 22), (24, 22), (25, 22),
        (22, 23), (23, 23), (24, 23), (25, 23),
        # Piatto e corno
        (18, 20), (19, 20), (20, 20), (21, 20), (22, 20), (23, 20), (24, 20), (25, 20), (26, 20), (27, 20), (28, 20), (29, 20),
        (19, 21), (20, 21), (21, 21), (22, 21), (23, 21), (24, 21), (25, 21), (26, 21), (27, 21)
    ]
    for x, y in anvil_pts:
        pix[x, y] = PAL['black']
    # Luce superiore sull'incudine
    for x in range(20, 28):
        pix[x, 20] = PAL['iron_light']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_forgia_aperta.png')
    print('Pixel art generata: sprite_forgia_aperta.png (192x128)')


def gen_pixel_forgia_chiusa():
    """Fucina chiusa: portone blindato in piastre d'acciaio rivettate e travi a croce in bronzo."""
    w, h = 48, 32
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Struttura esterna della fornace in mattoni
    for y in range(4, 30):
        for x in range(4, 44):
            if x == 4 or x == 43 or y == 4 or y == 29:
                pix[x, y] = PAL['black']
            else:
                is_mortar_y = (y % 5 == 4)
                row = y // 5
                shift = 4 if row % 2 == 1 else 0
                is_mortar_x = ((x + shift) % 8 == 0)
                if is_mortar_y or is_mortar_x:
                    pix[x, y] = PAL['stone_shadow']
                else:
                    pix[x, y] = PAL['stone_mid'] if (x + y) % 3 == 0 else PAL['stone_dark']

    # Camini
    for cx in [8, 34]:
        for y in range(1, 5):
            for x in range(cx, cx + 6):
                if y == 1 or x == cx or x == cx + 5:
                    pix[x, y] = PAL['black']
                else:
                    pix[x, y] = PAL['stone_mid']

    # Portone blindato chiuso (piastre di ferro)
    for y in range(10, 27):
        for x in range(12, 36):
            if y < 14 and (x < 15 or x > 32):
                continue
            if x == 12 or x == 35 or y == 26 or y == 10:
                pix[x, y] = PAL['black']
            else:
                pix[x, y] = PAL['iron_shadow'] if (x + y) % 2 == 0 else PAL['iron_dark']

    # Travi di rinforzo diagonali a X in bronzo
    for i in range(14):
        # Diagonale 1
        x1 = 15 + int(i * 1.3)
        y1 = 12 + i
        if 0 <= x1 < w and 0 <= y1 < h:
            pix[x1, y1] = PAL['gold_mid']
            pix[x1 + 1, y1] = PAL['gold_dark']
        # Diagonale 2
        x2 = 32 - int(i * 1.3)
        if 0 <= x2 < w and 0 <= y1 < h:
            pix[x2, y1] = PAL['gold_mid']
            pix[x2 - 1, y1] = PAL['gold_dark']

    # Borchie d'oro e sigillo centrale
    pix[23, 18] = PAL['gold_high']
    pix[24, 18] = PAL['gold_high']
    pix[23, 19] = PAL['gold_light']
    pix[24, 19] = PAL['gold_light']

    # Bagliore del fuoco che fuoriesce dalla fessura inferiore
    for x in range(14, 34):
        pix[x, 25] = PAL['fire_orange'] if x % 2 == 0 else PAL['fire_yellow']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_forgia_chiusa.png')
    print('Pixel art generata: sprite_forgia_chiusa.png (192x128)')


def gen_pixel_forziere():
    """Scrigno reale 16-bit: doghe di legno di quercia, bordi in ottone dorato, serratura e lingotti/gemme."""
    w, h = 48, 32
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Corpo del forziere in legno scuro
    for y in range(12, 29):
        for x in range(6, 42):
            if x == 6 or x == 41 or y == 28:
                pix[x, y] = PAL['black']
            else:
                pix[x, y] = PAL['wood_dark'] if x % 7 == 0 else (PAL['wood_mid'] if (x + y) % 2 == 0 else PAL['wood_light'])

    # Coperchio aperto arcuato in prospettiva
    for y in range(6, 13):
        indent = (12 - y) * 2
        for x in range(6 + indent, 42 - indent):
            if y == 6 or x == 6 + indent or x == 41 - indent:
                pix[x, y] = PAL['black']
            else:
                pix[x, y] = PAL['wood_mid'] if (x + y) % 2 == 0 else PAL['wood_dark']

    # Tesoro che trabocca dallo scrigno (Oro, lingotti e gemme)
    # Lingotti d'oro
    for bx, by in [(12, 11), (20, 10), (28, 11), (16, 9), (24, 8)]:
        for lx in range(bx, bx + 6):
            pix[lx, by] = PAL['gold_high']
            pix[lx, by + 1] = PAL['gold_light']
            pix[lx, by + 2] = PAL['gold_mid']
            pix[lx, by + 3] = PAL['gold_shadow']

    # Lingotti di ferro
    for bx, by in [(9, 13), (33, 13)]:
        for lx in range(bx, bx + 5):
            pix[lx, by] = PAL['iron_white']
            pix[lx, by + 1] = PAL['iron_light']
            pix[lx, by + 2] = PAL['iron_dark']

    # Rubini e gemme celesti scintillanti
    pix[19, 13] = PAL['ruby_light']
    pix[20, 13] = PAL['ruby_mid']
    pix[27, 13] = PAL['gem_cyan_white']
    pix[28, 13] = PAL['gem_cyan_light']

    # Fasce di rinforzo in ottone dorato sui bordi
    for y in range(13, 28):
        for x in [8, 9, 38, 39]:
            pix[x, y] = PAL['gold_light'] if y % 2 == 0 else PAL['gold_mid']

    # Fascia orizzontale mediana
    for x in range(7, 41):
        pix[x, 14] = PAL['gold_mid']
        pix[x, 15] = PAL['gold_dark']

    # Lucchetto regale con rubino
    for y in range(17, 23):
        for x in range(22, 26):
            pix[x, y] = PAL['gold_light']
    pix[23, 19] = PAL['ruby_light']
    pix[24, 19] = PAL['ruby_mid']
    pix[23, 20] = PAL['black']
    pix[24, 20] = PAL['black']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_forziere.png')
    print('Pixel art generata: sprite_forziere.png (192x128)')


def gen_pixel_muro():
    """Bastione difensivo in pietra 16-bit: mattoni di granito squadrati, merli e malta."""
    w, h = 48, 18
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Muro di fondo
    for y in range(4, 17):
        for x in range(2, 46):
            if x == 2 or x == 45 or y == 16:
                pix[x, y] = PAL['black']
            else:
                # Righe di mattoni sfalsate
                is_mortar_y = (y % 4 == 3)
                row = y // 4
                shift = 4 if row % 2 == 1 else 0
                is_mortar_x = ((x + shift) % 8 == 0)
                if is_mortar_y or is_mortar_x:
                    pix[x, y] = PAL['stone_shadow']
                else:
                    pix[x, y] = PAL['stone_mid'] if (x + y) % 2 == 0 else PAL['stone_light']

    # Merlature superiori
    merlons = [(4, 11), (15, 22), (26, 33), (37, 44)]
    for x1, x2 in merlons:
        for y in range(1, 5):
            for x in range(x1, x2):
                if y == 1 or x == x1 or x == x2 - 1:
                    pix[x, y] = PAL['black']
                else:
                    pix[x, y] = PAL['stone_light'] if y == 2 else PAL['stone_mid']

    # Sigillo runico nanico d'oro al centro
    pix[23, 9] = PAL['gold_high']
    pix[24, 9] = PAL['gold_high']
    pix[22, 10] = PAL['gold_light']
    pix[23, 10] = PAL['fire_dark']
    pix[24, 10] = PAL['fire_dark']
    pix[25, 10] = PAL['gold_light']
    pix[23, 11] = PAL['gold_dark']
    pix[24, 11] = PAL['gold_dark']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_muro.png')
    print('Pixel art generata: sprite_muro.png (192x72)')


def gen_pixel_balista():
    """Balista 16-bit: affusto girevole su pietra, grande arco teso in legno e dardo d'acciaio infuocato."""
    w, h = 32, 32
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Base circolare in pietra
    for y in range(18, 30):
        for x in range(6, 26):
            dx = x - 16
            dy = (y - 24) * 1.6
            if dx * dx + dy * dy < 70:
                if dx * dx + dy * dy >= 52:
                    pix[x, y] = PAL['black']
                else:
                    pix[x, y] = PAL['stone_mid'] if (x + y) % 2 == 0 else PAL['stone_dark']

    # Perno e torretta in legno
    for y in range(12, 23):
        for x in range(13, 19):
            pix[x, y] = PAL['wood_dark'] if x == 13 or x == 18 else PAL['wood_mid']

    # Grande arco da guerra in legno e ottone
    bow_pts = [
        (3, 16), (4, 15), (6, 13), (9, 11), (13, 10), (16, 9),
        (19, 10), (23, 11), (26, 13), (28, 15), (29, 16)
    ]
    for x, y in bow_pts:
        pix[x, y] = PAL['wood_light']
        pix[x, y + 1] = PAL['wood_dark']
        pix[x, y - 1] = PAL['black']

    # Corda tesa
    for x in range(4, 16):
        pix[x, 15] = PAL['iron_light']
    for x in range(16, 29):
        pix[x, 15] = PAL['iron_light']

    # Dardo centrale con punta di fuoco
    for y in range(4, 19):
        pix[15, y] = PAL['iron_light']
        pix[16, y] = PAL['iron_white']
    # Punta infuocata del dardo
    pix[15, 3] = PAL['fire_yellow']
    pix[16, 3] = PAL['fire_white']
    pix[15, 2] = PAL['fire_orange']
    pix[16, 2] = PAL['fire_orange']
    pix[15, 1] = PAL['fire_mid']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_balista.png')
    print('Pixel art generata: sprite_balista.png (128x128)')


def gen_pixel_scudo():
    """Scudo torre nanico (paddle) 16-bit: placca d'acciaio, borchie dorate, martello centrale."""
    w, h = 48, 16
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Sagoma a losanga dello scudo nanico
    for y in range(2, 14):
        indent = abs(7 - y)
        for x in range(2 + indent, 46 - indent):
            if x == 2 + indent or x == 45 - indent or y == 2 or y == 13:
                pix[x, y] = PAL['black']
            elif y == 3 or y == 12 or x < 5 + indent or x > 42 - indent:
                # Bordo dorato borchiato
                pix[x, y] = PAL['gold_light'] if (x + y) % 2 == 0 else PAL['gold_mid']
            else:
                # Corpo centrale in acciaio bluastro
                pix[x, y] = PAL['iron_light'] if y < 8 else PAL['iron_mid']

    # Stemma centrale del martello nanico
    # Testa martello
    for x in range(21, 27):
        pix[x, 5] = PAL['gold_high']
        pix[x, 6] = PAL['gold_light']
    # Manico
    pix[23, 7] = PAL['wood_mid']
    pix[24, 7] = PAL['wood_mid']
    pix[23, 8] = PAL['wood_mid']
    pix[24, 8] = PAL['wood_mid']
    pix[23, 9] = PAL['wood_dark']
    pix[24, 9] = PAL['wood_dark']

    # Borchie d'oro laterali
    for bx in [8, 14, 33, 39]:
        pix[bx, 7] = PAL['gold_high']
        pix[bx, 8] = PAL['gold_dark']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_scudo.png')
    print('Pixel art generata: sprite_scudo.png (192x64)')


def gen_pixel_sfera():
    """Sfera runica 16-bit: sfera metallica rotonda a pixel, luce speculare, incisione runica e bordo netto."""
    w, h = 16, 16
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Cerchio 16-bit pixel
    mask = [
        "....XXXXXX....",
        "..XXXXXXXXXX..",
        ".XXXXXXXXXXXX.",
        ".XXXXXXXXXXXX.",
        "XXXXXXXXXXXXXX",
        "XXXXXXXXXXXXXX",
        "XXXXXXXXXXXXXX",
        "XXXXXXXXXXXXXX",
        "XXXXXXXXXXXXXX",
        "XXXXXXXXXXXXXX",
        "XXXXXXXXXXXXXX",
        "XXXXXXXXXXXXXX",
        ".XXXXXXXXXXXX.",
        ".XXXXXXXXXXXX.",
        "..XXXXXXXXXX..",
        "....XXXXXX...."
    ]

    for y in range(16):
        row = mask[y]
        for x in range(14):
            if row[x] == 'X':
                # Ombreggiatura sferica (luce in alto a sinistra, ombra in basso a destra)
                dist_light = (x - 4) ** 2 + (y - 4) ** 2
                dist_edge = (x - 7) ** 2 + (y - 7) ** 2
                
                if dist_edge >= 42:
                    pix[x + 1, y] = PAL['black']
                elif dist_light < 5:
                    pix[x + 1, y] = PAL['iron_white'] # Specular highlight
                elif dist_light < 18:
                    pix[x + 1, y] = PAL['iron_light']
                elif dist_light < 40:
                    pix[x + 1, y] = PAL['iron_mid']
                else:
                    pix[x + 1, y] = PAL['iron_dark']

    # Croce runica d'oro al centro
    runic_pixels = [(7, 7), (8, 7), (6, 8), (7, 8), (8, 8), (9, 8), (7, 9), (8, 9)]
    for rx, ry in runic_pixels:
        pix[rx, ry] = PAL['gold_light']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_sfera.png')
    print('Pixel art generata: sprite_sfera.png (64x64)')


def gen_pixel_goblin():
    """Goblin 16-bit: elmo di ferro cornuto, grandi orecchie a punta, occhi rossi minacciosi e zanne."""
    w, h = 24, 24
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Orecchie a punta verdi
    for i in range(5):
        pix[4 - i, 10 + i] = PAL['goblin_dark']
        pix[5 - i, 10 + i] = PAL['goblin_mid']
        pix[6 - i, 10 + i] = PAL['goblin_light']
        pix[19 + i, 10 + i] = PAL['goblin_dark']
        pix[18 + i, 10 + i] = PAL['goblin_mid']
        pix[17 + i, 10 + i] = PAL['goblin_light']

    # Faccia goblin verde
    for y in range(8, 18):
        for x in range(7, 17):
            pix[x, y] = PAL['goblin_mid'] if (x + y) % 2 == 0 else PAL['goblin_light']

    # Elmo di ferro con corna
    # Calotta elmo
    for y in range(4, 10):
        for x in range(7, 17):
            if y == 4 or x == 7 or x == 16:
                pix[x, y] = PAL['black']
            else:
                pix[x, y] = PAL['iron_mid'] if y == 5 else PAL['iron_dark']
    # Corna
    pix[6, 5] = PAL['fire_white']
    pix[5, 4] = PAL['fire_white']
    pix[4, 3] = PAL['fire_white']
    pix[17, 5] = PAL['fire_white']
    pix[18, 4] = PAL['fire_white']
    pix[19, 3] = PAL['fire_white']

    # Occhi rossi incandescenti
    pix[9, 11] = PAL['fire_mid']
    pix[10, 11] = PAL['fire_yellow']
    pix[13, 11] = PAL['fire_yellow']
    pix[14, 11] = PAL['fire_mid']

    # Zanne e bocca
    pix[10, 15] = PAL['black']
    pix[11, 15] = PAL['black']
    pix[12, 15] = PAL['black']
    pix[13, 15] = PAL['black']
    pix[10, 14] = PAL['iron_white'] # Zanna sx
    pix[13, 14] = PAL['iron_white'] # Zanna dx

    # Corpo / Tunica in cuoio
    for y in range(18, 23):
        for x in range(6, 18):
            if x == 6 or x == 17 or y == 22:
                pix[x, y] = PAL['black']
            else:
                pix[x, y] = PAL['wood_dark'] if x % 2 == 0 else PAL['wood_shadow']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_goblin.png')
    print('Pixel art generata: sprite_goblin.png (96x96)')


def gen_pixel_caverna_sfondo():
    """Sfondo rocca nanica 16-bit: blocchi di pietra squadrati a pixel, pilastri intagliati e torce a muro."""
    w, h = 135, 240
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Muro di fondo in blocchi di granito sotterraneo
    block_h = 10
    block_w = 16
    for y in range(h):
        row = y // block_h
        is_mortar_y = (y % block_h == 0)
        shift = 8 if row % 2 == 1 else 0
        for x in range(w):
            is_mortar_x = ((x + shift) % block_w == 0)
            if is_mortar_y or is_mortar_x:
                pix[x, y] = PAL['black']
            else:
                # Trama di pietra antica ditherata
                if (x + y * 3) % 11 == 0:
                    pix[x, y] = PAL['stone_shadow']
                elif (x + y * 7) % 13 == 0:
                    pix[x, y] = PAL['stone_mid']
                else:
                    pix[x, y] = PAL['stone_dark']

    # Pilastri di roccia massiccia ai lati
    for y in range(h):
        for x in range(10):
            if x == 0 or x == 9:
                pix[x, y] = PAL['black']
            else:
                pix[x, y] = PAL['stone_mid'] if x < 4 else PAL['stone_dark']
        for x in range(w - 10, w):
            if x == w - 10 or x == w - 1:
                pix[x, y] = PAL['black']
            else:
                pix[x, y] = PAL['stone_dark'] if x < w - 4 else PAL['stone_shadow']

    # Torce a muro con pixel di fuoco ardente
    for tx, ty in [(20, 60), (w - 20, 60), (20, 150), (w - 20, 150)]:
        # Staffa in ferro
        pix[tx, ty + 2] = PAL['iron_light']
        pix[tx, ty + 3] = PAL['iron_dark']
        # Fiamma 16-bit
        pix[tx, ty] = PAL['fire_white']
        pix[tx - 1, ty] = PAL['fire_yellow']
        pix[tx + 1, ty] = PAL['fire_yellow']
        pix[tx, ty - 1] = PAL['fire_orange']
        pix[tx, ty - 2] = PAL['fire_mid']

    out = scale_nearest(im, 4) # 135*4 = 540, 240*4 = 960! Esattamente la risoluzione virtuale del gioco!
    out.save(OUT_DIR / 'sfondo_caverna.png')
    print('Pixel art generata: sfondo_caverna.png (540x960)')


def gen_pixel_foresta_sfondo():
    """Sfondo foresta nanica 16-bit: lastre di pietra muschiata, radici millenarie e lanterne silvane runiche."""
    w, h = 135, 240
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Muro e suolo in lastroni di roccia coperti di muschio scuro
    block_h = 10
    block_w = 16
    for y in range(h):
        row = y // block_h
        is_mortar_y = (y % block_h == 0)
        shift = 8 if row % 2 == 1 else 0
        for x in range(w):
            is_mortar_x = ((x + shift) % block_w == 0)
            if is_mortar_y or is_mortar_x:
                pix[x, y] = PAL['black']
            else:
                # Trama di muschio verde smeraldo scuro ditherato
                r = (x * 7 + y * 13) % 29
                if r < 4:
                    pix[x, y] = PAL['moss_dark']
                elif r < 14:
                    pix[x, y] = PAL['stone_dark']
                elif r < 23:
                    pix[x, y] = PAL['moss_mid']
                else:
                    pix[x, y] = PAL['moss_light']

    # Pilastri di tronchi secolari e radici intrecciate sui bordi laterali
    for y in range(h):
        for x in range(12):
            if x == 0 or x == 11:
                pix[x, y] = PAL['black']
            elif x < 5:
                pix[x, y] = PAL['wood_mid'] if (x + y) % 3 == 0 else PAL['wood_bark']
            else:
                pix[x, y] = PAL['wood_bark'] if (x + y) % 2 == 0 else PAL['wood_dark']
        for x in range(w - 12, w):
            if x == w - 12 or x == w - 1:
                pix[x, y] = PAL['black']
            elif x < w - 5:
                pix[x, y] = PAL['wood_bark'] if (x + y) % 2 == 0 else PAL['wood_dark']
            else:
                pix[x, y] = PAL['wood_mid'] if (x + y) % 3 == 0 else PAL['wood_bark']

    # Lanterne silvane runiche a parete (luce smeraldo/spora magica)
    for tx, ty in [(20, 60), (w - 20, 60), (20, 150), (w - 20, 150)]:
        # Supporto intagliato in ferro
        pix[tx, ty + 2] = PAL['iron_light']
        pix[tx, ty + 3] = PAL['iron_dark']
        # Gemma runica silvana luminosa
        pix[tx, ty] = PAL['gem_cyan_white']
        pix[tx - 1, ty] = PAL['moss_high']
        pix[tx + 1, ty] = PAL['moss_high']
        pix[tx, ty - 1] = PAL['shroom_spore']
        pix[tx, ty - 2] = PAL['leaf_emerald']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sfondo_foresta.png')
    print('Pixel art generata: sfondo_foresta.png (540x960)')


def gen_pixel_albero():
    """Ceppo di Albero Antico nanico (192x144): corteccia nodosa, muschio, anelli concentrici e rune dorate."""
    w, h = 48, 36
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Sagoma tronco/ceppo massiccio
    for y in range(6, 34):
        left = 6 + int((34 - y) * 0.3) + (y % 2)
        right = 42 - int((34 - y) * 0.25) - ((y + 1) % 2)
        for x in range(left, right):
            if x == left or x == right - 1 or y == 33:
                pix[x, y] = PAL['black']
            elif y < 14:
                # Superficie di taglio con anelli di crescita in legno
                dist_center = abs(x - 24) + abs(y - 10) * 1.5
                if dist_center < 4:
                    pix[x, y] = PAL['gold_light']
                elif int(dist_center) in (6, 7, 12, 13):
                    pix[x, y] = PAL['wood_dark']
                elif int(dist_center) in (8, 9, 14, 15):
                    pix[x, y] = PAL['wood_light']
                else:
                    pix[x, y] = PAL['wood_mid']
            else:
                # Corteccia esterna nodosa e ditherata
                if (x + y * 2) % 7 == 0:
                    pix[x, y] = PAL['moss_mid']
                elif (x + y) % 5 == 0:
                    pix[x, y] = PAL['wood_bark']
                elif x < left + 6:
                    pix[x, y] = PAL['wood_shadow'] if (x + y) % 2 == 0 else PAL['wood_dark']
                else:
                    pix[x, y] = PAL['wood_dark'] if (x + y) % 2 == 0 else PAL['wood_mid']

    # Bordo superiore del taglio
    for x in range(8, 40):
        pix[x, 5] = PAL['black']
        pix[x, 6] = PAL['wood_bark']

    # Muschio e rampicanti alla base
    for x in range(8, 40):
        if (x * 3) % 4 == 0:
            pix[x, 32] = PAL['moss_light']
            pix[x, 31] = PAL['moss_mid']
            pix[x, 30] = PAL['moss_dark']

    # Rune naniche dorate incise sulla corteccia
    runes = [(20, 20), (21, 21), (22, 22), (23, 21), (24, 20), (28, 22), (28, 23), (28, 24)]
    for rx, ry in runes:
        pix[rx, ry] = PAL['leaf_emerald']
        pix[rx + 1, ry] = PAL['moss_high']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_albero.png')
    print('Pixel art generata: sprite_albero.png (192x144)')


def gen_pixel_segheria():
    """Segheria runica nanica (192x128): telaio in legno e ferro, lama circolare d'acciaio dorato rotante."""
    w, h = 48, 32
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Struttura in pietra e travi alla base
    for y in range(4, 30):
        for x in range(4, 44):
            if x in (4, 43) or y in (4, 29):
                pix[x, y] = PAL['black']
            elif y >= 22:
                # Basamento in granito
                pix[x, y] = PAL['stone_mid'] if (x + y) % 2 == 0 else PAL['stone_dark']
            else:
                # Tetto e pareti in travi di legno massiccio
                pix[x, y] = PAL['wood_bark'] if (x + y) % 3 == 0 else PAL['wood_mid']

    # Tetto spiovente
    for x in range(3, 45):
        pix[x, 3] = PAL['black']
        pix[x, 4] = PAL['wood_light']

    # Apertura centrale di taglio (vano lama)
    for y in range(12, 26):
        for x in range(16, 32):
            pix[x, y] = PAL['black']

    # Tronco che entra a sinistra
    for y in range(18, 23):
        for x in range(8, 20):
            pix[x, y] = PAL['wood_light'] if y == 19 else PAL['wood_mid']

    # Tronco/assi lavorate che escono a destra
    for y in range(19, 23):
        for x in range(28, 40):
            pix[x, y] = PAL['gold_mid'] if y == 20 else PAL['wood_light']

    # Grande disco/lama circolare dentata centrale
    cx, cy = 24, 18
    for dy in range(-7, 8):
        for dx in range(-7, 8):
            d2 = dx * dx + dy * dy
            if d2 <= 49:
                x = cx + dx
                y = cy + dy
                if 40 <= d2 <= 49:
                    # Denti della sega
                    pix[x, y] = PAL['iron_white'] if (dx + dy) % 2 == 0 else PAL['black']
                elif d2 <= 6:
                    # Mozzo centrale in oro
                    pix[x, y] = PAL['gold_light']
                else:
                    # Disco d'acciaio lucido
                    pix[x, y] = PAL['iron_light'] if dx > 0 else PAL['iron_mid']

    # Canna fumaria / camino a vapore a sinistra
    for y in range(0, 5):
        for x in range(8, 12):
            pix[x, y] = PAL['iron_dark'] if x == 8 or x == 11 else PAL['iron_light']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_segheria.png')
    print('Pixel art generata: sprite_segheria.png (192x128)')


def gen_pixel_ragno():
    """Ragno silvano delle radici 16-bit (96x96): carapace scuro, 8 zampe ricurve e occhi rubino luminosi."""
    w, h = 24, 24
    im = create_pixel_canvas(w, h)
    pix = im.load()

    cx, cy = 12, 12

    # Addome posteriore (ovale superiore)
    for dy in range(-6, 2):
        for dx in range(-5, 6):
            if dx * dx * 1.3 + dy * dy * 1.1 <= 22:
                x = cx + dx
                y = cy + dy - 2
                if dx * dx * 1.3 + dy * dy * 1.1 >= 18:
                    pix[x, y] = PAL['black']
                else:
                    pix[x, y] = PAL['shroom_mid'] if (dx + dy) % 2 == 0 else PAL['spider_body']

    # Cefalotorace (corpo centrale)
    for dy in range(-2, 5):
        for dx in range(-4, 5):
            if dx * dx + dy * dy <= 14:
                x = cx + dx
                y = cy + dy + 2
                if dx * dx + dy * dy >= 10:
                    pix[x, y] = PAL['black']
                else:
                    pix[x, y] = PAL['spider_leg'] if dx == 0 else PAL['spider_body']

    # Occhi rubino scintillanti
    pix[cx - 2, cy + 3] = PAL['spider_eye']
    pix[cx - 1, cy + 3] = PAL['ruby_light']
    pix[cx + 1, cy + 3] = PAL['ruby_light']
    pix[cx + 2, cy + 3] = PAL['spider_eye']
    pix[cx - 1, cy + 2] = PAL['fire_yellow']
    pix[cx + 1, cy + 2] = PAL['fire_yellow']

    # Mandibole/cheliceri
    pix[cx - 2, cy + 5] = PAL['iron_light']
    pix[cx - 2, cy + 6] = PAL['black']
    pix[cx + 2, cy + 5] = PAL['iron_light']
    pix[cx + 2, cy + 6] = PAL['black']

    # 8 zampe snodate (4 a sinistra, 4 a destra)
    leg_paths = [
        # Zampe sinistre
        [(cx - 4, cy - 2), (cx - 7, cy - 5), (cx - 10, cy - 3)],
        [(cx - 4, cy), (cx - 8, cy - 2), (cx - 11, cy + 1)],
        [(cx - 4, cy + 2), (cx - 8, cy + 3), (cx - 11, cy + 6)],
        [(cx - 3, cy + 4), (cx - 6, cy + 7), (cx - 9, cy + 10)],
        # Zampe destre
        [(cx + 4, cy - 2), (cx + 7, cy - 5), (cx + 10, cy - 3)],
        [(cx + 4, cy), (cx + 8, cy - 2), (cx + 11, cy + 1)],
        [(cx + 4, cy + 2), (cx + 8, cy + 3), (cx + 11, cy + 6)],
        [(cx + 3, cy + 4), (cx + 6, cy + 7), (cx + 9, cy + 10)],
    ]
    for leg in leg_paths:
        for lx, ly in leg:
            if 0 <= lx < w and 0 <= ly < h:
                pix[lx, ly] = PAL['spider_leg']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_ragno.png')
    print('Pixel art generata: sprite_ragno.png (96x96)')


def gen_pixel_fungo():
    """Fungo elastico bouncer (96x96): cappello luminescente viola/magenta a pois turchesi con fusto flessibile."""
    w, h = 24, 24
    im = create_pixel_canvas(w, h)
    pix = im.load()

    cx, cy = 12, 10

    # Gambo spesso
    for y in range(12, 22):
        gw = 3 + int((y - 12) * 0.4)
        for x in range(cx - gw, cx + gw + 1):
            if x in (cx - gw, cx + gw) or y == 21:
                pix[x, y] = PAL['black']
            else:
                pix[x, y] = PAL['wood_light'] if x < cx else PAL['shroom_spore']

    # Cappello a cupola carnoso
    for dy in range(-7, 4):
        for dx in range(-10, 11):
            if dx * dx * 0.6 + dy * dy * 1.8 <= 32:
                x = cx + dx
                y = cy + dy
                if dx * dx * 0.6 + dy * dy * 1.8 >= 26:
                    pix[x, y] = PAL['black']
                else:
                    pix[x, y] = PAL['shroom_mid'] if dy > 0 else PAL['shroom_light']

    # Pois fluorescenti luminescenti sul cappello
    dots = [(-5, -2), (0, -4), (5, -2), (-2, 0), (3, 1)]
    for dx, dy in dots:
        pix[cx + dx, cy + dy] = PAL['shroom_spore']
        pix[cx + dx + 1, cy + dy] = PAL['gem_cyan_white']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_fungo.png')
    print('Pixel art generata: sprite_fungo.png (96x96)')


def gen_pixel_muro_legno():
    """Barricata di tronchi nanica (192x48): pali appuntiti verticali, traverse in ferro con chiodi e corde."""
    w, h = 48, 12
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # 6 Pali verticali di legno appuntito
    for p in range(6):
        px = p * 8
        # Punta del palo
        pix[px + 3, 0] = PAL['black']
        pix[px + 4, 0] = PAL['black']
        for y in range(1, 3):
            pix[px + 3 - y, y] = PAL['black']
            pix[px + 4 + y, y] = PAL['black']
            for x in range(px + 4 - y, px + 4 + y):
                pix[x, y] = PAL['wood_light']
        # Fusto del palo
        for y in range(3, 12):
            for x in range(px, px + 8):
                if x == px or x == px + 7 or y == 11:
                    pix[x, y] = PAL['black']
                elif x in (px + 1, px + 2):
                    pix[x, y] = PAL['wood_mid']
                elif x in (px + 3, px + 4):
                    pix[x, y] = PAL['wood_light']
                else:
                    pix[x, y] = PAL['wood_dark']

    # Due fasce di ferro orizzontali di rinforzo
    for y in (4, 8):
        for x in range(w):
            if pix[x, y] != PAL['void']:
                pix[x, y] = PAL['iron_light'] if x % 8 in (3, 4) else PAL['iron_dark']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_muro_legno.png')
    print('Pixel art generata: sprite_muro_legno.png (192x48)')


def gen_pixel_legno():
    """Ciocco di legname nanico (64x64): tronco con corteccia e taglio d'accetta."""
    w, h = 16, 16
    im = create_pixel_canvas(w, h)
    pix = im.load()

    for y in range(3, 13):
        for x in range(3, 13):
            if x in (3, 12) or y in (3, 12):
                pix[x, y] = PAL['black']
            elif x < 7:
                pix[x, y] = PAL['wood_bark']
            elif x == 7:
                pix[x, y] = PAL['wood_dark']
            else:
                dist = abs(x - 10) + abs(y - 8)
                pix[x, y] = PAL['gold_light'] if dist < 2 else PAL['wood_light']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_legno.png')
    print('Pixel art generata: sprite_legno.png (64x64)')


def gen_pixel_cristalli_sfondo():
    """Sfondo caverna dei cristalli 16-bit: lastre di ossidiana violacea, geodi azzurri e rune scintillanti."""
    w, h = 135, 240
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Muro di ossidiana violacea scura e basalto
    block_h = 10
    block_w = 16
    for y in range(h):
        row = y // block_h
        is_mortar_y = (y % block_h == 0)
        shift = 8 if row % 2 == 1 else 0
        for x in range(w):
            is_mortar_x = ((x + shift) % block_w == 0)
            if is_mortar_y or is_mortar_x:
                pix[x, y] = PAL['black']
            else:
                # Trama di ossidiana e riflessi ametista
                r = (x * 11 + y * 17) % 31
                if r < 4:
                    pix[x, y] = PAL['obsidian_black']
                elif r < 14:
                    pix[x, y] = PAL['obsidian_dark']
                elif r < 24:
                    pix[x, y] = PAL['obsidian_mid']
                else:
                    pix[x, y] = PAL['amethyst_dark']

    # Pilastri di cristallo e guglie di ossidiana ai bordi laterali
    for y in range(h):
        for x in range(12):
            if x == 0 or x == 11:
                pix[x, y] = PAL['black']
            elif x < 5:
                pix[x, y] = PAL['amethyst_mid'] if (x + y) % 3 == 0 else PAL['obsidian_mid']
            else:
                pix[x, y] = PAL['obsidian_dark'] if (x + y) % 2 == 0 else PAL['obsidian_black']
        for x in range(w - 12, w):
            if x == w - 12 or x == w - 1:
                pix[x, y] = PAL['black']
            elif x < w - 5:
                pix[x, y] = PAL['obsidian_dark'] if (x + y) % 2 == 0 else PAL['obsidian_black']
            else:
                pix[x, y] = PAL['amethyst_mid'] if (x + y) % 3 == 0 else PAL['obsidian_mid']

    # Piccoli geodi di cristallo ciano scintillanti incastonati nella roccia
    geodes = [(32, 45), (102, 70), (45, 110), (90, 140), (35, 180), (105, 205)]
    for gx, gy in geodes:
        pix[gx, gy] = PAL['crystal_high']
        pix[gx - 1, gy] = PAL['crystal_light']
        pix[gx + 1, gy] = PAL['crystal_mid']
        pix[gx, gy - 1] = PAL['crystal_light']
        pix[gx, gy + 1] = PAL['crystal_dark']

    # Prismi luminosi a parete (luce azzurra / ciano runica)
    for tx, ty in [(20, 60), (w - 20, 60), (20, 150), (w - 20, 150)]:
        pix[tx, ty + 2] = PAL['obsidian_light']
        pix[tx, ty + 3] = PAL['obsidian_mid']
        pix[tx, ty] = PAL['crystal_high']
        pix[tx - 1, ty] = PAL['crystal_light']
        pix[tx + 1, ty] = PAL['crystal_light']
        pix[tx, ty - 1] = PAL['amethyst_light']
        pix[tx, ty - 2] = PAL['crystal_light']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sfondo_cristalli.png')
    print('Pixel art generata: sfondo_cristalli.png (540x960)')


def gen_pixel_cristallo():
    """Giacimento di Cristalli Puri / Mithril (192x96): cluster di cristalli ciano sfaccettati su roccia."""
    w, h = 48, 24
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Base di roccia basaltica scura
    for y in range(14, 24):
        for x in range(4, 44):
            if x >= 4 + (24 - y) and x <= 44 - (24 - y):
                if y == 23 or x == 4 + (24 - y) or x == 44 - (24 - y):
                    pix[x, y] = PAL['black']
                else:
                    pix[x, y] = PAL['obsidian_dark'] if (x + y) % 2 == 0 else PAL['obsidian_mid']

    # Cluster di 5 cristalli a punta che emergono verso l'alto
    spires = [
        (10, 8, 5, 14),   # x, tip_y, half_w, base_y
        (18, 3, 6, 17),   # spira centrale alta
        (26, 5, 6, 18),   # spira gemella
        (34, 4, 5, 16),   # spira destra
        (40, 9, 4, 15),   # spira laterale
    ]

    for cx, ty, hw, by in spires:
        for y in range(ty, by):
            cur_w = max(1, int(hw * (y - ty) / (by - ty)))
            for x in range(cx - cur_w, cx + cur_w + 1):
                if x == cx - cur_w or x == cx + cur_w or y == ty:
                    pix[x, y] = PAL['crystal_dark']
                elif x < cx:
                    pix[x, y] = PAL['crystal_high'] if x == cx - 1 and y < ty + 5 else PAL['crystal_light']
                elif x == cx:
                    pix[x, y] = PAL['crystal_high'] if y < ty + 3 else PAL['crystal_mid']
                else:
                    pix[x, y] = PAL['crystal_mid'] if (x + y) % 2 == 0 else PAL['crystal_dark']

    # Riflessi brillanti e punti luce bianchi
    pix[18, 4] = PAL['white']
    pix[26, 6] = PAL['white']
    pix[34, 5] = PAL['white']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_cristallo.png')
    print('Pixel art generata: sprite_cristallo.png (192x96)')


def gen_pixel_tagliapietre():
    """Banco del Tagliapietre Runico (192x128): mola rotante a smeriglio, calibri nanici e cristalli in taglio."""
    w, h = 48, 32
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Basamento in pietra massiccia lavorata con decori di bronzo
    for y in range(12, 31):
        for x in range(4, 44):
            if x in (4, 43) or y in (12, 30):
                pix[x, y] = PAL['black']
            elif y in (13, 14):
                pix[x, y] = PAL['stone_light'] if x % 4 in (1, 2) else PAL['stone_mid']
            elif y > 24:
                pix[x, y] = PAL['iron_dark'] if (x + y) % 2 == 0 else PAL['stone_dark']
            else:
                pix[x, y] = PAL['stone_mid']

    # Ruota della Mola rotante a sinistra (diametro 14px)
    for y in range(4, 18):
        for x in range(8, 22):
            dx = x - 15
            dy = y - 11
            d2 = dx * dx + dy * dy
            if 36 <= d2 <= 49:
                pix[x, y] = PAL['black']
            elif 16 <= d2 < 36:
                # Disco rotante di smeriglio con faville azzurre
                pix[x, y] = PAL['crystal_high'] if (x + y * 2) % 5 == 0 else PAL['iron_light']
            elif d2 < 16:
                pix[x, y] = PAL['iron_dark']

    # Supporto mola in ferro battuto
    for y in range(14, 20):
        pix[14, y] = PAL['iron_light']
        pix[15, y] = PAL['iron_mid']

    # Cristallo grande montato sul carrello di taglio a destra
    for y in range(5, 13):
        cur_w = 4 - abs(y - 9)
        for x in range(32 - cur_w, 32 + cur_w + 1):
            if x in (32 - cur_w, 32 + cur_w) or y in (5, 12):
                pix[x, y] = PAL['crystal_dark']
            elif x < 32:
                pix[x, y] = PAL['crystal_high']
            else:
                pix[x, y] = PAL['crystal_light']

    # Scintille di sfaccettatura (cyan/white sparks)
    sparks = [(16, 8), (17, 7), (18, 9), (21, 6), (28, 9), (29, 8)]
    for sx, sy in sparks:
        pix[sx, sy] = PAL['crystal_high']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_tagliapietre.png')
    print('Pixel art generata: sprite_tagliapietre.png (192x128)')


def gen_pixel_forziere_cristallo():
    """Cassaforte delle Gemme (192x128): forziere d'ossidiana rinforzato con serrature runiche e gemme splendenti."""
    w, h = 48, 32
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Corpo forziere (ossidiana e finiture oro)
    for y in range(10, 30):
        for x in range(4, 44):
            if x in (4, 43) or y in (10, 29):
                pix[x, y] = PAL['black']
            elif x in (5, 6, 41, 42) or y in (11, 28):
                pix[x, y] = PAL['gold_mid'] if (x + y) % 2 == 0 else PAL['gold_light']
            else:
                pix[x, y] = PAL['obsidian_dark'] if (x + y) % 3 == 0 else PAL['obsidian_mid']

    # Coperchio sollevato / interno pieno di gemme
    for y in range(4, 11):
        for x in range(8, 40):
            if y == 4 or x in (8, 39):
                pix[x, y] = PAL['gold_dark']
            else:
                # Mucchio di gemme sfaccettate multicolore (ciano, ametista, oro)
                r = (x * 7 + y * 13) % 17
                if r < 6:
                    pix[x, y] = PAL['crystal_high']
                elif r < 11:
                    pix[x, y] = PAL['crystal_light']
                elif r < 14:
                    pix[x, y] = PAL['amethyst_light']
                else:
                    pix[x, y] = PAL['gold_high']

    # Serratura runica a prisma centrale
    for y in range(16, 23):
        for x in range(21, 27):
            if x in (21, 26) or y in (16, 22):
                pix[x, y] = PAL['gold_dark']
            else:
                pix[x, y] = PAL['crystal_high'] if (x + y) % 2 == 0 else PAL['crystal_light']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_forziere_cristallo.png')
    print('Pixel art generata: sprite_forziere_cristallo.png (192x128)')


def gen_pixel_prisma():
    """Prisma di Risonanza Cristallina (128x128): gemma di risonanza fluttuante su piedistallo runico."""
    w, h = 32, 32
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Piedistallo nanico inferiore (ossidiana intagliata e oro)
    for y in range(24, 31):
        pw = min(14, (y - 24) * 2 + 4)
        for x in range(16 - pw, 16 + pw + 1):
            if x == 16 - pw or x == 16 + pw or y == 30:
                pix[x, y] = PAL['black']
            elif y == 24:
                pix[x, y] = PAL['gold_mid']
            else:
                pix[x, y] = PAL['obsidian_mid'] if (x + y) % 2 == 0 else PAL['obsidian_dark']

    # Prisma ottagonale/diamante fluttuante (y: 3..21, x: 7..25)
    for y in range(3, 22):
        if y < 12:
            pw = (y - 3)
        else:
            pw = (21 - y)
        for x in range(16 - pw, 16 + pw + 1):
            if x == 16 - pw or x == 16 + pw:
                pix[x, y] = PAL['crystal_dark']
            elif x < 16:
                pix[x, y] = PAL['crystal_high'] if x <= 16 - pw + 2 else PAL['crystal_light']
            elif x == 16:
                pix[x, y] = PAL['white'] if y in (6, 7, 8) else PAL['crystal_high']
            else:
                pix[x, y] = PAL['crystal_mid'] if (x + y) % 2 == 0 else PAL['amethyst_mid']

    # Bagliore d'energia e sfere di risonanza
    pix[16, 2] = PAL['crystal_high']
    pix[15, 2] = PAL['white']
    pix[16, 22] = PAL['crystal_high']
    pix[8, 12] = PAL['crystal_high']
    pix[24, 12] = PAL['crystal_high']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_prisma.png')
    print('Pixel art generata: sprite_prisma.png (128x128)')


def gen_pixel_muro_ossidiana():
    """Barricata di Ossidiana (192x64): blocchi vulcanici scuri con giunture di malta di cristallo luminescente."""
    w, h = 48, 16
    im = create_pixel_canvas(w, h)
    pix = im.load()

    for y in range(h):
        for x in range(w):
            if y == 0 or y == h - 1 or x == 0 or x == w - 1:
                pix[x, y] = PAL['black']
            elif y in (5, 10) or (y < 5 and x in (16, 32)) or (5 < y < 10 and x in (8, 24, 40)) or (y > 10 and x in (16, 32)):
                # Giunture di malta runica scintillante ciano
                pix[x, y] = PAL['crystal_light'] if (x + y) % 3 == 0 else PAL['crystal_dark']
            else:
                pix[x, y] = PAL['obsidian_mid'] if (x + y) % 4 == 0 else (PAL['obsidian_dark'] if (x + y) % 2 == 0 else PAL['obsidian_black'])

    # Merli superiori e rivetti
    for mx in (4, 12, 20, 28, 36, 44):
        pix[mx, 2] = PAL['crystal_high']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_muro_ossidiana.png')
    print('Pixel art generata: sprite_muro_ossidiana.png (192x64)')


def gen_pixel_torretta_laser():
    """Torretta Laser a Prisma (128x128): torretta girevole con lente prismatica che focalizza raggi di luce."""
    w, h = 32, 32
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Piedistallo ottagonale corazzato
    for y in range(18, 31):
        pw = min(12, (y - 18) + 8)
        for x in range(16 - pw, 16 + pw + 1):
            if x in (16 - pw, 16 + pw) or y in (18, 30):
                pix[x, y] = PAL['black']
            elif y > 26:
                pix[x, y] = PAL['gold_dark'] if (x + y) % 2 == 0 else PAL['iron_dark']
            else:
                pix[x, y] = PAL['iron_mid'] if (x + y) % 2 == 0 else PAL['obsidian_mid']

    # Cupola focale e lente di quarzo
    for y in range(6, 18):
        for x in range(10, 23):
            dx = x - 16
            dy = y - 12
            d2 = dx * dx + dy * dy
            if d2 <= 36:
                if d2 >= 25:
                    pix[x, y] = PAL['gold_mid']
                elif d2 >= 9:
                    pix[x, y] = PAL['crystal_light'] if dx < 0 else PAL['crystal_mid']
                else:
                    pix[x, y] = PAL['white'] if dy < 0 else PAL['crystal_high']

    # Canne di focalizzazione ai 4 lati
    pix[16, 2] = PAL['laser_cyan']
    pix[16, 3] = PAL['laser_cyan']
    pix[16, 4] = PAL['white']
    pix[16, 5] = PAL['crystal_high']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_torretta_laser.png')
    print('Pixel art generata: sprite_torretta_laser.png (128x128)')


def gen_pixel_golem():
    """Golem di Quarzo (128x128): mostro massiccio fatto di massi di basalto e placche di quarzo ciano."""
    w, h = 32, 32
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Testa / Blocco superiore
    for y in range(4, 12):
        for x in range(11, 21):
            if x in (11, 20) or y in (4, 11):
                pix[x, y] = PAL['black']
            elif y in (7, 8) and x in (13, 14, 17, 18):
                # Occhi scintillanti a fenditura ciano
                pix[x, y] = PAL['crystal_high']
            else:
                pix[x, y] = PAL['obsidian_mid'] if (x + y) % 2 == 0 else PAL['obsidian_dark']

    # Torso massiccio corazzato di cristallo
    for y in range(12, 23):
        for x in range(7, 25):
            if x in (7, 24) or y == 22:
                pix[x, y] = PAL['black']
            elif 13 <= x <= 18 and 14 <= y <= 19:
                # Cuore di quarzo luminoso
                pix[x, y] = PAL['crystal_high'] if (x + y) % 2 == 0 else PAL['crystal_light']
            else:
                pix[x, y] = PAL['stone_mid'] if (x + y) % 3 == 0 else PAL['obsidian_mid']

    # Braccia e pugni rocciosi laterali
    for y in range(13, 27):
        for x in range(3, 7): # braccio sx
            if x == 3 or y == 26:
                pix[x, y] = PAL['black']
            else:
                pix[x, y] = PAL['obsidian_dark'] if y > 20 else PAL['crystal_light']
        for x in range(25, 29): # braccio dx
            if x == 28 or y == 26:
                pix[x, y] = PAL['black']
            else:
                pix[x, y] = PAL['obsidian_dark'] if y > 20 else PAL['crystal_mid']

    # Gambe pesanti
    for y in range(23, 30):
        for x in range(9, 14):
            pix[x, y] = PAL['obsidian_dark'] if y < 29 else PAL['black']
        for x in range(18, 23):
            pix[x, y] = PAL['obsidian_dark'] if y < 29 else PAL['black']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_golem.png')
    print('Pixel art generata: sprite_golem.png (128x128)')


def gen_pixel_spettro():
    """Spettro di Cristallo (128x128): fantasma etereo fluttuante fatto di schegge di ametista e ciano."""
    w, h = 32, 32
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Testa incappucciata fluttuante
    for y in range(4, 14):
        rw = 6 - abs(y - 8) // 2
        for x in range(16 - rw, 16 + rw + 1):
            if x in (16 - rw, 16 + rw) or y == 4:
                pix[x, y] = PAL['amethyst_dark']
            elif y in (8, 9) and x in (14, 18):
                pix[x, y] = PAL['crystal_high'] # Occhi spettrali
            else:
                pix[x, y] = PAL['amethyst_mid'] if (x + y) % 2 == 0 else PAL['amethyst_light']

    # Corpo etereo con lembi fluttuanti e sfaccettature
    for y in range(14, 28):
        wave = int(2.5 * math.sin((y + 4) * 0.5))
        rw = max(2, 8 - (y - 14) // 2)
        for x in range(16 - rw + wave, 16 + rw + wave + 1):
            if x == 16 - rw + wave or x == 16 + rw + wave or y == 27:
                pix[x, y] = PAL['crystal_dark'] if (x + y) % 2 == 0 else PAL['amethyst_dark']
            elif abs(x - 16 - wave) <= 2:
                pix[x, y] = PAL['crystal_high'] if (x + y) % 3 == 0 else PAL['crystal_light']
            else:
                pix[x, y] = PAL['amethyst_light'] if (x + y) % 2 == 0 else PAL['amethyst_mid']

    # Frammenti di cristallo orbitanti
    shards = [(6, 12), (25, 10), (8, 22), (24, 20), (16, 2)]
    for sx, sy in shards:
        pix[sx, sy] = PAL['crystal_high']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_spettro.png')
    print('Pixel art generata: sprite_spettro.png (128x128)')


def gen_pixel_gemma():
    """Icona Gemma Pura Raffinata (64x64): diamante/cristallo puro brillante da collezionare."""
    w, h = 16, 16
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Profilo brillante a diamante
    for y in range(2, 14):
        if y < 6:
            pw = 2 + (y - 2)
        else:
            pw = 5 - int((y - 6) * 0.8)
        pw = max(0, min(6, pw))
        left = 8 - pw
        right = 7 + pw
        for x in range(left, right + 1):
            if x == left or x == right or y in (2, 13):
                pix[x, y] = PAL['black']
            elif y in (3, 4) and x in (7, 8):
                pix[x, y] = PAL['white']
            elif x < 8:
                pix[x, y] = PAL['crystal_high'] if (x + y) % 2 == 0 else PAL['crystal_light']
            else:
                pix[x, y] = PAL['crystal_light'] if (x + y) % 2 == 0 else PAL['crystal_mid']

    pix[7, 3] = PAL['white']
    pix[8, 3] = PAL['white']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_gemma.png')
    print('Pixel art generata: sprite_gemma.png (64x64)')


def gen_pixel_hub_forgia():
    """Fucina dei Grandi Fabbri per l'Hub (128x128): camino fiammeggiante, incudine, martello e braci ardenti."""
    w, h = 32, 32
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Struttura della fucina in pietra
    for y in range(8, 30):
        for x in range(4, 28):
            if x in (4, 27) or y in (8, 29):
                pix[x, y] = PAL['black']
            elif y < 14:
                pix[x, y] = PAL['stone_mid'] if (x + y) % 2 == 0 else PAL['stone_dark']
            else:
                pix[x, y] = PAL['stone_dark'] if (x + y) % 3 == 0 else PAL['stone_shadow']

    # Tetto spiovente in tegole di bronzo
    for x in range(2, 30):
        pix[x, 7] = PAL['black']
        pix[x, 8] = PAL['gold_dark'] if x % 2 == 0 else PAL['gold_mid']

    # Canna fumaria e fumo
    for y in range(1, 8):
        for x in range(13, 19):
            pix[x, y] = PAL['iron_dark'] if x in (13, 18) else PAL['iron_light']
    # Nuvole di fumo
    for fx, fy in [(15, 0), (16, 0), (14, 1), (17, 1)]:
        pix[fx, fy] = PAL['iron_white']

    # Forno a bocca fiammeggiante al centro
    for y in range(17, 26):
        for x in range(10, 22):
            if x in (10, 21) or y in (17, 25):
                pix[x, y] = PAL['black']
            else:
                pix[x, y] = PAL['fire_dark'] if y == 18 else (PAL['fire_orange'] if y < 22 else PAL['fire_yellow'])
    pix[15, 21] = PAL['fire_white']
    pix[16, 21] = PAL['fire_white']

    # Incudine d'acciaio davanti al forno
    for y in range(23, 29):
        for x in range(12, 20):
            if y == 23 or y == 28 or x in (12, 19):
                pix[x, y] = PAL['black']
            else:
                pix[x, y] = PAL['iron_white'] if y == 24 and x in (14, 15) else PAL['iron_mid']

    # Martello nanico dorato appoggiato
    for i in range(5):
        pix[22 + i, 22 - i] = PAL['wood_mid']
    pix[25, 17] = PAL['gold_light']
    pix[26, 17] = PAL['gold_light']
    pix[25, 18] = PAL['gold_dark']
    pix[26, 18] = PAL['gold_dark']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_hub_forgia.png')
    print('Pixel art generata: sprite_hub_forgia.png (128x128)')


def gen_pixel_hub_taverna():
    """Taverna della Botte Nanica per l'Hub (128x128): legno caldo, boccale schiumante e finestra illuminata."""
    w, h = 32, 32
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Edificio in legno di quercia
    for y in range(8, 30):
        for x in range(4, 28):
            if x in (4, 27) or y in (8, 29):
                pix[x, y] = PAL['black']
            elif y < 14:
                pix[x, y] = PAL['ruby_dark'] if (x + y) % 2 == 0 else PAL['wood_bark']
            else:
                pix[x, y] = PAL['wood_mid'] if (x + y) % 3 == 0 else PAL['wood_light']

    # Tetto spiovente a scandole
    for x in range(2, 30):
        pix[x, 6] = PAL['black']
        pix[x, 7] = PAL['ruby_mid'] if x % 2 == 0 else PAL['ruby_dark']

    # Porta con arco in legno
    for y in range(18, 30):
        for x in range(12, 20):
            if x in (12, 19) or y == 18:
                pix[x, y] = PAL['black']
            else:
                pix[x, y] = PAL['wood_dark']
    pix[18, 24] = PAL['gold_light'] # Maniglia in ottone

    # Finestra calda illuminata a sinistra
    for y in range(15, 21):
        for x in range(6, 10):
            pix[x, y] = PAL['fire_yellow'] if (x + y) % 2 == 0 else PAL['fire_white']

    # Insegna a boccale di birra schiumante a destra
    for y in range(12, 18):
        for x in range(22, 26):
            if y == 12:
                pix[x, y] = PAL['fire_white'] # Schiuma
            else:
                pix[x, y] = PAL['gold_light'] # Birra
    # Manico del boccale
    pix[26, 14] = PAL['gold_dark']
    pix[26, 15] = PAL['gold_dark']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_hub_taverna.png')
    print('Pixel art generata: sprite_hub_taverna.png (128x128)')


def gen_pixel_hub_banca():
    """Tesoreria e Banca del Clan per l'Hub (128x128): caveau in granito, forzieri e mucchio d'oro."""
    w, h = 32, 32
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Mura massicce del caveau
    for y in range(6, 30):
        for x in range(4, 28):
            if x in (4, 27) or y in (6, 29):
                pix[x, y] = PAL['black']
            else:
                pix[x, y] = PAL['stone_mid'] if (x + y) % 2 == 0 else PAL['stone_dark']

    # Portone a caveau tondo in ferro e oro
    cx, cy = 16, 18
    for dy in range(-7, 8):
        for dx in range(-7, 8):
            d2 = dx * dx + dy * dy
            if d2 <= 49:
                x = cx + dx
                y = cy + dy
                if 42 <= d2 <= 49:
                    pix[x, y] = PAL['gold_mid'] if (dx + dy) % 2 == 0 else PAL['black']
                elif d2 <= 8:
                    pix[x, y] = PAL['gold_light'] # Runa centrale
                else:
                    pix[x, y] = PAL['iron_light'] if dx > 0 else PAL['iron_dark']

    # Cumulo di monete d'oro e gemme davanti
    for y in range(25, 30):
        for x in range(6, 26):
            if (x * 7 + y * 3) % 4 == 0:
                pix[x, y] = PAL['gold_high']
            elif (x + y) % 2 == 0:
                pix[x, y] = PAL['gold_light']
            elif (x + y) % 5 == 0:
                pix[x, y] = PAL['gem_cyan_light']
            else:
                pix[x, y] = PAL['gold_mid']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_hub_banca.png')
    print('Pixel art generata: sprite_hub_banca.png (128x128)')


def gen_pixel_hub_cancello():
    """Portale delle Spedizioni per l'Hub (128x128): arco scolpito nella roccia, binari e carrello da miniera."""
    w, h = 32, 32
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Arcata monumentale in roccia
    for y in range(4, 30):
        for x in range(2, 30):
            if y < 10 or x < 8 or x > 23:
                pix[x, y] = PAL['stone_dark'] if (x + y) % 2 == 0 else PAL['stone_mid']
            elif y >= 10 and 8 <= x <= 23:
                # Oscurità della galleria
                pix[x, y] = PAL['black']

    # Lanterne runiche ai lati dell'arco
    for ly in (12, 13):
        pix[6, ly] = PAL['gem_cyan_white']
        pix[25, ly] = PAL['gem_cyan_white']

    # Binari in ferro che escono dal buio
    for y in range(20, 30):
        pix[11, y] = PAL['iron_light'] if y % 2 == 0 else PAL['iron_dark']
        pix[20, y] = PAL['iron_light'] if y % 2 == 0 else PAL['iron_dark']
        if y in (22, 26):
            for x in range(10, 22):
                pix[x, y] = PAL['wood_dark']

    # Carrello da miniera
    for y in range(22, 28):
        for x in range(12, 20):
            if y == 22 or y == 27 or x in (12, 19):
                pix[x, y] = PAL['black']
            else:
                pix[x, y] = PAL['wood_mid'] if y < 25 else PAL['iron_dark']
    # Ruote del carrello
    pix[12, 28] = PAL['iron_light']
    pix[19, 28] = PAL['iron_light']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_hub_cancello.png')
    print('Pixel art generata: sprite_hub_cancello.png (128x128)')


def gen_pixel_nano():
    """Capoclan Nano Guerriero (96x96): elmo con corna d'oro, folta barba rossa, armatura e martello."""
    w, h = 24, 24
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # Elmo nanico con corna
    for y in range(2, 8):
        for x in range(7, 17):
            pix[x, y] = PAL['iron_light'] if x < 12 else PAL['iron_mid']
    # Corna dell'elmo
    pix[5, 2] = PAL['gold_light']
    pix[6, 3] = PAL['gold_mid']
    pix[18, 2] = PAL['gold_light']
    pix[17, 3] = PAL['gold_mid']

    # Occhi fieri
    pix[9, 8] = PAL['black']
    pix[14, 8] = PAL['black']
    pix[10, 8] = PAL['iron_white']
    pix[13, 8] = PAL['iron_white']

    # Grande barba rossa/arancione
    for y in range(9, 18):
        bw = 6 - int((y - 9) * 0.3)
        for x in range(12 - bw, 12 + bw + 1):
            if x == 12 - bw or x == 12 + bw or y == 17:
                pix[x, y] = PAL['fire_dark']
            else:
                pix[x, y] = PAL['fire_orange'] if (x + y) % 2 == 0 else PAL['gold_dark']
    # Anello ferma-barba in oro
    pix[11, 15] = PAL['gold_light']
    pix[12, 15] = PAL['gold_light']

    # Armatura nanica e gambe
    for y in range(17, 22):
        for x in range(8, 16):
            pix[x, y] = PAL['iron_mid'] if (x + y) % 2 == 0 else PAL['iron_dark']
    # Stivali
    pix[8, 22] = PAL['wood_dark']
    pix[9, 22] = PAL['wood_dark']
    pix[14, 22] = PAL['wood_dark']
    pix[15, 22] = PAL['wood_dark']

    out = scale_nearest(im, 4)
    out.save(OUT_DIR / 'sprite_nano.png')
    print('Pixel art generata: sprite_nano.png (96x96)')


def gen_pixel_app_icon():
    """Icona dell'App Fantasy Nanica 16-bit per Ferro e Rimbalzi:
    - Bordo in pietra runica e rivetti dorati
    - Sfondo ossidiana con braci incandescenti
    - Incudine d'acciaio con corno splendente
    - Due picconi da guerra nanici incrociati con aste di quercia
    - La Sfera Runica infuocata al centro con glifo nanico
    - Scintille dorate di forgia
    Generata a matrice pixel pura e scalata per tutti i formati mipmap e drawable (fino a 512x512).
    """
    w, h = 48, 48
    im = create_pixel_canvas(w, h)
    pix = im.load()

    # 1. Base arrotondata dell'icona (scudo quadrato con angoli smussati)
    for y in range(h):
        for x in range(w):
            dx = min(x, w - 1 - x)
            dy = min(y, h - 1 - y)
            dist_corner = 0
            if dx < 6 and dy < 6:
                dist_corner = (6 - dx) ** 2 + (6 - dy) ** 2
                if dist_corner > 36:
                    continue

            if dist_corner > 24 or x <= 1 or x >= w - 2 or y <= 1 or y >= h - 2:
                pix[x, y] = PAL['stone_shadow']
            elif dist_corner > 16 or x == 2 or x == w - 3 or y == 2 or y == h - 3:
                pix[x, y] = PAL['gold_dark'] if (x + y) % 2 == 0 else PAL['gold_mid']
            elif dist_corner > 10 or x == 3 or x == w - 4 or y == 3 or y == h - 4:
                pix[x, y] = PAL['gold_light'] if (x + y) % 4 == 0 else PAL['gold_mid']
            elif x == 4 or x == w - 5 or y == 4 or y == h - 5:
                pix[x, y] = PAL['black']
            else:
                if y > 34:
                    pix[x, y] = PAL['fire_shadow'] if (x + y) % 2 == 0 else PAL['fire_dark']
                elif y > 26:
                    pix[x, y] = PAL['fire_shadow'] if (x + y) % 3 == 0 else PAL['stone_dark']
                else:
                    pix[x, y] = PAL['black'] if (x + y) % 4 != 0 else PAL['stone_shadow']

    # Rivetti d'oro nei 4 angoli
    rivets = [(7, 7), (40, 7), (7, 40), (40, 40)]
    for rx, ry in rivets:
        pix[rx, ry] = PAL['gold_high']
        pix[rx - 1, ry] = PAL['gold_dark']
        pix[rx + 1, ry] = PAL['gold_dark']
        pix[rx, ry - 1] = PAL['gold_dark']
        pix[rx, ry + 1] = PAL['gold_dark']

    # 2. Picconi Nanici Incrociati (Aste di quercia che si incrociano a X)
    for i in range(27):
        px = 11 + i
        py = 37 - i
        pix[px, py] = PAL['wood_mid']
        if px + 1 < w:
            pix[px + 1, py] = PAL['wood_dark']

    for i in range(27):
        px = 36 - i
        py = 37 - i
        pix[px, py] = PAL['wood_mid']
        if px - 1 >= 0:
            pix[px - 1, py] = PAL['wood_shadow']

    # Teste d'acciaio dei picconi
    pick_left = [
        (8, 7), (9, 7), (10, 8), (11, 9), (12, 10), (13, 11), (14, 12),
        (7, 8), (8, 8), (9, 9), (10, 10), (11, 11),
        (6, 9), (7, 9), (8, 10)
    ]
    for px, py in pick_left:
        pix[px, py] = PAL['iron_light'] if py == 7 or px == 6 else PAL['iron_mid']
    pix[12, 12] = PAL['gold_light']
    pix[13, 13] = PAL['gold_mid']

    pick_right = [
        (39, 7), (38, 7), (37, 8), (36, 9), (35, 10), (34, 11), (33, 12),
        (40, 8), (39, 8), (38, 9), (37, 10), (36, 11),
        (41, 9), (40, 9), (39, 10)
    ]
    for px, py in pick_right:
        pix[px, py] = PAL['iron_light'] if py == 7 or px == 41 else PAL['iron_mid']
    pix[35, 12] = PAL['gold_light']
    pix[34, 13] = PAL['gold_mid']

    # 3. Grande Incudine Nanica al centro-basso (y=32..42, x=13..34)
    for y in range(39, 43):
        for x in range(16, 32):
            if x == 16 or x == 31 or y == 42:
                pix[x, y] = PAL['black']
            elif y == 39:
                pix[x, y] = PAL['iron_light']
            else:
                pix[x, y] = PAL['iron_mid'] if (x + y) % 2 == 0 else PAL['iron_dark']
    for y in range(36, 39):
        for x in range(19, 29):
            pix[x, y] = PAL['iron_dark'] if x == 19 or x == 28 else PAL['iron_mid']
    for y in range(32, 36):
        for x in range(13, 35):
            if x == 13 or x == 34 or y == 32:
                pix[x, y] = PAL['black']
            elif y == 33:
                pix[x, y] = PAL['iron_white'] if x > 16 else PAL['iron_light']
            else:
                pix[x, y] = PAL['iron_light'] if x > 20 else PAL['iron_mid']
    pix[12, 33] = PAL['iron_light']
    pix[12, 34] = PAL['black']

    # 4. Sfera Runica Infuocata ("Rimbalzi") al centro sopra l'incudine (cx=24, cy=21, r=6.5)
    cx, cy, r = 24, 21, 6.5
    for y in range(14, 29):
        for x in range(17, 32):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= r:
                if d > r - 1.0:
                    pix[x, y] = PAL['fire_shadow']
                elif d > r - 2.2:
                    pix[x, y] = PAL['fire_mid']
                elif d > r - 3.5:
                    pix[x, y] = PAL['fire_orange']
                elif d > r - 4.8:
                    pix[x, y] = PAL['fire_yellow']
                else:
                    pix[x, y] = PAL['fire_white']

    # Glifo / Martello runico al centro della sfera
    pix[23, 19] = PAL['gold_shadow']
    pix[24, 19] = PAL['gold_shadow']
    pix[25, 19] = PAL['gold_shadow']
    pix[22, 20] = PAL['gold_dark']
    pix[23, 20] = PAL['gold_high']
    pix[24, 20] = PAL['gold_high']
    pix[25, 20] = PAL['gold_high']
    pix[26, 20] = PAL['gold_dark']
    pix[24, 21] = PAL['gold_dark']
    pix[24, 22] = PAL['gold_dark']
    pix[24, 23] = PAL['gold_dark']

    # 5. Scintille di Forgia dorate e incandescenti
    sparks = [(17, 13), (30, 13), (24, 12), (15, 22), (32, 22), (20, 30), (28, 30)]
    for sx, sy in sparks:
        pix[sx, sy] = PAL['fire_yellow']

    # Salvataggio nei diversi formati
    res_base = pathlib.Path(__file__).resolve().parent.parent / 'android/app/src/main/res'
    
    icon_512 = im.resize((512, 512), Image.Resampling.NEAREST)
    icon_512.save(OUT_DIR / 'ic_launcher.png')

    round_mask = Image.new('L', (512, 512), 0)
    mask_draw = ImageDraw.Draw(round_mask)
    mask_draw.ellipse((8, 8, 504, 504), fill=255)
    icon_round_512 = icon_512.copy()
    icon_round_512.putalpha(round_mask)
    icon_round_512.save(OUT_DIR / 'ic_launcher_round.png')

    densities = {
        'mipmap-mdpi': 48,
        'mipmap-hdpi': 72,
        'mipmap-xhdpi': 96,
        'mipmap-xxhdpi': 144,
        'mipmap-xxxhdpi': 192,
    }
    for folder, size in densities.items():
        target_dir = res_base / folder
        target_dir.mkdir(parents=True, exist_ok=True)
        scaled = im.resize((size, size), Image.Resampling.NEAREST)
        scaled.save(target_dir / 'ic_launcher.png')
        r_mask = Image.new('L', (size, size), 0)
        d_mask = ImageDraw.Draw(r_mask)
        d_mask.ellipse((1, 1, size - 2, size - 2), fill=255)
        scaled_r = scaled.copy()
        scaled_r.putalpha(r_mask)
        scaled_r.save(target_dir / 'ic_launcher_round.png')

    print('Icona App Pixel Art generata per drawable e tutte le densita mipmap (48..512px)!')


if __name__ == '__main__':
    print('=== Inizio generazione Asset TRUE PIXEL ART (16-bit / 32-bit) ===')
    gen_pixel_miniera()
    gen_pixel_forgia_aperta()
    gen_pixel_forgia_chiusa()
    gen_pixel_forziere()
    gen_pixel_muro()
    gen_pixel_balista()
    gen_pixel_scudo()
    gen_pixel_sfera()
    gen_pixel_goblin()
    gen_pixel_caverna_sfondo()
    # Nuovi Sprite Foresta dei Nani (Fase 2)
    gen_pixel_foresta_sfondo()
    gen_pixel_albero()
    gen_pixel_segheria()
    gen_pixel_ragno()
    gen_pixel_fungo()
    gen_pixel_muro_legno()
    gen_pixel_legno()
    # Nuovi Sprite Caverna dei Cristalli (Fase 3)
    gen_pixel_cristalli_sfondo()
    gen_pixel_cristallo()
    gen_pixel_tagliapietre()
    gen_pixel_forziere_cristallo()
    gen_pixel_prisma()
    gen_pixel_muro_ossidiana()
    gen_pixel_torretta_laser()
    gen_pixel_golem()
    gen_pixel_spettro()
    gen_pixel_gemma()
    # Edifici e Personaggi Hub Interattivo
    gen_pixel_hub_forgia()
    gen_pixel_hub_taverna()
    gen_pixel_hub_banca()
    gen_pixel_hub_cancello()
    gen_pixel_nano()
    # Icona Ufficiale App Pixel Art
    gen_pixel_app_icon()
    print('=== Tutti gli sprite 16-bit Pixel Art generati con successo! ===')
