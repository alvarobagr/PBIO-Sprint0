/*
-----------------------------------------------------------------------------
Fichero: Medicion.java
Descripción: Representa una medición interpretada por la aplicación Android.
Copyright: Copyright (c) 2026 Álvaro Ballester Grau
Fecha: 04/10/2026
Autor: Álvaro Ballester Grau
Aportación: Modelo de datos utilizado para unificar las mediciones reales y
            ficticias del proyecto PBIO Sprint 0.
-----------------------------------------------------------------------------
*/

package com.example.abalgra1.pbio;


public class Medicion {

    private final String uuid;

    private final String nombreDispositivo;

    private final String tipo;

    private final int valor;

    private final int contador;

    private final int rssi;


    // -------------------------------------------------------------------------
    // uuid: Text,
    // nombre_dispositivo: Text,
    // tipo: Text,
    // valor: Z,
    // contador: N,
    // rssi: Z
    // -->
    // Medicion()
    //
    // Construye una medición con los datos ya interpretados por el teléfono.
    // -------------------------------------------------------------------------
    public Medicion(
            String uuid,
            String nombreDispositivo,
            String tipo,
            int valor,
            int contador,
            int rssi
    ) {

        this.uuid = uuid;
        this.nombreDispositivo = nombreDispositivo;
        this.tipo = tipo;
        this.valor = valor;
        this.contador = contador;
        this.rssi = rssi;
    }


    // -------------------------------------------------------------------------
    // getUuid() --> Text
    //
    // Devuelve el UUID del dispositivo asociado a la medición.
    // -------------------------------------------------------------------------
    public String getUuid() {
        return uuid;
    }


    // -------------------------------------------------------------------------
    // getNombreDispositivo() --> Text
    //
    // Devuelve el nombre del dispositivo.
    // -------------------------------------------------------------------------
    public String getNombreDispositivo() {
        return nombreDispositivo;
    }


    // -------------------------------------------------------------------------
    // getTipo() --> Text
    //
    // Devuelve el tipo de medición.
    // -------------------------------------------------------------------------
    public String getTipo() {
        return tipo;
    }


    // -------------------------------------------------------------------------
    // getValor() --> Z
    //
    // Devuelve el valor de la medición.
    // -------------------------------------------------------------------------
    public int getValor() {
        return valor;
    }


    // -------------------------------------------------------------------------
    // getContador() --> N
    //
    // Devuelve el contador asociado a la medición.
    // -------------------------------------------------------------------------
    public int getContador() {
        return contador;
    }


    // -------------------------------------------------------------------------
    // getRssi() --> Z
    //
    // Devuelve el RSSI recibido por el teléfono.
    // -------------------------------------------------------------------------
    public int getRssi() {
        return rssi;
    }
}