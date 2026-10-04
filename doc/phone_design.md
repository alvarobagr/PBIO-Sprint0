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

---

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

---

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

---

### Decodificación utilizada en Sprint 0

La trama iBeacon contiene:

```text
Prefijo        9 bytes
UUID          16 bytes
Major          2 bytes
Minor          2 bytes
TxPower        1 byte
```

La interpretación utilizada actualmente para el campo `major` es:

```text
+----------------+----------------+
| tipo_medicion  |   contador     |
|     8 bits     |    8 bits      |
+----------------+----------------+
```

Codificación utilizada en el código proporcionado:

```text
11 = CO2
12 = TEMPERATURA
13 = RUIDO
```

El byte más significativo de `major` representa el tipo de medición.

El byte menos significativo de `major` representa el contador.

El campo `minor` representa el valor de la medición.

Esta interpretación se mantiene inicialmente por fidelidad al código
proporcionado. Durante la prueba con la placa real se mostrarán también
directamente `major` y `minor` para comprobar la codificación utilizada
realmente por el emisor del Sprint 0.

---

### Clase `MainActivity`

Responsabilidad:

Coordinar la inicialización de Bluetooth, el escaneo BLE, la recepción de
resultados, la interpretación de la trama y la actualización de la interfaz
gráfica.

La clase utiliza `TramaIBeacon` y `Utilidades` para interpretar la información
recibida.

No contiene lógica de backend ni acceso a base de datos.

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
                 |
resultado: ScanResult
              --> mostrarInformacionDispositivoBTLE()
                 |
                 |
dispositivo_buscado: Text
              --> buscarEsteDispositivoBTLE()
                 |
                 |
                 | --> detenerBusquedaDispositivosBTLE()
                 |
                 |
                 | --> inicializarBlueTooth()
                 |
                 |
                 | --> botonBuscarDispositivosBTLEPulsado()
                 |
                 |
                 | --> botonBuscarNuestroDispositivoBTLEPulsado()
                 |
                 |
                 | --> botonDetenerBusquedaDispositivosBTLEPulsado()
                 |
                 |
                 | --> onCreate()
                 |
                 |
request_code: N,
resultados: [Z]
              --> onRequestPermissionsResult()
                 |
                 |
nombre_dispositivo: Text,
uuid: Text,
major: Z,
minor: Z,
tipo: N,
contador: N,
valor: Z,
rssi: Z,
tx_power: Z
              --> actualizarDatosSensor()
                 |
                 |
                 ------------------------------------------------
```

---

### Operaciones de `MainActivity`

#### Inicialización de Bluetooth

```text
inicializarBlueTooth()
```

Responsabilidad:

Inicializar el sistema Bluetooth del teléfono, obtener el escáner BLE y
gestionar los permisos necesarios para poder realizar el escaneo.

---

#### Buscar todos los dispositivos BLE

```text
buscarTodosLosDispositivosBTLE()
```

Responsabilidad:

Iniciar un escaneo Bluetooth Low Energy sin filtrar y procesar los dispositivos
detectados mediante un `ScanCallback`.

---

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

En el Sprint 0 el dispositivo buscado es:

```text
GTI-3A
```

---

#### Mostrar información de un dispositivo BLE

```text
resultado: ScanResult
    -->
mostrarInformacionDispositivoBTLE()
```

Responsabilidad:

Obtener la información del resultado BLE detectado, incluyendo:

```text
Nombre del dispositivo
RSSI
Bytes del anuncio
UUID
Major
Minor
Tipo
Contador
Valor
TxPower
```

Cuando la trama recibida presenta el formato iBeacon esperado, se utilizan
`TramaIBeacon` y `Utilidades` para separar e interpretar sus campos.

Después de interpretar la trama, se llama a:

```text
actualizarDatosSensor()
```

para mostrar los valores recibidos en la interfaz gráfica.

Esta operación no almacena datos ni realiza comunicaciones REST.

---

#### Actualizar datos del sensor

```text
nombre_dispositivo: Text,
uuid: Text,
major: Z,
minor: Z,
tipo: N,
contador: N,
valor: Z,
rssi: Z,
tx_power: Z
    -->
