/*
-----------------------------------------------------------------------------
Fichero: PBIO_O3_iBeacon.ino
Descripción: Lectura del sensor ULPSM-O3 968-046 y publicación de la medida
             mediante un anuncio iBeacon. Permite utilizar una medida real o
             una medida ficticia para las pruebas del Sprint 0 e indica cada
             medición publicada mediante un destello del LED integrado.
Copyright: Copyright (c) 2026 Álvaro Ballester Grau
Fecha: 06/10/2026
Autor: Álvaro Ballester Grau
Aportación: Integración del sensor O3 con iBeacon, incorporación de un modo
            de medida ficticia reproducible, indicador LED de medida y
            adaptación al proyecto PBIO.
-----------------------------------------------------------------------------
*/

#include <bluefruit.h>
#include <math.h>


// -----------------------------------------------------------------------------
// CONFIGURACION GENERAL
// -----------------------------------------------------------------------------

constexpr uint32_t BAUD_USB = 115200;


// -----------------------------------------------------------------------------
// MODO DE MEDIDA
//
// true  -> utiliza la medida ficticia.
// false -> utiliza la lectura real del sensor.
//
// Para la prueba del profesor basta con cambiar MEDIDA_FICTICIA_PPM.
//
// Ejemplos:
//
// 0.333 ppm -> se envía 333 en Minor.
// 0.750 ppm -> se envía 750 en Minor.
// -----------------------------------------------------------------------------

constexpr bool USAR_MEDIDA_FICTICIA = false;

constexpr float MEDIDA_FICTICIA_PPM = 0.333f;


// -----------------------------------------------------------------------------
// PINES ANALOGICOS
//
// SparkFun Pro nRF52840 Mini:
//
// AIN3 = P0.05 = Arduino A2
// AIN4 = P0.28 = Arduino A1
// AIN5 = P0.29 = Arduino A0
//
// SENSOR:
//
// VGAS  -> AIN3
// VREF  -> AIN4
// VTEMP -> AIN5
// -----------------------------------------------------------------------------

constexpr int PIN_VGAS = 5;

constexpr int PIN_VREF = 28;

constexpr int PIN_VTEMP = 29;


// -----------------------------------------------------------------------------
// ADC
//
// Resolución: 12 bits.
// Valores: 0 ... 4095.
// Referencia configurada: 0 ... 3.0 V.
// -----------------------------------------------------------------------------

constexpr float ADC_REFERENCIA = 3.0f;

constexpr float ADC_MAX = 4095.0f;


// -----------------------------------------------------------------------------
// SENSIBILIDAD INDIVIDUAL DEL SENSOR O3
//
// Valor obtenido para este sensor:
//
// -67.98 mV/ppm
//
// En voltios:
//
// -0.06798 V/ppm
// -----------------------------------------------------------------------------

constexpr float SENSIBILIDAD_O3 = -0.06798f;


// -----------------------------------------------------------------------------
// CONFIGURACION iBEACON
// -----------------------------------------------------------------------------

constexpr uint16_t FABRICANTE_ID = 0x004C;

constexpr int8_t TX_POWER_DBM = 4;

constexpr int8_t RSSI_A_1M = -53;


// -----------------------------------------------------------------------------
// IDENTIFICADOR DEL TIPO DE MEDIDA
//
// CO2         = 11
// TEMPERATURA = 12
// RUIDO       = 13
// O3          = 14
// -----------------------------------------------------------------------------

constexpr uint8_t ID_O3 = 14;


// -----------------------------------------------------------------------------
// INTERVALO ENTRE MEDIDAS
// -----------------------------------------------------------------------------

constexpr uint32_t INTERVALO_MEDIDA_MS = 2000;


// -----------------------------------------------------------------------------
// INDICADOR LED DE MEDIDA
//
// El LED integrado realiza un destello corto cada vez que una medida se ha
// publicado correctamente mediante iBeacon.
// -----------------------------------------------------------------------------

constexpr uint32_t DURACION_LED_MEDIDA_MS = 100;


// -----------------------------------------------------------------------------
// UUID
//
// EPSG-GTI-PROY-3A
// -----------------------------------------------------------------------------

uint8_t beaconUUID[16] = {

  'E', 'P', 'S', 'G',
  '-', 'G', 'T', 'I',
  '-', 'P', 'R', 'O',
  'Y', '-', '3', 'A'

};


// -----------------------------------------------------------------------------
// CONTADOR DE MEDIDAS
//
// uint8_t -> valores 0 ... 255.
// Al superar 255 vuelve automáticamente a 0.
// -----------------------------------------------------------------------------

uint8_t contador = 0;


// -----------------------------------------------------------------------------
// pin: Z --> leerADCpromedio() --> adc: N
//
// Realiza 20 lecturas ADC del pin indicado y devuelve su valor medio para
// reducir pequeñas variaciones y ruido.
// -----------------------------------------------------------------------------

