// =========================================
// LA VENTANA «turnos» de la pantalla de máster (sale de
// pantalla-master.js; se carga antes que él). Lo que usa del núcleo,
// por window.ocPM («PM.algo»); lo que el núcleo usa de aquí, a PM al final.
// =========================================
(function () {
    const PM = window.ocPM = window.ocPM || {};

    // Pasar de turno desde cualquier ventana (y decirlo).
    function pasarTurno(paso) {
        const T = window.ocTurnos;
        const d = T.leer();
        if (!d.lista.length) {
            return;
        }
        const { turno, acabadas } = T.pasar(d, paso);
        T.guardar(d, true);
        PM.avisar(ocT("Ronda {ronda}: le toca a {nombresDe}.{n}", { ronda: d.ronda, nombresDe: T.nombresDe(turno), n: acabadas.length ? ocT(" Se acaba: {join}.", { join: acabadas.join(", ") }) : "" }));
    }
    // Abrir (o traer) la iniciativa: en un modo, en su hueco si lo tiene.
    function irAIniciativa() {
        const ya = PM.estado.modulos.find((x) => x.tipo === "iniciativa");
        if (ya && ya.el) {
            if (ya.grupo && !ya.delante) {
                PM.verEnGrupo(ya, false);
            }
            PM.focoDe(ya).focus();
            return;
        }
        PM.anadir("iniciativa");
    }

    // ✎ en cada uno: su nombre, iniciativa, PG, CA y estados (en la ventana
    // de la iniciativa: window.ocTurnos.editar). Al cerrar, el foco vuelve
    // a su ✎ aunque la lista se haya vuelto a pintar.
    function botonEditar(c, dentro) {
        const id = c.id;
        const b = PM.botonDe("master-turnos-editar", "✎", () => window.ocTurnos.editar(id, {
            focus: () => (dentro.querySelector('[data-editar="' + CSS.escape(id) + '"]') || dentro).focus()
        }), ocT("Editar a {nombre}: PG, iniciativa, nombre y estados", { nombre: c.nombre }));
        b.dataset.editar = id;
        b.title = ocT("Editar: PG, iniciativa, nombre y estados");
        b.setAttribute("aria-haspopup", "dialog");
        return b;
    }

    // ⏩ TURNOS: a quién le toca, en grande (él o su grupo), los que van
    // después (dando la vuelta, con la ronda que empieza) y pasar de turno.
    function montarTurnos(dentro) {
        const T = window.ocTurnos;
        const caja = PM.crear("div", "master-turnos");
        const ronda = PM.crear("p", "master-turnos-ronda");
        const ahora = PM.crear("div", "master-turnos-ahora");
        const lista = PM.crear("ol", "master-turnos-lista");
        lista.setAttribute("aria-label", ocT("Los que van después"));
        const mandos = PM.crear("div", "master-turnos-mandos");
        mandos.append(PM.botonDe("oc-vida-accion", ocT("◀ anterior turno"), () => pasarTurno(-1), ocT("Volver al turno anterior")),
            PM.botonDe("oc-vida-accion es-principal", ocT("siguiente turno ▶"), () => pasarTurno(1), ocT("Pasar al siguiente turno")));
        function pintar() {
            const d = T.leer();
            if (!d.lista.length) {
                caja.replaceChildren(PM.crear("p", "oc-vacio", ocT("No hay nadie en la iniciativa. Añádelos en ⏱️ iniciativa, o con «⚔️ a la iniciativa» en el bestiario, los encuentros y la party.")), PM.botonDe("oc-vida-accion", ocT("⏱️ ir a la iniciativa"),
                    irAIniciativa));
                return;
            }
            if (d.modo !== "combate") {
                const orden = PM.crear("ol", "master-turnos-lista");
                orden.setAttribute("aria-label", ocT("El orden, de momento"));
                d.lista.forEach((c) => {
                    const li = PM.crear("li", "master-turnos-fila");
                    li.style.setProperty("--turno-color", PM.colorIni(c));
                    li.append(PM.crear("span", "master-turnos-nombre", c.nombre), PM.datosIni(c, true), botonEditar(c, dentro));
                    orden.appendChild(li);
                });
                caja.replaceChildren(PM.crear("p", "oc-hoja-nota", ocT("El combate no ha empezado ({n}{n2}", { n: d.lista.length, n2: d.lista.length === 1 ? ocT(" en la iniciativa).") : ocT(" en la iniciativa).") })),
                PM.botonDe("oc-vida-accion es-principal", ocT("⚔️ empezar el combate"), () => {
                    const x = T.leer();
                    T.empezar(x);
                    T.guardar(x, true);
                    PM.avisar(ocT("¡Combate! Ronda 1: le toca a {nombresDe}.", { nombresDe: T.nombresDe(T.delTurno(x, 0)) }));
                }), orden);
                return;
            }
            if (d.turno < 0 || d.turno >= d.lista.length) {
                d.turno = 0;
            }
            d.turno = T.inicioDe(d, d.turno);
            const turno = T.delTurno(d, d.turno);
            ronda.textContent = ocT("Ronda {ronda}", { ronda: d.ronda });
            ahora.replaceChildren(PM.crear("p", "master-turnos-le", ocT("le toca a")), ...turno.map((c) => {
                const fila = PM.crear("div", "master-turnos-fila es-ahora");
                fila.style.setProperty("--turno-color", PM.colorIni(c));
                fila.append(PM.crear("strong", "master-turnos-nombre", c.nombre), PM.datosIni(c, true), botonEditar(c, dentro));
                return fila;
            }));
            // Los demás turnos, en orden, desde el siguiente (los de la
            // ronda que viene, con su separador).
            const filas = [];
            let i = T.finDe(d, d.turno) + 1;
            let dadaVuelta = false;
            for (;;) {
                if (i >= d.lista.length) {
                    if (dadaVuelta || d.turno === 0) {
                        break;
                    }
                    dadaVuelta = true;
                    i = 0;
                    filas.push(PM.crear("li", "master-turnos-separa", ocT("ronda {n}", { n: d.ronda + 1 })));
                }
                if (i === d.turno) {
                    break;
                }
                const cs = T.delTurno(d, i);
                const li = PM.crear("li", "master-turnos-fila");
                li.style.setProperty("--turno-color", PM.colorIni(cs[0]));
                li.append(PM.crear("span", "master-turnos-nombre", T.nombresDe(cs)), cs.length === 1 ? PM.datosIni(cs[0], true)
                    : PM.crear("span", "master-turnos-chapa", ocT("{n} a la vez", { n: cs.length })));
                if (cs.length === 1) {
                    li.appendChild(botonEditar(cs[0], dentro));
                } else {
                    // Un grupo: un ✎ con el nombre de cada uno.
                    const cada = PM.crear("div", "master-turnos-cada");
                    cs.forEach((c) => {
                        const b = botonEditar(c, dentro);
                        b.textContent = "✎ " + c.nombre;
                        cada.appendChild(b);
                    });
                    li.appendChild(cada);
                }
                filas.push(li);
                i = T.finDe(d, i) + 1;
            }
            lista.replaceChildren(...filas);
            caja.replaceChildren(ronda, ahora, lista, mandos);
        }
        dentro.appendChild(caja);
        pintar();
        PM.mientrasEste(dentro, "oc-iniciativa:guardada", pintar);
        PM.mientrasEste(dentro, "oc-iniciativa:cambio", pintar);
    }

    Object.assign(PM, { montarTurnos, botonEditar });
})();
