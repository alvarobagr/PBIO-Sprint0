# Phone Component Design

## Component Design

### Objetivo del componente

El componente `phone` corresponde a la aplicación Android encargada de:

```text
recibir anuncios BLE
        |
        v
identificar una trama iBeacon válida
        |
        v
separar UUID / Major / Minor / TxPower
        |
        v
interpretar tipo / contador / valor
        |
        v
mostrar la información en pantalla
        |
        v
generar una Medicion
        |
        v
cliente REST (posteriormente)
```

La aplicación también incorpora una lógica fake para poder probar el sistema
sin depender de la recepción física del sensor.

---

### Tipos lógicos

```text
UUID = [N]_16

PrefijoIBeacon = [N]_9

Major = [N]_2

Minor = [N]_2

TramaBLE = [N]

Medicion=(
    uuid: Text,
    nombre_dispositivo: Text,
    tipo: Text,
    valor: Z,
    contador: N,
    rssi: Z
)
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

La clase únicamente representa y separa campos.

No interpreta el significado lógico de `major` o `minor`.

---

### Clase `Utilidades`

Responsabilidad:

Proporcionar conversiones auxiliares entre texto, bytes, números y UUID.

La clase no mantiene estado.

Todas sus operaciones son estáticas.

```text
                 --------- Utilidades ---------------------

texto: Text
              --> stringToBytes() --> [N]                 --x


uuid: Text
              --> stringToUUID() --> UUID                 --x


uuid: UUID
              --> uuidToString() --> Text                 --x


uuid: UUID
              --> uuidToHexString() --> Text              --x


bytes: [N]
              --> bytesToString() --> Text                --x


mas_significativos: Z,
menos_significativos: Z
              --> dosLongToBytes() --> [N]_16             --x


bytes: [N]
              --> bytesToInt() --> Z                      --x


bytes: [N]
              --> bytesToLong() --> Z                     --x


bytes: [N]
              --> bytesToIntOK() --> Z                    --x


bytes: [N]_2
              --> bytesToUInt16() --> N                   --x


bytes: [N]_2
              --> bytesToInt16ConSigno() --> Z            --x


bytes: [N]
              --> bytesToHexString() --> Text             --x

                 ------------------------------------------
```

---

### bytesToUInt16()

```text
bytes: [N]_2 --> bytesToUInt16() --> N
```

Responsabilidad:

Interpretar exactamente dos bytes como un entero de 16 bits sin signo.

Esta conversión se utiliza para obtener la representación bruta de campos
iBeacon como `major` y `minor`.

Ejemplos:

```text
01 4D --> 333

FF 3E --> 65342
```

---

### bytesToInt16ConSigno()

```text
bytes: [N]_2 --> bytesToInt16ConSigno() --> Z
```

Responsabilidad:

Interpretar exactamente dos bytes como un entero de 16 bits con signo.

Esta operación se utiliza para recuperar correctamente la concentración de O3
almacenada por Arduino dentro del campo `minor`.

Ejemplos:

```text
01 4D --> 333

00 4B --> 75

FF 3E --> -194
```

Los mismos 16 bits:

```text
FF 3E
```

pueden representar:

```text
65342
```

si se interpretan sin signo, o:

```text
-194
```

si se interpretan como `int16`.

---

### Decodificación iBeacon utilizada en Sprint 0

La trama iBeacon contiene:

```text
Prefijo         9 bytes

UUID           16 bytes

Major           2 bytes

Minor           2 bytes

TxPower         1 byte
```

El prefijo esperado es:

```text
02 01 06 1A FF 4C 00 02 15
```

La aplicación comprueba este prefijo antes de interpretar los datos como un
iBeacon válido.

---

### UUID

El UUID utilizado por el proyecto es:

```text
EPSG-GTI-PROY-3A
```

Los 16 bytes ASCII correspondientes son:

```text
45 50 53 47 2D 47 54 49 2D 50 52 4F 59 2D 33 41
```

---

### Codificación del campo Major

El campo `major` contiene dos valores:

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

Los códigos heredados del proyecto son:

```text
11 = CO2

12 = TEMPERATURA

