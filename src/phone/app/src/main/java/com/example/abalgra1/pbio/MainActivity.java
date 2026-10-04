/*
-----------------------------------------------------------------------------
Fichero: MainActivity.java
Descripción: Actividad principal encargada de controlar el escaneo Bluetooth
             Low Energy, interpretar las tramas iBeacon y mostrar los datos
             recibidos del sensor.
Copyright: Copyright (c) 2026 Álvaro Ballester Grau
Fecha: 04/10/2026
Autor: Álvaro Ballester Grau
Aportación: Adaptación del código proporcionado por los profesores al proyecto
            PBIO Sprint 0, compatibilidad con versiones actuales de Android y
            visualización de los datos recibidos mediante BLE.
-----------------------------------------------------------------------------
*/

package com.example.abalgra1.pbio;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String ETIQUETA_LOG = "PBIO-BTLE";

    private static final int CODIGO_PETICION_PERMISOS = 112233;

    private BluetoothLeScanner elEscanner;


    private final ScanCallback callbackDelEscaneo = new ScanCallback() {

        // ---------------------------------------------------------------------
        // resultado: ScanResult --> onScanResult()
        //
        // Procesa cada dispositivo BLE detectado durante el escaneo.
        // ---------------------------------------------------------------------
        @Override
        public void onScanResult(int callbackType, ScanResult resultado) {
            super.onScanResult(callbackType, resultado);

            mostrarInformacionDispositivoBTLE(resultado);
        }


        // ---------------------------------------------------------------------
        // resultados: [ScanResult] --> onBatchScanResults()
        //
        // Procesa un conjunto de resultados BLE recibidos simultáneamente.
        // ---------------------------------------------------------------------
        @Override
        public void onBatchScanResults(List<ScanResult> resultados) {
            super.onBatchScanResults(resultados);

            for (ScanResult resultado : resultados) {
                mostrarInformacionDispositivoBTLE(resultado);
            }
        }


        // ---------------------------------------------------------------------
        // codigo_error: Z --> onScanFailed()
        //
        // Informa mediante Logcat de un error producido durante el escaneo BLE.
        // ---------------------------------------------------------------------
        @Override
        public void onScanFailed(int codigoError) {
            super.onScanFailed(codigoError);

            Log.e(
                    ETIQUETA_LOG,
                    "Error durante el escaneo BLE. Código = " + codigoError
            );

            TextView textoEstado = findViewById(R.id.textoEstado);

            textoEstado.setText(
                    "Estado: Error de escaneo BLE (" + codigoError + ")"
            );
        }
    };


    // -------------------------------------------------------------------------
    // --> onCreate()
    //
    // Inicializa la interfaz gráfica y el sistema Bluetooth cuando se crea
    // la actividad.
    // -------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        inicializarBlueTooth();
    }


    // -------------------------------------------------------------------------
    // --> inicializarBlueTooth()
    //
    // Comprueba los permisos necesarios, obtiene el adaptador Bluetooth y
    // prepara el escáner Bluetooth Low Energy.
    // -------------------------------------------------------------------------
    private void inicializarBlueTooth() {

        Log.d(
                ETIQUETA_LOG,
                "inicializarBlueTooth()"
        );


        /*
         * Android 12 (API 31) y posteriores.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
                    ||
                    ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT
                        },
                        CODIGO_PETICION_PERMISOS
                );

                return;
            }

        } else {

            /*
             * Android 11 (API 30) y anteriores.
             */
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION
                        },
                        CODIGO_PETICION_PERMISOS
                );

                return;
            }
        }


        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);


        if (bluetoothManager == null) {

            Log.e(
                    ETIQUETA_LOG,
                    "No se ha podido obtener BluetoothManager."
            );

            return;
        }


        BluetoothAdapter bluetoothAdapter =
                bluetoothManager.getAdapter();


        if (bluetoothAdapter == null) {

            Log.e(
                    ETIQUETA_LOG,
                    "El dispositivo no dispone de Bluetooth."
            );

            Toast.makeText(
                    this,
                    "Este dispositivo no dispone de Bluetooth.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        if (!bluetoothAdapter.isEnabled()) {

            Log.d(
                    ETIQUETA_LOG,
                    "Bluetooth desactivado. Se solicita al usuario activarlo."
            );

            Intent activarBluetooth =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            startActivity(activarBluetooth);

            return;
        }


        elEscanner =
                bluetoothAdapter.getBluetoothLeScanner();


        if (elEscanner == null) {

            Log.e(
                    ETIQUETA_LOG,
                    "No se ha podido obtener BluetoothLeScanner."
            );

            return;
        }


        Log.d(
                ETIQUETA_LOG,
                "Bluetooth inicializado correctamente."
        );
    }


    // -------------------------------------------------------------------------
    // --> buscarTodosLosDispositivosBTLE()
    //
    // Inicia un escaneo BLE sin filtros para detectar todos los dispositivos
    // Bluetooth Low Energy próximos.
    // -------------------------------------------------------------------------
    private void buscarTodosLosDispositivosBTLE() {

        Log.d(
                ETIQUETA_LOG,
                "buscarTodosLosDispositivosBTLE()"
        );


        TextView textoEstado =
                findViewById(R.id.textoEstado);

        textoEstado.setText(
                "Estado: Buscando dispositivos BLE..."
        );


        if (elEscanner == null) {

            inicializarBlueTooth();

            if (elEscanner == null) {
                return;
            }
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                &&
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED) {

            return;
        }


        elEscanner.stopScan(
                callbackDelEscaneo
        );

        elEscanner.startScan(
                callbackDelEscaneo
        );


        Log.d(
                ETIQUETA_LOG,
                "Escaneo BLE iniciado."
        );
    }


    // -------------------------------------------------------------------------
    // dispositivo_buscado: Text --> buscarEsteDispositivoBTLE()
    //
    // Inicia un escaneo BLE filtrando los resultados mediante el nombre del
    // dispositivo indicado.
    // -------------------------------------------------------------------------
    private void buscarEsteDispositivoBTLE(
            String dispositivoBuscado
    ) {

        Log.d(
                ETIQUETA_LOG,
                "buscarEsteDispositivoBTLE(): " + dispositivoBuscado
        );


        TextView textoEstado =
                findViewById(R.id.textoEstado);

        textoEstado.setText(
                "Estado: Buscando " + dispositivoBuscado + "..."
        );


        if (elEscanner == null) {

            inicializarBlueTooth();

            if (elEscanner == null) {
                return;
            }
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                &&
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED) {

            return;
        }


        ScanFilter filtro =
                new ScanFilter.Builder()
                        .setDeviceName(dispositivoBuscado)
                        .build();


        List<ScanFilter> filtros =
                new ArrayList<>();

        filtros.add(
                filtro
        );


        ScanSettings settings =
                new ScanSettings.Builder()
                        .setScanMode(
                                ScanSettings.SCAN_MODE_LOW_LATENCY
                        )
                        .build();


        elEscanner.stopScan(
                callbackDelEscaneo
        );

        elEscanner.startScan(
                filtros,
                settings,
                callbackDelEscaneo
        );


        Log.d(
                ETIQUETA_LOG,
                "Escaneo filtrado iniciado para: "
                        + dispositivoBuscado
        );
    }


    // -------------------------------------------------------------------------
    // resultado: ScanResult --> mostrarInformacionDispositivoBTLE()
    //
    // Obtiene la información del dispositivo BLE detectado y, cuando la trama
    // presenta el formato esperado, utiliza TramaIBeacon y Utilidades para
    // interpretar sus campos.
    // -------------------------------------------------------------------------
    private void mostrarInformacionDispositivoBTLE(
            ScanResult resultado
    ) {

        if (resultado == null) {
            return;
        }


        BluetoothDevice dispositivo =
                resultado.getDevice();


        String nombreDispositivo =
                "(sin nombre)";


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                ||
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED) {

            if (dispositivo != null
                    &&
                    dispositivo.getName() != null) {

                nombreDispositivo =
                        dispositivo.getName();
            }
        }


        Log.d(
                ETIQUETA_LOG,
                "---------------------------------------------"
        );

        Log.d(
                ETIQUETA_LOG,
                "Dispositivo = " + nombreDispositivo
        );

        Log.d(
                ETIQUETA_LOG,
                "RSSI = " + resultado.getRssi()
        );


        ScanRecord scanRecord =
                resultado.getScanRecord();


        if (scanRecord == null) {

            Log.d(
                    ETIQUETA_LOG,
                    "El dispositivo no contiene ScanRecord."
            );

            return;
        }


        byte[] bytes =
                scanRecord.getBytes();


        if (bytes == null) {

            Log.d(
                    ETIQUETA_LOG,
                    "No se han obtenido bytes del anuncio."
            );

            return;
        }


        Log.d(
                ETIQUETA_LOG,
                "Longitud trama = " + bytes.length
        );

        Log.d(
                ETIQUETA_LOG,
                "Trama = "
                        + Utilidades.bytesToHexString(bytes)
        );


        /*
         * Trama mínima utilizada por TramaIBeacon:
         *
         * 9 bytes  -> prefijo
         * 16 bytes -> UUID
         * 2 bytes  -> major
         * 2 bytes  -> minor
         * 1 byte   -> txPower
         *
         * Total = 30 bytes
         */
        if (bytes.length < 30) {

            Log.d(
                    ETIQUETA_LOG,
                    "Trama demasiado corta para ser interpretada como iBeacon."
            );

            return;
        }


        /*
         * Prefijo esperado:
         *
         * 02 01 06 1A FF 4C 00 02 15
         */
        boolean esIBeacon =
                (bytes[0] & 0xFF) == 0x02
                        && (bytes[1] & 0xFF) == 0x01
                        && (bytes[2] & 0xFF) == 0x06
                        && (bytes[3] & 0xFF) == 0x1A
                        && (bytes[4] & 0xFF) == 0xFF
                        && (bytes[5] & 0xFF) == 0x4C
                        && (bytes[6] & 0xFF) == 0x00
                        && (bytes[7] & 0xFF) == 0x02
                        && (bytes[8] & 0xFF) == 0x15;


        if (!esIBeacon) {

            Log.d(
                    ETIQUETA_LOG,
                    "La trama no presenta el prefijo iBeacon esperado."
            );

            return;
        }


        TramaIBeacon tramaIBeacon =
                new TramaIBeacon(
                        bytes
                );


        String uuid =
                Utilidades.bytesToString(
                        tramaIBeacon.getUUID()
                );


        int major =
                Utilidades.bytesToInt(
                        tramaIBeacon.getMajor()
                );


        int minor =
                Utilidades.bytesToInt(
                        tramaIBeacon.getMinor()
                );


        /*
         * Interpretación inicial basada en el código proporcionado
         * por los profesores:
         *
         * byte alto de major -> tipo
         * byte bajo de major -> contador
         */
        int tipo =
                (major >> 8) & 0xFF;


        int contador =
                major & 0xFF;


        int valor =
                minor;


        int rssi =
                resultado.getRssi();


        int txPower =
                tramaIBeacon.getTxPower();


        Log.d(
                ETIQUETA_LOG,
                "UUID = " + uuid
        );

        Log.d(
                ETIQUETA_LOG,
                "Major = " + major
        );

        Log.d(
                ETIQUETA_LOG,
                "Minor = " + minor
        );

        Log.d(
                ETIQUETA_LOG,
                "Tipo = " + tipo
        );

        Log.d(
                ETIQUETA_LOG,
                "Contador = " + contador
        );

        Log.d(
                ETIQUETA_LOG,
                "Valor = " + valor
        );

        Log.d(
                ETIQUETA_LOG,
                "TxPower = " + txPower
        );


        actualizarDatosSensor(
                nombreDispositivo,
                uuid,
                major,
                minor,
                tipo,
                contador,
                valor,
                rssi,
                txPower
        );
    }


    // -------------------------------------------------------------------------
    // nombre_dispositivo: Text,
    // uuid: Text,
    // major: Z,
    // minor: Z,
    // tipo: N,
    // contador: N,
    // valor: Z,
    // rssi: Z,
    // tx_power: Z
    // -->
    // actualizarDatosSensor()
    //
    // Muestra en la interfaz gráfica los datos correspondientes al último
    // anuncio BLE válido recibido.
    // -------------------------------------------------------------------------
    private void actualizarDatosSensor(
            String nombreDispositivo,
            String uuid,
            int major,
            int minor,
            int tipo,
            int contador,
            int valor,
            int rssi,
            int txPower
    ) {

        runOnUiThread(() -> {

            TextView textoEstado =
                    findViewById(R.id.textoEstado);

            TextView textoDispositivo =
                    findViewById(R.id.textoDispositivo);

            TextView textoUUID =
                    findViewById(R.id.textoUUID);

            TextView textoMajor =
                    findViewById(R.id.textoMajor);

            TextView textoMinor =
                    findViewById(R.id.textoMinor);

            TextView textoTipo =
                    findViewById(R.id.textoTipo);

            TextView textoContador =
                    findViewById(R.id.textoContador);

            TextView textoValor =
                    findViewById(R.id.textoValor);

            TextView textoRSSI =
                    findViewById(R.id.textoRSSI);

            TextView textoTxPower =
                    findViewById(R.id.textoTxPower);


            textoEstado.setText(
                    "Estado: Sensor detectado"
            );

            textoDispositivo.setText(
                    "Dispositivo: " + nombreDispositivo
            );

            textoUUID.setText(
                    "UUID: " + uuid
            );

            textoMajor.setText(
                    "Major: " + major
            );

            textoMinor.setText(
                    "Minor: " + minor
            );

            textoTipo.setText(
                    "Tipo: " + tipo
            );

            textoContador.setText(
                    "Contador: " + contador
            );

            textoValor.setText(
                    "Valor: " + valor
            );

            textoRSSI.setText(
                    "RSSI: " + rssi + " dBm"
            );

            textoTxPower.setText(
                    "TxPower: " + txPower + " dBm"
            );
        });
    }


    // -------------------------------------------------------------------------
    // --> detenerBusquedaDispositivosBTLE()
    //
    // Detiene el escaneo Bluetooth Low Energy activo.
    // -------------------------------------------------------------------------
    private void detenerBusquedaDispositivosBTLE() {

        Log.d(
                ETIQUETA_LOG,
                "detenerBusquedaDispositivosBTLE()"
        );


        if (elEscanner == null) {
            return;
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                &&
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED) {

            return;
        }


        elEscanner.stopScan(
                callbackDelEscaneo
        );


        Log.d(
                ETIQUETA_LOG,
                "Escaneo BLE detenido."
        );


        TextView textoEstado =
                findViewById(R.id.textoEstado);

        textoEstado.setText(
                "Estado: Escaneo detenido"
        );
    }


    // -------------------------------------------------------------------------
    // --> botonBuscarDispositivosBTLEPulsado()
    //
    // Responde a la pulsación del botón que inicia la búsqueda de todos los
    // dispositivos BLE.
    // -------------------------------------------------------------------------
    public void botonBuscarDispositivosBTLEPulsado(
            View view
    ) {

        buscarTodosLosDispositivosBTLE();
    }


    // -------------------------------------------------------------------------
    // --> botonBuscarNuestroDispositivoBTLEPulsado()
    //
    // Responde a la pulsación del botón que busca específicamente el
    // dispositivo BLE utilizado en el proyecto.
    // -------------------------------------------------------------------------
    public void botonBuscarNuestroDispositivoBTLEPulsado(
            View view
    ) {

        buscarEsteDispositivoBTLE(
                "GTI-3A"
        );
    }


    // -------------------------------------------------------------------------
    // --> botonDetenerBusquedaDispositivosBTLEPulsado()
    //
    // Responde a la pulsación del botón que detiene el escaneo BLE.
    // -------------------------------------------------------------------------
    public void botonDetenerBusquedaDispositivosBTLEPulsado(
            View view
    ) {

        detenerBusquedaDispositivosBTLE();
    }


    // -------------------------------------------------------------------------
    // request_code: N, resultados: [Z] --> onRequestPermissionsResult()
    //
    // Procesa la respuesta del usuario a la solicitud de permisos Bluetooth
    // y continúa la inicialización cuando han sido concedidos.
    // -------------------------------------------------------------------------
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );


        if (requestCode != CODIGO_PETICION_PERMISOS) {
            return;
        }


        boolean todosConcedidos =
                grantResults.length > 0;


        for (int resultado : grantResults) {

            if (resultado != PackageManager.PERMISSION_GRANTED) {

                todosConcedidos = false;

                break;
            }
        }


        if (todosConcedidos) {

            Log.d(
                    ETIQUETA_LOG,
                    "Permisos Bluetooth concedidos."
            );

            inicializarBlueTooth();

        } else {

            Log.e(
                    ETIQUETA_LOG,
                    "No se han concedido los permisos necesarios."
            );

            Toast.makeText(
                    this,
                    "Son necesarios los permisos Bluetooth para buscar el sensor.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}