uint16_t leerADCpromedio(int pin)
{
  const int NUM_LECTURAS = 20;

  uint32_t suma = 0;


  for (int i = 0; i < NUM_LECTURAS; i++)
  {
    suma += analogRead(pin);

    delay(2);
  }


  return suma / NUM_LECTURAS;
}


// -----------------------------------------------------------------------------
// adc: N --> adcAVoltios() --> voltios: R
//
// Convierte una lectura ADC de 12 bits a voltios utilizando una referencia
// de 3.0 V.
// -----------------------------------------------------------------------------

float adcAVoltios(uint16_t adc)
{
  return ((float)adc * ADC_REFERENCIA) / ADC_MAX;
}


// -----------------------------------------------------------------------------
// leerSensorO3() --> ppm: R, exito: B
//
// Lee VGAS, VREF y VTEMP del sensor, convierte las lecturas a voltios y
// calcula la concentración de O3 en ppm.
//
// En la implementación actual:
//
// Vgas0 = Vref
//
// Se mantiene esta aproximación del código actual. La calibración física del
// sensor podrá revisarse posteriormente sin afectar al protocolo iBeacon.
// -----------------------------------------------------------------------------

bool leerSensorO3(float &ppm)
{
  // ---------------------------------------------------------------------------
  // LEER ADC
  // ---------------------------------------------------------------------------

  uint16_t adcVgas =
    leerADCpromedio(PIN_VGAS);

  uint16_t adcVref =
    leerADCpromedio(PIN_VREF);

  uint16_t adcVtemp =
    leerADCpromedio(PIN_VTEMP);


  // ---------------------------------------------------------------------------
  // CONVERTIR ADC A VOLTIOS
  // ---------------------------------------------------------------------------

  float vgas =
    adcAVoltios(adcVgas);

  float vref =
    adcAVoltios(adcVref);

  float vtemp =
    adcAVoltios(adcVtemp);


  // ---------------------------------------------------------------------------
  // REFERENCIA DE CERO
  //
  // Primera aproximación utilizada actualmente:
  //
  // Vgas0 = Vref
  // ---------------------------------------------------------------------------

  float vgas0 = vref;


  // ---------------------------------------------------------------------------
  // CALCULAR DELTAV
  // ---------------------------------------------------------------------------

  float deltaV =
    vgas - vgas0;


  // ---------------------------------------------------------------------------
  // CALCULAR O3
  //
  // ppm = DeltaV / sensibilidad
  // ---------------------------------------------------------------------------

  ppm =
    deltaV / SENSIBILIDAD_O3;


  // ---------------------------------------------------------------------------
  // MOSTRAR LECTURAS
  // ---------------------------------------------------------------------------

  Serial.println();

  Serial.println("--------------------------------");

  Serial.println("LECTURA SENSOR O3");

  Serial.println("--------------------------------");


  Serial.print("ADC Vgas  = ");
  Serial.println(adcVgas);

  Serial.print("ADC Vref  = ");
  Serial.println(adcVref);

  Serial.print("ADC Vtemp = ");
  Serial.println(adcVtemp);


  Serial.println();


  Serial.print("Vgas  = ");
  Serial.print(vgas, 5);
  Serial.println(" V");


  Serial.print("Vref  = ");
  Serial.print(vref, 5);
  Serial.println(" V");


  Serial.print("Vtemp = ");
  Serial.print(vtemp, 5);
  Serial.println(" V");


  Serial.println();


  Serial.print("Vgas0 = ");
  Serial.print(vgas0, 5);
  Serial.println(" V");


  Serial.print("DeltaV = ");
  Serial.print(deltaV * 1000.0f, 3);
  Serial.println(" mV");


  Serial.print("Sensibilidad = ");

  Serial.print(
    SENSIBILIDAD_O3 * 1000.0f,
    2
  );

  Serial.println(" mV/ppm");


  Serial.print("O3 calculado = ");
  Serial.print(ppm, 4);
  Serial.println(" ppm");


  Serial.println("--------------------------------");


  return true;
}


// -----------------------------------------------------------------------------
// ppm: R, numero_medida: N --> publicarIBeacon()
//
// Convierte la concentración de O3 a un entero con signo en ppb y la publica
// mediante iBeacon.
//
// MAJOR:
//
// byte alto -> ID del tipo de medida.
// byte bajo -> contador.
//
// MINOR:
//
// ppm * 1000.
//
// Ejemplos:
//
//  0.333 ppm -> 333
// -0.215 ppm -> -215
//
// El valor se codifica como int16_t para conservar el signo.
// -----------------------------------------------------------------------------

