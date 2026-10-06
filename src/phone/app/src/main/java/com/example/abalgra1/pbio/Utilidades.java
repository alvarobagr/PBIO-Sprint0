/*
-----------------------------------------------------------------------------
Fichero: Utilidades.java
Descripción: Funciones auxiliares para realizar conversiones entre texto,
             UUID, bytes y valores numéricos utilizados por la aplicación.
Copyright: Copyright (c) 2026 Álvaro Ballester Grau
Fecha: 06/10/2026
Autor: Álvaro Ballester Grau
Código base: Jordi Bataller i Mascarell
Aportación: Adaptación al package del proyecto PBIO Sprint 0 y adición de
            conversiones específicas para interpretar los 16 bits del campo
            Minor del iBeacon tanto sin signo como con signo.
-----------------------------------------------------------------------------
*/

package com.example.abalgra1.pbio;


import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.UUID;


public class Utilidades {


    // -------------------------------------------------------------------------
    // texto: Text --> stringToBytes() --> [Z]
    //
    // Convierte una cadena de texto en un vector de bytes.
    // -------------------------------------------------------------------------
    public static byte[] stringToBytes(String texto) {

        return texto.getBytes();
    }


    // -------------------------------------------------------------------------
    // uuid: Text --> stringToUUID() --> UUID
    //
    // Convierte un texto de exactamente 16 caracteres en un UUID manteniendo
    // directamente sus bytes.
    // -------------------------------------------------------------------------
    public static UUID stringToUUID(String uuid) {

        if (uuid.length() != 16) {

            throw new Error(
                    "stringToUUID: string no tiene 16 caracteres"
            );
        }


        String masSignificativo =
                uuid.substring(
                        0,
                        8
                );


        String menosSignificativo =
                uuid.substring(
                        8,
                        16
                );


        return new UUID(
                Utilidades.bytesToLong(
                        masSignificativo.getBytes()
                ),
                Utilidades.bytesToLong(
                        menosSignificativo.getBytes()
                )
        );
    }


    // -------------------------------------------------------------------------
    // uuid: UUID --> uuidToString() --> Text
    //
    // Convierte un UUID en la cadena de 16 caracteres representada por sus
    // bytes.
    // -------------------------------------------------------------------------
    public static String uuidToString(UUID uuid) {

        return bytesToString(
                dosLongToBytes(
                        uuid.getMostSignificantBits(),
                        uuid.getLeastSignificantBits()
                )
        );
    }


    // -------------------------------------------------------------------------
    // uuid: UUID --> uuidToHexString() --> Text
    //
    // Devuelve en formato hexadecimal los bytes de un UUID.
    // -------------------------------------------------------------------------
    public static String uuidToHexString(UUID uuid) {

        return bytesToHexString(
                dosLongToBytes(
                        uuid.getMostSignificantBits(),
                        uuid.getLeastSignificantBits()
                )
        );
    }


    // -------------------------------------------------------------------------
    // bytes: [Z] --> bytesToString() --> Text
    //
    // Convierte un vector de bytes en texto interpretando cada byte como un
    // carácter.
    // -------------------------------------------------------------------------
    public static String bytesToString(byte[] bytes) {

        if (bytes == null) {

            return "";
        }


        StringBuilder sb =
                new StringBuilder();


        for (byte b : bytes) {

            sb.append(
                    (char) b
            );
        }


        return sb.toString();
    }


    // -------------------------------------------------------------------------
    // mas_significativos: Z,
    // menos_significativos: Z
    // --> dosLongToBytes() --> [Z]_16
    //
    // Convierte dos valores long en un vector de 16 bytes.
    // -------------------------------------------------------------------------
    public static byte[] dosLongToBytes(
            long masSignificativos,
            long menosSignificativos
    ) {

        ByteBuffer buffer =
                ByteBuffer.allocate(
                        2 * Long.BYTES
                );


        buffer.putLong(
                masSignificativos
        );


        buffer.putLong(
                menosSignificativos
        );


        return buffer.array();
    }


    // -------------------------------------------------------------------------
    // bytes: [Z] --> bytesToInt() --> Z
    //
    // Convierte el vector de bytes a entero utilizando BigInteger.
    //
    // Esta función procede del código proporcionado por los profesores y se
    // mantiene para conservar la compatibilidad con el código original.
    // -------------------------------------------------------------------------
    public static int bytesToInt(byte[] bytes) {

        return new BigInteger(
                bytes
        ).intValue();
    }


    // -------------------------------------------------------------------------
    // bytes: [Z] --> bytesToLong() --> Z
    //
    // Convierte el vector de bytes a un valor long utilizando BigInteger.
    // -------------------------------------------------------------------------
    public static long bytesToLong(byte[] bytes) {

        return new BigInteger(
                bytes
        ).longValue();
    }


    // -------------------------------------------------------------------------
    // bytes: [Z] --> bytesToIntOK() --> Z
    //
    // Conversión alternativa incluida en el código proporcionado por los
    // profesores.
    //
    // Se conserva su comportamiento original durante Sprint 0.
    // -------------------------------------------------------------------------
    public static int bytesToIntOK(byte[] bytes) {

        if (bytes == null) {

            return 0;
        }


        if (bytes.length > 4) {

            throw new Error(
                    "demasiados bytes para pasar a int"
            );
        }


        int res = 0;


        for (byte b : bytes) {

            res =
                    (res << 8)
                            +
                            (b & 0xFF);
        }


        /*
         * Se conserva esta condición tal como aparece en el código
         * proporcionado por los profesores.
         */
        if ((bytes[0] & 0x8) != 0) {

            res =
                    -(~(byte) res) - 1;
        }


        return res;
    }


    // -------------------------------------------------------------------------
    // bytes: [N]_2 --> bytesToUInt16() --> N
    //
    // Interpreta exactamente dos bytes como un entero de 16 bits sin signo.
    //
    // Se utiliza para mostrar la representación bruta de campos iBeacon como
    // Major y Minor.
    //
    // Ejemplo:
    //
    // FF 3E --> 65342
    // -------------------------------------------------------------------------
    public static int bytesToUInt16(byte[] bytes) {

        if (bytes == null || bytes.length != 2) {

            throw new IllegalArgumentException(
                    "bytesToUInt16 necesita exactamente 2 bytes"
            );
        }


        return ((bytes[0] & 0xFF) << 8)
                |
                (bytes[1] & 0xFF);
    }


    // -------------------------------------------------------------------------
    // bytes: [N]_2 --> bytesToInt16ConSigno() --> Z
    //
    // Interpreta exactamente dos bytes como un entero de 16 bits con signo.
    //
    // Se utiliza para recuperar correctamente el valor de O3 almacenado por
    // Arduino en el campo Minor del iBeacon.
    //
    // Ejemplo:
    //
    // FF 3E --> -194
    // -------------------------------------------------------------------------
    public static int bytesToInt16ConSigno(byte[] bytes) {

        int valorSinSigno =
                bytesToUInt16(
                        bytes
                );


        return (short) valorSinSigno;
    }


    // -------------------------------------------------------------------------
    // bytes: [Z] --> bytesToHexString() --> Text
    //
    // Convierte un vector de bytes a representación hexadecimal.
    // -------------------------------------------------------------------------
    public static String bytesToHexString(byte[] bytes) {

        if (bytes == null) {

            return "";
        }


        StringBuilder sb =
                new StringBuilder();


        for (byte b : bytes) {

            sb.append(
                    String.format(
                            "%02x",
                            b
                    )
            );

            sb.append(':');
        }


        return sb.toString();
    }
}