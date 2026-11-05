// EL LANZADOR: un ejecutable que va al lado de la carpeta «archivos»
// (la app: index.html, estilos, scripts…). Al abrirlo sirve
// la app en http://127.0.0.1:47321 (sólo para este ordenador; no se ve
// desde la red) y la abre en una ventana propia (Edge o Chrome en modo
// app; si no, el navegador de siempre). Si ya estaba abierto, sólo abre
// otra ventana. Se cierra solo cuando lleva un rato sin ninguna ventana
// de la app abierta (app.js le avisa cada 20 s de que sigue viva).
//
// El puerto es siempre el mismo a propósito: lo que guarda la app
// (campañas, notas, creaciones…) va con la dirección, y si cambiara se
// «perdería» todo.
//
// Busca la carpeta «archivos» a su lado (Windows y Linux: el .exe y la
// carpeta, juntos en «Pantalla de máster/») o, en un Mac, dentro de la
// propia «Pantalla de máster.app» (Contents/Resources/archivos: macOS
// mueve las apps descargadas a un sitio aparte la primera vez, y lo de
// al lado no lo encontraría). Se construye con
// herramientas/construir-ejecutables.py.
package main

import (
	"crypto/sha256"
	"encoding/hex"
	"mime"
	"net"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"sync"
	"sync/atomic"
	"time"
)

const direccion = "127.0.0.1:47321"
const url = "http://" + direccion + "/"

// La versión (la de sw.js, la pone construir-ejecutables.py): si al
// abrirlo hay otro lanzador en marcha con otra versión, se le pide que se
// cierre y se ocupa su sitio, para no seguir sirviendo la app vieja.
var version = "dev"

// Cuándo se supo por última vez de una ventana de la app.
var ultimo atomic.Int64

// Las ventanas abiertas (cada una con su id) y cuándo se supo de cada
// una. Al cerrarse, la ventana avisa (/__adios); cuando no queda ninguna,
// el lanzador se cierra a los pocos segundos (por si sólo era recargar),
// y así se pueden borrar o mover sus archivos enseguida.
var (
	ventanas   = map[string]int64{}
	ventanasMu sync.Mutex
	vacioDesde atomic.Int64
)

func idDe(r *http.Request) string {
	b := make([]byte, 64)
	n, _ := r.Body.Read(b)
	return string(b[:n])
}

func main() {
	carpeta := buscarArchivos()
	escucha, err := net.Listen("tcp", direccion)
	if err != nil {
		// Ya hay uno en marcha. Si es esta misma versión y desde esta misma
		// carpeta, sólo abrir; si es otra versión u otra copia (otra
		// descarga, con su «mis-datos»), que se cierre y quedarse con el
		// sitio: si no, se verían los datos de la otra copia.
		if versionEnMarcha() == version && dondeEnMarcha() == huella(carpeta) {
			abrir()
			return
		}
		cliente := http.Client{Timeout: 2 * time.Second}
		if r, e := cliente.Post(url+"__salir", "text/plain", nil); e == nil {
			r.Body.Close()
		}
		for i := 0; i < 30 && err != nil; i++ {
			time.Sleep(200 * time.Millisecond)
			escucha, err = net.Listen("tcp", direccion)
		}
		if err != nil {
			// No se va (uno muy viejo, o otro programa): abrir lo que hay.
			abrir()
			return
		}
	}
	mime.AddExtensionType(".webmanifest", "application/manifest+json")
	mime.AddExtensionType(".woff2", "font/woff2")
	mime.AddExtensionType(".ogg", "audio/ogg")
	mime.AddExtensionType(".json", "application/json")
	mime.AddExtensionType(".svg", "image/svg+xml")
	mime.AddExtensionType(".js", "text/javascript")
	if carpeta == "" {
		escucha.Close()
		avisarFalta("No encuentro la carpeta «archivos». Tiene que estar junto a este programa, tal como viene en el .zip.")
		return
	}
	servidor := http.FileServer(http.Dir(carpeta))
	carpetaDatos = buscarCarpetaDatos(carpeta)
	mux := http.NewServeMux()
	montarAlmacen(mux)
	mux.HandleFunc("/__version", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Cache-Control", "no-store")
		w.Write([]byte(version))
	})
	mux.HandleFunc("/__donde", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Cache-Control", "no-store")
		w.Write([]byte(huella(carpeta)))
	})
	mux.HandleFunc("/__salir", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			http.Error(w, "", http.StatusMethodNotAllowed)
			return
		}
		w.WriteHeader(http.StatusNoContent)
		go func() {
			time.Sleep(100 * time.Millisecond)
			os.Exit(0)
		}()
	})
	// Aquí no hace falta guardar la app para sin conexión (ya está en el
	// ordenador), y lo guardado podía dejar la versión vieja delante: en
	// vez del service worker de la web, uno que borra lo guardado, se da
	// de baja y recarga las ventanas.
	mux.HandleFunc("/sw.js", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "text/javascript")
		w.Header().Set("Cache-Control", "no-store")
		w.Write([]byte(swLimpiar))
	})
	mux.HandleFunc("/__vivo", func(w http.ResponseWriter, r *http.Request) {
		ahora := time.Now().Unix()
		ultimo.Store(ahora)
		if id := idDe(r); id != "" {
			ventanasMu.Lock()
			ventanas[id] = ahora
			ventanasMu.Unlock()
		}
		w.WriteHeader(http.StatusNoContent)
	})
	mux.HandleFunc("/__adios", func(w http.ResponseWriter, r *http.Request) {
		id := idDe(r)
		ventanasMu.Lock()
		delete(ventanas, id)
		if len(ventanas) == 0 {
			vacioDesde.Store(time.Now().Unix())
		}
		ventanasMu.Unlock()
		w.WriteHeader(http.StatusNoContent)
	})
	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		ultimo.Store(time.Now().Unix())
		w.Header().Set("Cache-Control", "no-cache")
		servidor.ServeHTTP(w, r)
	})
	ultimo.Store(time.Now().Unix())
	go func() {
		for range time.Tick(time.Second) {
			ahora := time.Now().Unix()
			ventanasMu.Lock()
			// Una ventana que lleva más de 3 minutos sin dar señales
			// (cerrada a lo bruto): fuera. Las minimizadas avisan, como
			// poco, una vez por minuto.
			for id, t := range ventanas {
				if ahora-t > 180 {
					delete(ventanas, id)
					vacioDesde.Store(ahora)
				}
			}
			quedan := len(ventanas)
			ventanasMu.Unlock()
			// Sin ventanas desde hace 4 s (lo justo para recargar): se acabó.
			if quedan == 0 && vacioDesde.Load() > 0 && ahora-vacioDesde.Load() >= 4 {
				os.Exit(0)
			}
			// Por si acaso: dos minutos sin noticias de nadie.
			if ahora-ultimo.Load() > 120 {
				os.Exit(0)
			}
		}
	}()
	go func() {
		time.Sleep(300 * time.Millisecond)
		abrir()
	}()
	http.Serve(escucha, mux)
}

