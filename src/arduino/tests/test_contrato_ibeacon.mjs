/*
-----------------------------------------------------------------------------
Fichero: test_contrato_ibeacon.mjs
Descripción: Pruebas automáticas del contrato Arduino-iBeacon a partir del
             firmware del Sprint 0, sin placa conectada.
Copyright: Copyright (c) 2026 Álvaro Ballester Grau
Fecha: 14/10/2026
Autor: Álvaro Ballester Grau
Aportación: Verificación estática de UUID, codificación Major/Minor,
            límites, publicidad BLE y flujo de publicación.
-----------------------------------------------------------------------------
*/

import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

// El firmware real está en src/arduino/leeryemitir/leeryemitir.ino.
const rutaFirmware = new URL("../leeryemitir/leeryemitir.ino", import.meta.url);
const firmware = await readFile(rutaFirmware, "utf8");

// Eliminamos comentarios antes de verificar el código real.
const codigo = firmware
    .replace(/\/\*[\s\S]*?\*\//g, "")
    .replace(/\/\/[^\r\n]*/g, "");

const sinEspacios = codigo.replace(/\s+/g, "");


// -------------------------------------------------------------------------
// valor_ppm: R --> codificarMinorPrueba() --> valor: Z, bits: N
//
// Modelo independiente de la conversión documentada, usado para probar
// casos límite y la interpretación del entero con signo.
// -------------------------------------------------------------------------
function codificarMinorPrueba(valorPpm) {

    const valor = Math.max(
        -32768,
        Math.min(
            32767,
            Math.sign(valorPpm) * Math.round(Math.abs(valorPpm * 1000))
        )
    );

    const bits = valor & 0xFFFF;

    const reconstruido = bits >= 0x8000
        ? bits - 0x10000
        : bits;

    return { valor, bits, reconstruido };
}


// -------------------------------------------------------------------------
// tipo: N, contador: N --> codificarMajorPrueba() --> major: N
//
// Modelo de los dos octetos que componen el campo Major de iBeacon.
// -------------------------------------------------------------------------
function codificarMajorPrueba(tipo, contador) {

    return ((tipo & 0xFF) << 8) | (contador & 0xFF);
}


test("El firmware anuncia el UUID de 16 bytes del proyecto", () => {

    const inicializador = codigo.match(
        /beaconUUID\s*\[\s*16\s*\]\s*=\s*\{([^}]+)\}/
    );

    assert.ok(
        inicializador,
        "No aparece el array beaconUUID de 16 bytes"
    );

    const caracteres = [
        ...inicializador[1].matchAll(/'([^']{1})'/g)
    ].map(x => x[1]);

    assert.equal(caracteres.length, 16);
    assert.equal(caracteres.join(""), "EPSG-GTI-PROY-3A");
});


test("El identificador de O3 es 14 y el fabricante iBeacon es Apple", () => {

    assert.match(
        codigo,
        /constexpr\s+uint8_t\s+ID_O3\s*=\s*14\s*;/
    );

    assert.match(
        codigo,
        /constexpr\s+uint16_t\s+FABRICANTE_ID\s*=\s*0x004C\s*;/
    );

    assert.match(
        sinEspacios,
        /beacon\.setManufacturer\(FABRICANTE_ID\)/
    );
});


test("Major contiene el tipo en el byte alto y el contador en el bajo", () => {

    assert.match(
        sinEspacios,
        /uint16_tmajor=\(\(uint16_t\)ID_O3<<8\)\|numeroMedida;/
    );

    assert.equal(codificarMajorPrueba(14, 42), 3626);
    assert.equal(codificarMajorPrueba(14, 255), 3839);
    assert.equal(codificarMajorPrueba(14, 256), 3584);
});


test("Minor convierte ppm a ppb y conserva el signo de 16 bits", () => {

    assert.match(
        sinEspacios,
        /roundf\(ppm\*1000\.0f\)/
    );

    assert.match(
        sinEspacios,
        /int16_tppmCodificado=\(int16_t\)ppmPor1000;/
    );

    assert.match(
        sinEspacios,
        /uint16_tminor=\(uint16_t\)ppmCodificado;/
    );

    for (const [ppm, ppb] of [
        [0.333, 333],
        [0.750, 750],
        [-0.194, -194]
    ]) {

        const datos = codificarMinorPrueba(ppm);

        assert.equal(datos.valor, ppb);
        assert.equal(datos.reconstruido, ppb);
    }

    assert.equal(codificarMinorPrueba(-0.194).bits, 65342);
    assert.equal(codificarMinorPrueba(-0.0005).valor, -1);
});


test("El firmware limita la medida a los extremos de int16_t", () => {

    assert.match(
        sinEspacios,
        /if\(ppmPor1000>32767\)\{ppmPor1000=32767;/
    );

    assert.match(
        sinEspacios,
        /if\(ppmPor1000<-32768\)\{ppmPor1000=-32768;/
    );

    assert.equal(codificarMinorPrueba(40).valor, 32767);
    assert.equal(codificarMinorPrueba(-40).valor, -32768);
});


test("El anuncio es iBeacon no conectable y publica el nombre GTI-3A", () => {

    assert.match(
        sinEspacios,
        /Bluefruit\.setName\("GTI-3A"\)/
    );

    assert.match(
        sinEspacios,
        /Bluefruit\.Advertising\.setBeacon\(beacon\)/
    );

    assert.match(
        sinEspacios,
        /Bluefruit\.ScanResponse\.addName\(\)/
    );

    assert.match(
        sinEspacios,
        /BLE_GAP_ADV_TYPE_NONCONNECTABLE_SCANNABLE_UNDIRECTED/
    );

    assert.match(
        sinEspacios,
        /Bluefruit\.Advertising\.start\(0\)/
    );
});


test("El firmware mantiene los modos de medición real y ficticia", () => {

    assert.match(
        codigo,
        /constexpr\s+bool\s+USAR_MEDIDA_FICTICIA\s*=/
    );

    assert.match(
        codigo,
        /constexpr\s+float\s+MEDIDA_FICTICIA_PPM\s*=/
    );

    assert.match(
        sinEspacios,
        /if\(USAR_MEDIDA_FICTICIA\)/
    );

    assert.match(
        sinEspacios,
        /leerSensorO3\(ppm\)/
    );
});


test("El ciclo publica, indica con LED e incrementa el contador", () => {

    const posicion = codigo.indexOf("void loop()");

    assert.ok(
        posicion >= 0,
        "No se encuentra loop()"
    );

    const ciclo = codigo
        .slice(posicion)
        .replace(/\s+/g, "");

    assert.match(
        ciclo,
        /publicarIBeacon\(ppm,contador\);parpadearLedMedida\(\);contador\+\+;/
    );
});