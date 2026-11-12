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
//   - Si se eligió una carpeta (⚙ opciones), lo guardado va allí, como
//     «mis-datos» en el programa: localStorage se cambia por uno que lee y
//     escribe en la app clave a clave (Carpeta.java), y window.ocAlmacen
//     es como el del programa (arranque.js) más guardarArchivo/borrarArchivo.
//   - Si suena algo largo (música, ambiente), se lo dice a la app para que
//     siga sonando con la app detrás (Servicio.java).
// =========================================
(function () {
    "use strict";
    var nativo = window.ocAndroidNativo;
    if (!nativo) {
        return;
    }

    // ---- LO GUARDADO, EN LA CARPETA ----
    if (nativo.carpetaHay()) {
        var navegador = window.localStorage;
        var almacen = {
            getItem: function (k) {
                return JSON.parse(nativo.datoLeer(String(k)));
            },
            setItem: function (k, v) {
                nativo.datoPoner(String(k), String(v));
            },
            removeItem: function (k) {
                nativo.datoQuitar(String(k));
            },
            clear: function () {
                nativo.datosVaciar();
            },
            key: function (i) {
                return JSON.parse(nativo.datosClaves())[i] || null;
            }
        };
        Object.defineProperty(almacen, "length", { get: function () {
            return JSON.parse(nativo.datosClaves()).length;
        } });
        var cambiado = false;
        try {
            Object.defineProperty(window, "localStorage", { configurable: true, get: function () {
                return almacen;
            } });
            cambiado = true;
        } catch (e) {
            cambiado = false;
        }
        if (cambiado) {
            window.ocAlmacen = {
                enCarpeta: true,
                android: true,
                nombre: nativo.carpetaNombre(),
                vacio: nativo.datosVacios(),
                navegador: navegador,
                guardarYa: function () {
                    return nativo.datosGuardarYa() ? Promise.resolve() : Promise.reject(new Error("no se ha guardado"));
                },
                // Un archivo subido (imagen, PDF, música) a «archivos/» de la carpeta.
                guardarArchivo: function (id, blob) {
                    return pasar(blob, String(id), blob.type || "").then(function (tok) {
                        if (!nativo.archivoGuardar(tok, String(id))) {
                            throw new Error("no se ha guardado");
                        }
                    });
                },
                borrarArchivo: function (id) {
                    nativo.archivoBorrar(String(id));
                    return Promise.resolve();
                },
                // «ordenado/»: lo mismo con sus nombres, para mirarlo (la
                // app lo escribe en su hilo, sin esperar).
                ordenado: {
                    escribir: function (ruta, texto) {
                        nativo.ordenadoEscribir(String(ruta), String(texto));
                        return Promise.resolve();
                    },
                    copiar: function (ruta, id) {
                        nativo.ordenadoCopiar(String(ruta), String(id));
                        return Promise.resolve();
                    },
                    borrar: function (ruta) {
                        nativo.ordenadoBorrar(String(ruta));
                        return Promise.resolve();
                    }
                }
            };
        }
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
    // Pasa el blob a la app; luego se termina con descargaTerminar (guardar
    // o ver) o archivoGuardar (a la carpeta).
    function pasar(blob, nombre, tipo) {
        var id = nativo.descargaEmpezar(nombre || "archivo", tipo);
        if (!id) {
            return Promise.reject(new Error("no se ha podido preparar"));
        }
        var pos = 0;
        function siguiente() {
            if (pos >= blob.size) {
                return Promise.resolve(id);
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
        return siguiente().catch(function (e) {
            nativo.descargaCancelar(id);
            throw e;
        });
    }
    function mandar(blob, nombre, modo) {
        pasar(blob, nombre, blob.type || "").then(function (id) {
            nativo.descargaTerminar(id, modo);
        }).catch(function () {
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

    // ---- ¿SUENA ALGO LARGO? (música, ambiente en bucle; no los efectos) ----
    var suenan = new Set();
    var dicho = false;
    function contar() {
        var si = suenan.size > 0;
        if (si !== dicho) {
            dicho = si;
            nativo.sonando(si);
        }
    }
    function mirar(el) {
        if (el.__ocMirado) {
            return;
        }
        el.__ocMirado = true;
        el.addEventListener("playing", function () {
            if (el.loop || !isFinite(el.duration) || el.duration > 20) {
                suenan.add(el);
                contar();
            }
        });
        ["pause", "ended", "emptied", "abort", "error"].forEach(function (t) {
            el.addEventListener(t, function () {
                suenan.delete(el);
                contar();
            });
        });
    }
    var tocar = HTMLMediaElement.prototype.play;
    HTMLMediaElement.prototype.play = function () {
        mirar(this);
        return tocar.apply(this, arguments);
    };
    document.addEventListener("play", function (e) {
        if (e.target instanceof HTMLMediaElement) {
            mirar(e.target);
        }
    }, true);
    // Lo de Web Audio (los sonidos propios del ambiente).
    var Fuente = window.AudioScheduledSourceNode;
    if (Fuente && Fuente.prototype.start) {
        var empezar = Fuente.prototype.start;
        Fuente.prototype.start = function () {
            var n = this;
            if (n.loop || (n.buffer && n.buffer.duration > 20)) {
                suenan.add(n);
                contar();
                n.addEventListener("ended", function () {
                    suenan.delete(n);
                    contar();
                });
            }
            return empezar.apply(this, arguments);
        };
    }
    window.addEventListener("pagehide", function () {
        suenan.clear();
        contar();
    });

    var recibidos = [];

    function visible(el) {
        return el.isConnected && el.getClientRects().length > 0;
    }

    window.ocAndroid = {
        version: nativo.version(),
        // ⚙ opciones: dónde se guarda, la pantalla encendida y abrir fuera.
        carpetaPerdida: nativo.carpetaPerdida(),
        elegirCarpeta: function () {
            nativo.carpetaElegir();
        },
        quitarCarpeta: function () {
            nativo.carpetaQuitar();
        },
        encendida: function (si) {
            nativo.encendida(!!si);
        },
        abrirFuera: function (url) {
            nativo.abrirFuera(String(url));
        },
        // La copia automática del día (en la carpeta).
        copiaFalta: function () {
            return nativo.copiaFalta();
        },
        guardarCopia: function (texto) {
            return nativo.copiaGuardar(String(texto));
        },
        // Lo que llega con «Abrir con» o «Compartir»: la app lo deja en
        // /__recibido/<id>; aquí se hace File y se pasa a window.ocRecibir
        // (pantalla-master.js) cuando ya está (si no, espera en la cola).
        hayRecibidos: function () {
            var nuevos = JSON.parse(nativo.recibidos() || "[]");
            Promise.all(nuevos.map(function (x) {
                return fetch("/__recibido/" + encodeURIComponent(x.id), { cache: "no-store" }).then(function (r) {
                    return r.ok ? r.blob() : null;
                }).then(function (b) {
                    return b ? new File([b], x.nombre, { type: x.tipo || b.type }) : null;
                });
            })).then(function (fs) {
                recibidos = recibidos.concat(fs.filter(Boolean));
                if (recibidos.length && window.ocRecibir) {
                    var dar = recibidos;
                    recibidos = [];
                    window.ocRecibir(dar);
                }
            });
        },
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