void publicarIBeacon(
  float ppm,
  uint8_t numeroMedida
)
{
  // ---------------------------------------------------------------------------
  // PPM -> ENTERO
  // ---------------------------------------------------------------------------

  int32_t ppmPor1000 =
    (int32_t)roundf(
      ppm * 1000.0f
    );


  // ---------------------------------------------------------------------------
  // LIMITAR AL RANGO DE int16_t
  // ---------------------------------------------------------------------------

  if (ppmPor1000 > 32767)
  {
    ppmPor1000 = 32767;
  }


  if (ppmPor1000 < -32768)
  {
    ppmPor1000 = -32768;
  }


  int16_t ppmCodificado =
    (int16_t)ppmPor1000;


  // ---------------------------------------------------------------------------
  // MAJOR
  //
  // byte alto -> ID O3 = 14
  // byte bajo -> contador
  // ---------------------------------------------------------------------------

  uint16_t major =
    ((uint16_t)ID_O3 << 8)
    |
    numeroMedida;


  // ---------------------------------------------------------------------------
  // MINOR
  //
  // Los bits del int16_t se almacenan dentro del uint16_t utilizado por
  // iBeacon.
  // ---------------------------------------------------------------------------

  uint16_t minor =
    (uint16_t)ppmCodificado;


  // ---------------------------------------------------------------------------
  // DETENER PUBLICIDAD ANTERIOR
  // ---------------------------------------------------------------------------

  if (Bluefruit.Advertising.isRunning())
  {
    Bluefruit.Advertising.stop();
  }


  // ---------------------------------------------------------------------------
  // LIMPIAR DATOS
  // ---------------------------------------------------------------------------

  Bluefruit.Advertising.clearData();

  Bluefruit.ScanResponse.clearData();


  // ---------------------------------------------------------------------------
  // CREAR iBEACON
  // ---------------------------------------------------------------------------

  BLEBeacon beacon(

    beaconUUID,

    major,

    minor,

    RSSI_A_1M

  );


  beacon.setManufacturer(
    FABRICANTE_ID
  );


  Bluefruit.Advertising.setBeacon(
    beacon
  );


  // ---------------------------------------------------------------------------
  // NOMBRE DEL DISPOSITIVO
  // ---------------------------------------------------------------------------

  Bluefruit.ScanResponse.addName();


  // ---------------------------------------------------------------------------
  // iBEACON NO CONECTABLE
  // ---------------------------------------------------------------------------

  Bluefruit.Advertising.setType(

    BLE_GAP_ADV_TYPE_NONCONNECTABLE_SCANNABLE_UNDIRECTED

  );


  // ---------------------------------------------------------------------------
  // INTERVALO DE PUBLICIDAD
  //
  // 160 unidades BLE = 100 ms.
  // ---------------------------------------------------------------------------

  Bluefruit.Advertising.setInterval(
    160,
    160
  );


  // ---------------------------------------------------------------------------
  // INICIAR PUBLICIDAD
  // ---------------------------------------------------------------------------

  Bluefruit.Advertising.start(0);


  // ---------------------------------------------------------------------------
  // MOSTRAR DATOS DEL iBEACON
  // ---------------------------------------------------------------------------

  Serial.println();

  Serial.println("==============================");

  Serial.println("         iBeacon O3");

  Serial.println("==============================");


  Serial.print("ID sensor = ");
  Serial.println(ID_O3);


  Serial.print("Contador = ");
  Serial.println(numeroMedida);


  Serial.print("O3 enviado = ");
  Serial.print(ppm, 4);
  Serial.println(" ppm");


  Serial.print("O3 x1000 = ");
  Serial.println(ppmCodificado);


  Serial.print("Major decimal = ");
  Serial.println(major);


  Serial.print(
    "Minor decimal mostrado BLE = "
  );

  Serial.println(minor);


  Serial.print(
    "Minor interpretado int16_t = "
  );

  Serial.println(
    (int16_t)minor
  );


  Serial.println("==============================");

  Serial.println();
}


// -----------------------------------------------------------------------------
// --> parpadearLedMedida()
//
// Realiza un destello corto con el LED integrado de la placa para indicar que
// una medida ha sido obtenida y publicada correctamente mediante iBeacon.
// -----------------------------------------------------------------------------

void parpadearLedMedida()
{
  digitalWrite(
    LED_BUILTIN,
    HIGH
  );


  delay(
    DURACION_LED_MEDIDA_MS
  );


  digitalWrite(
    LED_BUILTIN,
    LOW
  );
}


// -----------------------------------------------------------------------------
// --> setup()
//
// Inicializa puerto serie, ADC, pines analógicos, LED integrado y Bluetooth
// Low Energy. Configura el nombre BLE como GTI-3A.
// -----------------------------------------------------------------------------

