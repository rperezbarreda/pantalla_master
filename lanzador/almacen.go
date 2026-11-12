package main

// EL ALMACÉN: lo que guarda la app (campañas, tableros, notas, tus
// creaciones, ajustes…) y los archivos que se suben (imágenes, PDF,
// audios) van a la carpeta «mis-datos», junto al programa, y no al
// navegador. Así cada copia de la carpeta lleva lo suyo: una descarga
// nueva empieza vacía, y borrar la carpeta lo borra todo.
//
//   GET  /__datos            → {clave: valor} (mis-datos/almacen.json)
//   POST /__datos            ← {clave: valor} entero (se escribe de una vez)
//   GET  /__archivos         → [{id, tipo, tam}]
//   GET  /__archivos/<id>    → el archivo
//   PUT  /__archivos/<id>    ← el archivo (Content-Type: su tipo)
//   DELETE /__archivos/<id>
//   PUT  /__ordenado/<ruta>  ← un archivo de «ordenado» (o, con
//                              ?archivo=<id>, la copia de uno subido)
//   DELETE /__ordenado/<ruta>
//
// «mis-datos/ordenado» es lo mismo con sus nombres, para mirarlo (cada
// campaña, tablero, personaje, documento, canción…): lo escribe la web
// (ver «LA CARPETA ORDENADA» en pantalla-master.js) y la app no lo lee.
//
// Sólo atiende a la propia app (Host y Origin 127.0.0.1:47321): otra web
// abierta en el navegador no puede leerlo ni escribirlo.

import (
	"encoding/json"
	"io"
	"net/http"
	neturl "net/url"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"sync"
	"time"
)

var (
	carpetaDatos string
	almacenMu    sync.Mutex
	idValido     = regexp.MustCompile(`^[A-Za-z0-9_-]{1,80}$`)
	trozoValido  = regexp.MustCompile(`^[^\\/:*?"<>|\x00-\x1f]{1,120}$`)
)

// La ruta en el disco de algo de «ordenado» («campañas/Mi campaña/…»), o
// "" si algún trozo no vale.
func rutaOrdenada(ruta string) string {
	if len(ruta) > 600 {
		return ""
	}
	trozos := strings.Split(ruta, "/")
	for _, t := range trozos {
		if !trozoValido.MatchString(t) || t == "." || t == ".." {
			return ""
		}
	}
	return filepath.Join(append([]string{carpetaDatos, "ordenado"}, trozos...)...)
}

// Dónde va «mis-datos»: junto a la carpeta «archivos» (en un Mac, junto a
// la .app); si ahí no se puede escribir, en la carpeta de datos del usuario.
func buscarCarpetaDatos(archivos string) string {
	junto := filepath.Join(filepath.Dir(archivos), "mis-datos")
	if strings.Contains(archivos, ".app"+string(filepath.Separator)+"Contents") {
		i := strings.Index(archivos, ".app"+string(filepath.Separator))
		junto = filepath.Join(filepath.Dir(archivos[:i+4]), "mis-datos")
	}
	if os.MkdirAll(filepath.Join(junto, "archivos"), 0o755) == nil {
		prueba := filepath.Join(junto, ".prueba")
		if os.WriteFile(prueba, []byte("x"), 0o644) == nil {
			os.Remove(prueba)
			return junto
		}
	}
	base, err := os.UserConfigDir()
	if err != nil {
		base = os.TempDir()
	}
	otra := filepath.Join(base, "Pantalla de master", "mis-datos")
	os.MkdirAll(filepath.Join(otra, "archivos"), 0o755)
	return otra
}

// Sólo la propia app.
func esDeLaApp(r *http.Request) bool {
	if r.Host != direccion {
		return false
	}
	o := r.Header.Get("Origin")
	return o == "" || o == "http://"+direccion
}

// Escribir de una vez: a un temporal y luego cambiarle el nombre (así un
// corte a medias no deja el archivo roto).
func escribirSeguro(ruta string, datos []byte) error {
	tmp := ruta + ".tmp"
	if err := os.WriteFile(tmp, datos, 0o644); err != nil {
		return err
	}
	return os.Rename(tmp, ruta)
}

