#!/usr/bin/env python3
"""Hace los paquetes para descargar: un lanzador que se abre con doble
clic y, a su lado, la carpeta «archivos» con la app (ver
lanzador/main.go). Necesita Go 1.22 o más.

    python3 herramientas/construir-ejecutables.py

Deja en dist/ (no va al repo; se publican como «release» en GitHub, a
mano o con .github/workflows/ejecutables.yml):
    PantallaMaster-Windows.zip   Pantalla de máster/
                                     Pantalla de máster.exe
                                     archivos/
    PantallaMaster-Linux.zip     Pantalla de máster/
                                     pantalla-de-master
                                     archivos/
    PantallaMaster-macOS-AppleSilicon.zip y -Intel.zip
                                 Pantalla de máster.app (en un Mac, la
                                 carpeta va dentro de la .app: macOS
                                 mueve las apps descargadas y no
                                 encontraría lo de al lado)

Antes pasa precache.py (para que lo de dentro vaya al día). El icono de
Windows sale de imagenes/icono-512.png (con rsrc, si está instalado:
go install github.com/akavel/rsrc@latest; si no, sin icono).
"""
import os
import re
import shutil
import subprocess
import sys
import zipfile

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LANZADOR = os.path.join(RAIZ, "lanzador")
DIST = os.path.join(RAIZ, "dist")
DENTRO = ["index.html", "reproductor.html", "jugadores.html", "manifest.webmanifest", "sw.js", "estilos", "scripts", "datos", "tipografias", "imagenes", "sonidos", "idiomas"]
NOMBRE = "Pantalla de máster"
VERSION = "dev"

PLIST = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
<key>CFBundleName</key><string>%s</string>
<key>CFBundleDisplayName</key><string>%s</string>
<key>CFBundleIdentifier</key><string>es.richi.pantallamaster</string>
<key>CFBundleExecutable</key><string>pantalla-master</string>
<key>CFBundleIconFile</key><string>icono.icns</string>
<key>CFBundlePackageType</key><string>APPL</string>
<key>CFBundleShortVersionString</key><string>1.0</string>
<key>LSUIElement</key><true/>
</dict></plist>
""" % (NOMBRE, NOMBRE)


def ejecutar(orden, **kw):
    print("  $", " ".join(orden))
    subprocess.run(orden, check=True, **kw)


def archivos_de_la_app():
    """[(ruta en el disco, ruta dentro de «archivos»)] de todo lo de la app."""
    lista = []
    for x in DENTRO:
        o = os.path.join(RAIZ, x)
        if os.path.isfile(o):
            lista.append((o, x))
            continue
        for base, _, archivos in os.walk(o):
            for a in sorted(archivos):
                ruta = os.path.join(base, a)
                if not a.endswith((".txt", ".md", ".py")):
                    lista.append((ruta, os.path.relpath(ruta, RAIZ).replace(os.sep, "/")))
    return lista


def empaquetar(zipf, ejecutable, nombre_ejecutable, carpeta, extra=()):
    """Un .zip con el ejecutable y «archivos» dentro de «carpeta»."""
    with zipfile.ZipFile(zipf, "w", zipfile.ZIP_DEFLATED) as z:
        info = zipfile.ZipInfo(carpeta + "/" + nombre_ejecutable)
        info.external_attr = 0o100755 << 16
        info.compress_type = zipfile.ZIP_DEFLATED
        with open(ejecutable, "rb") as f:
            z.writestr(info, f.read())
        for o, d in archivos_de_la_app():
            z.write(o, carpeta + "/archivos/" + d)
        for d, contenido in extra:
            z.writestr(carpeta + "/" + d, contenido)


def main():
    ejecutar([sys.executable, os.path.join(RAIZ, "herramientas", "precache.py")])
    global VERSION
    with open(os.path.join(RAIZ, "sw.js"), encoding="utf-8") as f:
        VERSION = re.search(r'const VERSION = "([0-9a-f]+)"', f.read()).group(1)
    shutil.rmtree(os.path.join(LANZADOR, "app"), ignore_errors=True)
    shutil.rmtree(DIST, ignore_errors=True)
    os.makedirs(DIST)

    # El icono de Windows (un .ico con varios tamaños, como recurso).
    syso = os.path.join(LANZADOR, "rsrc_windows_amd64.syso")
    rsrc = shutil.which("rsrc") or os.path.expanduser("~/go/bin/rsrc")
    icns = None
    try:
        from PIL import Image
        ico = os.path.join(LANZADOR, "icono.ico")
        Image.open(os.path.join(RAIZ, "imagenes", "icono-512.png")).save(ico, sizes=[(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
        if os.path.exists(rsrc):
            ejecutar([rsrc, "-arch", "amd64", "-ico", ico, "-o", syso])
        icns = os.path.join(LANZADOR, "icono.icns")
        Image.open(os.path.join(RAIZ, "imagenes", "icono-512.png")).save(icns)
    except Exception:
        print("  (sin Pillow: sin iconos)")

    def go(goos, goarch, salida, extra=()):
        entorno = dict(os.environ, GOOS=goos, GOARCH=goarch, CGO_ENABLED="0")
        ejecutar(["go", "build", "-trimpath", "-ldflags", "-s -w -X main.version=" + VERSION + "".join(" " + e for e in extra),
                  "-o", salida, "."],
                 cwd=LANZADOR, env=entorno)

    tmp = os.path.join(DIST, "tmp")
    os.makedirs(tmp)
    go("windows", "amd64", os.path.join(tmp, "w.exe"), ["-H=windowsgui"])
    empaquetar(os.path.join(DIST, "PantallaMaster-Windows.zip"), os.path.join(tmp, "w.exe"), NOMBRE + ".exe", NOMBRE)
    go("linux", "amd64", os.path.join(tmp, "l"))
    empaquetar(os.path.join(DIST, "PantallaMaster-Linux.zip"), os.path.join(tmp, "l"), "pantalla-de-master", NOMBRE)
    for arch, nombre in (("arm64", "AppleSilicon"), ("amd64", "Intel")):
        binario = os.path.join(tmp, "m-" + arch)
        go("darwin", arch, binario)
        app = NOMBRE + ".app/Contents"
        extra = [("Info.plist", PLIST)]
        with zipfile.ZipFile(os.path.join(DIST, "PantallaMaster-macOS-%s.zip" % nombre), "w", zipfile.ZIP_DEFLATED) as z:
            z.writestr(app + "/Info.plist", PLIST)
            info = zipfile.ZipInfo(app + "/MacOS/pantalla-master")
            info.external_attr = 0o100755 << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            with open(binario, "rb") as f:
                z.writestr(info, f.read())
            if icns:
                z.write(icns, app + "/Resources/icono.icns")
            for o, d in archivos_de_la_app():
                z.write(o, app + "/Resources/archivos/" + d)
    shutil.rmtree(tmp)
    for a in sorted(os.listdir(DIST)):
        print("  dist/%s  (%.1f MB)" % (a, os.path.getsize(os.path.join(DIST, a)) / 1e6))


if __name__ == "__main__":
    main()
