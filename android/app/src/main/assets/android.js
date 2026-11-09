// =========================================
// LA APP DE ANDROID: lo que en el navegador hace el propio navegador y en
// el WebView de Android no hay (ver android/app/src/main/java/…/Base.java).
// La app lo mete en cada página antes que ningún otro script, así que la
// web no cambia: sólo mira window.ocAndroid para saber que está aquí.
//   - Sin service worker (los archivos ya van dentro de la app).
//   - Las descargas (enlaces con «download» a blob: o data:) se guardan
//     donde se elija con el selector de Android; los blob: que se abrían
//     en otra pestaña (un PDF, una imagen), con la app que toque.
//   - window.open: la pantalla de jugadores y el reproductor, en su
//     ventana (o en la pantalla externa); lo de fuera, en el navegador.
//   - Las barras del sistema, del color del tema (<meta name="theme-color">).
//   - Atrás cierra lo que haya abierto (como Escape).
// =========================================
(function () {
    "use strict";
    var nativo = window.ocAndroidNativo;
    if (!nativo) {
        return;
    }

    try {
        if (navigator.serviceWorker) {
            navigator.serviceWorker.register = function () {
                return Promise.reject(new Error("En la app de Android no hace falta."));
            };
        }
    } catch (e) {
        // Sin él: la web ya lo da por perdido.
    }

    // Los blob: que hace la página, para poder leerlos luego (la CSP no
    // deja pedirlos con fetch). Al revocarlos se olvidan, con margen para
    // terminar lo que se esté guardando.
    var blobs = new Map();
    var crearUrl = URL.createObjectURL.bind(URL);
    var revocarUrl = URL.revokeObjectURL.bind(URL);
    URL.createObjectURL = function (o) {
        var u = crearUrl(o);
        if (o instanceof Blob) {
            blobs.set(u, o);
        }
        return u;
    };
    URL.revokeObjectURL = function (u) {
        revocarUrl(u);
        setTimeout(function () {
            blobs.delete(u);
        }, 120000);
    };

    function blobDe(href) {
        if (/^blob:/.test(href)) {
            return blobs.get(href) || null;
        }
        var m = /^data:([^;,]*)((?:;[^;,]*)*),(.*)$/s.exec(href);
        if (!m) {
            return null;
        }
        try {
            if (/;base64/i.test(m[2])) {
                var bin = atob(m[3]);
                var bytes = new Uint8Array(bin.length);
                for (var i = 0; i < bin.length; i++) {
                    bytes[i] = bin.charCodeAt(i);
                }
                return new Blob([bytes], { type: m[1] });
            }
            return new Blob([decodeURIComponent(m[3])], { type: m[1] || "text/plain" });
        } catch (e) {
            return null;
        }
    }

    function enBase64(buf) {
        var b = new Uint8Array(buf);
        var s = "";
        for (var i = 0; i < b.length; i += 0x8000) {
            s += String.fromCharCode.apply(null, b.subarray(i, i + 0x8000));
        }
        return btoa(s);
    }

    // Se pasa a la app por trozos (las copias de seguridad con música
    // pueden ser grandes).
    var TROZO = 768 * 1024;
    function mandar(blob, nombre, modo) {
        var id = nativo.descargaEmpezar(nombre || "archivo", blob.type || "");
        if (!id) {
            nativo.aviso("No se ha podido preparar el archivo.");
            return;
        }
        var pos = 0;
        function siguiente() {
            if (pos >= blob.size) {
                nativo.descargaTerminar(id, modo);
                return Promise.resolve();
            }
            var trozo = blob.slice(pos, pos + TROZO);
            pos += TROZO;
            return trozo.arrayBuffer().then(function (b) {
                if (!nativo.descargaTrozo(id, enBase64(b))) {
                    throw new Error("trozo");
                }
                return siguiente();
            });
        }
        siguiente().catch(function () {
            nativo.descargaCancelar(id);
            nativo.aviso("No se ha podido guardar el archivo.");
        });
    }

    // Los enlaces a blob: y data: (los de descargar los crea la página y
    // los pulsa ella misma, ya dentro del documento: pasan por aquí).
    document.addEventListener("click", function (e) {
        var a = e.target && e.target.closest ? e.target.closest("a[href]") : null;
        if (!a || !/^(blob|data):/.test(a.href)) {
            return;
        }
        e.preventDefault();
        var b = blobDe(a.href);
        if (!b) {
            nativo.aviso("No se ha podido leer el archivo.");
            return;
        }
        var guardar = a.hasAttribute("download");
        mandar(b, guardar ? a.getAttribute("download") : "", guardar ? "guardar" : "ver");
    }, true);

    function falsa() {
        return { closed: false, focus: function () {}, blur: function () {}, close: function () {}, postMessage: function () {} };
    }
    window.open = function (url) {
        var u;
        try {
            u = new URL(String(url || ""), location.href);
        } catch (e) {
            return null;
        }
        if (u.origin === location.origin) {
            nativo.abrirVentana(u.pathname.replace(/^\/+/, ""));
            return falsa();
        }
        if (/^(https?|mailto):$/.test(u.protocol)) {
            nativo.abrirFuera(u.href);
            return falsa();
        }
        if (/^(blob|data):$/.test(u.protocol)) {
            var b = blobDe(u.href);
            if (b) {
                mandar(b, "", "ver");
                return falsa();
            }
        }
        return null;
    };

    function pintarBarras() {
        var m = document.querySelector('meta[name="theme-color"]');
        if (m && m.getAttribute("content")) {
            nativo.color(m.getAttribute("content"));
        }
    }
    document.addEventListener("DOMContentLoaded", function () {
        pintarBarras();
        var m = document.querySelector('meta[name="theme-color"]');
        if (m) {
            new MutationObserver(pintarBarras).observe(m, { attributes: true, attributeFilter: ["content"] });
        }
    });

    function visible(el) {
        return el.isConnected && el.getClientRects().length > 0;
    }

    window.ocAndroid = {
        version: nativo.version(),
        // Atrás: cierra la ventanita o el diálogo de encima (como Escape).
        // Si no hay ninguno, devuelve false y la app hace lo suyo.
        atras: function () {
            var abiertos = Array.prototype.filter.call(document.querySelectorAll('dialog[open], [aria-modal="true"]'), visible);
            if (!abiertos.length) {
                return false;
            }
            var el = abiertos[abiertos.length - 1];
            var dentro = el.contains(document.activeElement) ? document.activeElement : el;
            dentro.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape", code: "Escape", keyCode: 27, bubbles: true, cancelable: true }));
            if (el.tagName === "DIALOG" && el.open) {
                el.close();
            }
            if (visible(el) && !(el.tagName === "DIALOG" && !el.open)) {
                var cerrar = el.querySelector('.oc-menu-ventana-cerrar, button[aria-label="Cerrar"]');
                if (cerrar) {
                    cerrar.click();
                }
            }
            return true;
        }
    };
})();
