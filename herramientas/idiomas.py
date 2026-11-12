#!/usr/bin/env python3
"""Los idiomas de la app. Lo llama precache.py (y la Action «idiomas» de
GitHub cuando Weblate sube traducciones), así que no hace falta pasarlo a
mano:

    python3 herramientas/idiomas.py

Hace esto:
  1. idiomas/es/interfaz.json: la plantilla de la interfaz. Todos los textos
     que pasan por ocT("…") en scripts/ y los que están escritos tal cual en
     los .html (títulos, etiquetas, aria-label…). Clave = valor = el texto en
     castellano. Es lo que Weblate enseña para traducir.
  2. idiomas/es/compendio.json: la plantilla del compendio (bestiario,
     conjuros, objetos, reglas, rasgos, tablas, nombres…): cada texto
     distinto de datos/, sin los campos que no se traducen (los mismos que
     FIJOS y PIEZAS en scripts/arranque.js).
  3. Para cada idioma (cada carpeta de idiomas/ que no sea «es»):
     idiomas/<código>.js, el diccionario de la interfaz que carga la app
     (desde idiomas/<código>/interfaz.json, lo que escribe Weblate). El del
     compendio, idiomas/<código>/compendio.json, lo lee la app tal cual.
  4. idiomas/lista.json: los idiomas que hay y cuánto tienen traducido
     (lo enseña el selector de Aa).

Para añadir un idioma: crearlo en Weblate (o una carpeta idiomas/<código>
con interfaz.json y compendio.json que tengan «{}») y ponerle nombre en
NOMBRES, aquí abajo.
"""
import html.parser
import json
import os
import re

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
IDIOMAS = os.path.join(RAIZ, "idiomas")
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
    for nombre in sorted(os.listdir(SCRIPTS)):
        if not nombre.endswith(".js"):
            continue
        with open(os.path.join(SCRIPTS, nombre), encoding="utf-8") as f:
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


def main():
    interfaz = plantilla_interfaz()
    compendio = plantilla_compendio()
    escribir(os.path.join(IDIOMAS, "es", "interfaz.json"), interfaz)
    escribir(os.path.join(IDIOMAS, "es", "compendio.json"), compendio)
    lista = [{"codigo": "es", "nombre": NOMBRES["es"], "interfaz": 100, "compendio": 100}]
    for codigo in sorted(os.listdir(IDIOMAS)):
        carpeta = os.path.join(IDIOMAS, codigo)
        if codigo == "es" or not os.path.isdir(carpeta) or not CODIGO.match(codigo):
            continue
        ti = traducidos(leer(os.path.join(carpeta, "interfaz.json"), {}), interfaz)
        ruta_c = os.path.join(carpeta, "compendio.json")
        if leer(ruta_c) is None:
            escribir(ruta_c, {})
        if leer(os.path.join(carpeta, "interfaz.json")) is None:
            escribir(os.path.join(carpeta, "interfaz.json"), {})
        tc = traducidos(leer(ruta_c, {}), compendio)
        with open(os.path.join(IDIOMAS, codigo + ".js"), "w", encoding="utf-8") as f:
            f.write("// Generado por herramientas/idiomas.py desde idiomas/%s/interfaz.json: no tocar a mano.\n" % codigo)
            f.write("window.ocIdioma.poner(")
            json.dump(ti, f, ensure_ascii=False, indent=0, sort_keys=True)
            f.write(");\n")
        lista.append({"codigo": codigo, "nombre": NOMBRES.get(codigo, codigo),
                      "interfaz": int(100 * len(ti) / max(1, len(interfaz))),
                      "compendio": int(100 * len(tc) / max(1, len(compendio)))})
    # Los .js de idiomas que ya no tienen carpeta, fuera.
    for a in os.listdir(IDIOMAS):
        if a.endswith(".js") and not os.path.isdir(os.path.join(IDIOMAS, a[:-3])):
            os.remove(os.path.join(IDIOMAS, a))
    escribir(os.path.join(IDIOMAS, "lista.json"), lista)
    print("idiomas: %d textos de interfaz, %d del compendio; %s." % (
        len(interfaz), len(compendio), ", ".join("%s %d%%/%d%%" % (x["codigo"], x["interfaz"], x["compendio"]) for x in lista[1:]) or "ninguno más"))


if __name__ == "__main__":
    main()