func montarAlmacen(mux *http.ServeMux) {
	mux.HandleFunc("/__datos", func(w http.ResponseWriter, r *http.Request) {
		if !esDeLaApp(r) {
			http.Error(w, "", http.StatusForbidden)
			return
		}
		ruta := filepath.Join(carpetaDatos, "almacen.json")
		w.Header().Set("Cache-Control", "no-store")
		switch r.Method {
		case http.MethodGet:
			almacenMu.Lock()
			b, err := os.ReadFile(ruta)
			almacenMu.Unlock()
			if err != nil || !json.Valid(b) {
				b = []byte("{}")
			}
			w.Header().Set("Content-Type", "application/json")
			w.Write(b)
		case http.MethodPost:
			b, err := io.ReadAll(io.LimitReader(r.Body, 200<<20))
			var mapa map[string]string
			if err != nil || json.Unmarshal(b, &mapa) != nil {
				http.Error(w, "", http.StatusBadRequest)
				return
			}
			almacenMu.Lock()
			err = escribirSeguro(ruta, b)
			almacenMu.Unlock()
			if err != nil {
				http.Error(w, "", http.StatusInternalServerError)
				return
			}
			w.WriteHeader(http.StatusNoContent)
		default:
			http.Error(w, "", http.StatusMethodNotAllowed)
		}
	})
	// Un personaje de D&D Beyond (su ficha pública), para importarlo: la
	// app no puede preguntar a otras webs; el lanzador sí.
	mux.HandleFunc("/__ddb", func(w http.ResponseWriter, r *http.Request) {
		if !esDeLaApp(r) {
			http.Error(w, "", http.StatusForbidden)
			return
		}
		id := r.URL.Query().Get("id")
		if !regexp.MustCompile(`^\d{4,12}$`).MatchString(id) {
			http.Error(w, "", http.StatusBadRequest)
			return
		}
		cliente := http.Client{Timeout: 20 * time.Second}
		pet, _ := http.NewRequest(http.MethodGet, "https://character-service.dndbeyond.com/character/v5/character/"+id, nil)
		pet.Header.Set("User-Agent", "Mozilla/5.0 (Pantalla de master)")
		pet.Header.Set("Accept", "application/json")
		resp, err := cliente.Do(pet)
		if err != nil {
			http.Error(w, "", http.StatusBadGateway)
			return
		}
		defer resp.Body.Close()
		b, _ := io.ReadAll(io.LimitReader(resp.Body, 20<<20))
		w.Header().Set("Content-Type", "application/json")
		w.Header().Set("Cache-Control", "no-store")
		w.WriteHeader(resp.StatusCode)
		w.Write(b)
	})
	// Un personaje de Nivel20 (su página pública, en HTML), igual: sólo
	// la página de un personaje y su lista de conjuros, nada más.
	rutaNivel20 := regexp.MustCompile(`^/games/[a-z0-9-]{1,40}/characters/[a-z0-9-]{1,120}(/spell_list\?[A-Za-z0-9_=&%.-]{0,200})?$`)
	mux.HandleFunc("/__nivel20", func(w http.ResponseWriter, r *http.Request) {
		if !esDeLaApp(r) {
			http.Error(w, "", http.StatusForbidden)
			return
		}
		ruta := r.URL.Query().Get("ruta")
		if !rutaNivel20.MatchString(ruta) {
			http.Error(w, "", http.StatusBadRequest)
			return
		}
		cliente := http.Client{Timeout: 20 * time.Second}
		pet, _ := http.NewRequest(http.MethodGet, "https://nivel20.com"+ruta, nil)
		pet.Header.Set("User-Agent", "Mozilla/5.0 (Pantalla de master)")
		pet.Header.Set("Accept", "text/html")
		resp, err := cliente.Do(pet)
		if err != nil {
			http.Error(w, "", http.StatusBadGateway)
			return
		}
		defer resp.Body.Close()
		b, _ := io.ReadAll(io.LimitReader(resp.Body, 20<<20))
		w.Header().Set("Content-Type", "text/plain; charset=utf-8")
		w.Header().Set("Cache-Control", "no-store")
		w.WriteHeader(resp.StatusCode)
		w.Write(b)
	})
	// La foto de un personaje que se importa (de D&D Beyond o Nivel20),
	// para guardarla con él y verla sin conexión. Sólo imágenes y sólo de
	// esas webs.
	hostsImagen := regexp.MustCompile(`^([a-z0-9-]+\.)*(dndbeyond\.com|cursecdn\.com|nivel20\.com)$`)
	mux.HandleFunc("/__imagen", func(w http.ResponseWriter, r *http.Request) {
		if !esDeLaApp(r) {
			http.Error(w, "", http.StatusForbidden)
			return
		}
		u, err := neturl.Parse(r.URL.Query().Get("url"))
		if err != nil || u.Scheme != "https" || !hostsImagen.MatchString(strings.ToLower(u.Hostname())) || u.Port() != "" {
			http.Error(w, "", http.StatusBadRequest)
			return
		}
		cliente := http.Client{Timeout: 20 * time.Second, CheckRedirect: func(req *http.Request, via []*http.Request) error {
			if len(via) > 3 || req.URL.Scheme != "https" || !hostsImagen.MatchString(strings.ToLower(req.URL.Hostname())) {
				return http.ErrUseLastResponse
			}
			return nil
		}}
		pet, _ := http.NewRequest(http.MethodGet, u.String(), nil)
		pet.Header.Set("User-Agent", "Mozilla/5.0 (Pantalla de master)")
		resp, err := cliente.Do(pet)
		if err != nil {
			http.Error(w, "", http.StatusBadGateway)
			return
		}
		defer resp.Body.Close()
		tipo := resp.Header.Get("Content-Type")
		if resp.StatusCode != http.StatusOK || !strings.HasPrefix(tipo, "image/") || strings.Contains(tipo, "svg") {
			http.Error(w, "", http.StatusBadGateway)
			return
		}
		b, _ := io.ReadAll(io.LimitReader(resp.Body, 10<<20))
		w.Header().Set("Content-Type", tipo)
		w.Header().Set("Cache-Control", "no-store")
		w.Header().Set("X-Content-Type-Options", "nosniff")
		w.Write(b)
	})
	mux.HandleFunc("/__archivos", func(w http.ResponseWriter, r *http.Request) {
		if !esDeLaApp(r) {
			http.Error(w, "", http.StatusForbidden)
			return
		}
		type uno struct {
			ID   string `json:"id"`
			Tipo string `json:"tipo"`
			Tam  int64  `json:"tam"`
		}
		lista := []uno{}
		entradas, _ := os.ReadDir(filepath.Join(carpetaDatos, "archivos"))
		for _, e := range entradas {
			n := e.Name()
			if e.IsDir() || strings.HasSuffix(n, ".tipo") || strings.HasSuffix(n, ".tmp") || !idValido.MatchString(n) {
				continue
			}
			info, err := e.Info()
			if err != nil {
				continue
			}
			tipo, _ := os.ReadFile(filepath.Join(carpetaDatos, "archivos", n+".tipo"))
			lista = append(lista, uno{n, string(tipo), info.Size()})
		}
		w.Header().Set("Content-Type", "application/json")
		w.Header().Set("Cache-Control", "no-store")
		json.NewEncoder(w).Encode(lista)
	})
	mux.HandleFunc("/__ordenado/", func(w http.ResponseWriter, r *http.Request) {
		if !esDeLaApp(r) {
			http.Error(w, "", http.StatusForbidden)
			return
		}
		ruta := rutaOrdenada(strings.TrimPrefix(r.URL.Path, "/__ordenado/"))
		if ruta == "" {
			http.Error(w, "", http.StatusBadRequest)
			return
		}
		switch r.Method {
		case http.MethodPut:
			var b []byte
			var err error
			if id := r.URL.Query().Get("archivo"); id != "" {
				if !idValido.MatchString(id) {
					http.Error(w, "", http.StatusBadRequest)
					return
				}
				b, err = os.ReadFile(filepath.Join(carpetaDatos, "archivos", id))
			} else {
				b, err = io.ReadAll(io.LimitReader(r.Body, 60<<20))
			}
			if err != nil {
				http.Error(w, "", http.StatusBadRequest)
				return
			}
			if os.MkdirAll(filepath.Dir(ruta), 0o755) != nil || escribirSeguro(ruta, b) != nil {
				http.Error(w, "", http.StatusInternalServerError)
				return
			}
			w.WriteHeader(http.StatusNoContent)
		case http.MethodDelete:
			os.Remove(ruta)
			// Y su carpeta, si se ha quedado vacía (hasta «ordenado»).
			base := filepath.Join(carpetaDatos, "ordenado")
			for d := filepath.Dir(ruta); d != base && strings.HasPrefix(d, base); d = filepath.Dir(d) {
				if os.Remove(d) != nil {
					break
				}
			}
			w.WriteHeader(http.StatusNoContent)
		default:
			http.Error(w, "", http.StatusMethodNotAllowed)
		}
	})
	mux.HandleFunc("/__archivos/", func(w http.ResponseWriter, r *http.Request) {
		if !esDeLaApp(r) {
			http.Error(w, "", http.StatusForbidden)
			return
		}
		id := strings.TrimPrefix(r.URL.Path, "/__archivos/")
		if !idValido.MatchString(id) {
			http.Error(w, "", http.StatusBadRequest)
			return
		}
		ruta := filepath.Join(carpetaDatos, "archivos", id)
		switch r.Method {
		case http.MethodGet:
			tipo, _ := os.ReadFile(ruta + ".tipo")
			b, err := os.ReadFile(ruta)
			if err != nil {
				http.Error(w, "", http.StatusNotFound)
				return
			}
			if len(tipo) > 0 {
				w.Header().Set("Content-Type", string(tipo))
			}
			w.Header().Set("Cache-Control", "no-store")
			w.Write(b)
		case http.MethodPut:
			b, err := io.ReadAll(io.LimitReader(r.Body, 60<<20))
			if err != nil {
				http.Error(w, "", http.StatusBadRequest)
				return
			}
			tipo := r.Header.Get("Content-Type")
			if len(tipo) > 100 {
				tipo = ""
			}
			if escribirSeguro(ruta, b) != nil || os.WriteFile(ruta+".tipo", []byte(tipo), 0o644) != nil {
				http.Error(w, "", http.StatusInternalServerError)
				return
			}
			w.WriteHeader(http.StatusNoContent)
		case http.MethodDelete:
			os.Remove(ruta)
			os.Remove(ruta + ".tipo")
			w.WriteHeader(http.StatusNoContent)
		default:
			http.Error(w, "", http.StatusMethodNotAllowed)
		}
	})
}
