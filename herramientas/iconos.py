#!/usr/bin/env python3
"""LOS ICONOS DE LA APP, a partir de la cruz de azufre en píxeles del blog
(herramientas/iconos/cruz-azufre.png: la del favicon de richi.blog, con sus
cuatro colores y sin suavizar).

Hace, con el mismo marco de siempre (el cuadrado morado oscuro redondeado):
  imagenes/icono.svg, icono-512.png, icono-192.png, icono-180.png
  imagenes/icono-maskable-512.png (todo fondo, la cruz dentro de la zona segura)
  android/…/drawable/ic_frente.xml y ic_mono.xml (el icono adaptable de Android:
  la cruz y, para los iconos con el color del sistema, su silueta con la
  cruz hueca)
  android/…/drawable/ic_aviso.xml (el iconito blanco del aviso de la música)
  android/…/mipmap-*/ic_launcher.png (Android 7)
Los del ejecutable (Windows, macOS) salen de icono-512.png al construirlos.

La cruz se agranda siempre en múltiplos enteros (sin suavizar): que se vean
los píxeles. Uso: python3 herramientas/iconos.py (necesita Pillow).
"""
import math
import os

from PIL import Image, ImageDraw

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ORIGEN = os.path.join(RAIZ, "herramientas", "iconos", "cruz-azufre.png")
IMAGENES = os.path.join(RAIZ, "imagenes")
RES = os.path.join(RAIZ, "android", "app", "src", "main", "res")
FONDO = (0x12, 0x09, 0x1C, 255)


def cruz():
    im = Image.open(ORIGEN).convert("RGBA")
    return im.crop(im.getbbox())


def colores(im):
    """{color: [(x, y), …]} de los píxeles opacos."""
    c = {}
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            if p[3] > 0:
                c.setdefault(p[:3], []).append((x, y))
    return c


def oscuro(c):
    return min(c, key=lambda k: sum(k))


def con_marco(lado, escala, redondeo, todo_fondo=False):
    g = cruz()
    lienzo = Image.new("RGBA", (lado, lado), (0, 0, 0, 0))
    d = ImageDraw.Draw(lienzo)
    if todo_fondo:
        d.rectangle([0, 0, lado, lado], fill=FONDO)
    else:
        d.rounded_rectangle([0, 0, lado - 1, lado - 1], radius=redondeo, fill=FONDO)
    grande = g.resize((g.width * escala, g.height * escala), Image.NEAREST)
    lienzo.alpha_composite(grande, ((lado - grande.width) // 2, (lado - grande.height) // 2))
    return lienzo


def caminos(c, x0=0, y0=0):
    """Un trazado por color: cada tramo de píxeles seguidos, un rectángulo."""
    trazos = {}
    for color, pts in c.items():
        filas = {}
        for x, y in pts:
            filas.setdefault(y, []).append(x)
        partes = []
        for y in sorted(filas):
            xs = sorted(filas[y])
            ini = prev = xs[0]
            for x in xs[1:] + [None]:
                if x is not None and x == prev + 1:
                    prev = x
                    continue
                partes.append("M%d %dh%dv1h-%dz" % (ini + x0, y + y0, prev - ini + 1, prev - ini + 1))
                if x is not None:
                    ini = prev = x
        trazos[color] = "".join(partes)
    return trazos


def svg():
    g = cruz()
    escala = 384 // max(g.width, g.height)
    ancho, alto = g.width * escala, g.height * escala
    tr = caminos(colores(g))
    lineas = ['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">',
              '  <rect width="512" height="512" rx="96" fill="#12091c"/>',
              '  <g transform="translate(%d %d) scale(%d)" shape-rendering="crispEdges">'
              % ((512 - ancho) // 2, (512 - alto) // 2, escala)]
    for color, d in tr.items():
        lineas.append('    <path fill="#%02x%02x%02x" d="%s"/>' % (color + (d,)))
    lineas += ["  </g>", "</svg>", ""]
    with open(os.path.join(IMAGENES, "icono.svg"), "w", encoding="utf-8") as f:
        f.write("\n".join(lineas))


def vector_android(nombre, silueta, dp=108):
    """El primer plano del icono adaptable (108 dp): la cruz entera dentro
    del círculo seguro de 66 dp, con un poco de aire. Con dp=24, el del
    aviso: la silueta llenando casi todo."""
    g = cruz()
    c = colores(g)
    if silueta:
        # Los iconos con el color del sistema sólo usan la forma: la
        # silueta dorada con la cruz hueca (sin el trazo oscuro).
        c = {(255, 255, 255): [pt for k, pts in c.items() if k != oscuro(c) for pt in pts]}
    # Lo más lejos del centro que llega la cruz, en píxeles suyos.
    cx, cy = g.width / 2, g.height / 2
    radio = max(math.hypot(x + 0.5 - cx, y + 0.5 - cy) + 0.71 for pts in colores(g).values() for x, y in pts)
    lado = math.ceil(radio * 2 * 108 / 62) if dp == 108 else max(g.width, g.height) + 4
    # (en el de 108: el círculo de la cruz, 62 de los 108 dp)
    x0, y0 = (lado - g.width) // 2, (lado - g.height) // 2
    tr = caminos(c, x0, y0)
    lineas = ['<?xml version="1.0" encoding="utf-8"?>',
              "<!-- Hecho por herramientas/iconos.py: la cruz de azufre del blog, en píxeles. -->",
              '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
              '    android:width="%ddp"' % dp, '    android:height="%ddp"' % dp,
              '    android:viewportWidth="%d"' % lado, '    android:viewportHeight="%d">' % lado]
    for color, d in tr.items():
        lineas += ["    <path", '        android:fillColor="#%02X%02X%02X"' % color, '        android:pathData="%s" />' % d]
    lineas += ["</vector>", ""]
    with open(os.path.join(RES, "drawable", nombre), "w", encoding="utf-8") as f:
        f.write("\n".join(lineas))


def main():
    svg()
    con_marco(512, 6, 96).save(os.path.join(IMAGENES, "icono-512.png"), optimize=True)
    con_marco(192, 2, 36).save(os.path.join(IMAGENES, "icono-192.png"), optimize=True)
    con_marco(180, 2, 34).save(os.path.join(IMAGENES, "icono-180.png"), optimize=True)
    # Maskable: la cruz dentro del círculo del 80 % (lo que nunca se recorta).
    g = cruz()
    radio = max(math.hypot(x + 0.5 - g.width / 2, y + 0.5 - g.height / 2) + 0.71
                for pts in colores(g).values() for x, y in pts)
    escala = int(0.4 * 512 // radio)
    con_marco(512, escala, 0, todo_fondo=True).save(os.path.join(IMAGENES, "icono-maskable-512.png"), optimize=True)
    if os.path.isdir(RES):
        vector_android("ic_frente.xml", False)
        vector_android("ic_mono.xml", True)
        vector_android("ic_aviso.xml", True, dp=24)
        grande = Image.open(os.path.join(IMAGENES, "icono-512.png"))
        for carpeta, px in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
            grande.resize((px, px), Image.LANCZOS).save(os.path.join(RES, "mipmap-" + carpeta, "ic_launcher.png"), optimize=True)
    print("Iconos hechos (maskable: cruz x%d)." % escala)


if __name__ == "__main__":
    main()