void setup()
{
  // ---------------------------------------------------------------------------
  // SERIAL
  // ---------------------------------------------------------------------------

  Serial.begin(BAUD_USB);

  delay(1000);


  Serial.println();

  Serial.println("==============================");

  Serial.println(" ULPSM-O3 968-046");

  Serial.println(" Sensor + ADC + iBeacon");

  Serial.println("==============================");

  Serial.println();


  // ---------------------------------------------------------------------------
  // ADC
  // ---------------------------------------------------------------------------

  analogReference(
    AR_INTERNAL_3_0
  );


  analogReadResolution(12);


  pinMode(
    PIN_VGAS,
    INPUT
  );


  pinMode(
    PIN_VREF,
    INPUT
  );


  pinMode(
    PIN_VTEMP,
    INPUT
  );


  // ---------------------------------------------------------------------------
  // LED INTEGRADO
  // ---------------------------------------------------------------------------

  pinMode(
    LED_BUILTIN,
    OUTPUT
  );


  digitalWrite(
    LED_BUILTIN,
    LOW
  );


  // ---------------------------------------------------------------------------
  // BLE
  // ---------------------------------------------------------------------------

  Bluefruit.begin();


  /*
   * Evitamos que la librería utilice automáticamente el LED integrado para
   * indicar estados de conexión BLE. El control del LED queda reservado para
   * indicar la publicación de medidas.
   */
  Bluefruit.autoConnLed(false);


  Bluefruit.setTxPower(
    TX_POWER_DBM
  );


  /*
   * Este nombre coincide con el utilizado por la aplicación Android.
   */
  Bluefruit.setName(
    "GTI-3A"
  );


  // ---------------------------------------------------------------------------
  // INFORMACION INICIAL
  // ---------------------------------------------------------------------------

  Serial.println(
    "Inicializacion terminada."
  );


  if (USAR_MEDIDA_FICTICIA)
  {
    Serial.println(
      "MODO: MEDIDA FICTICIA"
    );


    Serial.print(
      "Valor ficticio = "
    );


    Serial.print(
      MEDIDA_FICTICIA_PPM,
      4
    );


    Serial.println(
      " ppm"
    );
  }
  else
  {
    Serial.println(
      "MODO: SENSOR REAL"
    );
  }


  Serial.println();


  Serial.println(
    "CONEXIONES:"
  );


  Serial.println(
    "VGAS  -> AIN3 -> A2"
  );


  Serial.println(
    "VREF  -> AIN4 -> A1"
  );


  Serial.println(
    "VTEMP -> AIN5 -> A0"
  );


  Serial.println();


  Serial.println(
    "ADC = 12 bits / referencia 3.0 V"
  );


  Serial.println(
    "Sensibilidad O3 = -67.98 mV/ppm"
  );


  Serial.println(
    "LED integrado = indicador de medida publicada"
  );


  Serial.println();
}


// -----------------------------------------------------------------------------
// --> loop()
//
// Obtiene una concentración de O3 real o ficticia, la publica mediante
// iBeacon, realiza un destello con el LED integrado, incrementa el contador y
// espera hasta la siguiente medida.
// -----------------------------------------------------------------------------

void loop()
{
  float ppm;

  bool medidaCorrecta = false;


  // ---------------------------------------------------------------------------
  // OBTENER MEDIDA
  // ---------------------------------------------------------------------------

  if (USAR_MEDIDA_FICTICIA)
  {
    ppm =
      MEDIDA_FICTICIA_PPM;


    medidaCorrecta = true;


    Serial.println();

    Serial.println("--------------------------------");

    Serial.println("MEDIDA FICTICIA");

    Serial.println("--------------------------------");


    Serial.print("O3 ficticio = ");


    Serial.print(
      ppm,
      4
    );


    Serial.println(
      " ppm"
    );


    Serial.println("--------------------------------");
  }
  else
  {
    medidaCorrecta =
      leerSensorO3(ppm);
  }


  // ---------------------------------------------------------------------------
  // PUBLICAR MEDIDA
  // ---------------------------------------------------------------------------

  if (medidaCorrecta)
  {
    Serial.println();


    Serial.print(
      "CONCENTRACION O3 FINAL: "
    );


    Serial.print(
      ppm,
      4
    );


    Serial.println(
      " ppm"
    );


    publicarIBeacon(

      ppm,

      contador

    );


    // -------------------------------------------------------------------------
    // INDICADOR VISUAL
    //
    // El destello se realiza después de publicar el iBeacon. Por tanto,
    // visualmente indica que la medida ha sido procesada y publicada.
    // -------------------------------------------------------------------------

    parpadearLedMedida();


    contador++;
  }
  else
  {
    Serial.println(
      "ERROR: no se ha podido obtener una medida"
    );
  }


  // ---------------------------------------------------------------------------
  // ESPERAR HASTA LA SIGUIENTE MEDIDA
  // ---------------------------------------------------------------------------

  delay(
    INTERVALO_MEDIDA_MS
  );
}