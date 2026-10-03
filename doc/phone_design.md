# Phone Component Design

## Component Design

### Tipos lógicos

```text
UUID = [N]_16
PrefijoIBeacon = [N]_9
Major = [N]_2
Minor = [N]_2
TramaBLE = [N]
```

### Clase `TramaIBeacon`

Responsabilidad:

Representar y separar los distintos campos de una trama iBeacon recibida por
el teléfono.

```text
                 --------- TramaIBeacon ------------------
                 |
                 | prefijo: [N]_9
                 | uuid: [N]_16
                 | major: [N]_2
                 | minor: [N]_2
                 | tx_power: Z
                 | los_bytes: [N]
                 | adv_flags: [N]_3
                 | adv_header: [N]_2
                 | company_id: [N]_2
                 | ibeacon_type: N
                 | ibeacon_length: N
                 |
                 |
bytes: [N]    --> TramaIBeacon() -->
                 |
                 |
[N]_9         <-- getPrefijo() <--
                 |
                 |
[N]_16        <-- getUUID() <--
                 |
                 |
[N]_2         <-- getMajor() <--
                 |
                 |
[N]_2         <-- getMinor() <--
                 |
                 |
Z             <-- getTxPower() <--
                 |
                 |
[N]           <-- getLosBytes() <--
                 |
                 |
[N]_3         <-- getAdvFlags() <--
                 |
                 |
[N]_2         <-- getAdvHeader() <--
                 |
                 |
[N]_2         <-- getCompanyID() <--
                 |
                 |
N             <-- getiBeaconType() <--
                 |
                 |
N             <-- getiBeaconLength() <--
                 |
                 ------------------------------------------
```

### Clase `Utilidades`

Responsabilidad:

Proporcionar conversiones auxiliares entre texto, bytes, números y UUID.

La clase no mantiene estado. Sus operaciones son estáticas.

```text
                 --------- Utilidades ---------------------

texto: Text  --> stringToBytes() --> [N]              --x


uuid: Text   --> stringToUUID() --> UUID              --x


uuid: UUID   --> uuidToString() --> Text              --x


uuid: UUID   --> uuidToHexString() --> Text           --x


bytes: [N]   --> bytesToString() --> Text             --x


mas_significativos: Z,
menos_significativos: Z
              --> dosLongToBytes() --> [N]_16         --x


bytes: [N]   --> bytesToInt() --> Z                   --x


bytes: [N]   --> bytesToLong() --> Z                  --x


bytes: [N]   --> bytesToIntOK() --> Z                 --x


bytes: [N]   --> bytesToHexString() --> Text          --x

                 ------------------------------------------
```

### Decodificación utilizada en Sprint 0

La trama iBeacon contiene:

```text
Prefijo        9 bytes
UUID          16 bytes
Major          2 bytes
Minor          2 bytes
TxPower        1 byte
```

El campo `major` contiene dos valores:

```text
+----------------+----------------+
| tipo_medicion  |   contador     |
|     8 bits     |    8 bits      |
+----------------+----------------+
```

Codificación de tipos:

```text
11 = CO2
12 = TEMPERATURA
13 = RUIDO
```

El byte más significativo de `major` representa el tipo de medición.

El byte menos significativo de `major` representa el contador.

El campo `minor` representa el valor de la medición.

### Clase `MainActivity`

Responsabilidad:

Coordinar la inicialización de Bluetooth, el escaneo BLE, la recepción de
resultados y las acciones realizadas desde la interfaz gráfica.

La clase utiliza `TramaIBeacon` y `Utilidades` para interpretar la información
recibida, pero no contiene lógica de backend ni acceso a base de datos.

```text
                 ---------------- MainActivity ----------------
                 |
                 | ETIQUETA_LOG: Text                         --x
                 | CODIGO_PETICION_PERMISOS: N               --x
                 | elEscanner: BluetoothLeScanner
                 | callbackDelEscaneo: ScanCallback
                 |
                 |
                 | --> buscarTodosLosDispositivosBTLE()
                 |
resultado: ScanResult
              --> mostrarInformacionDispositivoBTLE()
                 |
dispositivo_buscado: Text
              --> buscarEsteDispositivoBTLE()
                 |
                 | --> detenerBusquedaDispositivosBTLE()
                 |
                 | --> inicializarBlueTooth()
                 |
                 | --> botonBuscarDispositivosBTLEPulsado()
                 |
                 | --> botonBuscarNuestroDispositivoBTLEPulsado()
                 |
                 | --> botonDetenerBusquedaDispositivosBTLEPulsado()
                 |
                 | --> onCreate()
                 |
request_code: N,
resultados: [Z]
              --> onRequestPermissionsResult()
                 |
                 ------------------------------------------------
```

### Operaciones de `MainActivity`

#### Inicialización de Bluetooth

```text
inicializarBlueTooth()
```

Responsabilidad:

Inicializar el sistema Bluetooth del teléfono, obtener el escáner BLE y
gestionar los permisos necesarios para poder realizar el escaneo.

#### Buscar todos los dispositivos BLE

```text
buscarTodosLosDispositivosBTLE()
```

Responsabilidad:

Iniciar un escaneo Bluetooth Low Energy sin filtrar y procesar los dispositivos
detectados mediante un `ScanCallback`.

#### Buscar un dispositivo BLE concreto

```text
dispositivo_buscado: Text
    -->
buscarEsteDispositivoBTLE()
```

Responsabilidad:

Iniciar un escaneo BLE para localizar el dispositivo cuyo nombre se proporciona.

El diseño original proporcionado por los profesores utiliza un `ScanFilter`
basado en el nombre del dispositivo.

