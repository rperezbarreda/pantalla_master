#!/usr/bin/env python3
"""Los idiomas de la app. Lo llama precache.py (y la Action «idiomas» de
GitHub cuando alguien sube una traducción), así que no hace falta pasarlo
a mano:

    python3 herramientas/idiomas.py

Las traducciones están en traducciones/, un archivo por idioma (ver
traducciones/LEEME.md, que es lo que lee quien traduce):
  - traducciones/es.json: el ORIGINAL, que se rehace aquí (no se toca a
    mano). Clave = valor = el texto en castellano. Lleva todo: los textos de
    la interfaz (los ocT("…") de scripts/ y lo escrito en los .html) y los del
    compendio (cada texto distinto de datos/, sin los campos de FIJOS y
    PIEZAS, los mismos que en scripts/arranque.js).
  - traducciones/<código>.json (ca, gl, eu…): lo traducido. Aquí se le
    añaden, vacíos, los textos nuevos de la app (así quien traduce ve lo que
    falta) y se le quitan los que la app ya no usa. Lo vacío sale en
    castellano.

Y de ahí hace lo que lee la app, en idiomas/ (generado, no se toca):
  - idiomas/<código>.js: lo de la interfaz (se carga antes que todo);
  - idiomas/<código>.json: lo del compendio (se pide al cargar los datos);
  - idiomas/lista.json: los idiomas y cuánto llevan (el selector de Aa).

Un idioma nuevo: traducciones/<código>.json con «{}» (se llena solo con
todos los textos vacíos) y su nombre en NOMBRES, aquí abajo.
"""
import html.parser
import json
import os
import re
import shutil

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
IDIOMAS = os.path.join(RAIZ, "idiomas")
TRADUCCIONES = os.path.join(RAIZ, "traducciones")
SCRIPTS = os.path.join(RAIZ, "scripts")
HTMLS = ["index.html", "jugadores.html", "reproductor.html"]
# El nombre de cada idioma en ese idioma.
NOMBRES = {"es": "Castellano", "ca": "Català", "gl": "Galego", "eu": "Euskara", "ast": "Asturianu",
           "an": "Aragonés", "oc": "Occitan", "pt": "Português", "en": "English", "fr": "Français",
           "it": "Italiano", "de": "Deutsch"}
# Los mismos que en scripts/arranque.js.
FIJOS = {"en", "id", "fuente", "url", "clave", "silabas", "emoji", "icono", "ilus", "ilusDe", "apellido", "glifos",
         "conjuros", "asalariados", "donde", "cr", "clases", "tiendas", "tienda", "cat", "rareza", "escuela", "tipo",
         "de", "habitat", "marca", "regla", "tipos", "estilos", "et"}
PIEZAS = {"ini", "m", "f", "n", "fin"}
LETRAS = re.compile(r"[A-Za-zÀ-ÿ]")
ATRIBUTOS = {"aria-label", "title", "placeholder", "alt", "aria-description"}
CODIGO = re.compile(r"^[a-z]{2,3}(-[A-Za-z0-9]{2,8})?$")


def leer(ruta, si_no=None):
    try:
        with open(ruta, encoding="utf-8") as f:
            return json.load(f)
    except (OSError, ValueError):
        return si_no


def escribir(ruta, datos):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(datos, f, ensure_ascii=False, indent=2)
        f.write("\n")


# ---- La interfaz ----
OCT = re.compile(r"""\bocT\(\s*(?:"((?:[^"\\\n]|\\.)*)"|'((?:[^'\\\n]|\\.)*)')""")


def cadena_js(s):
    s = s.replace("\\'", "'")
    return json.loads('"' + s + '"')


