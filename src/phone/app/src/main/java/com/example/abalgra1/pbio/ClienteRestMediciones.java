/*
-----------------------------------------------------------------------------
Fichero: ClienteRestMediciones.java
Descripcion: Cliente HTTPS para enviar una Medicion al servidor REST de PBIO.
Copyright: Copyright (c) 2026 Álvaro Ballester Grau
Fecha: 12/10/2026
Autor: Álvaro Ballester Grau
Aportacion: Envio manual de mediciones mediante POST y JSON sin bloquear la UI
            y sin guardar el token de autorizacion en el codigo fuente.
-----------------------------------------------------------------------------
*/

package com.example.abalgra1.pbio;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class ClienteRestMediciones {

    private static final String URL_MEDICIONES =
            "https://abalgra1.upv.edu.es/api/mediciones";

    private final ExecutorService ejecutor =
            Executors.newSingleThreadExecutor();

    public interface CallbackEnvio {
        void alTerminar(int codigoHTTP, String respuesta, String error);
    }

    // -------------------------------------------------------------------------
    // medicion: Medicion --> generarJSON() --> Text
    //
    // Crea los seis campos JSON que espera el backend PHP.
    // -------------------------------------------------------------------------
    public static String generarJSON(Medicion medicion) throws Exception {
        JSONObject json = new JSONObject();
        json.put("uuid", medicion.getUuid());
        json.put("nombre_dispositivo", medicion.getNombreDispositivo());
        json.put("tipo", medicion.getTipo());
        json.put("valor", medicion.getValor());
        json.put("contador", medicion.getContador());
        json.put("rssi", medicion.getRssi());
        return json.toString();
    }

    // -------------------------------------------------------------------------
    // medicion: Medicion, token: Text, callback: CallbackEnvio
    //     --> enviarMedicion()
    //
    // Realiza POST HTTPS en un hilo de trabajo y notifica su resultado.
    // El callback se ejecuta en ese hilo (MainActivity lo pasa a la UI).
    // -------------------------------------------------------------------------
    public void enviarMedicion(
            Medicion medicion,
            String token,
            CallbackEnvio callback
    ) {
        ejecutor.execute(() -> {
            int codigo = -1;
            String respuesta = "";
            String error = null;
            HttpsURLConnection conexion = null;

            try {
                byte[] cuerpo = generarJSON(medicion)
                        .getBytes(StandardCharsets.UTF_8);

                URL url = new URL(URL_MEDICIONES);
                conexion = (HttpsURLConnection) url.openConnection();
                conexion.setRequestMethod("POST");
                conexion.setConnectTimeout(10000);
                conexion.setReadTimeout(10000);
                conexion.setInstanceFollowRedirects(false);
                conexion.setDoOutput(true);
                conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conexion.setRequestProperty("Accept", "application/json");
                conexion.setRequestProperty("X-PBIO-TOKEN", token);
                conexion.setFixedLengthStreamingMode(cuerpo.length);

                try (OutputStream salida = conexion.getOutputStream()) {
                    salida.write(cuerpo);
                }

                codigo = conexion.getResponseCode();
                InputStream entrada = codigo >= 200 && codigo < 300
                        ? conexion.getInputStream()
                        : conexion.getErrorStream();
                respuesta = leerRespuesta(entrada);

                if (codigo != 201) {
                    error = "Respuesta no satisfactoria del servidor";
                }
            } catch (Exception excepcion) {
                error = excepcion.getClass().getSimpleName() + ": "
                        + excepcion.getMessage();
            } finally {
                if (conexion != null) {
                    conexion.disconnect();
                }
            }

            callback.alTerminar(codigo, respuesta, error);
        });
    }

    // -------------------------------------------------------------------------
    // entrada: InputStream --> leerRespuesta() --> Text
    //
    // Lee hasta 2048 caracteres de la respuesta HTTP para diagnóstico.
    // -------------------------------------------------------------------------
    private static String leerRespuesta(InputStream entrada) throws IOException {
        if (entrada == null) {
            return "";
        }

        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
            StringBuilder resultado = new StringBuilder();
            char[] buffer = new char[512];
            int cantidad;

            while (resultado.length() < 2048 &&
                    (cantidad = lector.read(buffer, 0,
                            Math.min(buffer.length, 2048 - resultado.length()))) != -1) {
                resultado.append(buffer, 0, cantidad);
            }
            return resultado.toString();
        }
    }

    // -------------------------------------------------------------------------
    // --> cerrar()
    //
    // Libera el ejecutor de red cuando la actividad se destruye.
    // -------------------------------------------------------------------------
    public void cerrar() {
        ejecutor.shutdownNow();
    }
}