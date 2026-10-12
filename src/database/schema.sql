/*
-----------------------------------------------------------------------------
Fichero: schema.sql
Descripción: Creación de la tabla utilizada para almacenar las mediciones
             recibidas por el sistema PBIO.
Copyright: Copyright (c) 2026 Álvaro Ballester Grau
Fecha: 07/10/2026
Autor: Álvaro Ballester Grau
Aportación: Diseño e implementación de la base de datos utilizada en Sprint 0
            para almacenar mediciones de O3 recibidas desde el backend.
-----------------------------------------------------------------------------
*/


-- ---------------------------------------------------------------------------
-- Eliminación de la tabla anterior
--
-- Permite volver a ejecutar este script durante el desarrollo y las pruebas.
-- ---------------------------------------------------------------------------

DROP TABLE IF EXISTS medicion;


-- ---------------------------------------------------------------------------
-- Creación de la tabla MEDICION
--
-- Una fila representa una medición ya interpretada por Android.
--
-- Android/backend proporciona:
--
-- uuid
-- nombre_dispositivo
-- tipo
-- valor
-- contador
-- rssi
--
-- La base de datos genera:
--
-- id_medicion
-- fecha_hora
-- ---------------------------------------------------------------------------

CREATE TABLE medicion (

    id_medicion BIGINT UNSIGNED
        NOT NULL
        AUTO_INCREMENT,

    uuid VARCHAR(36)
        NOT NULL,

    nombre_dispositivo VARCHAR(100)
        NOT NULL,

    tipo VARCHAR(30)
        NOT NULL,

    valor INT
        NOT NULL,

    contador TINYINT UNSIGNED
        NOT NULL,

    rssi SMALLINT
        NOT NULL,

    fecha_hora DATETIME
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_medicion)

);