13 = RUIDO

14 = O3
```

Para el sensor actual:

```text
tipo = 14
```

Android obtiene el tipo mediante:

```text
tipo = major[15..8]
```

y el contador mediante:

```text
contador = major[7..0]
```

Ejemplo observado:

```text
Major = 3647

tipo = 14

contador = 63
```

porque:

```text
(14 << 8) + 63 = 3647
```

---

### Codificación del campo Minor

El campo `minor` contiene la concentración de O3 expresada en ppb.

Arduino obtiene este valor mediante:

```text
valor = round(ppm * 1000)
```

y lo codifica utilizando un entero de 16 bits con signo.

Por tanto:

```text
0.333 ppm  --> 333 ppb

0.075 ppm  --> 75 ppb

-0.194 ppm --> -194 ppb
```

El campo iBeacon transporta los 16 bits sin indicar explícitamente si deben
interpretarse con o sin signo.

Por ese motivo Android mantiene dos representaciones:

```text
minor_bruto: N

valor: Z
```

Ejemplo real observado:

```text
Minor bruto = 65342

Valor = -194 ppb
```

Ambos representan los mismos 16 bits:

```text
0xFF3E
```

---

### Contrato Arduino - Android

La comunicación entre ambos componentes queda definida como:

```text
Nombre BLE = "GTI-3A"

UUID = "EPSG-GTI-PROY-3A"

tipo O3 = 14
```

El campo `major` contiene:

```text
major[15..8] = tipo

major[7..0] = contador
```

El campo `minor` contiene:

```text
valor O3 en ppb codificado como int16
```

Ejemplo:

```text
Arduino:

tipo = 14

contador = 63

valor = -194 ppb
```

produce:

```text
Major = 3647

Minor bruto = 65342
```

y Android reconstruye:

```text
tipo = 14

contador = 63

valor = -194 ppb
```

---

### Clase `Medicion`

Responsabilidad:

Representar una medición ya interpretada por el teléfono y preparada para ser
utilizada por las siguientes capas del sistema.

```text
                 ------------ Medicion -----------------
                 |
                 | uuid: Text
                 | nombre_dispositivo: Text
                 | tipo: Text
                 | valor: Z
                 | contador: N
                 | rssi: Z
                 |
                 |
uuid: Text,
nombre_dispositivo: Text,
tipo: Text,
valor: Z,
contador: N,
rssi: Z
              --> Medicion() -->
                 |
                 |
Text          <-- getUuid() <--
                 |
                 |
Text          <-- getNombreDispositivo() <--
                 |
                 |
Text          <-- getTipo() <--
                 |
                 |
Z             <-- getValor() <--
                 |
                 |
N             <-- getContador() <--
                 |
                 |
Z             <-- getRssi() <--
                 |
                 ---------------------------------------
```

La medición no contiene datos propios de la codificación BLE como:

```text
Major

Minor

TxPower
```

Estos campos pertenecen a la recepción Bluetooth.

Las capas posteriores del sistema reciben únicamente la información lógica ya
interpretada.

---

### Clase `LogicaFakeTelefono`

Responsabilidad:

Generar una medición ficticia con el mismo formato lógico que utilizarán las
mediciones reales obtenidas mediante Bluetooth.

La clase no mantiene estado.

```text
                 -------- LogicaFakeTelefono --------

                 | --> crearMedicionFake() --> Medicion --x

                 -------------------------------------
```

---

### crearMedicionFake()

```text
crearMedicionFake() --> Medicion
```

Responsabilidad:

Crear una medición ficticia conocida y reproducible.

La medición utilizada es:

```text
uuid = "EPSG-GTI-PROY-3A"

nombre_dispositivo = "GTI-3A"

tipo = "O3"

valor = 333

contador = 30

rssi = -68
```

La lógica fake no realiza:

```text
Bluetooth

REST

base de datos
```

Su única responsabilidad es generar una `Medicion`.

---

### Flujo de la lógica fake

```text
LogicaFakeTelefono
        |
        v
crearMedicionFake()
        |
        v
