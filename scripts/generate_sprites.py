#!/usr/bin/env python3
"""
Generatore di Sprite Vettoriali e Texture Fantasy per Ferro e Rimbalzi.
Produce asset grafici ad alta risoluzione (512x512 supersampling con filtro Lanczos),
completi di finiture metalliche, ombreggiature e bordature per risaltare su mobile.
"""
import math
import pathlib
from PIL import Image, ImageDraw, ImageFilter

OUT_DIR = pathlib.Path(__file__).resolve().parent.parent / 'android/app/src/main/res/drawable'
OUT_DIR.mkdir(parents=True, exist_ok=True)

# Palette Nanica Fantasy
COL_BG_DARK = (20, 12, 9, 255)
COL_STONE_DARK = (41, 37, 36, 255)
COL_STONE_MID = (68, 64, 60, 255)
COL_STONE_LIGHT = (120, 113, 108, 255)
COL_GOLD_BRIGHT = (251, 191, 36, 255)
COL_GOLD_DEEP = (217, 119, 6, 255)
COL_BRONZE = (180, 83, 9, 255)
COL_IRON_LIGHT = (226, 232, 240, 255)
COL_IRON_MID = (148, 163, 184, 255)
COL_IRON_DARK = (30, 41, 59, 255)
COL_FIRE_YELLOW = (254, 240, 138, 255)
COL_FIRE_ORANGE = (249, 115, 22, 255)
COL_FIRE_RED = (220, 38, 38, 255)
COL_WOOD_DARK = (69, 26, 3, 255)
COL_WOOD_MID = (120, 53, 15, 255)
COL_WOOD_LIGHT = (180, 83, 9, 255)
COL_GEM_CYAN = (34, 211, 238, 255)
COL_GEM_RUBY = (244, 63, 94, 255)
COL_GOBLIN_SKIN = (74, 222, 128, 255)
COL_GOBLIN_DARK = (22, 101, 52, 255)
COL_INK = (15, 10, 8, 255)
COL_WHITE = (255, 255, 255, 255)


def finalize_sprite(im_hi, target_size=(256, 256), outline_width=8, outline_color=(254, 243, 199, 220), drop_shadow=True):
    """Aggiunge bordo anti-alias e ombreggiatura morbida, scalando con Lanczos."""
    w, h = im_hi.size
    alpha = im_hi.split()[-1]

    # Ombra d'appoggio morbida
    shadow_layer = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    if drop_shadow:
        shadow_mask = alpha.filter(ImageFilter.GaussianBlur(radius=16))
        shadow_img = Image.new('RGBA', (w, h), (0, 0, 0, 160))
        shadow_layer.paste(shadow_img, (0, 12), shadow_mask)

    # Bordo sagomato die-cut per massima leggibilità su sfondi scuri
    border_layer = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    if outline_width > 0:
        dilated = alpha.filter(ImageFilter.MaxFilter(outline_width * 2 + 1))
        dilated = dilated.filter(ImageFilter.GaussianBlur(radius=outline_width * 0.35))
        dilated = dilated.point(lambda p: 255 if p > 80 else int((p / 80.0) ** 1.5 * 255))
        border_fill = Image.new('RGBA', (w, h), outline_color)
        border_layer.paste(border_fill, (0, 0), dilated)

    # Compositing: Ombra -> Bordo -> Immagine principale
    out = Image.alpha_composite(shadow_layer, border_layer)
    out = Image.alpha_composite(out, im_hi)
    return out.resize(target_size, Image.Resampling.LANCZOS)


