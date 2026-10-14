/*
-----------------------------------------------------------------------------
Fichero: app.js
Descripción: Presentación de datos y eventos de la interfaz web PBIO.
Copyright: Copyright (c) 2026 Álvaro Ballester Grau
Fecha: 13/10/2026
Autor: Álvaro Ballester Grau
Aportación: GUI sin comunicación HTTP directa: consume el proxy del cliente.
-----------------------------------------------------------------------------
*/

import {
    LogicaMedicionesCliente
} from "../frontend_business_logic/LogicaMedicionesCliente.js";


const logicaMediciones = new LogicaMedicionesCliente();

let consultaEnCurso = false;


// -------------------------------------------------------------------------
// valor: Text|N|Z --> mostrarDato() --> Text
//
// Produce un texto presentable para un campo de la interfaz.
// -------------------------------------------------------------------------
function mostrarDato(valor) {

    return (
        valor === undefined ||
        valor === null ||
        valor === ""
    )
        ? "—"
        : String(valor);
}


// -------------------------------------------------------------------------
// medicion: Medicion? --> presentarUltimaMedicion()
//
// Muestra la última medición en el panel superior.
// -------------------------------------------------------------------------
function presentarUltimaMedicion(medicion) {

    document.getElementById("valorUltima").textContent =
        mostrarDato(medicion?.valor);

    document.getElementById("nombreDispositivo").textContent =
        mostrarDato(medicion?.nombre_dispositivo);

    document.getElementById("fechaUltima").textContent =
        mostrarDato(medicion?.fecha_hora);

    document.getElementById("contadorUltima").textContent =
        mostrarDato(medicion?.contador);

    document.getElementById("rssiUltima").textContent =
        medicion?.rssi === undefined || medicion?.rssi === null
            ? "—"
            : `${medicion.rssi} dBm`;
}


// -------------------------------------------------------------------------
// mediciones: [Medicion] --> presentarHistorial()
//
// Renderiza la tabla mediante textContent para no interpretar datos como HTML.
// -------------------------------------------------------------------------
function presentarHistorial(mediciones) {

    const cuerpo = document.getElementById("filasMediciones");

    cuerpo.replaceChildren();

    if (mediciones.length === 0) {

        const fila = document.createElement("tr");

        const celda = document.createElement("td");

        celda.colSpan = 6;
        celda.textContent = "Todavía no hay mediciones registradas.";
        celda.className = "sin-datos";

        fila.appendChild(celda);

        cuerpo.appendChild(fila);

        return;
    }

    for (const medicion of mediciones) {

        const fila = document.createElement("tr");

        const columnas = [
            medicion.id_medicion,
            medicion.fecha_hora,
            medicion.tipo,
            `${mostrarDato(medicion.valor)} ppb`,
            medicion.contador,
            medicion.rssi === null || medicion.rssi === undefined
                ? "—"
                : `${medicion.rssi} dBm`
        ];

        for (const valor of columnas) {

            const celda = document.createElement("td");

            celda.textContent = mostrarDato(valor);

            fila.appendChild(celda);
        }

        cuerpo.appendChild(fila);
    }
}


// -------------------------------------------------------------------------
// --> actualizarPanel()
//
// Obtiene datos por la interfaz de dominio del proxy y actualiza la pantalla.
// -------------------------------------------------------------------------
async function actualizarPanel() {

    if (consultaEnCurso) {
        return;
    }

    consultaEnCurso = true;

    const boton = document.getElementById("botonActualizar");

    const estado = document.getElementById("estadoConsulta");

    boton.disabled = true;

    estado.textContent = "Consultando el servidor…";

    try {

        const [ultima, historial] = await Promise.all([
            logicaMediciones.obtenerUltimaMedicion("O3"),
            logicaMediciones.listarMediciones(5)
        ]);

        presentarUltimaMedicion(ultima);

        presentarHistorial(historial);

        estado.textContent =
            `Datos actualizados: ${new Date().toLocaleTimeString("es-ES")}`;

    } catch (error) {

        estado.textContent =
            "No se ha podido actualizar. Comprueba la conexión y vuelve a intentarlo.";

        console.error("PBIO GUI:", error.message);

    } finally {

        consultaEnCurso = false;

        boton.disabled = false;
    }
}


// -------------------------------------------------------------------------
// --> inicializarInterfaz()
//
// Registra el botón y prepara la actualización periódica de la pantalla.
// -------------------------------------------------------------------------
function inicializarInterfaz() {

    document.getElementById("botonActualizar").addEventListener(
        "click",
        actualizarPanel
    );

    actualizarPanel();

    window.setInterval(() => {

        if (!document.hidden) {

            actualizarPanel();
        }

    }, 15000);
}


inicializarInterfaz();
