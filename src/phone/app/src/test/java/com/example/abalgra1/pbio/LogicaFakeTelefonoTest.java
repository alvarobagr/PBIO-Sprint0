/*
-----------------------------------------------------------------------------
Fichero: LogicaFakeTelefonoTest.java
Descripción: Tests automáticos de la lógica fake del teléfono.
Copyright: Copyright (c) 2026 Álvaro Ballester Grau
Fecha: 04/10/2026
Autor: Álvaro Ballester Grau
Aportación: Verificación automática de la medición ficticia utilizada durante
            Sprint 0.
-----------------------------------------------------------------------------
*/

package com.example.abalgra1.pbio;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class LogicaFakeTelefonoTest {

    // -------------------------------------------------------------------------
    // --> comprobarMedicionFake()
    //
    // Comprueba que la lógica fake genera exactamente la medición esperada.
    // -------------------------------------------------------------------------
    @Test
    public void comprobarMedicionFake() {

        Medicion medicion =
                LogicaFakeTelefono.crearMedicionFake();


        assertEquals(
                "EPSG-GTI-PROY-3A",
                medicion.getUuid()
        );

        assertEquals(
                "GTI-3A",
                medicion.getNombreDispositivo()
        );

        assertEquals(
                "O3",
                medicion.getTipo()
        );

        assertEquals(
                333,
                medicion.getValor()
        );

        assertEquals(
                30,
                medicion.getContador()
        );

        assertEquals(
                -68,
                medicion.getRssi()
        );
    }
}