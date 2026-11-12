# Traducir la pantalla de máster

La app está escrita en castellano. Aquí hay un archivo por idioma, y cualquiera puede ayudar a traducirlo:

| Archivo | Idioma |
|---|---|
| `es.json` | Castellano: el **original**. No se cambia (se rehace solo). |
| `ca.json` | Català |
| `gl.json` | Galego |
| `eu.json` | Euskara |

## Cómo se traduce

Cada archivo es una lista de textos. A la izquierda, el texto en castellano (no se toca); a la derecha, entre comillas, su traducción:

```json
"Quitar esta ventana": "Treure aquesta finestra",
"Guardado: {nombre}.": "Desat: {nombre}.",
"🎲 generar PNJ": ""
```

- Lo que está vacío (`""`) aún no está traducido y sale en castellano. Se puede traducir poco a poco: cada texto que traduces ya se ve en la app.
- **Lo que va entre llaves no se traduce:** `{nombre}`, `{n}`… es lo que la app pone ahí (un nombre, un número). Se deja igual, donde toque en la frase.
- Respeta los emojis, los símbolos del principio (＋, ✕, ⬇…), los espacios del principio y del final, y las marcas: `<strong>…</strong>`, `**negrita**`, `*cursiva*`, `\n` (salto de línea).
- Las comillas dentro de una traducción van con barra: `\"`. Mejor usar las españolas, « ».
- Los primeros textos son los de la interfaz (botones, menús, avisos); los que vienen después, los del compendio (reglas, bestiario, conjuros, objetos, tablas). Si vas a empezar por algo, empieza por arriba.
- Cuando la app tiene textos nuevos, aparecen solos en cada archivo, vacíos.

Sirve cualquier editor de texto que guarde en UTF-8 (Visual Studio Code, Notepad++, el editor de GitHub…).

## Cómo probarla

En la app: **Aa → 🌐 idioma**, elige el idioma y pulsa **«🧪 probar un archivo de traducción…»**. Elige tu archivo (por ejemplo, `ca.json`) y la app se pone con tu traducción. Se queda así en ese dispositivo hasta que pulses «✕ quitar la traducción de prueba».

## Cómo mandarla

- **Con GitHub:** en este repositorio, abre el archivo de tu idioma, pulsa el lápiz (✏️ *Edit this file*), traduce y pulsa *Commit changes* → *Propose changes* para abrir un *pull request*. Al aceptarlo, la app se pone al día sola.
- **Sin GitHub:** manda el archivo a Richi por [richi.blog](https://richi.blog).

## Un idioma nuevo

Pide que se añada, o abre un *pull request* con un archivo nuevo con el código del idioma (por ejemplo, `ast.json` para el asturiano) que tenga sólo `{}` dentro: al aceptarlo se llena solo con todos los textos, vacíos.
