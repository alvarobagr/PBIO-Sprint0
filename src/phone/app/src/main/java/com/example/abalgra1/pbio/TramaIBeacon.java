/*
-----------------------------------------------------------------------------
Fichero: TramaIBeacon.java
Descripcion: Divide una trama iBeacon en prefijo, UUID, Major, Minor,
             potencia de transmision y campos de publicidad BLE.
Codigo base: Jordi Bataller i Mascarell (autor original).
Adaptacion documental: Alvaro Ballester Grau.
Fecha de adaptacion documental: 14/10/2026.
Aportacion: Cabeceras en notacion de diseno logico exigidas por el agente v2.
            No se modifica ninguna instruccion Java del codigo original.
-----------------------------------------------------------------------------
*/

package com.example.abalgra1.pbio;

import java.util.Arrays;

// -----------------------------------------------------------------------------------
// @author: Jordi Bataller i Mascarell
// -----------------------------------------------------------------------------------
public class TramaIBeacon {
    private byte[] prefijo = null; // 9 bytes
    private byte[] uuid = null; // 16 bytes
    private byte[] major = null; // 2 bytes
    private byte[] minor = null; // 2 bytes
    private byte txPower = 0; // 1 byte

    private byte[] losBytes;

    private byte[] advFlags = null; // 3 bytes
    private byte[] advHeader = null; // 2 bytes
    private byte[] companyID = new byte[2]; // 2 bytes
    private byte iBeaconType = 0; // 1 byte
    private byte iBeaconLength = 0; // 1 byte

    // -------------------------------------------------------------------------------
    // getPrefijo() --> prefijo: [N]_9
    //
    // Devuelve los nueve octetos iniciales de la trama iBeacon.
    // -------------------------------------------------------------------------------
    public byte[] getPrefijo() {
        return prefijo;
    }

    // -------------------------------------------------------------------------------
    // getUUID() --> uuid: [N]_16
    //
    // Devuelve los dieciseis octetos identificadores del beacon.
    // -------------------------------------------------------------------------------
    public byte[] getUUID() {
        return uuid;
    }

    // -------------------------------------------------------------------------------
    // getMajor() --> major: [N]_2
    //
    // Devuelve los dos octetos que codifican tipo y contador.
    // -------------------------------------------------------------------------------
    public byte[] getMajor() {
        return major;
    }

    // -------------------------------------------------------------------------------
    // getMinor() --> minor: [N]_2
    //
    // Devuelve los dos octetos del valor medido.
    // -------------------------------------------------------------------------------
    public byte[] getMinor() {
        return minor;
    }

    // -------------------------------------------------------------------------------
    // getTxPower() --> tx_power: Z
    //
    // Devuelve la potencia de transmision calibrada.
    // -------------------------------------------------------------------------------
    public byte getTxPower() {
        return txPower;
    }

    // -------------------------------------------------------------------------------
    // getLosBytes() --> bytes: [N]
    //
    // Devuelve los octetos originales del anuncio recibido.
    // -------------------------------------------------------------------------------
    public byte[] getLosBytes() {
        return losBytes;
    }

    // -------------------------------------------------------------------------------
    // getAdvFlags() --> adv_flags: [N]_3
    //
    // Devuelve los tres octetos del campo de indicadores BLE.
    // -------------------------------------------------------------------------------
    public byte[] getAdvFlags() {
        return advFlags;
    }

    // -------------------------------------------------------------------------------
    // getAdvHeader() --> adv_header: [N]_2
    //
    // Devuelve los dos octetos de la cabecera del anuncio.
    // -------------------------------------------------------------------------------
    public byte[] getAdvHeader() {
        return advHeader;
    }

    // -------------------------------------------------------------------------------
    // getCompanyID() --> company_id: [N]_2
    //
    // Devuelve el identificador de fabricante de dos octetos.
    // -------------------------------------------------------------------------------
    public byte[] getCompanyID() {
        return companyID;
    }

    // -------------------------------------------------------------------------------
    // getiBeaconType() --> tipo_ibeacon: N
    //
    // Devuelve el octeto que identifica el tipo iBeacon.
    // -------------------------------------------------------------------------------
    public byte getiBeaconType() {
        return iBeaconType;
    }

    // -------------------------------------------------------------------------------
    // getiBeaconLength() --> longitud_ibeacon: N
    //
    // Devuelve el octeto de longitud del bloque iBeacon.
    // -------------------------------------------------------------------------------
    public byte getiBeaconLength() {
        return iBeaconLength;
    }

    // -------------------------------------------------------------------------------
    // bytes: [N] --> TramaIBeacon()
    //
    // Descompone la trama BLE en los campos iBeacon conocidos.
    // Precondicion: la trama contiene al menos 30 octetos y comienza
    // con el prefijo esperado; la validacion previa corresponde al llamador.
    // -------------------------------------------------------------------------------
    public TramaIBeacon(byte[] bytes) {
        this.losBytes = bytes;

        prefijo = Arrays.copyOfRange(losBytes, 0, 8 + 1); // 9 bytes
        uuid = Arrays.copyOfRange(losBytes, 9, 24 + 1); // 16 bytes
        major = Arrays.copyOfRange(losBytes, 25, 26 + 1); // 2 bytes
        minor = Arrays.copyOfRange(losBytes, 27, 28 + 1); // 2 bytes
        txPower = losBytes[29]; // 1 byte

        advFlags = Arrays.copyOfRange(prefijo, 0, 2 + 1); // 3 bytes
        advHeader = Arrays.copyOfRange(prefijo, 3, 4 + 1); // 2 bytes
        companyID = Arrays.copyOfRange(prefijo, 5, 6 + 1); // 2 bytes
        iBeaconType = prefijo[7]; // 1 byte
        iBeaconLength = prefijo[8]; // 1 byte

    } // ()
} // class
// -----------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------