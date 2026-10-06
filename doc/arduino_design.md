# Arduino Component Design

## Component Design

### Objetivo del componente

El componente Arduino obtiene una concentración de O3 y la publica mediante
Bluetooth Low Energy utilizando el formato iBeacon.

La concentración puede proceder de dos orígenes:

```text
Sensor O3 real
      |
      v
leerSensorO3()
      |
      |
      +--------------------+
                           |
Medida ficticia ----------+
                           |
                           v
                    publicarIBeacon()
                           |
                           v
                      BLE / iBeacon
                           |
                           v
                  parpadearLedMedida()
```

El modo ficticio permite realizar una prueba reproducible del sistema completo
sin depender de la concentración real medida por el sensor.

El LED integrado permite confirmar visualmente que una medida ha sido obtenida
y publicada correctamente.

---

### Tipos lógicos

```text
UUID = [N]_16

MedidaO3=(
    tipo: N,
    contador: N,
    valor: Z
)
```

Donde:

```text
tipo      = identificador de la magnitud medida
contador  = número de medida, en el rango 0..255
valor     = concentración de O3 expresada en ppb
```

---

### Constantes principales

```text
BAUD_USB: N = 115200

PIN_VGAS: Z = 5
PIN_VREF: Z = 28
PIN_VTEMP: Z = 29

ADC_REFERENCIA: R = 3.0
ADC_MAX: R = 4095.0

SENSIBILIDAD_O3: R = -0.06798

FABRICANTE_ID: N = 0x004C
TX_POWER_DBM: Z = 4
RSSI_A_1M: Z = -53

ID_O3: N = 14

INTERVALO_MEDIDA_MS: N = 2000

DURACION_LED_MEDIDA_MS: N = 100

USAR_MEDIDA_FICTICIA: B
MEDIDA_FICTICIA_PPM: R
```

El UUID utilizado es:

```text
EPSG-GTI-PROY-3A
```

El nombre BLE utilizado por el dispositivo es:

```text
GTI-3A
```

---

### Estado del componente

```text
beacon_uuid: UUID
contador: N
```

El contador representa el número de medida transmitida.

Su implementación utiliza 8 bits, por lo que sus valores se encuentran en:

```text
0..255
```

Después de 255 vuelve a 0.

---

### leerADCpromedio()

```text
pin: Z --> leerADCpromedio() --> adc: N
```

Responsabilidad:

Realizar varias lecturas ADC sobre el pin analógico indicado y devolver el
valor medio obtenido.

La implementación realiza 20 lecturas para reducir pequeñas variaciones y
ruido.

---

### adcAVoltios()

```text
adc: N --> adcAVoltios() --> voltios: R
```

Responsabilidad:

Convertir una lectura ADC de 12 bits a voltios utilizando una referencia de
3.0 V.

La relación utilizada es:

```text
voltios = adc * 3.0 / 4095
```

---

### leerSensorO3()

```text
leerSensorO3() --> ppm: R, exito: B
```

Responsabilidad:

Obtener una medida real del sensor de O3.

La operación realiza:

```text
1. Lee VGAS.
2. Lee VREF.
3. Lee VTEMP.
4. Convierte las lecturas ADC a voltios.
5. Calcula DeltaV.
6. Aplica la sensibilidad individual del sensor.
7. Devuelve la concentración en ppm.
```

La sensibilidad utilizada es:

```text
-67.98 mV/ppm
```

equivalente a:

```text
-0.06798 V/ppm
```

La implementación actual utiliza como primera aproximación:

```text
Vgas0 = Vref
```

Por tanto:

```text
DeltaV = Vgas - Vgas0
```

y:

```text
ppm = DeltaV / sensibilidad
```

El valor `VTEMP` se mide y se muestra, pero en el estado actual de Sprint 0 no
se utiliza para realizar compensación de temperatura.

---

### publicarIBeacon()

```text
ppm: R,
numero_medida: N
    -->
publicarIBeacon()
```

Responsabilidad:

