/*
-----------------------------------------------------------------------------
Fichero: test_database.sql
Descripción: Pruebas reproducibles de la tabla medicion utilizada por PBIO.
             Comprueba inserción, valores positivos y negativos, límites del
             contador, fecha automática y consulta de la última medición.
Copyright: Copyright (c) 2026 Álvaro Ballester Grau
Fecha: 10/10/2026
Autor: Álvaro Ballester Grau
Aportación: Pruebas automáticas/reproducibles de la base de datos de Sprint 0.
-----------------------------------------------------------------------------
*/


-- ===========================================================================
-- INICIO DE LAS PRUEBAS
-- ===========================================================================

START TRANSACTION;


/* ---------------------------------------------------------------------------
   TEST 1
   Inserción de una medición positiva conocida.
   --------------------------------------------------------------------------- */

INSERT INTO medicion (
    uuid,
    nombre_dispositivo,
    tipo,
    valor,
    contador,
    rssi
)
VALUES (
    'EPSG-GTI-PROY-3A',
    'GTI-3A',
    'O3',
    333,
    30,
    -68
);


SELECT
    CASE
        WHEN EXISTS (
            SELECT 1
            FROM medicion
            WHERE uuid = 'EPSG-GTI-PROY-3A'
              AND nombre_dispositivo = 'GTI-3A'
              AND tipo = 'O3'
              AND valor = 333
              AND contador = 30
              AND rssi = -68
        )
        THEN 'OK - TEST 1: medicion positiva almacenada correctamente'
        ELSE 'ERROR - TEST 1'
    END AS resultado;


/* ---------------------------------------------------------------------------
   TEST 2
   Inserción de un valor negativo.

   Es importante porque el valor procedente de Minor se interpreta como int16
   con signo y pueden existir valores negativos.
   --------------------------------------------------------------------------- */

INSERT INTO medicion (
    uuid,
    nombre_dispositivo,
    tipo,
    valor,
    contador,
    rssi
)
VALUES (
    'EPSG-GTI-PROY-3A',
    'GTI-3A',
    'O3',
    -194,
    63,
    -54
);


SELECT
    CASE
        WHEN EXISTS (
            SELECT 1
            FROM medicion
            WHERE uuid = 'EPSG-GTI-PROY-3A'
              AND tipo = 'O3'
              AND valor = -194
              AND contador = 63
              AND rssi = -54
        )
        THEN 'OK - TEST 2: valor negativo almacenado correctamente'
        ELSE 'ERROR - TEST 2'
    END AS resultado;


/* ---------------------------------------------------------------------------
   TEST 3
   Comprobación del límite inferior del contador.
   --------------------------------------------------------------------------- */

INSERT INTO medicion (
    uuid,
    nombre_dispositivo,
    tipo,
    valor,
    contador,
    rssi
)
VALUES (
    'EPSG-GTI-PROY-3A',
    'GTI-3A',
    'O3',
    100,
    0,
    -60
);


SELECT
    CASE
        WHEN EXISTS (
            SELECT 1
            FROM medicion
            WHERE valor = 100
              AND contador = 0
        )
        THEN 'OK - TEST 3: contador 0 almacenado correctamente'
        ELSE 'ERROR - TEST 3'
    END AS resultado;


/* ---------------------------------------------------------------------------
   TEST 4
   Comprobación del límite superior del contador.
   --------------------------------------------------------------------------- */

INSERT INTO medicion (
    uuid,
    nombre_dispositivo,
    tipo,
    valor,
    contador,
    rssi
)
VALUES (
    'EPSG-GTI-PROY-3A',
    'GTI-3A',
    'O3',
    200,
    255,
    -61
);


SELECT
    CASE
        WHEN EXISTS (
            SELECT 1
            FROM medicion
            WHERE valor = 200
              AND contador = 255
        )
        THEN 'OK - TEST 4: contador 255 almacenado correctamente'
        ELSE 'ERROR - TEST 4'
    END AS resultado;


/* ---------------------------------------------------------------------------
   TEST 5
   Comprobación de generación automática de id_medicion y fecha_hora.
   --------------------------------------------------------------------------- */

SELECT
    CASE
        WHEN id_medicion IS NOT NULL
             AND id_medicion > 0
             AND fecha_hora IS NOT NULL
        THEN 'OK - TEST 5: id y fecha generados automaticamente'
        ELSE 'ERROR - TEST 5'
    END AS resultado
FROM medicion
WHERE uuid = 'EPSG-GTI-PROY-3A'
ORDER BY id_medicion DESC
LIMIT 1;


/* ---------------------------------------------------------------------------
   TEST 6
   Obtención de la última medición.

   La aplicación web necesitará una operación equivalente.
   --------------------------------------------------------------------------- */

SELECT
    id_medicion,
    uuid,
    nombre_dispositivo,
    tipo,
    valor,
    contador,
    rssi,
    fecha_hora
FROM medicion
WHERE tipo = 'O3'
ORDER BY id_medicion DESC
LIMIT 1;


/* ---------------------------------------------------------------------------
   TEST 7
   Comprobación de que todos los registros de prueba contienen los campos
   obligatorios.
   --------------------------------------------------------------------------- */

SELECT
    CASE
        WHEN COUNT(*) = 0
        THEN 'OK - TEST 7: no existen campos obligatorios nulos'
        ELSE 'ERROR - TEST 7: existen campos nulos'
    END AS resultado
FROM medicion
WHERE uuid IS NULL
   OR nombre_dispositivo IS NULL
   OR tipo IS NULL
   OR valor IS NULL
   OR contador IS NULL
   OR rssi IS NULL
   OR fecha_hora IS NULL;


/* ---------------------------------------------------------------------------
   RESUMEN VISUAL DE LOS DATOS INTRODUCIDOS DURANTE LA PRUEBA
   --------------------------------------------------------------------------- */

SELECT
    id_medicion,
    uuid,
    nombre_dispositivo,
    tipo,
    valor,
    contador,
    rssi,
    fecha_hora
FROM medicion
WHERE uuid = 'EPSG-GTI-PROY-3A'
ORDER BY id_medicion ASC;


/* ---------------------------------------------------------------------------
   FIN DE LA PRUEBA

   Se utiliza ROLLBACK para que los registros introducidos durante los tests no
   permanezcan almacenados en la base de datos.
   --------------------------------------------------------------------------- */

ROLLBACK;