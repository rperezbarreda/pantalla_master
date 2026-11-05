#!/usr/bin/env python3
"""Prepara la app para funcionar sin conexión. Hay que pasarlo SIEMPRE
antes de subir un cambio:

    python3 herramientas/precache.py

Hace dos cosas:
  1. sonidos/indice.json: qué grabaciones hay en sonidos/ (la app sólo
     enseña las que estén). Rutas como «weather/rain_on_roof».
  2. sw.js: el service worker, desde herramientas/sw-plantilla.js, con la
     lista de todos los archivos de la app (para guardarlos al instalar)
     y una versión que es la huella de todos ellos: si cambia cualquiera,
     cambia la versión y la app avisa de que hay una nueva.
"""
import hashlib
import json
import os

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CARPETAS = ["estilos", "scripts", "datos", "tipografias", "imagenes", "sonidos"]
SUELTOS = ["index.html", "reproductor.html", "jugadores.html", "manifest.webmanifest"]
NO = {".txt", ".md", ".py"}


def main():
    sonidos = os.path.join(RAIZ, "sonidos")
    rutas = []
    for base, _, archivos in os.walk(sonidos):
        for a in archivos:
            if a.endswith(".ogg"):
                rel = os.path.relpath(os.path.join(base, a), sonidos)[:-4].replace(os.sep, "/")
                rutas.append(rel)
    rutas.sort()
    with open(os.path.join(sonidos, "indice.json"), "w", encoding="utf-8") as f:
        json.dump({"rutas": rutas}, f, ensure_ascii=False, indent=1)
        f.write("\n")

    lista = list(SUELTOS)
    for c in CARPETAS:
        for base, _, archivos in os.walk(os.path.join(RAIZ, c)):
            for a in sorted(archivos):
                if os.path.splitext(a)[1] in NO:
                    continue
                lista.append(os.path.relpath(os.path.join(base, a), RAIZ).replace(os.sep, "/"))
    lista.sort()
    huella = hashlib.sha256()
    for r in lista:
        huella.update(r.encode())
        with open(os.path.join(RAIZ, r), "rb") as f:
            huella.update(f.read())
    with open(os.path.join(RAIZ, "herramientas", "sw-plantilla.js"), encoding="utf-8") as f:
        sw = f.read()
    sw = sw.replace("__VERSION__", huella.hexdigest()[:12]).replace("__ARCHIVOS__", json.dumps(["./"] + lista, ensure_ascii=False, indent=4))
    with open(os.path.join(RAIZ, "sw.js"), "w", encoding="utf-8") as f:
        f.write(sw)
    peso = sum(os.path.getsize(os.path.join(RAIZ, r)) for r in lista)
    print("sw.js: %d archivos (%.1f MB), versión %s; %d grabaciones en sonidos/." % (len(lista), peso / 1e6, huella.hexdigest()[:12], len(rutas)))


if __name__ == "__main__":
    main()