Medicion
        |
        +------------------> interfaz Android
        |
        +------------------> cliente REST
                              posteriormente
```

La medición fake utiliza la misma estructura que utilizará una medición
procedente del sensor real:

```text
Sensor BLE ------\
                  \
                   --> Medicion --> REST
                  /
Lógica Fake -----/
```

---

### Clase `MainActivity`

Responsabilidad:

Coordinar:

```text
inicialización Bluetooth

gestión de permisos

escaneo BLE

recepción de resultados

validación básica de iBeacon

interpretación de la trama

actualización de la interfaz gráfica

ejecución de la lógica fake desde la interfaz
```

La clase utiliza:

```text
TramaIBeacon

Utilidades

Medicion

LogicaFakeTelefono
```

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
nombre_dispositivo: Text,
uuid: Text,
major: N,
minor: N,
tipo: N,
contador: N,
valor: Z,
rssi: Z,
tx_power: Z
              --> actualizarDatosSensor()
                 |
                 |
medicion: Medicion
              --> mostrarMedicionFake()
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
                 | --> botonCargarMedicionFakePulsado()
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
                 ------------------------------------------------
```

---

### inicializarBlueTooth()

```text
inicializarBlueTooth()
```

Responsabilidad:

Inicializar el sistema Bluetooth del teléfono, comprobar los permisos
necesarios y obtener el escáner BLE.

Para Android 12 y posteriores se utilizan:

```text
BLUETOOTH_SCAN

BLUETOOTH_CONNECT

ACCESS_FINE_LOCATION

ACCESS_COARSE_LOCATION
```

Los permisos:

```text
BLUETOOTH_SCAN

BLUETOOTH_CONNECT
```

corresponden al grupo de permisos que Android muestra al usuario como
dispositivos cercanos.

Para la ubicación se considera suficiente que esté concedido:

```text
ACCESS_FINE_LOCATION
```

o:

```text
ACCESS_COARSE_LOCATION
```

Para Android 11 y anteriores se utiliza:

```text
ACCESS_FINE_LOCATION
```

para permitir el escaneo BLE.

Si Bluetooth está desactivado, la aplicación solicita al usuario que lo active.

Durante las pruebas físicas también se comprobó que el servicio general de
ubicación del teléfono debe estar activado en el dispositivo utilizado para que
el escaneo detecte correctamente el beacon.

La aplicación no activa automáticamente el servicio de ubicación.

---

### buscarTodosLosDispositivosBTLE()

```text
buscarTodosLosDispositivosBTLE()
```

Responsabilidad:

Iniciar un escaneo BLE sin filtros.

Los resultados obtenidos se procesan mediante:

```text
callbackDelEscaneo
```

Antes de iniciar el escaneo se comprueba que el escáner esté disponible y que
los permisos necesarios estén concedidos.

---

### buscarEsteDispositivoBTLE()

```text
dispositivo_buscado: Text
    -->
buscarEsteDispositivoBTLE()
```

Responsabilidad:

Iniciar un escaneo BLE utilizando un filtro de nombre.

El dispositivo utilizado en Sprint 0 es:

```text
GTI-3A
```

El filtro se realiza mediante el nombre BLE del dispositivo.

---

### mostrarInformacionDispositivoBTLE()

```text
resultado: ScanResult
    -->
mostrarInformacionDispositivoBTLE()
```

Responsabilidad:

Procesar un resultado BLE recibido.

El flujo es:

```text
ScanResult
    |
    v
obtener dispositivo
    |
    v
obtener nombre
    |
    v
obtener RSSI
    |
    v
obtener ScanRecord
    |
    v
obtener bytes
    |
    v
comprobar longitud >= 30
    |
    v
comprobar prefijo iBeacon
    |
    v
TramaIBeacon
    |
    +--> UUID
    |
    +--> Major
    |
    +--> Minor
    |
    +--> TxPower
    |
    v
Utilidades
    |
    +--> Major sin signo
    |
    +--> Minor bruto sin signo
    |
    +--> Minor como int16 con signo
    |
    v
tipo / contador / valor
    |
    v
actualizarDatosSensor()
```

La operación no almacena datos.

