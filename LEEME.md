# Pantalla de máster

Una pantalla de máster para rol (D&D 5e, SRD 5.2) que funciona **sin conexión** en el navegador del ordenador y en tablets: un tablero de ventanas que se mueven con la iniciativa, las reglas, el bestiario, los conjuros, los dados, las notas y documentos, los generadores (dungeons, negocios, PNJ, botín, encuentros…), el sonido ambiente, los efectos de sonido, la música y tus creaciones.

Sale de la pantalla de máster de [El Blog de Richi](https://richi.blog/web/pantalla_master/pantalla_master.html), con su diseño y sus temas de accesibilidad, pero sin el blog alrededor.

## Usarla

- **Con un doble clic (lo más fácil):** baja el `.zip` de tu sistema de [la última versión](https://github.com/rperezbarreda/pantalla_master/releases/latest) y descomprímelo. Dentro hay una carpeta «Pantalla de máster» con el programa y, a su lado, la carpeta «archivos» (la app: no hace falta tocarla). Abre el programa con doble clic: se abre en su propia ventana, sin internet y sin instalar nada.
  - **Windows:** `PantallaMaster-Windows.zip` → «Pantalla de máster.exe». La primera vez Windows puede avisar («Windows protegió tu PC», porque el programa no está firmado): «Más información» → «Ejecutar de todas formas».
  - **Mac:** el `.zip` de tu procesador (Apple Silicon = M1, M2…; si no, Intel) → «Pantalla de máster» (aquí la carpeta va dentro de la app). La primera vez: clic derecho → Abrir (no está firmada por Apple).
  - **Linux:** `PantallaMaster-Linux.zip` → `pantalla-de-master`.
  - El programa y la carpeta «archivos» tienen que estar juntos: si mueves uno, mueve los dos (o la carpeta «Pantalla de máster» entera). Puedes hacerle un acceso directo al programa.
  - Usa Edge o Chrome en modo app si los tienes; si no, el navegador de siempre. Se cierra solo al cerrar la ventana.
  - Lo que guardes con el programa (campañas, tableros, notas, tus creaciones, imágenes, PDF, música…) va a la carpeta **«mis-datos»**, junto al programa (en un Mac, junto a la app). Cada copia de la carpeta lleva lo suyo: una descarga nueva empieza vacía, y para llevarte lo tuyo basta copiar «mis-datos» (o ⚙ → exportar / importar). Si el navegador tenía lo de una versión anterior, la entrada ofrece «📥 traer lo de antes».
- **En el ordenador:** abre la web y, si quieres, instálala desde el navegador (Chrome/Edge: el icono de instalar de la barra de direcciones).
- **En una tablet:** ábrela en el navegador y «Añadir a pantalla de inicio» (iPad: Safari → Compartir; Android: Chrome → menú).
- La primera vez se guarda entera en el dispositivo; desde entonces funciona **sin internet**. Cuando hay una versión nueva, avisa con «↻ actualizar» (no se recarga sola en mitad de una partida).

## Dónde se guarda lo tuyo

Todo lo que haces (campañas, tableros, notas, documentos, imágenes, música, tus creaciones…) se queda **sólo en el dispositivo**, en el navegador: no se sube a ningún sitio. Para no perderlo o pasarlo a otro dispositivo: ⚙ → exportar (y luego importar).

## Para quien la mantiene

No hay que compilar nada: son archivos estáticos.

```
index.html              la app
manifest.webmanifest    para instalarla
sw.js                   GENERADO: el service worker (sin conexión)
scripts/                arranque.js (tema antes de pintar), app.js (Aa, sw),
                        pantalla-master.js, personajes.js, tema.js, ajustes.js
estilos/                los del blog + app.css (lo propio de la app) y moderno.css
                        windows.css y lapiz.css (los otros tres diseños, Aa → diseño:
                        «moderno», «windows 98» y «a lápiz»; sin ellos, el retro de siempre)
datos/                  el SRD 5.2 en español (CC BY 4.0) y las tablas
                        (compendio/: reglas.json, nombres.json, master-tablas.json…)
tipografias/            las letras (con sus licencias OFL)
imagenes/               iconos y fondos
sonidos/                las grabaciones (ver sonidos/LEEME.txt)
herramientas/           precache.py, construir-ejecutables.py y la plantilla del sw
lanzador/               el ejecutable (Go)
```

**Las reglas y los nombres se cambian en sus JSON** (`datos/personajes/compendio/`):

- `reglas.json`: `reglas` (cada una con su `id`, que no se cambia, su grupo, nombre, icono, alias y texto; `opcional: true` para las que no son del SRD, como el flanqueo) y `marcas` (las de la iniciativa: icono, resumen corto y la regla que abren). Desde la app, cada regla tiene «✎ cambiar» y «↺ volver a la original»: lo cambiado se guarda aparte («richi-master-reglas», va con exportar y en «mis-datos») y el archivo no se toca.
- `nombres.json`: las sílabas y trozos del generador de nombres. Desde la app («✎ cambiar esta» en el generador) se cambian las de siempre y se crean **tablas de nombres propias** (nombres enteros: títulos, femeninos, masculinos, neutros y apellidos, o lugares en dos partes), en «richi-master-nombres».

**La edición** también es de cada campaña («edicion»: `2024` o `2014`; sin ella, 2024), se elige al crearla y se cambia en ⚙ opciones. Con 2014, las reglas, el bestiario, los conjuros y los objetos (la tienda y el botín) salen de `datos/personajes/compendio/2014/` (el SRD 5.1 en español de Wizards, CC BY 4.0, pasado de su PDF oficial y con las medidas en pies y libras); lo demás (los rasgos) es el de 2024. Los id de las reglas son los mismos en las dos cuando la regla es la misma, así que lo que cambias en la app («richi-master-reglas») vale para las dos.

**Las medidas** son de cada campaña («unidades» en su JSON guardado: `imperial` o `metrico`; sin ella, imperial). Se eligen al crearla y se cambian en ⚙ opciones. Los datos van siempre en pies, libras y millas (como el SRD); en métrico se pasa al vuelo lo que sale en pantalla (5 pies = 1,5 m, 1 lb = 0,5 kg, 1 milla = 1,5 km), sin tocar lo que escribe cada uno (`aplicarUnidades` en `scripts/pantalla-master.js`).

**Antes de subir cualquier cambio:**

```
python3 herramientas/precache.py
```

Rehace `sw.js` con la lista de archivos y una versión nueva (si no, los dispositivos se quedan con la vieja) y `sonidos/indice.json` con las grabaciones que haya.

**Los ejecutables** (`lanzador/main.go`; lo guardado, a `mis-datos/` con `lanzador/almacen.go`, y en `scripts/arranque.js` el `localStorage` que escribe allí: un servidor pequeño en Go que sirve la carpeta «archivos» de su lado en `http://127.0.0.1:47321`, sólo para ese ordenador; el puerto es fijo para que no se pierda lo guardado): `python3 herramientas/construir-ejecutables.py` deja los .zip en `dist/` (necesita Go). Al subir a `main`, GitHub los construye solo y los publica en la release «ultima» (`.github/workflows/ejecutables.yml`).

**Para probar en local:** `python3 -m http.server` en esta carpeta y abrir `http://localhost:8000` (el modo sin conexión necesita https o localhost; con `file://` no va).

**Publicarla:** vale cualquier hosting estático. En GitHub Pages: Settings → Pages → «Deploy from a branch» → `main` / raíz. Las rutas son relativas, así que funciona también en `usuario.github.io/pantalla-master/`.

## Licencia

El código es [MIT](LICENSE): cualquiera puede usarlo, cambiarlo y compartirlo, siempre que mantenga el aviso de copyright. Lo que no es código lleva su propia licencia (detallada en [LICENSE](LICENSE)): el SRD 5.2, CC BY 4.0; las tipografías, OFL. En el repositorio sólo va contenido del SRD y propio: nada de otros libros.

## Política de firma de código

Los ejecutables de Windows se firman con un certificado de [SignPath Foundation](https://signpath.org/) (cuando el proyecto esté aceptado):

- **Quién:** Richi ([@rperezbarreda](https://github.com/rperezbarreda)) es el autor y quien aprueba cada versión.
- **Cómo:** los ejecutables se construyen sólo en GitHub Actions, a partir del código de este repositorio (`.github/workflows/ejecutables.yml`); nada se firma desde un ordenador.
- **Qué:** sólo se firman las versiones publicadas desde `main`, después de aprobarlas a mano en SignPath.
- **Privacidad:** el programa no envía datos a ningún sitio. Sólo se conecta a internet cuando tú pides importar un personaje de D&D Beyond o de Nivel20; todo lo demás se guarda en tu ordenador, en la carpeta `mis-datos`.

## Créditos

- Reglas, monstruos, conjuros, objetos, rasgos, dotes y habilidades: este trabajo incluye material del *System Reference Document 5.2* de Wizards of the Coast LLC, disponible en [dndbeyond.com/srd](https://www.dndbeyond.com/srd), con licencia [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/legalcode), traducido al español (no es una traducción oficial). Los rasgos y dotes, a partir de [5e-bits/5e-database](https://github.com/5e-bits/5e-database) (MIT).
- Dungeons & Dragons y D&D son marcas de Wizards of the Coast; este proyecto no está afiliado ni respaldado por ellos.
- Grabaciones (cuando estén en `sonidos/`): [Google Sound Library](https://developers.google.com/assistant/tools/sound-library), CC BY 4.0.
- Tipografías: Space Grotesk, DM Mono, Jersey 10, Pangolin, Patrick Hand, Caveat, Inter, Pixelify Sans, VT323, Atkinson Hyperlegible y Noto, con sus licencias libres (las de Google Fonts).
