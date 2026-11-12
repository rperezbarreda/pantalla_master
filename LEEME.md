# Pantalla de máster

Una pantalla de máster para rol (D&D 5e, SRD 5.2) que funciona **sin conexión** en el navegador del ordenador y en tablets: un tablero de ventanas que se mueven con la iniciativa, las reglas, el bestiario, los conjuros, los dados, las notas y documentos, los generadores (dungeons, negocios, PNJ, botín, encuentros…), el sonido ambiente, los efectos de sonido, la música y tus creaciones.

Es gratis, sin anuncios y sin nada de pago. Sale de la pantalla de máster de [El Blog de Richi](https://richi.blog/web/pantalla_master/pantalla_master.html), con su diseño y sus temas de accesibilidad, pero sin el blog alrededor.

## Usarla

- **Con un doble clic (lo más fácil):** baja el `.zip` de tu sistema de [la última versión](https://github.com/rperezbarreda/pantalla_master/releases/latest) y descomprímelo. Dentro hay una carpeta «Pantalla de máster» con el programa y, a su lado, la carpeta «archivos» (la app: no hace falta tocarla). Abre el programa con doble clic: se abre en su propia ventana, sin internet y sin instalar nada.
  - **Windows:** `PantallaMaster-Windows.zip` → «Pantalla de máster.exe». La primera vez Windows puede avisar («Windows protegió tu PC», porque el programa no está firmado): «Más información» → «Ejecutar de todas formas».
  - **Mac:** el `.zip` de tu procesador (Apple Silicon = M1, M2…; si no, Intel) → «Pantalla de máster» (aquí la carpeta va dentro de la app). La primera vez: clic derecho → Abrir (no está firmada por Apple).
  - **Linux:** `PantallaMaster-Linux.zip` → `pantalla-de-master`.
  - El programa y la carpeta «archivos» tienen que estar juntos: si mueves uno, mueve los dos (o la carpeta «Pantalla de máster» entera). Puedes hacerle un acceso directo al programa.
  - Usa Edge o Chrome en modo app si los tienes; si no, el navegador de siempre. Se cierra solo al cerrar la ventana.
  - Lo que guardes con el programa (campañas, tableros, notas, tus creaciones, imágenes, PDF, música…) va a la carpeta **«mis-datos»**, junto al programa (en un Mac, junto a la app). Cada copia de la carpeta lleva lo suyo: una descarga nueva empieza vacía, y para llevarte lo tuyo basta copiar «mis-datos» (o ⚙ → exportar / importar). Si el navegador tenía lo de una versión anterior, la entrada ofrece «📥 traer lo de antes».
- **En una tablet Android (la app):** baja `PantallaMaster-Android.apk` de [la última versión](https://github.com/rperezbarreda/pantalla_master/releases/latest) en la tablet y ábrelo; Android pedirá permiso para instalar apps de esa fuente (el navegador o el gestor de archivos). Lleva todo dentro: funciona sin conexión desde el primer momento y no depende de ninguna web. Internet sólo se usa si pides importar un personaje por su enlace (D&D Beyond, Nivel20) o abres un enlace de fuera (se abre en el navegador).
  - La pantalla de jugadores sale en la pantalla externa si hay una conectada (una tele o un monitor por HDMI o USB-C); si no, en otra ventana de la app, al lado si cabe (pantalla partida). El reproductor de música, en su ventana.
  - Atrás cierra lo que haya abierto (como Escape); sin nada abierto, la app se queda en segundo plano tal cual estaba.
  - Exportar y descargar abren el selector de Android para elegir dónde guardar; los PDF e imágenes de documentos se abren con su app.
  - **Dónde se guarda** (⚙ opciones): dentro de la app (se borra si la desinstalas) o en una carpeta que elijas, por ejemplo «Documentos/Pantalla de máster». En la carpeta va igual que «mis-datos» en el programa (`almacen.json` y `archivos/`): sigue ahí aunque desinstales la app y se puede copiar del ordenador a la tablet y al revés. Al elegir una carpeta vacía, la entrada ofrece traer lo que había dentro de la app.
  - **Copias de seguridad:** guardando en una carpeta, la app deja sola una copia al día en su `copias/` (se quedan las cinco últimas; se importan con ⚙ → importar). Guardando dentro de la app, la entrada recuerda hacer una copia si la última es de hace más de dos semanas.
  - **Abrir con / Compartir:** desde otra app (el gestor de archivos, Drive, WhatsApp…) se le pueden pasar a la pantalla de máster una copia de seguridad (pregunta antes de importarla: lo reemplaza todo), imágenes y PDF (van a 📂 documentos) y audios (a 🎵 música, carpeta «recibidas»).
  - **Los botones de volumen** de la tablet son siempre los de la música.
  - **La música sigue sonando** con la app detrás (mirando otra cosa o con la pantalla apagada): mientras suena música o un ambiente, la app lo dice en la barra de notificaciones (Android 13 o más pide permiso para ese aviso una vez).
  - **La pantalla no se apaga sola** mientras la app está a la vista (⚙ opciones → «La pantalla, durante la partida»; en Android viene encendida; en un navegador que lo permita también se puede elegir).
  - **Actualizaciones** (⚙ opciones): «buscar actualizaciones» mira en GitHub si hay una versión nueva (sólo al pulsarlo); si la hay, la baja con el navegador y, al abrirla, Android la instala encima sin perder nada.
  - Con el dedo: deslizar mueve el tablero y la fila de pestañas; dos dedos amplían el tablero y los dibujos (y se mueven por ellos); mantener pulsada la barra de una ventana o una pestaña la coge para moverla. Con lápiz, la palma no pinta.
  - Usa el tamaño de letra del sistema, se puede ampliar la página con dos dedos fuera del tablero y funciona con TalkBack y con teclado.
- **En el ordenador:** abre la web y, si quieres, instálala desde el navegador (Chrome/Edge: el icono de instalar de la barra de direcciones).
- **En una tablet:** ábrela en el navegador y «Añadir a pantalla de inicio» (iPad: Safari → Compartir; Android: Chrome → menú).
- La primera vez se guarda entera en el dispositivo; desde entonces funciona **sin internet**. Cuando hay una versión nueva, avisa con «↻ actualizar» (no se recarga sola en mitad de una partida).

## Los modos

Además de los tableros (las pestañas, donde las ventanas van donde las dejas), están los **modos**: pantallas fijas que se llenan solas, sin arrastrar nada (pensadas para la tablet). Se cambia de modo con el menú de abajo a la derecha (que se pliega con −):

- **⚔️ combate:** ⏩ *turnos* (a quién le toca, en grande; quién va después; anterior y siguiente turno), 🐲 *criatura del turno* (su bloque del bestiario o, si es de la party, su ficha resumida), condiciones y reglas en pestañas, y la ⏱️ iniciativa para editarla. Las dos primeras están en 🧰, en «▶️ turno a turno».
- **📝 notas:** los documentos y seis post-its.
- **🛡️ party:** 🧝 *fichas de la party*: eliges un personaje y ves su ficha como el visor de fichas del blog (tarjeta con PG, CA, retrato y características; historia, ficha, criaturas, PNJ, imágenes, extra y lo del máster).
- En un hueco vacío, «＋ elegir ventana»; en el ✎ de una ventana, la pestaña «🧩 hueco» la cambia por otra o le pone otra en pestaña. «＋» crea un modo (vacío, con una rejilla, o como uno de serie); el ✎ de junto a las pestañas cambia el de ahora (nombre, dibujo, rejilla) o lo borra; «▦ tableros» vuelve a las pestañas.

## Idiomas

La app está en castellano y la traduce la comunidad (de momento, al catalán, gallego y euskera): cada idioma es un archivo de la carpeta [`traducciones/`](traducciones/), y allí mismo está [cómo ayudar](traducciones/LEEME.md). Se elige el idioma en **Aa → 🌐 idioma**, que dice cuánto lleva traducido cada uno; lo que aún no está traducido sale en castellano.

Para ver una traducción antes de que llegue a la app: en Aa, elige el idioma y pulsa «🧪 probar un archivo de traducción…» con el archivo (por ejemplo, `ca.json`). Se queda puesta en ese dispositivo hasta «✕ quitar la traducción de prueba».

## Dónde se guarda lo tuyo

Todo lo que haces (campañas, tableros, notas, documentos, imágenes, música, tus creaciones…) se queda **sólo en el dispositivo**: en el navegador, en la carpeta «mis-datos» (el programa de escritorio) o en la app de Android (dentro de ella, y se borra si se desinstala, o en la carpeta que elijas). No se sube a ningún sitio. Para no perderlo o pasarlo a otro dispositivo: ⚙ → exportar (y luego importar).

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
traducciones/           un JSON por idioma: lo que traduce la comunidad (ver «Los idiomas»)
idiomas/                GENERADO: lo que lee la app, desde traducciones/
herramientas/           precache.py, idiomas.py, construir-ejecutables.py, iconos.py y la plantilla del sw
lanzador/               el ejecutable (Go)
android/                la app de Android (Java, sin librerías; ver abajo)
```

**Los modos** (`pantalla-master.js`): son como los perfiles, con `rejilla` (una de `REJILLAS`: huecos `[columna, fila, ancho, alto]` en 12 × 6) y sus ventanas con `hueco`; van en la campaña (`modos`, `modoActivo`). Los de serie, en `MODOS_DE_SERIE`. Pasar de turno lo hacen igual la iniciativa y la ventana de turnos con `window.ocTurnos` (`personajes.js`).

**Los idiomas** (`scripts/arranque.js`, `herramientas/idiomas.py`, `idiomas/`):

- Cada texto de la interfaz va en `ocT("texto en castellano")`, con las partes que cambian como `{nombre}`: `ocT("Quitado: {nombre}.", { nombre: x.nombre })`. Nunca se pega un texto con `+` («"Quitar " + nombre»): se escribe la frase entera con su `{…}`, que el orden de las palabras cambia de un idioma a otro. Lo que es un identificador (clases, claves, valores que se comparan) no va en `ocT`. En castellano `ocT` sólo pone las variables, así que la app es la misma.
- Lo escrito tal cual en los `.html` (títulos, `aria-label`, `title`, `placeholder`) se traduce solo al cargar; con `translate="no"` en un elemento, eso no.
- El compendio (`datos/`) se traduce al cargarse (`window.ocIdioma.datos`, en `pedir`, `pedirNombres` y `ocReglas.cargar`), menos los campos de `FIJOS` en `arranque.js` (los nombres en inglés, los id, y las categorías con las que filtra el programa: clases, rarezas, escuelas, tipos…; esas salen en castellano). Si se añade un campo de ese tipo, va en `FIJOS` aquí **y** en `herramientas/idiomas.py`.
- `python3 herramientas/idiomas.py` (lo llama `precache.py`) rehace `traducciones/es.json`, el **original** (todos los `ocT("…")`, los textos de los `.html` y cada texto distinto de `datos/`; la interfaz primero), y pone al día cada `traducciones/<código>.json`: le añade vacíos los textos nuevos y le quita los que ya no se usan. Desde ellos hace lo que lee la app: `idiomas/<código>.js` (la interfaz, se carga antes que todo), `idiomas/<código>.json` (el compendio) e `idiomas/lista.json` (los idiomas y cuánto llevan, para Aa).
- Las claves son el propio texto en castellano: si se cambia un texto, su traducción se pierde y vuelve a salir vacío para traducirlo otra vez.
- Las traducciones llegan como *pull request* que cambia `traducciones/<código>.json` (o se ponen a mano); al subirlas, la Action `idiomas` (`.github/workflows/idiomas.yml`) rehace `idiomas/` y `sw.js` y lo sube en otro commit.
- **Un idioma nuevo:** `traducciones/<código>.json` con `{}` y su nombre en `NOMBRES` de `herramientas/idiomas.py`.

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

**Los iconos** (la cruz de azufre en píxeles del blog): `python3 herramientas/iconos.py` los hace todos a partir de `herramientas/iconos/cruz-azufre.png` (los de `imagenes/`, el maskable y los de Android); los de Windows y macOS salen de `icono-512.png` al construir los ejecutables.

**Los ejecutables** (`lanzador/main.go`; lo guardado, a `mis-datos/` con `lanzador/almacen.go`, y en `scripts/arranque.js` el `localStorage` que escribe allí: un servidor pequeño en Go que sirve la carpeta «archivos» de su lado en `http://127.0.0.1:47321`, sólo para ese ordenador; el puerto es fijo para que no se pierda lo guardado): `python3 herramientas/construir-ejecutables.py` deja los .zip en `dist/` (necesita Go). Al subir a `main`, GitHub los construye solo y los publica en la release «ultima» (`.github/workflows/ejecutables.yml`).

**La app de Android** (`android/`): una actividad con un WebView que sirve la web desde el APK (`https://appassets.androidplatform.net/`, sin service worker). Al compilar, Gradle copia en `assets/web/` lo mismo que lleva el ejecutable (menos `sw.js`), así que **no hay que tocar nada en `android/` cuando cambia la web**. Lo que en el navegador hace el navegador va en `android/app/src/main/assets/android.js` (se mete en cada página antes que el resto) y en el Java (`Base.java`: elegir archivos, guardar, abrir, atrás; `Principal.java`: la pantalla externa y las ventanas; `Red.java`: importar por enlace, como el lanzador). La web sólo mira `window.ocAndroid` para saber que está dentro (sin el aviso de la web, importar por enlace, los PDF con su visor).

- **Lo guardado en carpeta** (`Carpeta.java`): la elige cada uno con el selector de Android (permiso sólo a esa carpeta); una sola copia de los datos para todas las ventanas (cada una lee y escribe clave a clave por el puente, así la de jugadores no pisa a la del máster) que se escribe en `almacen.json` un momento después de cada cambio y al dejar la app (antes, `almacen.copia.json`, por si se corta). Los archivos se leen como en el programa (`GET /__archivos`) y se escriben por el puente (`window.ocAlmacen.guardarArchivo`). Si dentro de la carpeta elegida hay un `mis-datos` con datos (copiado del ordenador), se usa ese.
- **Abrir con / Compartir** (`Recibidos.java`): lo que llega se copia a la caché (el permiso para leerlo dura poco) y la web lo pide (`/__recibido/<id>`) y lo reparte (`recibirArchivos` en `pantalla-master.js`). La copia automática del día la hace la web (`datosDeCopia`, sin archivos) y la escribe la app en `copias/`.
- **La música** (`Servicio.java`): `android.js` mira si suena algo largo (música, un ambiente en bucle; no los efectos) y la app tiene un servicio «en primer plano» con su aviso mientras tanto (se va 20 s después de que pare, para el cambio de canción).
- **Los tableros de serie y el tutorial** son otros en la app de Android (`PERFILES_ANDROID` y `TUTORIAL_TABLETA` en `pantalla-master.js`): la disposición hecha en una Galaxy Tab A7 y un tutorial para el dedo, sin teclado. En las campañas que ya había, las tarjetas del tutorial que nadie ha tocado se cambian solas por las de la tableta.
- **Las actualizaciones** (`Red.java`): el título de la release «ultima» lleva el número de la ejecución de `ejecutables.yml` («versión N»), que es el `versionCode` del APK; la app compara los dos.
- **De prueba:** cada subida a `desarrollo` hace «Máster (prueba)» (`.github/workflows/android.yml`; en Actions, «android-prueba» → la ejecución → Artifacts). Es otra app (`richi.pantallamaster.prueba`; la de verdad, `richi.pantallamaster`): convive con la de verdad y se instala encima de la anterior sin perder lo guardado (va firmada con `android/prueba.keystore`, que sólo sirve para eso).
- **La de verdad:** al subir a `main`, `ejecutables.yml` hace el APK firmado, lo pone en la release «ultima» y deja el `.aab` para Google Play en Artifacts («PantallaMaster-Android-Play»). Necesita cuatro secretos del repo (Settings → Secrets and variables → Actions): `ANDROID_ALMACEN_BASE64` (el almacén de claves en base64), `ANDROID_ALMACEN_CLAVE`, `ANDROID_ALIAS` y `ANDROID_ALIAS_CLAVE`. Sin ellos se publica lo de escritorio y la de Android no. El almacén se hace una vez (`keytool -genkeypair -keystore pantalla-master.jks -alias pantalla-master -keyalg RSA -keysize 4096 -validity 10000`) y **no se puede perder**: sin él, las versiones nuevas no se instalan encima de las viejas.
- **Google Play:** la ficha usa `imagenes/icono-maskable-512.png` como icono; el `.aab` se sube a mano desde la consola de Play (con «Firma de apps de Google Play»: Google firma lo que se instala desde Play, así que esa app y la del APK de GitHub no se actualizan la una con la otra). Es gratis, sin anuncios y sin compras; no recoge datos.
- **Compilar en local:** Android Studio (abre `android/`) o `gradle -p android assembleDebug` con el SDK de Android y Gradle 8.14.

**Para probar en local:** `python3 -m http.server` en esta carpeta y abrir `http://localhost:8000` (el modo sin conexión necesita https o localhost; con `file://` no va).

**Publicarla:** vale cualquier hosting estático (sin `android/`, que no es parte de la web). En GitHub Pages: Settings → Pages → «Deploy from a branch» → `main` / raíz. Las rutas son relativas, así que funciona también en `usuario.github.io/pantalla-master/`.

## Licencia

El código es [MIT](LICENSE): cualquiera puede usarlo, cambiarlo y compartirlo, siempre que mantenga el aviso de copyright. Lo que no es código lleva su propia licencia (detallada en [LICENSE](LICENSE)): el SRD 5.2, CC BY 4.0; las tipografías, OFL. En el repositorio sólo va contenido del SRD y propio: nada de otros libros.

## Política de firma de código

Los ejecutables de Windows se firman con un certificado de [SignPath Foundation](https://signpath.org/) (cuando el proyecto esté aceptado):

- **Quién:** Richi ([@rperezbarreda](https://github.com/rperezbarreda)) es el autor y quien aprueba cada versión.
- **Cómo:** los ejecutables se construyen sólo en GitHub Actions, a partir del código de este repositorio (`.github/workflows/ejecutables.yml`); nada se firma desde un ordenador.
- **Qué:** sólo se firman las versiones publicadas desde `main`, después de aprobarlas a mano en SignPath.
- **Privacidad:** ni el programa ni la app de Android envían datos a ningún sitio. Sólo se conectan a internet cuando tú pides importar un personaje de D&D Beyond o de Nivel20, buscar actualizaciones (la app de Android, que pregunta a GitHub) o abres un enlace de fuera (en el navegador); todo lo demás se guarda en tu dispositivo (en el ordenador, en la carpeta `mis-datos`; en Android, dentro de la app o en tu carpeta).
- **Android:** el APK se firma también sólo en GitHub Actions, con la clave de los secretos del repositorio. Sus permisos: internet (lo de arriba), el servicio y el aviso de la música (para que siga con la app detrás) y no dormir el procesador mientras suena; la carpeta, sólo la que eliges tú.

## Créditos

- Reglas, monstruos, conjuros, objetos, rasgos, dotes y habilidades: este trabajo incluye material del *System Reference Document 5.2* de Wizards of the Coast LLC, disponible en [dndbeyond.com/srd](https://www.dndbeyond.com/srd), con licencia [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/legalcode), traducido al español (no es una traducción oficial). Los rasgos y dotes, a partir de [5e-bits/5e-database](https://github.com/5e-bits/5e-database) (MIT).
- Dungeons & Dragons y D&D son marcas de Wizards of the Coast; este proyecto no está afiliado ni respaldado por ellos.
- Grabaciones (cuando estén en `sonidos/`): [Google Sound Library](https://developers.google.com/assistant/tools/sound-library), CC BY 4.0.
- Tipografías: Space Grotesk, DM Mono, Jersey 10, Pangolin, Patrick Hand, Caveat, Inter, Pixelify Sans, VT323, Atkinson Hyperlegible y Noto, con sus licencias libres (las de Google Fonts).