No realiza comunicaciones REST.

---

### Interpretación de Major en Android

Android obtiene `major` mediante:

```text
bytesToUInt16()
```

Posteriormente:

```text
tipo = (major >> 8) & 0xFF

contador = major & 0xFF
```

Para el sensor O3:

```text
tipo = 14
```

---

### Interpretación de Minor en Android

Android mantiene:

```text
minor = representación bruta sin signo
```

mediante:

```text
bytesToUInt16()
```

y obtiene el valor lógico mediante:

```text
bytesToInt16ConSigno()
```

Por tanto:

```text
0x014D
    |
    +--> Minor = 333
    |
    +--> Valor = 333
```

mientras que:

```text
0xFF3E
    |
    +--> Minor = 65342
    |
    +--> Valor = -194
```

---

### actualizarDatosSensor()

```text
nombre_dispositivo: Text,
uuid: Text,
major: N,
minor: N,
tipo: N,
contador: N,
valor: Z,
rssi: Z,
tx_power: Z
    -->
actualizarDatosSensor()
```

Responsabilidad:

Mostrar en la interfaz gráfica la información correspondiente al último anuncio
BLE válido recibido.

La interfaz muestra:

```text
Estado

Dispositivo

UUID

Major

Minor

Tipo

Contador

Valor

RSSI

TxPower
```

Para O3 se presenta:

```text
Tipo: O3 (14)
```

y el valor incluye su unidad:

```text
Valor: -194 ppb
```

La operación únicamente presenta información.

No modifica la medición.

No almacena información.

No realiza comunicaciones REST.

---

### detenerBusquedaDispositivosBTLE()

```text
detenerBusquedaDispositivosBTLE()
```

Responsabilidad:

Detener el escaneo BLE activo utilizando el mismo `ScanCallback` empleado para
iniciarlo.

---

### botonBuscarDispositivosBTLEPulsado()

```text
botonBuscarDispositivosBTLEPulsado()
```

Responsabilidad:

Solicitar el inicio de un escaneo BLE sin filtro.

El parámetro `View` utilizado por Android se considera un detalle del framework
y no forma parte de la firma lógica.

---

### botonBuscarNuestroDispositivoBTLEPulsado()

```text
botonBuscarNuestroDispositivoBTLEPulsado()
```

Responsabilidad:

Solicitar la búsqueda del dispositivo:

```text
GTI-3A
```

El parámetro `View` utilizado por Android se omite del diseño lógico.

---

### botonDetenerBusquedaDispositivosBTLEPulsado()

```text
botonDetenerBusquedaDispositivosBTLEPulsado()
```

Responsabilidad:

Solicitar la detención del escaneo BLE activo.

---

### botonCargarMedicionFakePulsado()

```text
botonCargarMedicionFakePulsado()
```

Responsabilidad:

Solicitar una medición a:

```text
LogicaFakeTelefono.crearMedicionFake()
```

y mostrarla en la interfaz.

El flujo es:

```text
Cargar medición fake
        |
        v
botonCargarMedicionFakePulsado()
        |
        v
LogicaFakeTelefono.crearMedicionFake()
        |
        v
Medicion
        |
        v
mostrarMedicionFake()
```

---

### mostrarMedicionFake()

```text
medicion: Medicion --> mostrarMedicionFake()
```

Responsabilidad:

Mostrar una medición ficticia en la interfaz.

Los datos pertenecientes únicamente al protocolo BLE se muestran como:

```text
Major: No aplica

Minor: No aplica

TxPower: No aplica
```

Los datos de la medición se muestran normalmente:

```text
Dispositivo: GTI-3A

UUID: EPSG-GTI-PROY-3A

Tipo: O3

Contador: 30

Valor: 333 ppb

RSSI: -68 dBm
```

---

### onCreate()

```text
onCreate()
```

Responsabilidad:

Inicializar la pantalla principal y comenzar la inicialización Bluetooth cuando
se crea la actividad.

El `Bundle` proporcionado por Android es un detalle del framework y se omite en
la firma lógica.

---

### onRequestPermissionsResult()

