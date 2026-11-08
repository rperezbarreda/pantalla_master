// =========================================
// EL SERVICE WORKER: guarda la app entera en el dispositivo al
// instalarla y la sirve desde ahí, con o sin conexión.
// ARCHIVO GENERADO por herramientas/precache.py: no lo edites aquí, sino
// en herramientas/sw-plantilla.js. La versión cambia sola cuando cambia
// cualquier archivo, y entonces la app avisa de que hay una nueva.
// =========================================
const VERSION = "__VERSION__";
const CACHE = "pantalla-master-" + VERSION;
const ARCHIVOS = __ARCHIVOS__;

// Cada archivo se pide con «?v=VERSION» y sin la caché del navegador: así
// no se cuela uno viejo que tuviera guardado el navegador o Cloudflare (el
// sitio va detrás). Se guarda con su nombre de siempre.
self.addEventListener("install", (e) => {
    e.waitUntil(caches.open(CACHE).then((c) => Promise.all(ARCHIVOS.map((a) => {
        const u = new URL(a, self.location);
        u.searchParams.set("v", VERSION);
        return fetch(u, { cache: "reload" }).then((r) => {
            if (!r.ok) {
                throw new Error(a + ": " + r.status);
            }
            return c.put(a, r);
        });
    }))));
});

// La nueva sólo entra cuando la app lo pide («↻ actualizar»), o sola si
// no había ninguna.
self.addEventListener("message", (e) => {
    if (e.data === "activar") {
        self.skipWaiting();
    }
});

self.addEventListener("activate", (e) => {
    e.waitUntil(caches.keys().then((ks) => Promise.all(ks.filter((k) => k.indexOf("pantalla-master-") === 0 && k !== CACHE)
        .map((k) => caches.delete(k)))).then(() => self.clients.claim()));
});

// Primero lo guardado; si no está (un sonido que no se precarga), a la
// red, y se guarda para la próxima.
self.addEventListener("fetch", (e) => {
    const u = new URL(e.request.url);
    if (e.request.method !== "GET" || u.origin !== location.origin) {
        return;
    }
    e.respondWith(caches.match(e.request, { ignoreSearch: true }).then((r) => r || fetch(e.request).then((resp) => {
        if (resp.ok && resp.type === "basic") {
            const copia = resp.clone();
            caches.open(CACHE).then((c) => c.put(e.request, copia));
        }
        return resp;
    }).catch(() => (e.request.mode === "navigate" ? caches.match("index.html") : Response.error()))));
});