class LectorHtml(html.parser.HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.textos = []
        self.saltar = 0
        self.pila = []

    def handle_starttag(self, tag, attrs):
        a = dict(attrs)
        sin = tag in ("script", "style", "textarea") or a.get("translate") == "no"
        if tag not in ("meta", "link", "input", "img", "br", "hr", "source"):
            self.pila.append(sin)
            if sin:
                self.saltar += 1
        if self.saltar:
            return
        for k, v in attrs:
            if v and k in ATRIBUTOS and LETRAS.search(v):
                self.textos.append(" ".join(v.split()))
        if tag == "meta" and a.get("name") == "description" and a.get("content"):
            self.textos.append(" ".join(a["content"].split()))

    def handle_startendtag(self, tag, attrs):
        self.handle_starttag(tag, attrs)

    def handle_endtag(self, tag):
        if tag in ("meta", "link", "input", "img", "br", "hr", "source") or not self.pila:
            return
        if self.pila.pop():
            self.saltar -= 1

    def handle_data(self, data):
        t = " ".join(data.split())
        if not self.saltar and t and LETRAS.search(t):
            self.textos.append(t)


def plantilla_interfaz():
    textos = []
    rutas = sorted(os.path.join(base, a) for base, _, archivos in os.walk(SCRIPTS) for a in archivos if a.endswith(".js"))
    for ruta in rutas:
        with open(ruta, encoding="utf-8") as f:
            codigo = f.read()
        for m in OCT.finditer(codigo):
            textos.append(cadena_js(m.group(1) if m.group(1) is not None else m.group(2)))
    for h in HTMLS:
        lector = LectorHtml()
        with open(os.path.join(RAIZ, h), encoding="utf-8") as f:
            lector.feed(f.read())
        textos.extend(lector.textos)
    vistos = {}
    for t in textos:
        if t and t not in vistos:
            vistos[t] = t
    return vistos


# ---- El compendio ----
def plantilla_compendio():
    textos = {}

    def recorrer(x, campo, con_piezas):
        if isinstance(x, str):
            if campo in FIJOS or (con_piezas and campo in PIEZAS) or not LETRAS.search(x):
                return
            textos.setdefault(x, x)
        elif isinstance(x, list):
            for y in x:
                recorrer(y, campo, con_piezas)
        elif isinstance(x, dict):
            piezas = con_piezas or "especies" in x or "apellidos" in x
            for k, v in x.items():
                if k not in FIJOS:
                    recorrer(v, k, piezas)

    datos = os.path.join(RAIZ, "datos")
    for base, _, archivos in sorted(os.walk(datos)):
        for a in sorted(archivos):
            if a.endswith(".json"):
                recorrer(leer(os.path.join(base, a), {}), "", False)
    return textos


def traducidos(dic, plantilla):
    if not isinstance(dic, dict):
        return {}
    return {k: v for k, v in dic.items() if k in plantilla and isinstance(v, str) and v.strip()}


def porcentaje(dic, plantilla):
    hechos = sum(1 for k, v in dic.items() if v != k or len(k) < 4)
    return int(100 * hechos / max(1, len(plantilla)))


def main():
    interfaz = plantilla_interfaz()
    compendio = plantilla_compendio()
    # La plantilla: todo junto (la interfaz primero), cada texto una vez.
    plantilla = dict(interfaz)
    for k in compendio:
        plantilla.setdefault(k, k)
    escribir(os.path.join(TRADUCCIONES, "es.json"), plantilla)
    os.makedirs(IDIOMAS, exist_ok=True)
    lista = [{"codigo": "es", "nombre": NOMBRES["es"], "interfaz": 100, "compendio": 100}]
    hechos = set()
    for nombre in sorted(os.listdir(TRADUCCIONES)):
        codigo = nombre[:-5]
        if not nombre.endswith(".json") or codigo == "es" or not CODIGO.match(codigo):
            continue
        dic = leer(os.path.join(TRADUCCIONES, nombre), {})
        if not isinstance(dic, dict):
            print("traducciones/%s no es un JSON válido: no se toca." % nombre)
            continue
        # El archivo, con todos los textos de ahora y en el mismo orden que
        # el original: lo traducido, y lo que falta vacío.
        escribir(os.path.join(TRADUCCIONES, nombre),
                 {k: dic[k] if isinstance(dic.get(k), str) else "" for k in plantilla})
        ti = traducidos(dic, interfaz)
        tc = traducidos(dic, compendio)
        with open(os.path.join(IDIOMAS, codigo + ".js"), "w", encoding="utf-8") as f:
            f.write("// Generado por herramientas/idiomas.py desde traducciones/%s.json: no tocar a mano.\n" % codigo)
            f.write("window.ocIdioma.poner(")
            json.dump(ti, f, ensure_ascii=False, indent=0, sort_keys=True)
            f.write(");\n")
        with open(os.path.join(IDIOMAS, codigo + ".json"), "w", encoding="utf-8") as f:
            json.dump(tc, f, ensure_ascii=False, separators=(",", ":"), sort_keys=True)
        hechos.update([codigo + ".js", codigo + ".json"])
        lista.append({"codigo": codigo, "nombre": NOMBRES.get(codigo, codigo),
                      "interfaz": porcentaje(ti, interfaz), "compendio": porcentaje(tc, compendio)})
    # Lo de idiomas que ya no tiene traducción, fuera.
    for a in os.listdir(IDIOMAS):
        ruta = os.path.join(IDIOMAS, a)
        if a != "lista.json" and a not in hechos:
            if os.path.isdir(ruta):
                shutil.rmtree(ruta)
            else:
                os.remove(ruta)
    escribir(os.path.join(IDIOMAS, "lista.json"), lista)
    print("idiomas: %d textos de interfaz, %d del compendio; %s." % (
        len(interfaz), len(compendio), ", ".join("%s %d%%/%d%%" % (x["codigo"], x["interfaz"], x["compendio"]) for x in lista[1:]) or "ninguno más"))


if __name__ == "__main__":
    main()