```text
request_code: N,
resultados: [Z]
    -->
onRequestPermissionsResult()
```

Responsabilidad:

Procesar el resultado de la solicitud de permisos necesarios para Bluetooth y
ubicación.

En Android 12 y posteriores los permisos se consideran correctos cuando:

```text
BLUETOOTH_SCAN = concedido

BLUETOOTH_CONNECT = concedido
```

y además se ha concedido al menos uno de:

```text
ACCESS_FINE_LOCATION

ACCESS_COARSE_LOCATION
```

Por tanto:

```text
BLUETOOTH_SCAN
        &&
BLUETOOTH_CONNECT
        &&
(
    ACCESS_FINE_LOCATION
            ||
    ACCESS_COARSE_LOCATION
)
```

En Android 11 y anteriores se requiere:

```text
ACCESS_FINE_LOCATION
```

Cuando los permisos necesarios se encuentran disponibles:

```text
onRequestPermissionsResult()
        |
        v
inicializarBlueTooth()
```

Si no se han concedido, la aplicación informa al usuario mediante un mensaje.

Los parámetros adicionales utilizados únicamente por el framework Android no
forman parte de la firma lógica.

---

### Interfaz gráfica Android

La interfaz conserva los tres controles principales del código proporcionado:

```text
Buscar Dispositivos BTLE

Detener búsqueda Dispositivos BTLE

Buscar nuestro dispositivo BTLE
```

y añade:

```text
Cargar medición fake
```

La zona de información muestra:

```text
Estado

Dispositivo

UUID

Major

Minor

Tipo

Contador

Valor

RSSI

TxPower
```

Antes de recibir datos:

```text
Estado: Esperando sensor...
```

Durante una búsqueda filtrada:

```text
Estado: Buscando GTI-3A...
```

Cuando se recibe un iBeacon válido:

```text
Estado: Sensor detectado
```

---

### Ejemplo de pantalla con medida real negativa

Ejemplo basado en un valor negativo observado durante las pruebas:

```text
------------------------------------------------

PBIO - Sensor BLE


[ Buscar Dispositivos BTLE ]

[ Detener búsqueda Dispositivos BTLE ]

[ Buscar nuestro dispositivo BTLE ]

[ Cargar medición fake ]


Estado: Sensor detectado

Dispositivo: GTI-3A

UUID: EPSG-GTI-PROY-3A

Major: 3647

Minor: 65342

Tipo: O3 (14)

Contador: 63

Valor: -194 ppb

RSSI: -29 dBm

TxPower: -53 dBm

------------------------------------------------
```

El RSSI de recepción cambia dependiendo de la distancia y las condiciones del
entorno.

---

### Ejemplo de recepción mediante la aplicación PBIO

Durante la prueba física con la propia aplicación desarrollada se obtuvo:

```text
Estado: Sensor detectado

Dispositivo: GTI-3A

UUID: EPSG-GTI-PROY-3A

Major: 3628

Minor: 1239

Tipo: O3 (14)

Contador: 44

Valor: 1239 ppb

RSSI: -54 dBm

TxPower: -53 dBm
```

La codificación de `Major` coincide con el contrato definido:

```text
14 * 256 + 44 = 3628
```

Por tanto:

```text
tipo = 14

contador = 44
```

El valor recibido mediante `Minor` fue:

```text
1239
```

y se mostró correctamente como:

```text
1239 ppb
```

---

### Diferencia entre RSSI y TxPower

`RSSI` representa la potencia con la que el teléfono recibe el anuncio en ese
momento.

Ejemplo:

```text
RSSI = -54 dBm
```

`TxPower` representa el valor de referencia incluido dentro del iBeacon.

En el emisor actual:

```text
TxPower = -53 dBm
```

Son valores diferentes y no deben confundirse.

---

### Flujo completo BLE - Android