actualizarDatosSensor()
```

Responsabilidad:

Actualizar la interfaz gráfica con los datos correspondientes al último anuncio
BLE válido recibido.

La función únicamente presenta información.

No modifica la medición.

No almacena información.

No realiza comunicaciones REST.

---

#### Detener búsqueda BLE

```text
detenerBusquedaDispositivosBTLE()
```

Responsabilidad:

Detener el escaneo BLE activo utilizando el mismo `ScanCallback` empleado para
iniciar la búsqueda.

---

#### Acciones de la interfaz

```text
botonBuscarDispositivosBTLEPulsado()
```

Responsabilidad:

Iniciar la búsqueda de todos los dispositivos BLE.

```text
botonBuscarNuestroDispositivoBTLEPulsado()
```

Responsabilidad:

Iniciar la búsqueda del dispositivo BLE utilizado por el proyecto.

```text
botonDetenerBusquedaDispositivosBTLEPulsado()
```

Responsabilidad:

Detener la búsqueda BLE activa.

Los parámetros `View` utilizados por Android para gestionar los eventos de los
botones son detalles de implementación del framework y no forman parte de la
firma lógica.

---

#### Creación de la actividad

```text
onCreate()
```

Responsabilidad:

Inicializar la pantalla principal y comenzar la inicialización Bluetooth cuando
se crea la actividad.

El objeto `Bundle` recibido por Android se considera un detalle del framework
y no forma parte de la firma lógica.

---

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

Si todos los permisos necesarios han sido concedidos, se continúa la
inicialización Bluetooth.

Los parámetros adicionales proporcionados por Android que únicamente forman
parte del mecanismo del framework se omiten del diseño lógico.

---

### Interfaz gráfica Android

La interfaz mantiene los tres controles principales presentes en el ejemplo
proporcionado por los profesores:

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

A estos controles se añade una zona de visualización destinada a mostrar el
último anuncio BLE válido recibido.

La pantalla mostrará:

```text
Estado
Nombre del dispositivo
UUID
Major
Minor
Tipo
Contador
Valor
RSSI
TxPower
```

Antes de recibir una trama válida se mostrará un estado equivalente a:

```text
Estado: Esperando sensor...
```

Después de recibir una trama válida, la interfaz se actualizará con los datos
obtenidos.

Un ejemplo conceptual de la pantalla es:

```text
------------------------------------------------
PBIO - Sensor BLE

[ Buscar Dispositivos BTLE ]

[ Detener búsqueda Dispositivos BTLE ]

[ Buscar nuestro dispositivo BTLE ]


Estado: Sensor detectado

Dispositivo: GTI-3A
UUID: EPSG-GTI-PROY-3A

Major: 30
Minor: 333

Tipo: ...
Contador: ...
Valor: 333

RSSI: -68 dBm
TxPower: -73 dBm
------------------------------------------------
```

Los valores del ejemplo anterior son únicamente ilustrativos.

Los valores mostrados realmente procederán del anuncio BLE recibido.

---

### Flujo de visualización

```text
ScanResult
    |
    v
mostrarInformacionDispositivoBTLE()
    |
    +--> obtener RSSI
    |
    +--> obtener ScanRecord
    |
    +--> obtener bytes
    |
    +--> comprobar longitud
    |
    +--> comprobar prefijo iBeacon
    |
    +--> TramaIBeacon
    |
    +--> Utilidades
    |
    v
UUID
Major
Minor
Tipo
Contador
Valor
RSSI
TxPower
    |
    v
actualizarDatosSensor()
    |
    v
