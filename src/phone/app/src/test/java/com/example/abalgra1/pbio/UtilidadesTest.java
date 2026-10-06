/*
-----------------------------------------------------------------------------
Fichero: UtilidadesTest.java
Descripción: Tests automáticos de las conversiones utilizadas en el protocolo
             Arduino-Android.
Copyright: Copyright (c) 2026 Álvaro Ballester Grau
Fecha: 06/10/2026
Autor: Álvaro Ballester Grau
Aportación: Verificación de la interpretación sin signo y con signo de los
            16 bits recibidos mediante el campo Minor del iBeacon.
-----------------------------------------------------------------------------
*/

package com.example.abalgra1.pbio;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class UtilidadesTest {

    // -------------------------------------------------------------------------
    // --> comprobarMinorPositivo()
    //
    // Comprueba que un Minor positivo se interpreta correctamente tanto
    // en su representación bruta como en su representación con signo.
    // -------------------------------------------------------------------------
    @Test
    public void comprobarMinorPositivo() {

        byte[] minor = {
                (byte) 0x01,
                (byte) 0x4D
        };


        int minorBruto =
                Utilidades.bytesToUInt16(minor);


        int valor =
                Utilidades.bytesToInt16ConSigno(minor);


        assertEquals(
                333,
                minorBruto
        );


        assertEquals(
                333,
                valor
        );
    }


    // -------------------------------------------------------------------------
    // --> comprobarMinorNegativo()
    //
    // Comprueba el caso observado físicamente en el beacon: los bits que
    // aparecen como 65342 sin signo representan realmente -194 como int16_t.
    // -------------------------------------------------------------------------
    @Test
    public void comprobarMinorNegativo() {

        byte[] minor = {
                (byte) 0xFF,
                (byte) 0x3E
        };


        int minorBruto =
                Utilidades.bytesToUInt16(minor);


        int valor =
                Utilidades.bytesToInt16ConSigno(minor);


        assertEquals(
                65342,
                minorBruto
        );


        assertEquals(
                -194,
                valor
        );
    }
}