```text
Arduino
   |
   v
iBeacon
   |
   v
BluetoothLeScanner
   |
   v
ScanResult
   |
   v
ScanRecord
   |
   v
bytes
   |
   v
validación iBeacon
   |
   v
TramaIBeacon
   |
   +--> UUID
   |
   +--> Major
   |
   +--> Minor
   |
   +--> TxPower
   |
   v
Utilidades
   |
   +--> bytesToUInt16(Major)
   |
   +--> bytesToUInt16(Minor)
   |
   +--> bytesToInt16ConSigno(Minor)
   |
   v
tipo
contador
valor
   |
   v
MainActivity
   |
   v
interfaz
```

---

### Pruebas automáticas

#### `TramaIBeaconTest`

Existe un test automático para verificar la separación e interpretación básica
de una trama iBeacon.

```text
comprobarTramaIBeacon()
```

Comprueba:

```text
UUID

Major

Minor

TxPower

tipo

contador

valor
```

La prueba utiliza una trama ficticia conocida.

Esto permite validar la lógica de separación de la trama sin disponer
físicamente del beacon.

---

#### `LogicaFakeTelefonoTest`

Existe un test automático para comprobar que:

```text
LogicaFakeTelefono.crearMedicionFake()
```

genera exactamente:

```text
uuid = "EPSG-GTI-PROY-3A"

nombre_dispositivo = "GTI-3A"

tipo = "O3"

valor = 333

contador = 30

rssi = -68
```

---

#### `UtilidadesTest`

Existe un test automático específico para comprobar la interpretación del
campo `minor`.

Se comprueba un valor positivo:

```text
bytes = 0x014D

bytesToUInt16() = 333

bytesToInt16ConSigno() = 333
```

y un valor negativo:

```text
bytes = 0xFF3E

bytesToUInt16() = 65342

bytesToInt16ConSigno() = -194
```

El segundo caso reproduce una situación observada físicamente en el beacon
real.

Los tests Android han sido ejecutados mediante:

```text
./gradlew test
```

obteniendo:

```text
BUILD SUCCESSFUL
```

---

### Pruebas en emulador

La aplicación se ha ejecutado en un emulador Android.

Se ha comprobado:

```text
inicio de la aplicación

inicialización Bluetooth

inicio de escaneo BLE

detención del escaneo

escaneo filtrado para GTI-3A

actualización del estado de la interfaz

visualización de una medición fake

ausencia de cierres inesperados
```

El emulador no se utiliza como prueba de recepción física del beacon.

---

### Comprobación física del beacon

El beacon real ha sido comprobado físicamente con un teléfono Android.

Inicialmente se utilizó una aplicación externa de análisis BLE para verificar
directamente la información emitida por la placa.

En esa prueba se observó:

```text
Nombre = GTI-3A

Tipo = iBeacon

Company = 0x004C

UUID = EPSG-GTI-PROY-3A

Major = 3647

Minor = 65342

RSSI at 1m = -53 dBm
```

El `Major` observado permitió comprobar:

```text
tipo = 14

contador = 63
```

porque:

```text
14 * 256 + 63 = 3647
```

El `Minor` observado permitió comprobar la necesidad de distinguir entre:

```text
Minor bruto = 65342

Valor con signo = -194 ppb
```

Posteriormente se realizó una segunda prueba utilizando directamente la propia
aplicación PBIO desarrollada para Sprint 0.

La aplicación detectó físicamente el beacon y mostró:

```text
Estado = Sensor detectado

Dispositivo = GTI-3A

UUID = EPSG-GTI-PROY-3A

Major = 3628

Minor = 1239

Tipo = O3 (14)

Contador = 44

Valor = 1239 ppb

RSSI = -54 dBm

TxPower = -53 dBm
```

La interpretación del `Major` fue correcta:

```text
14 * 256 + 44 = 3628
```

por lo que:

```text
tipo = 14

contador = 44
```

El campo `Minor` fue recibido con el valor:

```text
1239
```

y la aplicación lo interpretó correctamente como:

```text
1239 ppb
```

La prueba física confirma el flujo:

```text
Arduino
    |
    v
iBeacon real
    |
    v
teléfono Android
    |
    v
aplicación PBIO
    |
    v
TramaIBeacon
    |
    v
Utilidades
    |
    v
tipo / contador / valor
    |
    v
interfaz gráfica
```