Interfaz gráfica
```

Los mismos datos se podrán mantener también en Logcat durante el desarrollo
para facilitar la depuración.

Los campos `Major` y `Minor` permanecerán visibles durante Sprint 0 porque
permiten comprobar directamente la información enviada por la placa.

Esto es especialmente importante mientras no se haya validado con un teléfono
Android físico la codificación exacta del anuncio BLE generado por el emisor.

---

### Tests actuales

Existe un test unitario para comprobar la separación e interpretación básica
de una trama iBeacon:

```text
comprobarTramaIBeacon()
```

El test utiliza una trama ficticia y comprueba:

- UUID.
- Major.
- Minor.
- TxPower.
- Tipo de medición extraído de Major.
- Contador extraído de Major.
- Valor obtenido de Minor.

Los tests permiten comprobar la lógica de interpretación sin necesitar
físicamente la placa BLE.

La recepción real del anuncio Bluetooth deberá comprobarse posteriormente con
un dispositivo Android físico compatible con BLE.

---

## Design Clarifications

- El código de `TramaIBeacon` y `Utilidades` parte del código proporcionado por
  los profesores y se mantiene con la máxima fidelidad posible.

- El `package` original se adapta al package del proyecto actual:

  ```text
  com.example.abalgra1.pbio
  ```

- La estructura y los nombres principales de `MainActivity` también se basan en
  el código proporcionado por los profesores.

- Se han realizado únicamente las adaptaciones necesarias para utilizar el
  código en versiones modernas de Android.

- Para Android 12 y posteriores se utilizan los permisos:

  ```text
  BLUETOOTH_SCAN
  BLUETOOTH_CONNECT
  ```

- Para versiones anteriores se conserva el uso del permiso de localización
  necesario para el escaneo BLE.

- La aplicación no activa Bluetooth silenciosamente. Si Bluetooth está
  desactivado, Android solicita al usuario que lo active.

- Antes de acceder a los bytes de un `ScanRecord` se comprueba que el objeto
  recibido no sea nulo.

- Antes de construir `TramaIBeacon` se comprueba que la trama tenga una
  longitud suficiente.

- Se comprueba el prefijo esperado de iBeacon antes de interpretar una trama
  como tal.

- `TramaIBeacon` únicamente representa y separa los campos de la trama.

- `Utilidades` contiene únicamente funciones auxiliares de conversión y no
  mantiene estado.

- `MainActivity` coordina el escaneo y la presentación, pero no contiene lógica
  de backend ni acceso a base de datos.

- La visualización de los datos no modifica la información recibida.

- El RSSI recibido por el teléfono y el `txPower` incluido dentro del iBeacon
  son datos diferentes.

- La interpretación de `major` utilizada inicialmente sigue el código
  proporcionado:

  ```text
  byte más significativo -> tipo
  byte menos significativo -> contador
  ```

- Esta interpretación todavía deberá contrastarse con la trama emitida por la
  placa real del Sprint 0.

- Por ese motivo se muestran también los valores `Major` y `Minor` sin ocultar.

- Actualmente se conserva `bytesToIntOK()` tal como aparece en el material
  proporcionado. Cualquier corrección posterior deberá justificarse y
  documentarse antes de modificar su comportamiento.

- El proyecto Android utiliza Minimum SDK API 26.

- La aplicación se ha ejecutado en un emulador Android y se ha comprobado que
  se pueden iniciar y detener los escaneos BLE sin provocar errores.

- La recepción del sensor físico queda pendiente de validación mediante un
  teléfono Android físico.

---

## General Rules

- **Programming Language:** Java.

- **Function/Method Headers:** Cada función o método debe incluir su diseño
  lógico dentro de un bloque de comentarios delimitado por líneas
  discontinuas.

  Ejemplo:

  ```text
  // --------------------------------------------------
  // major: [N]_2 --> obtenerTipoMedicion() --> N
  //
  // Obtiene el tipo codificado en el campo major.
  // --------------------------------------------------
  ```

- **File Headers:** Cada fichero de código fuente debe incluir una cabecera con:

  ```text
  nombre del fichero
  descripción
  copyright
  fecha
  autor
  aportación
  ```

- **Author:** Las nuevas aportaciones realizadas en este Sprint se documentarán
  con:

  ```text
  Álvaro Ballester Grau
  ```

- **Code Readability:** El código debe ser claro y autoexplicativo. Se evitarán
  comentarios que únicamente repitan lo que ya expresa el código.

- **Separation of Responsibilities:** La interpretación BLE, las comunicaciones
  REST y la interfaz gráfica deben mantenerse separadas.

  No se introducirá lógica de backend ni acceso a base de datos en el teléfono.

- **Compatibility:** Se mantendrá el código proporcionado por los profesores
  siempre que sea compatible y funcional.

  Los cambios necesarios deberán justificarse y documentarse.

- **Automated Testing:** Se generarán tests unitarios o de integración para las
  funciones y métodos críticos.

  Siempre que sea posible, la lógica de decodificación deberá poder probarse
  sin disponer físicamente de la placa.

- **Design Consistency:** Cualquier función nueva deberá aparecer primero en
  este diseño.

  Su implementación deberá conservar el mismo nombre, responsabilidad,
  entradas y salidas lógicas.

- **Repository Consistency:** El diseño almacenado en `doc/phone_design.md`
  deberá mantenerse sincronizado con el código existente en:

  ```text
  src/phone/
  ```