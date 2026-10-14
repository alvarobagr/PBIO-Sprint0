/*
-----------------------------------------------------------------------------
Fichero: test_gui.mjs
Descripción: Pruebas estáticas de acoplamiento y controles de la GUI.
Copyright: Copyright (c) 2026 Álvaro Ballester Grau
Fecha: 13/10/2026
Autor: Álvaro Ballester Grau
Aportación: Comprobar la ausencia de transporte HTTP en la interfaz.
-----------------------------------------------------------------------------
*/

import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const raiz = new URL("../", import.meta.url);

test("la GUI no realiza peticiones HTTP directas", async () => {
    const codigo = await readFile(new URL("app.js", raiz), "utf8");
    assert.doesNotMatch(codigo, /\bfetch\s*\(|\bXMLHttpRequest\b|\bWebSocket\b/);
    assert.match(codigo, /LogicaMedicionesCliente/);
    assert.match(codigo, /obtenerUltimaMedicion\("O3"\)/);
    assert.match(codigo, /listarMediciones\(5\)/);
});

test("la interfaz contiene los controles de consulta y presentación", async () => {
    const html = await readFile(new URL("index.html", raiz), "utf8");
    for (const nombre of ["valorUltima", "filasMediciones", "botonActualizar", "estadoConsulta"]) {
        assert.match(html, new RegExp(`id="${nombre}"`));
    }
});

test("el navegador no necesita tokens para consultar mediciones", async () => {
    const codigo = await readFile(new URL("app.js", raiz), "utf8");
    const html = await readFile(new URL("index.html", raiz), "utf8");
    assert.doesNotMatch(codigo + html, /PBIO_API_TOKEN|X-PBIO-TOKEN|api_token/i);
});