Por tanto, el contrato de comunicación Arduino-Android ha sido validado
físicamente utilizando tanto una aplicación externa de análisis BLE como la
propia aplicación PBIO.

Durante la prueba también se comprobó que, en el teléfono Android utilizado,
además de conceder los permisos necesarios, el servicio general de ubicación
del dispositivo debe encontrarse activado para que el escaneo BLE detecte el
beacon correctamente.

Esta activación del servicio de ubicación se realiza actualmente por el usuario
desde el sistema operativo y no forma parte de la lógica automática de la
aplicación.

---

## Design Clarifications

- El código de `TramaIBeacon` y gran parte de `Utilidades` procede del código
  proporcionado por los profesores.

- Se conserva su estructura y sus funciones originales siempre que es posible.

- El `package` se adapta al proyecto actual:

  ```text
  com.example.abalgra1.pbio
  ```

- El proyecto Android utiliza:

  ```text
  Minimum SDK API 26
  ```

- La aplicación incorpora las adaptaciones necesarias para versiones modernas
  de Android.

- Para Android 12 y posteriores se utilizan los permisos:

  ```text
  BLUETOOTH_SCAN

  BLUETOOTH_CONNECT

  ACCESS_FINE_LOCATION

  ACCESS_COARSE_LOCATION
  ```

- `BLUETOOTH_SCAN` y `BLUETOOTH_CONNECT` forman parte del grupo de permisos que
  Android presenta al usuario como dispositivos cercanos.

- Para la ubicación en Android 12 y posteriores se acepta:

  ```text
  ACCESS_FINE_LOCATION
  ```

  o:

  ```text
  ACCESS_COARSE_LOCATION
  ```

- Para Android 11 y anteriores se utiliza:

  ```text
  ACCESS_FINE_LOCATION
  ```

  para permitir el escaneo BLE.

- La aplicación no activa Bluetooth silenciosamente.

- Si Bluetooth está desactivado se solicita al usuario que lo active.

- Durante las pruebas físicas se comprobó que en el teléfono utilizado el
  servicio general de ubicación debe encontrarse activado para detectar el
  beacon.

- La aplicación no comprueba ni activa automáticamente ese servicio general de
  ubicación.

- Antes de acceder al contenido de un `ScanRecord` se comprueba que no sea nulo.

- Antes de construir `TramaIBeacon` se comprueba que la longitud recibida sea
  suficiente.

- Se comprueba el prefijo iBeacon esperado antes de interpretar una trama.

- `TramaIBeacon` únicamente representa y separa campos.

- `Utilidades` únicamente proporciona operaciones auxiliares de conversión.

- `MainActivity` coordina permisos, escaneo, interpretación y presentación.

- `Medicion` representa la información lógica que utilizarán las siguientes
  capas del sistema.

- `LogicaFakeTelefono` genera datos de prueba y no realiza comunicaciones.

- El nombre BLE definitivo utilizado en Sprint 0 es:

  ```text
  GTI-3A
  ```

- El UUID definitivo es:

  ```text
  EPSG-GTI-PROY-3A
  ```

- El identificador de O3 es:

  ```text
  14
  ```

- La codificación definitiva de `major` es:

  ```text
  byte alto -> tipo

  byte bajo -> contador
  ```

- Esta codificación ha sido contrastada físicamente con el beacon real.

- El campo `minor` transporta un entero de 16 bits con signo.

- `bytesToUInt16()` se añade específicamente para interpretar los 16 bits como
  valor bruto sin signo.

- `bytesToInt16ConSigno()` se añade específicamente para recuperar el valor de
  O3 con signo.

- Las funciones originales:

  ```text
  bytesToInt()

  bytesToIntOK()
  ```

  se mantienen para conservar la máxima fidelidad posible al código
  proporcionado.

- `bytesToIntOK()` se conserva sin modificar su comportamiento original.

- `Major` y `Minor` se mantienen visibles en pantalla durante Sprint 0 para
  facilitar la verificación y depuración del protocolo.

- En la interfaz:

  ```text
  Minor
  ```

  representa el valor bruto de 16 bits.

- En la interfaz:

  ```text
  Valor
  ```

  representa la concentración de O3 ya interpretada y expresada en ppb.

