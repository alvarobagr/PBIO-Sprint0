/*
-----------------------------------------------------------------------------
Fichero: test_rest_http.mjs
Descripción: Pruebas de integración HTTP de lectura y autorización del REST PBIO.
Copyright: Copyright (c) 2026 Álvaro Ballester Grau
Fecha: 14/10/2026
Autor: Álvaro Ballester Grau
Aportación: Verificar endpoints y errores HTTP sin credenciales ni escrituras
            en la base de datos de producción.
-----------------------------------------------------------------------------
*/

import test from "node:test";
import assert from "node:assert/strict";

const SERVIDOR = (
    process.env.PBIO_REST_BASE_URL || "https://abalgra1.upv.edu.es"
).replace(/\/+$/, "");

const UUID_SENSOR = "EPSG-GTI-PROY-3A";


// -------------------------------------------------------------------------
// ruta: Text, opciones: OpcionesHTTP --> consultarREST() --> RespuestaPrueba
//
// Realiza una petición de prueba y devuelve su código de estado y JSON.
// No envía ninguna credencial privada.
// -------------------------------------------------------------------------
async function consultarREST(ruta, opciones = {}) {

    const respuesta = await fetch(`${SERVIDOR}${ruta}`, {
        method: "GET",
        redirect: "error",
        signal: AbortSignal.timeout(15000),
        ...opciones,
        headers: {
            Accept: "application/json",
            ...opciones.headers
        }
    });

    assert.match(
        respuesta.headers.get("content-type") ?? "",
        /^application\/json\b/i,
        `Se esperaba JSON en ${ruta} (HTTP ${respuesta.status})`
    );

    return {
        estado: respuesta.status,
        datos: await respuesta.json()
    };
}


// -------------------------------------------------------------------------
// medicion: Medicion --> comprobarMedicion() --> B
//
// Comprueba el contrato de datos sin exigir un valor concreto de O3.
// -------------------------------------------------------------------------
function comprobarMedicion(medicion) {

    assert.ok(medicion && typeof medicion === "object");
    assert.equal(Array.isArray(medicion), false);

    assert.ok(Number.isInteger(medicion.id_medicion));
    assert.equal(typeof medicion.uuid, "string");
    assert.equal(typeof medicion.nombre_dispositivo, "string");
    assert.equal(medicion.tipo, "O3");

    assert.ok(Number.isInteger(medicion.valor));
    assert.ok(Number.isInteger(medicion.contador));
    assert.ok(Number.isInteger(medicion.rssi));

    assert.equal(typeof medicion.fecha_hora, "string");
}


test("GET /api/mediciones devuelve un listado JSON", async () => {

    const { estado, datos } = await consultarREST(
        "/api/mediciones?limit=5"
    );

    assert.equal(estado, 200);
    assert.ok(Array.isArray(datos));
    assert.ok(datos.length <= 5);

    datos.forEach(comprobarMedicion);
});


test("GET /api/mediciones respeta limit=1", async () => {

    const { estado, datos } = await consultarREST(
        "/api/mediciones?limit=1"
    );

    assert.equal(estado, 200);
    assert.ok(Array.isArray(datos));
    assert.ok(datos.length <= 1);

    datos.forEach(comprobarMedicion);
});


test("GET /api/mediciones/ultima devuelve O3 o indica ausencia", async () => {

    const { estado, datos } = await consultarREST(
        "/api/mediciones/ultima?tipo=O3"
    );

    if (estado === 404) {
        assert.equal(datos.error, "NO_ENCONTRADO");
        return;
    }

    assert.equal(estado, 200);
    comprobarMedicion(datos);
});


test("GET por dispositivo solo devuelve su UUID", async () => {

    const ruta =
        `/api/dispositivos/${UUID_SENSOR}/mediciones?limit=5`;

    const { estado, datos } = await consultarREST(ruta);

    assert.equal(estado, 200);
    assert.ok(Array.isArray(datos));
    assert.ok(datos.length <= 5);

    for (const medicion of datos) {
        comprobarMedicion(medicion);
        assert.equal(medicion.uuid, UUID_SENSOR);
    }
});


test("GET de un dispositivo desconocido devuelve lista vacía", async () => {

    const { estado, datos } = await consultarREST(
        "/api/dispositivos/PBIO-UUID-PRUEBA-SIN-DATOS/mediciones?limit=5"
    );

    assert.equal(estado, 200);
    assert.deepEqual(datos, []);
});


test("GET rechaza limit=0 con HTTP 400", async () => {

    const { estado, datos } = await consultarREST(
        "/api/mediciones?limit=0"
    );

    assert.equal(estado, 400);
    assert.equal(datos.error, "DATOS_INVALIDOS");
});


test("GET rechaza limit=101 con HTTP 400", async () => {

    const { estado, datos } = await consultarREST(
        "/api/mediciones?limit=101"
    );

    assert.equal(estado, 400);
    assert.equal(datos.error, "DATOS_INVALIDOS");
});


test("GET rechaza un límite no numérico con HTTP 400", async () => {

    const { estado, datos } = await consultarREST(
        "/api/mediciones?limit=abc"
    );

    assert.equal(estado, 400);
    assert.equal(datos.error, "DATOS_INVALIDOS");
});


test("GET ultima rechaza un tipo no admitido con HTTP 400", async () => {

    const { estado, datos } = await consultarREST(
        "/api/mediciones/ultima?tipo=OTRO"
    );

    assert.equal(estado, 400);
    assert.equal(datos.error, "DATOS_INVALIDOS");
});


test("GET de una ruta inexistente devuelve HTTP 404", async () => {

    const { estado, datos } = await consultarREST(
        "/api/ruta-inexistente"
    );

    assert.equal(estado, 404);
    assert.equal(datos.error, "RUTA_NO_ENCONTRADA");
});


test("POST sin token devuelve HTTP 401 y no registra datos", async () => {

    const { estado, datos } = await consultarREST(
        "/api/mediciones",
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: "{}"
        }
    );

    assert.equal(estado, 401);
    assert.equal(datos.error, "NO_AUTORIZADO");
});