#### Mostrar información de un dispositivo BLE

```text
resultado: ScanResult
    -->
mostrarInformacionDispositivoBTLE()
```

Responsabilidad:

Obtener la información del resultado BLE detectado, incluyendo dispositivo,
RSSI y bytes del anuncio.

Cuando la trama recibida sea válida, utilizar `TramaIBeacon` y `Utilidades`
para separar y mostrar sus campos.

Esta operación no almacena datos ni realiza comunicaciones REST.

#### Detener búsqueda BLE

```text
detenerBusquedaDispositivosBTLE()
```

Responsabilidad:

Detener el escaneo BLE activo utilizando el mismo `ScanCallback` con el que se
inició la búsqueda.

#### Acciones de la interfaz

```text
botonBuscarDispositivosBTLEPulsado()
```

Inicia la búsqueda de todos los dispositivos BLE.

```text
botonBuscarNuestroDispositivoBTLEPulsado()
```

Inicia la búsqueda del dispositivo utilizado por el proyecto.

```text
botonDetenerBusquedaDispositivosBTLEPulsado()
```

Detiene la búsqueda BLE activa.

Los parámetros `View` utilizados por Android para los eventos de los botones
se consideran un detalle de implementación de la interfaz y no forman parte de
la firma lógica.

#### Creación de la actividad

```text
onCreate()
```

Responsabilidad:

Inicializar la pantalla principal y comenzar la inicialización Bluetooth cuando
se crea la actividad.

El objeto `Bundle` recibido por Android se considera un detalle del framework y
no forma parte de la firma lógica.

#### Resultado de petición de permisos

```text
request_code: N,
resultados: [Z]
    -->
onRequestPermissionsResult()
```

Responsabilidad:

Procesar el resultado de la solicitud de permisos necesaria para utilizar
Bluetooth.

Los parámetros adicionales proporcionados por Android que únicamente forman
parte del mecanismo del framework se omiten del diseño lógico.


### Interfaz gráfica Android

La interfaz inicial mantiene el diseño básico proporcionado por los profesores.

Contiene tres botones:

```text
Buscar Dispositivos BTLE
        |
        v
botonBuscarDispositivosBTLEPulsado()


Detener búsqueda Dispositivos BTLE
        |
        v
botonDetenerBusquedaDispositivosBTLEPulsado()


Buscar nuestro dispositivo BTLE
        |
        v
botonBuscarNuestroDispositivoBTLEPulsado()
```

En esta fase la información de los dispositivos detectados se muestra
principalmente mediante el sistema de log de Android.

La interfaz podrá ampliarse posteriormente si es necesario para el proyecto,
pero cualquier ampliación deberá añadirse primero a este documento de diseño.

### Tests actuales

Existe un test unitario para comprobar la separación e interpretación básica
de una trama iBeacon:

```text
comprobarTramaIBeacon()
```

El test comprueba:

- UUID.
- Major.
- Minor.
- TxPower.
- Tipo de medición extraído de Major.
- Contador extraído de Major.
- Valor obtenido de Minor.

---

## Design Clarifications

- El código de `TramaIBeacon` y `Utilidades` parte del código proporcionado por
  los profesores y se mantiene con la máxima fidelidad posible.

- El `package` original se adapta al package del proyecto actual:

  `com.example.abalgra1.pbio`

- `TramaIBeacon` únicamente representa y separa los campos de la trama.
  No contiene lógica de comunicaciones REST ni lógica de interfaz gráfica.

- `Utilidades` contiene únicamente funciones auxiliares de conversión y no
  mantiene estado.

- La interpretación de `major` utilizada en Sprint 0 sigue la codificación del
  emisor: los 8 bits más significativos representan el tipo y los 8 bits menos
  significativos representan el contador.

- El RSSI recibido por el teléfono y el `txPower` incluido en el iBeacon son
  datos diferentes. `txPower` pertenece a la trama iBeacon.

- Actualmente se conserva `bytesToIntOK()` tal como aparece en el material
  proporcionado. Cualquier corrección posterior deberá justificarse y
  documentarse antes de modificar su comportamiento.

- El proyecto Android utiliza Minimum SDK API 26.

---

## General Rules

- **Programming Language:** Java.

- **Function/Method Headers:** Cada función o método debe incluir su diseño
  lógico dentro de un bloque de comentarios delimitado por líneas discontinuas.

  Ejemplo:

  ```text
  // --------------------------------------------------
  // major: [N]_2 --> obtenerTipoMedicion() --> N
  //
  // Obtiene el tipo codificado en el campo major.
  // --------------------------------------------------
  ```

- **File Headers:** Cada fichero de código fuente debe incluir una cabecera con:
  nombre del fichero, descripción, copyright, fecha, autor y aportación.

- **Code Readability:** El código debe ser claro y autoexplicativo. Se evitarán
  comentarios que únicamente repitan lo que ya expresa el código.

- **Separation of Responsibilities:** La interpretación BLE, las comunicaciones
  REST y la interfaz gráfica deben mantenerse separadas. No se introducirá
  lógica de backend ni acceso a base de datos en el teléfono.

- **Compatibility:** Se mantendrá el código proporcionado por los profesores
  siempre que sea compatible y funcional. Los cambios necesarios deberán
  justificarse y documentarse.

- **Automated Testing:** Se generarán tests unitarios o de integración para las
  funciones y métodos críticos. Siempre que sea posible, la lógica de
  decodificación deberá poder probarse sin disponer físicamente de la placa.

- **Design Consistency:** Cualquier función nueva deberá aparecer primero en
  este diseño y su implementación deberá conservar el mismo nombre,
  responsabilidad, entradas y salidas lógicas.