- El RSSI recibido por el teléfono y el `TxPower` del iBeacon son valores
  diferentes.

- La lógica fake no sustituye a la recepción real.

- La lógica fake permite desarrollar y probar las capas posteriores de forma
  reproducible.

- La recepción del beacon real ha sido comprobada mediante un teléfono Android
  físico utilizando una aplicación externa de análisis BLE.

- La recepción del beacon también ha sido comprobada físicamente mediante la
  propia aplicación PBIO.

- La aplicación PBIO ha detectado correctamente:

  ```text
  GTI-3A
  ```

  y ha mostrado:

  ```text
  UUID

  Major

  Minor

  tipo O3

  contador

  valor

  RSSI

  TxPower
  ```

- Por tanto, la recepción física Arduino-Android ya se considera validada para
  Sprint 0.

- El futuro cliente REST recibirá una `Medicion` ya interpretada.

- El cliente REST no deberá conocer:

  ```text
  Major

  Minor

  estructura iBeacon

  representación int16
  ```

- La separación de responsabilidades queda definida como:

  ```text
  Arduino
      |
      v
  codificación BLE
      |
      v
  Android
      |
      v
  interpretación
      |
      v
  Medicion
      |
      v
  REST
  ```

---

## General Rules

- **Programming Language:** Java.

- **Function/Method Headers:** Cada función o método debe incluir su diseño
  lógico en un bloque de comentarios delimitado por líneas discontinuas.

  Ejemplo:

  ```text
  // --------------------------------------------------
  // bytes: [N]_2 --> bytesToUInt16() --> N
  //
  // Interpreta dos bytes como entero sin signo.
  // --------------------------------------------------
  ```

- **File Headers:** Cada fichero de código fuente debe incluir:

  ```text
  nombre del fichero

  descripción

  copyright

  fecha

  autor

  aportación
  ```

- **Author:** Las nuevas aportaciones realizadas en Sprint 0 se documentan con:

  ```text
  Álvaro Ballester Grau
  ```

- **Code Readability:** El código debe ser claro y autoexplicativo.

  Los comentarios deben explicar decisiones, contratos o comportamientos que
  no resulten evidentes únicamente leyendo el código.

- **Separation of Responsibilities:** La interpretación BLE, el modelo lógico
  de medición, las futuras comunicaciones REST y el backend deben mantenerse
  separados.

- `TramaIBeacon` no realiza lógica REST.

- `Utilidades` no mantiene estado.

- `LogicaFakeTelefono` no realiza comunicaciones externas.

- `MainActivity` no accede a la base de datos.

- `MainActivity` tampoco realiza actualmente comunicaciones REST.

- **Compatibility:** Se conserva el código proporcionado por los profesores
  siempre que sea compatible y funcional.

- Toda modificación necesaria debe estar justificada y documentada.

- **Automated Testing:** Se deben mantener tests automáticos para los métodos
  críticos.

  Actualmente existen pruebas para:

  ```text
  separación de trama iBeacon

  interpretación de Major

  lógica fake

  Minor positivo

  Minor negativo
  ```

- Los tests deben poder ejecutarse sin disponer físicamente del sensor siempre
  que la operación comprobada no dependa directamente del hardware.

- Las pruebas que dependan del hardware deben documentarse por separado como
  pruebas físicas.

- **Design Consistency:** Cualquier operación nueva debe aparecer en este diseño.

- La implementación debe conservar:

  ```text
  nombre

  entradas

  salidas

  responsabilidad
  ```

  descritos en el diseño.

- **Repository Consistency:** Este diseño:

  ```text
  doc/phone_design.md
  ```

  debe permanecer sincronizado con:

  ```text
  src/phone/
  ```

- Los cambios en el protocolo Arduino-Android deben actualizarse tanto en:

  ```text
  doc/arduino_design.md
  ```

  como en:

  ```text
  doc/phone_design.md
  ```

  antes de considerarse definitivos.

- Las futuras incorporaciones del cliente REST deberán añadirse al diseño antes
  de modificar la implementación del componente Android.