def gen_miniera():
    """Giacimento montano di ferro con cristalli celesti e picconi dorati incrociati."""
    im = Image.new('RGBA', (512, 512), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # 1. Roccia granitica massiccia
    rock_pts = [
        (80, 420), (100, 260), (160, 140), (260, 90),
        (370, 130), (430, 240), (440, 420), (320, 440), (180, 440)
    ]
    draw.polygon(rock_pts, fill=COL_STONE_DARK, outline=COL_INK)

    # Faccette rocciose tridimensionali
    draw.polygon([(160, 140), (260, 90), (270, 240), (170, 260)], fill=COL_STONE_LIGHT)
    draw.polygon([(260, 90), (370, 130), (360, 260), (270, 240)], fill=COL_STONE_MID)
    draw.polygon([(100, 260), (170, 260), (180, 430), (90, 410)], fill=(50, 44, 40, 255))
    draw.polygon([(360, 260), (430, 240), (430, 410), (330, 430)], fill=(32, 28, 25, 255))

    # 2. Venature metalliche argentee di ferro grezzo
    draw.line([(140, 220), (190, 250), (230, 230)], fill=COL_IRON_LIGHT, width=12)
    draw.line([(290, 260), (340, 290), (380, 270)], fill=COL_IRON_LIGHT, width=12)

    # 3. Cristalli azzurri incastonati
    for cx, cy, sz in [(240, 160, 36), (330, 200, 28), (170, 310, 30)]:
        pts = [(cx, cy - sz), (cx + sz * 0.7, cy), (cx, cy + sz), (cx - sz * 0.7, cy)]
        draw.polygon(pts, fill=COL_GEM_CYAN, outline=COL_WHITE)
        draw.polygon([(cx, cy - sz), (cx + sz * 0.7, cy), (cx, cy)], fill=(165, 243, 252, 255))

    # 4. Picconi nanici dorati incrociati
    for angle, offs in [(math.radians(38), -1), (math.radians(-38), 1)]:
        # Manico in legno
        x1 = 256 - math.sin(angle) * 160
        y1 = 280 + math.cos(angle) * 160
        x2 = 256 + math.sin(angle) * 160
        y2 = 280 - math.cos(angle) * 160
        draw.line([(x1, y1), (x2, y2)], fill=COL_WOOD_MID, width=18)
        draw.line([(x1, y1), (x2, y2)], fill=COL_GOLD_BRIGHT, width=6)
        # Testa a doppia punta del piccone
        hx1 = x2 - math.cos(angle) * 60
        hy1 = y2 - math.sin(angle) * 60
        hx2 = x2 + math.cos(angle) * 60
        hy2 = y2 + math.sin(angle) * 60
        draw.line([(hx1, hy1), (hx2, hy2)], fill=COL_IRON_LIGHT, width=22)
        draw.ellipse([x2 - 16, y2 - 16, x2 + 16, y2 + 16], fill=COL_GOLD_DEEP, outline=COL_INK, width=4)

    final = finalize_sprite(im, target_size=(256, 192), outline_width=10, outline_color=(254, 240, 138, 240))
    final.save(OUT_DIR / 'sprite_miniera.png')
    print('Generato sprite_miniera.png')


def gen_forgia_aperta():
    """Fucina nanica fusa con crogiolo incandescente, fiamma viva e incudine scura."""
    im = Image.new('RGBA', (512, 512), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # 1. Struttura in pietra e mattoni refrattari
    draw.rounded_rectangle([60, 80, 452, 450], radius=32, fill=COL_STONE_DARK, outline=COL_INK, width=14)
    # Camini superiori
    draw.rounded_rectangle([100, 40, 170, 100], radius=12, fill=COL_STONE_MID, outline=COL_INK, width=8)
    draw.rounded_rectangle([342, 40, 412, 100], radius=12, fill=COL_STONE_MID, outline=COL_INK, width=8)

    # Cerchiature in ferro e borchie dorate
    draw.line([(60, 130), (452, 130)], fill=COL_BRONZE, width=16)
    draw.line([(60, 410), (452, 410)], fill=COL_BRONZE, width=16)
    for bx in [100, 180, 256, 332, 412]:
        draw.ellipse([bx - 10, 120, bx + 10, 140], fill=COL_GOLD_BRIGHT, outline=COL_INK, width=4)
        draw.ellipse([bx - 10, 400, bx + 10, 420], fill=COL_GOLD_BRIGHT, outline=COL_INK, width=4)

    # 2. Bocca della fornace incandescente (Lava & Fuoco del drago)
    draw.rounded_rectangle([96, 160, 416, 380], radius=24, fill=COL_FIRE_RED)
    draw.rounded_rectangle([112, 176, 400, 364], radius=20, fill=COL_FIRE_ORANGE)
    draw.rounded_rectangle([130, 200, 382, 344], radius=16, fill=COL_FIRE_YELLOW)

    # Bagliore del metallo liquido
    draw.ellipse([160, 270, 352, 340], fill=COL_WHITE)

    # 3. Grande Incudine nanica al centro
    draw.rectangle([190, 300, 322, 360], fill=COL_IRON_DARK, outline=COL_INK, width=6)
    draw.rectangle([210, 250, 302, 300], fill=(24, 20, 18, 255), outline=COL_INK, width=6)
    draw.polygon([(180, 220), (332, 220), (312, 250), (200, 250)], fill=COL_IRON_MID, outline=COL_INK, width=6)
    draw.polygon([(332, 220), (370, 235), (332, 250)], fill=COL_IRON_LIGHT, outline=COL_INK, width=4)

    # Martello del fabbro appoggiato
    draw.line([(170, 200), (250, 270)], fill=COL_WOOD_MID, width=10)
    draw.rectangle([145, 185, 185, 215], fill=COL_IRON_LIGHT, outline=COL_INK, width=4)

    final = finalize_sprite(im, target_size=(256, 192), outline_width=10, outline_color=(254, 215, 170, 240))
    final.save(OUT_DIR / 'sprite_forgia_aperta.png')
    print('Generato sprite_forgia_aperta.png')


def gen_forgia_chiusa():
    """Fucina con pesante portone blindato in ferro borchiato e rune difensive."""
    im = Image.new('RGBA', (512, 512), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # Struttura in pietra
    draw.rounded_rectangle([60, 80, 452, 450], radius=32, fill=COL_STONE_DARK, outline=COL_INK, width=14)
    # Camini superiori
    draw.rounded_rectangle([100, 40, 170, 100], radius=12, fill=COL_STONE_MID, outline=COL_INK, width=8)
    draw.rounded_rectangle([342, 40, 412, 100], radius=12, fill=COL_STONE_MID, outline=COL_INK, width=8)

    # Portone blindato in piastre d'acciaio scuro
    draw.rounded_rectangle([96, 160, 416, 380], radius=24, fill=(35, 30, 28, 255), outline=COL_INK, width=10)

    # Travi di rinforzo a croce
    draw.line([(96, 160), (416, 380)], fill=COL_BRONZE, width=20)
    draw.line([(96, 380), (416, 160)], fill=COL_BRONZE, width=20)

    # Sigillo runico protettivo centrale (Scudo e martello)
    draw.ellipse([206, 220, 306, 320], fill=COL_GOLD_DEEP, outline=COL_GOLD_BRIGHT, width=8)
    draw.polygon([(256, 235), (290, 265), (275, 305), (237, 305), (222, 265)], fill=COL_STONE_DARK)

    # Fessure da cui trapela luce fusa ardente
    draw.line([(120, 365), (392, 365)], fill=COL_FIRE_ORANGE, width=6)
    draw.line([(120, 175), (392, 175)], fill=COL_FIRE_ORANGE, width=6)

    final = finalize_sprite(im, target_size=(256, 192), outline_width=10, outline_color=(254, 240, 138, 220))
    final.save(OUT_DIR / 'sprite_forgia_chiusa.png')
    print('Generato sprite_forgia_chiusa.png')


def gen_forziere():
    """Scrigno reale nanico in legno di quercia rinforzato con ottone, lingotti e gemme."""
    im = Image.new('RGBA', (512, 512), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # 1. Corpo del forziere in rovere scuro
    draw.rounded_rectangle([60, 160, 452, 440], radius=28, fill=COL_WOOD_DARK, outline=COL_INK, width=12)
    # Doghe in legno
    for x in [130, 210, 302, 382]:
        draw.line([(x, 160), (x, 440)], fill=(45, 18, 5, 255), width=6)

    # 2. Coperchio aperto arcuato con interno dorato
    draw.polygon([(60, 160), (452, 160), (480, 80), (32, 80)], fill=(75, 30, 8, 255), outline=COL_INK, width=10)

    # 3. Tesoro straripante: Lingotti d'oro e ferro forgiato
    # Lingotti d'oro
    for bx, by in [(120, 140), (220, 130), (320, 140), (170, 110), (270, 100)]:
        draw.polygon([(bx, by + 30), (bx + 80, by + 30), (bx + 95, by), (bx + 15, by)], fill=COL_GOLD_BRIGHT, outline=COL_INK, width=4)
        draw.polygon([(bx + 15, by), (bx + 95, by), (bx + 85, by - 15), (bx + 5, by - 15)], fill=(254, 240, 138, 255), outline=COL_INK, width=4)

    # Lingotti d'acciaio / ferro
    for bx, by in [(90, 165), (350, 165), (220, 165)]:
        draw.polygon([(bx, by + 25), (bx + 70, by + 25), (bx + 85, by), (bx + 15, by)], fill=COL_IRON_LIGHT, outline=COL_INK, width=4)

    # Monete e gemme
    for mx, my, col in [(180, 165, COL_GOLD_BRIGHT), (240, 180, COL_GOLD_DEEP), (280, 165, COL_GEM_RUBY), (320, 180, COL_GEM_CYAN)]:
        draw.ellipse([mx - 14, my - 14, mx + 14, my + 14], fill=col, outline=COL_WHITE, width=3)

    # 4. Bordature metalliche e lucchetto dorato con rubino
    draw.rectangle([60, 160, 452, 195], fill=COL_BRONZE, outline=COL_INK, width=8)
    draw.rectangle([60, 405, 452, 440], fill=COL_BRONZE, outline=COL_INK, width=8)
    draw.rectangle([100, 160, 140, 440], fill=COL_GOLD_DEEP, outline=COL_INK, width=6)
    draw.rectangle([372, 160, 412, 440], fill=COL_GOLD_DEEP, outline=COL_INK, width=6)

    # Lucchetto centrale regale
    draw.rounded_rectangle([220, 240, 292, 340], radius=14, fill=COL_GOLD_BRIGHT, outline=COL_INK, width=8)
    draw.ellipse([242, 270, 270, 298], fill=COL_GEM_RUBY, outline=COL_INK, width=4)

    final = finalize_sprite(im, target_size=(256, 192), outline_width=10, outline_color=(254, 240, 138, 250))
    final.save(OUT_DIR / 'sprite_forziere.png')
    print('Generato sprite_forziere.png')


def gen_muro():
    """Bastione difensivo in blocchi squadrati di granito nanico con merlature e sigilli."""
    im = Image.new('RGBA', (512, 256), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # 1. Base del bastione in pietra massiccia
    draw.rounded_rectangle([20, 40, 492, 230], radius=16, fill=COL_STONE_DARK, outline=COL_INK, width=10)

    # Merlature superiori
    for mx in [30, 130, 230, 330, 430]:
        draw.rectangle([mx, 15, mx + 52, 45], fill=COL_STONE_MID, outline=COL_INK, width=6)

    # Blocchi di mattoni con bisellature
    for y, shift in [(50, 0), (105, 45), (160, 0)]:
        for x in range(25, 480, 90):
            bx = x + shift
            if bx + 80 <= 490:
                draw.rounded_rectangle([bx, y, bx + 78, y + 46], radius=6, fill=COL_STONE_MID, outline=COL_STONE_LIGHT, width=3)
                draw.line([(bx + 4, y + 4), (bx + 74, y + 4)], fill=(168, 162, 158, 255), width=3)

    # Sigillo runico nanico al centro
    draw.ellipse([216, 95, 296, 175], fill=COL_GOLD_DEEP, outline=COL_GOLD_BRIGHT, width=6)
    draw.polygon([(256, 108), (280, 155), (232, 155)], fill=COL_STONE_DARK)

    final = finalize_sprite(im, target_size=(256, 128), outline_width=8, outline_color=(226, 232, 240, 240))
    final.save(OUT_DIR / 'sprite_muro.png')
    print('Generato sprite_muro.png')


def gen_balista():
    """Balista a ripetizione automatica nanica con affusto girevole e dardo infuocato."""
    im = Image.new('RGBA', (512, 512), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # 1. Piattaforma circolare girevole in pietra runica
    draw.ellipse([80, 260, 432, 470], fill=COL_STONE_DARK, outline=COL_INK, width=12)
    draw.ellipse([110, 280, 402, 450], fill=COL_STONE_MID, outline=COL_BRONZE, width=8)

    # Borchie di fissaggio a terra
    for ang in [0, 45, 90, 135, 180, 225, 270, 315]:
        rad = math.radians(ang)
        bx = 256 + math.cos(rad) * 125
        by = 365 + math.sin(rad) * 65
        draw.ellipse([bx - 8, by - 8, bx + 8, by + 8], fill=COL_GOLD_BRIGHT, outline=COL_INK, width=3)

    # 2. Torretta centrale e affusto in legno e bronzo
    draw.rounded_rectangle([200, 180, 312, 380], radius=18, fill=COL_WOOD_DARK, outline=COL_INK, width=10)
    draw.line([(256, 120), (256, 360)], fill=COL_BRONZE, width=28)

    # 3. Grande arco da guerra composito orizzontale
    bow_pts = [(40, 220), (140, 150), (256, 140), (372, 150), (472, 220)]
    for i in range(len(bow_pts) - 1):
        draw.line([bow_pts[i], bow_pts[i + 1]], fill=COL_WOOD_LIGHT, width=26)
        draw.line([bow_pts[i], bow_pts[i + 1]], fill=COL_GOLD_BRIGHT, width=8)

    # Corda tesa in acciaio runico
    draw.line([(40, 220), (256, 260)], fill=COL_IRON_LIGHT, width=6)
    draw.line([(472, 220), (256, 260)], fill=COL_IRON_LIGHT, width=6)

    # 4. Dardo d'acciaio con punta infuocata pronto al tiro
    draw.line([(256, 270), (256, 70)], fill=COL_IRON_LIGHT, width=16)
    draw.polygon([(256, 40), (232, 85), (280, 85)], fill=COL_FIRE_YELLOW, outline=COL_FIRE_ORANGE, width=4)
    # Piume di coda rosse
    draw.polygon([(240, 270), (230, 290), (256, 280)], fill=COL_FIRE_RED)
    draw.polygon([(272, 270), (282, 290), (256, 280)], fill=COL_FIRE_RED)

    final = finalize_sprite(im, target_size=(256, 256), outline_width=10, outline_color=(254, 240, 138, 240))
    final.save(OUT_DIR / 'sprite_balista.png')
    print('Generato sprite_balista.png')


def gen_scudo_nanico():
    """Scudo torre nanico intagliato con testa di cinghiale e borchie d'oro (Paddle)."""
    im = Image.new('RGBA', (512, 256), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # 1. Corpo dello scudo a losanga massiccia
    shield_pts = [
        (30, 128), (90, 40), (422, 40), (482, 128),
        (422, 216), (90, 216)
    ]
    draw.polygon(shield_pts, fill=COL_WOOD_DARK, outline=COL_INK)

    # Finitura in ferro azzurrato
    inner_pts = [
        (46, 128), (98, 56), (414, 56), (466, 128),
        (414, 200), (98, 200)
    ]
    draw.polygon(inner_pts, fill=(51, 65, 85, 255), outline=COL_BRONZE, width=8)

    # 2. Fascia dorata centrale con rune
    draw.rectangle([140, 70, 372, 186], fill=COL_GOLD_DEEP, outline=COL_GOLD_BRIGHT, width=6)

    # Cresta centrale nanica (Testa di martello e stella a 8 punte)
    cx, cy = 256, 128
    draw.ellipse([cx - 36, cy - 36, cx + 36, cy + 36], fill=COL_IRON_LIGHT, outline=COL_INK, width=6)
    draw.polygon([(cx, cy - 30), (cx + 8, cy - 8), (cx + 30, cy), (cx + 8, cy + 8),
                  (cx, cy + 30), (cx - 8, cy + 8), (cx - 30, cy), (cx - 8, cy - 8)], fill=COL_FIRE_RED)

    # Borchie d'oro angolari
    for bx, by in [(90, 60), (422, 60), (90, 196), (422, 196), (40, 128), (472, 128)]:
        draw.ellipse([bx - 12, by - 12, bx + 12, by + 12], fill=COL_GOLD_BRIGHT, outline=COL_INK, width=4)

    final = finalize_sprite(im, target_size=(256, 128), outline_width=8, outline_color=(254, 243, 199, 250))
    final.save(OUT_DIR / 'sprite_scudo.png')
    print('Generato sprite_scudo.png')


def gen_sfera_runica():
    """Sfera runica magica con incisioni luminescenti e nucleo metallico."""
    im = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    cx, cy, r = 128, 128, 96
    # 1. Base sfera metallica con sfumatura radiale
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=COL_IRON_LIGHT, outline=COL_INK, width=8)

    # Ombreggiatura interna
    draw.ellipse([cx - r + 16, cy - r + 16, cx + r - 8, cy + r - 8], fill=(203, 213, 225, 255))
    draw.ellipse([cx - r + 40, cy - r + 40, cx + r - 12, cy + r - 12], fill=COL_IRON_MID)

    # 2. Incisioni runiche dorate
    draw.line([(cx - 50, cy), (cx + 50, cy)], fill=COL_GOLD_DEEP, width=10)
    draw.line([(cx, cy - 50), (cx, cy + 50)], fill=COL_GOLD_DEEP, width=10)
    draw.line([(cx - 35, cy - 35), (cx + 35, cy + 35)], fill=COL_GOLD_BRIGHT, width=8)
    draw.line([(cx + 35, cy - 35), (cx - 35, cy + 35)], fill=COL_GOLD_BRIGHT, width=8)

    # Riflesso speculare brillante
    draw.ellipse([cx - 55, cy - 55, cx - 25, cy - 25], fill=COL_WHITE)

    final = finalize_sprite(im, target_size=(128, 128), outline_width=6, outline_color=(254, 240, 138, 255))
    final.save(OUT_DIR / 'sprite_sfera.png')
    print('Generato sprite_sfera.png')


def gen_goblin():
    """Goblin invasore con elmo cornuto, occhi gialli fiammeggianti e zanne."""
    im = Image.new('RGBA', (512, 512), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # 1. Grandi orecchie a punta goblin
    draw.polygon([(180, 240), (40, 180), (140, 290)], fill=COL_GOBLIN_SKIN, outline=COL_INK, width=8)
    draw.polygon([(170, 235), (70, 195), (140, 275)], fill=(248, 113, 113, 255)) # Interno rosa

    draw.polygon([(332, 240), (472, 180), (372, 290)], fill=COL_GOBLIN_SKIN, outline=COL_INK, width=8)
    draw.polygon([(342, 235), (442, 195), (372, 275)], fill=(248, 113, 113, 255))

    # 2. Testa e corpo del goblin
    draw.rounded_rectangle([150, 340, 362, 490], radius=32, fill=(113, 63, 18, 255), outline=COL_INK, width=10)
    draw.ellipse([160, 170, 352, 360], fill=COL_GOBLIN_SKIN, outline=COL_INK, width=10)

    # 3. Elmo di ferro con corna
    draw.ellipse([145, 110, 367, 240], fill=COL_IRON_DARK, outline=COL_INK, width=10)
    # Cerchiatura metallica
    draw.rectangle([145, 180, 367, 215], fill=COL_IRON_MID, outline=COL_INK, width=6)

    # Corna ossee ricurve
    draw.polygon([(160, 150), (60, 80), (140, 110)], fill=(254, 243, 199, 255), outline=COL_INK, width=8)
    draw.polygon([(352, 150), (452, 80), (372, 110)], fill=(254, 243, 199, 255), outline=COL_INK, width=8)

    # 4. Viso: Occhi gialli minacciosi, naso adunco e zanne
    draw.polygon([(185, 230), (235, 250), (195, 270)], fill=COL_GOLD_BRIGHT, outline=COL_INK, width=4)
    draw.ellipse([215, 245, 227, 257], fill=COL_FIRE_RED) # Pupilla rossa

    draw.polygon([(327, 230), (277, 250), (317, 270)], fill=COL_GOLD_BRIGHT, outline=COL_INK, width=4)
    draw.ellipse([285, 245, 297, 257], fill=COL_FIRE_RED)

    # Naso e bocca piena di zanne
    draw.polygon([(256, 240), (235, 290), (277, 290)], fill=COL_GOBLIN_DARK, outline=COL_INK, width=4)
    draw.line([(190, 320), (322, 320)], fill=COL_INK, width=8)
    draw.polygon([(210, 320), (220, 300), (230, 320)], fill=COL_WHITE)
    draw.polygon([(282, 320), (292, 300), (302, 320)], fill=COL_WHITE)

    final = finalize_sprite(im, target_size=(256, 256), outline_width=10, outline_color=(239, 68, 68, 240))
    final.save(OUT_DIR / 'sprite_goblin.png')
    print('Generato sprite_goblin.png')


def gen_caverna_sfondo():
    """Texture di sfondo della rocca sotterranea con pietre scure, venature e pilastri."""
    w, h = 540, 960
    im = Image.new('RGBA', (w, h), COL_BG_DARK)
    draw = ImageDraw.Draw(im)

    # Blocchi di roccia muraria sullo sfondo
    block_h = 48
    for row in range(h // block_h + 2):
        y = row * block_h
        shift = 30 if row % 2 == 1 else 0
        for col in range(-1, w // 70 + 2):
            x = col * 70 + shift
            draw.rectangle([x, y, x + 66, y + block_h - 4], fill=(28, 17, 13, 255), outline=(15, 9, 7, 255), width=2)
            if (row + col) % 7 == 0:
                # Piccola gemma luminescente nella roccia
                draw.ellipse([x + 30, y + 20, x + 36, y + 26], fill=COL_GEM_CYAN)

    # Pilastri laterali intagliati
    draw.rectangle([0, 0, 40, h], fill=(38, 24, 18, 255), outline=COL_INK, width=4)
    draw.rectangle([w - 40, 0, w, h], fill=(38, 24, 18, 255), outline=COL_INK, width=4)

    # Bagliore caldo centrale
    glow = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    g_draw = ImageDraw.Draw(glow)
    g_draw.ellipse([w // 2 - 200, 200, w // 2 + 200, 600], fill=(249, 115, 22, 35))
    glow = glow.filter(ImageFilter.GaussianBlur(radius=60))
    im = Image.alpha_composite(im, glow)

    im.resize((w, h), Image.Resampling.LANCZOS).save(OUT_DIR / 'sfondo_caverna.png')
    print('Generato sfondo_caverna.png')


if __name__ == '__main__':
    print('=== Inizio generazione Sprite Fantasy Nanici ===')
    gen_miniera()
    gen_forgia_aperta()
    gen_forgia_chiusa()
    gen_forziere()
    gen_muro()
    gen_balista()
    gen_scudo_nanico()
    gen_sfera_runica()
    gen_goblin()
    gen_caverna_sfondo()
    print('=== Tutti gli sprite fantasy sono stati generati con successo in res/drawable! ===')

