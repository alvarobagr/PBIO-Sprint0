/*
-----------------------------------------------------------------------------
Fichero: LogicaFakeTelefono.java
Descripción: Lógica ficticia del cliente Android utilizada para generar una
             medición conocida durante las pruebas del Sprint 0.
Copyright: Copyright (c) 2026 Álvaro Ballester Grau
Fecha: 04/10/2026
Autor: Álvaro Ballester Grau
Aportación: Implementación de la lógica fake necesaria para probar el sistema
            sin depender de la recepción BLE real.
-----------------------------------------------------------------------------
*/

package com.example.abalgra1.pbio;


public class LogicaFakeTelefono {

    // -------------------------------------------------------------------------
    // crearMedicionFake() --> Medicion
    //
    // Genera una medición ficticia conocida y reproducible para realizar
    // pruebas del sistema sin utilizar físicamente el sensor BLE.
    // -------------------------------------------------------------------------
    public static Medicion crearMedicionFake() {

        return new Medicion(
                "EPSG-GTI-PROY-3A",
                "GTI-3A",
                "O3",
                333,
                30,
                -68
        );
    }
}