const swLimpiar = `self.addEventListener("install", () => self.skipWaiting());
self.addEventListener("activate", (e) => e.waitUntil(caches.keys()
    .then((ks) => Promise.all(ks.map((k) => caches.delete(k))))
    .then(() => self.registration.unregister())
    .then(() => self.clients.matchAll({ type: "window" }))
    .then((cs) => cs.forEach((c) => c.navigate(c.url)))));
`

// Una huella de la carpeta «archivos» (para saber si el lanzador en marcha
// es esta misma copia sin enseñar la ruta).
func huella(carpeta string) string {
	if carpeta == "" {
		return ""
	}
	h := sha256.Sum256([]byte(strings.ToLower(filepath.Clean(carpeta))))
	return hex.EncodeToString(h[:8])
}

// De qué copia es el lanzador en marcha ("" si no contesta o es uno viejo).
func dondeEnMarcha() string {
	cliente := http.Client{Timeout: 2 * time.Second}
	r, err := cliente.Get(url + "__donde")
	if err != nil {
		return ""
	}
	defer r.Body.Close()
	if r.StatusCode != http.StatusOK {
		return ""
	}
	b := make([]byte, 64)
	n, _ := r.Body.Read(b)
	return string(b[:n])
}

// La versión del lanzador que ya está en marcha ("" si no contesta).
func versionEnMarcha() string {
	cliente := http.Client{Timeout: 2 * time.Second}
	r, err := cliente.Get(url + "__version")
	if err != nil {
		return ""
	}
	defer r.Body.Close()
	if r.StatusCode != http.StatusOK {
		return ""
	}
	b := make([]byte, 64)
	n, _ := r.Body.Read(b)
	return string(b[:n])
}

// Dónde está la carpeta «archivos» (con su index.html).
func buscarArchivos() string {
	exe, err := os.Executable()
	if err != nil {
		return ""
	}
	if real, err := filepath.EvalSymlinks(exe); err == nil {
		exe = real
	}
	dir := filepath.Dir(exe)
	for _, c := range []string{
		filepath.Join(dir, "archivos"),
		filepath.Join(dir, "..", "Resources", "archivos"),
		filepath.Join(dir, "..", "..", "..", "archivos"),
	} {
		if _, err := os.Stat(filepath.Join(c, "index.html")); err == nil {
			return c
		}
	}
	return ""
}

// La app en una ventana propia: Edge o Chrome en modo app (sin barra de
// direcciones); si no hay, el navegador de siempre.
func abrir() {
	switch runtime.GOOS {
	case "windows":
		for _, p := range candidatosWindows() {
			if _, err := os.Stat(p); err == nil {
				if exec.Command(p, "--app="+url).Start() == nil {
					return
				}
			}
		}
		exec.Command("rundll32", "url.dll,FileProtocolHandler", url).Start()
	case "darwin":
		for _, a := range []string{"Google Chrome", "Microsoft Edge", "Brave Browser"} {
			if _, err := os.Stat("/Applications/" + a + ".app"); err == nil {
				if exec.Command("open", "-na", a, "--args", "--app="+url).Run() == nil {
					return
				}
			}
		}
		exec.Command("open", url).Start()
	default:
		for _, n := range []string{"google-chrome", "chromium", "chromium-browser", "microsoft-edge"} {
			if p, err := exec.LookPath(n); err == nil {
				if exec.Command(p, "--app="+url).Start() == nil {
					return
				}
			}
		}
		exec.Command("xdg-open", url).Start()
	}
}

func candidatosWindows() []string {
	var lista []string
	for _, base := range []string{os.Getenv("ProgramFiles(x86)"), os.Getenv("ProgramFiles"), os.Getenv("LocalAppData")} {
		if base == "" {
			continue
		}
		lista = append(lista,
			filepath.Join(base, "Microsoft", "Edge", "Application", "msedge.exe"),
			filepath.Join(base, "Google", "Chrome", "Application", "chrome.exe"))
	}
	return lista
}
