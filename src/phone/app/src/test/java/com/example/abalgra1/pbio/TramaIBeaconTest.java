package com.example.abalgra1.pbio;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TramaIBeaconTest {

    @Test
    public void comprobarTramaIBeacon() {

        byte[] trama = new byte[] {

                // Prefijo: 9 bytes
                0x02, 0x01, 0x06,
                0x1A, (byte) 0xFF,
                0x4C, 0x00,
                0x02,
                0x15,

                // UUID: "EPSG-GTI-PROY-3A" = 16 bytes
                'E', 'P', 'S', 'G',
                '-', 'G', 'T', 'I',
                '-', 'P', 'R', 'O',
                'Y', '-', '3', 'A',

                // Major = tipo 11 (CO2) + contador 27
                0x0B, 0x1B,

                // Minor = valor 123
                0x00, 0x7B,

                // TxPower = -53
                (byte) -53
        };

        TramaIBeacon beacon = new TramaIBeacon(trama);

        // Convertimos major y minor a enteros
        int major = Utilidades.bytesToInt(beacon.getMajor());
        int minor = Utilidades.bytesToInt(beacon.getMinor());

// El byte alto de major contiene el tipo de medición
        int tipo = (major >> 8) & 0xFF;

// El byte bajo de major contiene el contador
        int contador = major & 0xFF;

// Minor contiene el valor de la medición
        int valor = minor;

        assertEquals(
                "EPSG-GTI-PROY-3A",
                Utilidades.bytesToString(beacon.getUUID())
        );

        assertArrayEquals(
                new byte[]{0x0B, 0x1B},
                beacon.getMajor()
        );

        assertArrayEquals(
                new byte[]{0x00, 0x7B},
                beacon.getMinor()
        );

        assertEquals(
                -53,
                beacon.getTxPower()
        );
        assertEquals(11, tipo);
        assertEquals(27, contador);
        assertEquals(123, valor);
    }
}