Codificar una concentración de O3 dentro de los campos `major` y `minor` de un
iBeacon y comenzar su publicación mediante BLE.

La concentración se transforma primero a:

```text
valor = round(ppm * 1000)
```

Por tanto:

```text
0.333 ppm  --> 333 ppb
0.075 ppm  --> 75 ppb
-0.215 ppm --> -215 ppb
```

El valor se limita al rango:

```text
-32768..32767
```

para poder representarlo como entero de 16 bits con signo.

---

### Codificación del campo Major

El campo `major` utiliza 16 bits:

```text
+----------------+----------------+
| tipo_medicion  |   contador     |
|     8 bits     |    8 bits      |
+----------------+----------------+
```

La codificación utilizada es:

```text
major = (tipo << 8) | contador
```

Para O3:

```text
tipo = 14
```

Ejemplo:

```text
tipo = 14
contador = 42

major = (14 << 8) + 42
major = 3626
```

---

### Codificación del campo Minor

El campo `minor` contiene el valor de la concentración expresado en ppb.

Lógicamente:

```text
minor: Z
```

Aunque el protocolo iBeacon transporta el campo como 16 bits sin signo, los
bits corresponden a un entero de 16 bits con signo.

Por tanto:

```text
333  --> 333
75   --> 75
-215 --> representación binaria de int16_t(-215)
```

El receptor debe reconstruir el valor original interpretando los 16 bits como
un entero con signo.

---

### Estructura del iBeacon

```text
UUID
    =
EPSG-GTI-PROY-3A

Major
    =
tipo + contador

Minor
    =
valor O3 en ppb con signo

TxPower
    =
RSSI_A_1M
```

El fabricante utilizado es:

```text
0x004C
```

El nombre BLE anunciado en la respuesta de escaneo es:

```text
GTI-3A
```

---

### parpadearLedMedida()

```text
parpadearLedMedida()
```

Responsabilidad:

Realizar un destello corto con el LED integrado de la placa para indicar
visualmente que una medida ha sido obtenida y publicada correctamente.

El LED se enciende durante:

```text
100 ms
```

y después vuelve a apagarse.

El flujo es:

```text
publicarIBeacon()
        |
        v
parpadearLedMedida()
        |
        +--> LED ON
        |
        +--> esperar 100 ms
        |
        +--> LED OFF
```

El LED tiene únicamente función de indicación visual.

No modifica:

```text
valor de la medida
major
minor
contador
publicidad BLE
```

El destello se realiza tanto para medidas reales como para medidas ficticias.

---

### setup()

```text
setup()
```

Responsabilidad:

Inicializar los elementos necesarios antes de comenzar las medidas.

La operación realiza:

```text
Puerto serie
    |
    v
ADC a 12 bits
    |
    v
Referencia ADC de 3.0 V
    |
    v
Configuración de pines del sensor
    |
    v
Configuración LED integrado
    |
    v
Bluefruit
    |
    v
Potencia de transmisión
    |
    v
Nombre BLE = GTI-3A
```

El LED integrado se configura inicialmente apagado.

La gestión automática del LED por parte de Bluefruit se desactiva mediante:

```text
Bluefruit.autoConnLed(false)
```

De esta forma, el LED queda reservado como indicador de publicación de
medidas.

También se informa mediante el puerto serie del modo activo:

```text
MEDIDA FICTICIA
```

o:

```text
SENSOR REAL
```

---

### loop()

```text
loop()
```

Responsabilidad:

Obtener periódicamente una concentración de O3, publicarla mediante iBeacon,
indicar visualmente la publicación, incrementar el contador y esperar hasta la
siguiente medida.

El origen de la concentración depende de:

```text
USAR_MEDIDA_FICTICIA
```

El flujo es:

```text
                  USAR_MEDIDA_FICTICIA
                         |
             +-----------+-----------+
             |                       |
           true                    false
             |                       |
             v                       v
MEDIDA_FICTICIA_PPM          leerSensorO3()
             |                       |
             +-----------+-----------+
                         |
                         v
                  concentración ppm
                         |
                         v
                  publicarIBeacon()
                         |
                         v
                 parpadearLedMedida()
                         |
                         v
                  contador = contador + 1
                         |
                         v
                       espera
```

El intervalo configurado entre nuevas iteraciones es:

```text
2000 ms
```

El destello del LED dura:

```text
100 ms
```

---

### Modo de medida ficticia

La prueba reproducible del Sprint 0 utiliza:

```text
USAR_MEDIDA_FICTICIA = true
```

y un valor configurable:

```text
MEDIDA_FICTICIA_PPM
```

Ejemplo:

```text
MEDIDA_FICTICIA_PPM = 0.333
```

produce:

```text
valor = 333 ppb
minor = 333
```

Para cambiar la medida utilizada durante una demostración únicamente debe
modificarse:

```text
MEDIDA_FICTICIA_PPM
```

El resto del proceso de publicación es exactamente el mismo que se utiliza con
una medida procedente del sensor real.

Después de publicar correctamente la medida también se produce el destello del
LED integrado.

---

### Modo de medida real

Para utilizar físicamente el sensor:

```text
USAR_MEDIDA_FICTICIA = false
```

El flujo pasa a ser:

```text
VGAS
VREF
VTEMP
 |
 v
ADC
 |
 v
Voltios
 |
 v
DeltaV
 |
 v
Sensibilidad individual
 |
 v
O3 ppm
 |
 v
O3 ppb
 |
 v
iBeacon
 |
 v
LED
```

El LED confirma visualmente que la medida real ha llegado hasta la fase de
publicación BLE.

---

### Contrato Arduino - Android

La comunicación entre Arduino y Android queda definida como:

```text
Nombre BLE = "GTI-3A"

UUID = "EPSG-GTI-PROY-3A"

tipo = 14
```

El campo `major` contiene:

```text
major[15..8] = tipo
major[7..0]  = contador
```

El campo `minor` contiene:

```text
valor O3 en ppb representado como entero de 16 bits con signo
```

Android debe producir finalmente una medición lógica equivalente a:

```text
Medicion=(
    uuid: Text,
    nombre_dispositivo: Text,
    tipo: Text,
    valor: Z,
    contador: N,
    rssi: Z
)
```

Para un anuncio con:

```text
tipo = 14
contador = 42
minor = 75
```

Android debe interpretar:

```text
tipo = "O3"
contador = 42
valor = 75
```

El indicador LED no forma parte del protocolo Arduino-Android.

---

### Pruebas realizadas

#### Prueba de medida ficticia

Con:

```text
USAR_MEDIDA_FICTICIA = true
MEDIDA_FICTICIA_PPM = 0.333
```

se verificó:

```text
O3 enviado = 0.3330 ppm
O3 x1000 = 333
Minor interpretado = 333
```

También se verificó el contador incluido en `major`.

Se realizó una segunda prueba modificando la medida ficticia a:

```text
0.750 ppm
```

y se obtuvo:

```text
Minor = 750
```

confirmando que el valor introducido en el código se transmite mediante
iBeacon.

#### Prueba de medida real

Con:

```text
USAR_MEDIDA_FICTICIA = false
```

se comprobó que el sistema obtiene valores reales de:

```text
VGAS
VREF
VTEMP
DeltaV
O3
```

y los publica posteriormente mediante iBeacon.

Ejemplos observados:

```text
O3 = 0.0754 ppm --> 75 ppb
O3 = 0.0539 ppm --> 54 ppb
```

Estas pruebas verifican el funcionamiento de la cadena de adquisición y
codificación.

No constituyen por sí mismas una calibración metrológica definitiva del sensor.

#### Prueba del indicador LED

Cada publicación correcta de una medida debe producir:

```text
LED ON
100 ms
LED OFF
```

El destello debe producirse una vez por cada medida publicada.

La prueba debe realizarse tanto con:

```text
USAR_MEDIDA_FICTICIA = true
```

como con:

```text
USAR_MEDIDA_FICTICIA = false
```

---

## Design Clarifications

- El sensor utilizado es un sensor de O3 conectado a entradas analógicas del
  SparkFun Pro nRF52840 Mini.

- Se conserva la sensibilidad individual:

  ```text
  -67.98 mV/ppm
  ```

- La sensibilidad y el ajuste de cero son conceptos distintos.

- En Sprint 0 se mantiene:

  ```text
  Vgas0 = Vref
  ```

  como aproximación actualmente utilizada.

- La compensación mediante `VTEMP` no forma parte todavía de la implementación.

- El objetivo principal de Sprint 0 es verificar la cadena completa de
  adquisición, comunicación y almacenamiento.

- El modo ficticio no utiliza una ruta de comunicación distinta.

  Tanto la medida ficticia como la medida real terminan utilizando:

  ```text
  publicarIBeacon()
  ```

- Después de una publicación correcta se llama a:

  ```text
  parpadearLedMedida()
  ```

- El LED integrado tiene únicamente función de indicación visual y no forma
  parte de los datos transmitidos.

- La gestión automática del LED de conexión de Bluefruit se mantiene
  deshabilitada para evitar interferencias con el indicador de medida.

- El nombre BLE se establece como:

  ```text
  GTI-3A
  ```

  para coincidir con el cliente Android.

- El UUID utilizado en todo el proyecto es:

  ```text
  EPSG-GTI-PROY-3A
  ```

- El identificador asignado a O3 es:

  ```text
  14
  ```

- El valor transmitido en `minor` se expresa en ppb.

- El valor de `minor` debe interpretarse como entero de 16 bits con signo.

- La publicación iBeacon es no conectable.

- Se mantiene un contador de 8 bits dentro del byte menos significativo de
  `major`.

- El contador puede repetirse después de 255 y no debe utilizarse como
  identificador global único de una medición.

- La calibración científica definitiva del sensor puede revisarse de forma
  independiente sin modificar el contrato de comunicación Arduino-Android.

---

## General Rules

- **Programming Language:** C++ / Arduino.

- **Function Headers:** Cada función deberá incluir su diseño lógico dentro de
  un bloque de comentarios delimitado por líneas discontinuas.

  Ejemplo:

  ```text
  // --------------------------------------------------
  // adc: N --> adcAVoltios() --> voltios: R
  //
  // Convierte una lectura ADC a voltios.
  // --------------------------------------------------
  ```

- **File Headers:** Cada fichero de código fuente deberá indicar:

  ```text
  nombre
  descripción
  copyright
  fecha
  autor
  aportación
  ```

- **Author:** Las aportaciones realizadas para este Sprint se documentan con:

  ```text
  Álvaro Ballester Grau
  ```

- **Code Readability:** Los nombres de las variables y funciones deben ser
  claros y los comentarios deben explicar decisiones o comportamiento, no
  repetir innecesariamente el código.

- **Separation of Responsibilities:** La placa únicamente adquiere, codifica,
  publica la medida e informa visualmente de la publicación.

  No realiza comunicaciones REST ni acceso a base de datos.

- **Visual Feedback:** El LED integrado únicamente indica que una medida se ha
  publicado correctamente.

  El estado del LED no modifica ni determina el contenido de la medición.

- **Reproducible Testing:** El modo de medida ficticia debe permitir introducir
  una concentración conocida y comprobar de forma reproducible los campos
  `major` y `minor` generados.

- **Compatibility:** Las modificaciones deben conservar el funcionamiento del
  código base siempre que sea posible.

- **Design Consistency:** Cualquier cambio en la codificación del iBeacon o en
  el comportamiento del indicador LED deberá reflejarse previamente en este
  documento.

- **Repository Consistency:** El diseño almacenado en:

  ```text
  doc/arduino_design.md
  ```

  deberá coincidir con la implementación existente en:

  ```text
  src/arduino/
  ```