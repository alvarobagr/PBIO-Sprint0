/*
-----------------------------------------------------------------------------
Fichero: test_logica_mediciones_cliente.mjs
Descripción: Tests unitarios del proxy de lógica de negocio del frontend.
Copyright: Copyright (c) 2026 Álvaro Ballester Grau
Fecha: 13/10/2026
Autor: Álvaro Ballester Grau
Aportación: Pruebas sin realizar peticiones a Internet ni modificar MySQL.
-----------------------------------------------------------------------------
*/

import test from "node:test";
import assert from "node:assert/strict";
import { LogicaMedicionesCliente } from "../LogicaMedicionesCliente.js";

// -------------------------------------------------------------------------
// estado: N, datos: [Medicion]|Medicion --> respuestaFake() --> Respuesta
//
// Construye una respuesta de prueba para simular el servidor.
// -------------------------------------------------------------------------
function respuestaFake(estado, datos) {
    return {
        ok: estado >= 200 && estado < 300,
        status: estado,
        json: async () => datos
    };
}

test("listarMediciones conserva valores negativos y utiliza limit=5", async () => {
    let rutaSolicitada = "";
    const proxy = new LogicaMedicionesCliente(async (ruta) => {
        rutaSolicitada = ruta;
        return respuestaFake(200, [{ id_medicion: 11, valor: -32, tipo: "O3" }]);
    });
    const lista = await proxy.listarMediciones(5);
    assert.equal(rutaSolicitada, "/api/mediciones?limit=5");
    assert.equal(lista[0].valor, -32);
});

test("obtenerUltimaMedicion mantiene el contrato Medicion?", async () => {
    const proxy = new LogicaMedicionesCliente(async (ruta) => {
        assert.equal(ruta, "/api/mediciones/ultima?tipo=O3");
        return respuestaFake(200, { id_medicion: 10, valor: 119, tipo: "O3" });
    });
    assert.equal((await proxy.obtenerUltimaMedicion("O3")).valor, 119);
});

test("obtenerUltimaMedicion devuelve null si el servidor indica ausencia", async () => {
    const proxy = new LogicaMedicionesCliente(async () => respuestaFake(404, { error: "SIN_DATOS" }));
    assert.equal(await proxy.obtenerUltimaMedicion("O3"), null);
});

test("rechaza un límite fuera de rango sin consultar el servidor", async () => {
    const proxy = new LogicaMedicionesCliente(async () => { throw new Error("No debería llamarse"); });
    await assert.rejects(proxy.listarMediciones(101), RangeError);
    await assert.rejects(proxy.listarMediciones(0), RangeError);
});

test("informa del error si el backend no responde correctamente", async () => {
    const proxy = new LogicaMedicionesCliente(async () => respuestaFake(500, { error: "ERROR_INTERNO" }));
    await assert.rejects(proxy.listarMediciones(5), /No se pudieron consultar/);
});

test("rechaza una estructura de listado incorrecta", async () => {
    const proxy = new LogicaMedicionesCliente(async () => respuestaFake(200, { valor: 123 }));
    await assert.rejects(proxy.listarMediciones(5), /listado no válido/);
});
