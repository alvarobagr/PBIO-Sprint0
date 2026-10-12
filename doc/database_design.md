# Database Component Design

## Component Design

### Objetivo del componente

El componente `database` es responsable de almacenar de forma persistente las
mediciones recibidas por el sistema.

La base de datos recibe únicamente mediciones ya interpretadas por las capas
anteriores.

Por tanto, la base de datos no conoce detalles propios del protocolo BLE como:

```text
Major

Minor

TxPower

estructura iBeacon

representación int16
```

El flujo previsto es:

```text
Arduino
    |
    v
Bluetooth / iBeacon
    |
    v
Android
    |
    v
Medicion
    |
    v
REST
    |
    v
lógica de negocio
    |
    v
base de datos
```

---

### Alcance de Sprint 0

Para Sprint 0 se utiliza una estructura deliberadamente simple basada en una
única tabla:

```text
MEDICION
```

La tabla almacena tanto la identificación del dispositivo como los datos de la
medición.

No se crea una tabla independiente para dispositivos porque no es necesaria
para cumplir los requisitos actuales del Sprint 0 y añadiría complejidad sin
aportar funcionalidad necesaria para la demostración.

---

### Tipo lógico de una medición recibida

Las capas anteriores del sistema utilizan:

```text
MedicionEntrada=(

    uuid: Text,

    nombre_dispositivo: Text,

    tipo: Text,

    valor: Z,

    contador: N,

    rssi: Z
)
```

Una vez almacenada en la base de datos, la medición queda representada como:

```text
Medicion=(

    id: N,

    uuid: Text,

    nombre_dispositivo: Text,

    tipo: Text,

    valor: Z,

    contador: N,

    rssi: Z,

    fecha_hora: Text
)
```

---

### Tabla `medicion`

```text
                 ---------------- MEDICION ----------------

                 | id_medicion: N
                 | uuid: Text
                 | nombre_dispositivo: Text
                 | tipo: Text
                 | valor: Z
                 | contador: N
                 | rssi: Z
                 | fecha_hora: Text

                 ------------------------------------------
```

---

### Campo `id_medicion`

```text
id_medicion: N
```

Responsabilidad:

Identificar de forma única cada medición almacenada.

Características:

```text
entero positivo

clave primaria

autoincremental
```

El identificador es generado por la base de datos.

No es enviado por Android.

---

### Campo `uuid`

```text
uuid: Text
```

Responsabilidad:

Identificar el dispositivo que produjo la medición.

Para el dispositivo utilizado durante Sprint 0:

```text
EPSG-GTI-PROY-3A
```

El UUID ya llega interpretado desde Android.

La base de datos no debe reconstruirlo a partir de bytes BLE.

---

### Campo `nombre_dispositivo`

```text
nombre_dispositivo: Text
```

Responsabilidad:

Guardar el nombre BLE lógico asociado al dispositivo.

Para Sprint 0:

```text
GTI-3A
```

---

### Campo `tipo`

```text
tipo: Text
```

Responsabilidad:

Indicar el tipo lógico de medición almacenada.

Para el sensor utilizado durante Sprint 0:

```text
O3
```

La base de datos almacena el significado lógico:

```text
O3
```

y no el identificador BLE:

```text
14
```

La conversión:

```text
14 --> O3
```

es responsabilidad de Android.

---

### Campo `valor`

```text
valor: Z
```

Responsabilidad:

Almacenar el valor de la medición.

Para O3 la unidad utilizada durante Sprint 0 es:

```text
ppb
```

El campo debe permitir tanto valores positivos como negativos.

Ejemplos válidos:

```text
333

1239

75

-194
```

El valor ya llega interpretado desde Android.

La base de datos no conoce la representación `int16` utilizada en el campo
`Minor` del iBeacon.

---

### Campo `contador`

```text
contador: N
```

Responsabilidad:

Almacenar el contador enviado por el nodo sensor.

El contador se codifica en Arduino utilizando 8 bits.

Por tanto:

```text
0 <= contador <= 255
```

El contador puede volver a cero después de alcanzar su valor máximo.

Por este motivo:

```text
contador
```

no debe utilizarse como identificador único de una medición.

---

### Campo `rssi`

```text
rssi: Z
```

Responsabilidad:

Almacenar la intensidad de señal con la que Android recibió el anuncio BLE.

Ejemplo:

```text
-54
```

La unidad utilizada es:

```text
dBm
```

El RSSI pertenece a la recepción realizada por el teléfono y puede cambiar
entre anuncios incluso cuando la medición de O3 sea la misma.

---

### Campo `fecha_hora`

```text
fecha_hora: Text
```

Responsabilidad:

Registrar el momento en que la medición es almacenada en el servidor.

La fecha y hora son generadas por la base de datos.

Android no necesita enviarlas.

Esto evita depender del reloj del teléfono o del nodo sensor.

---

### Correspondencia Android - Base de datos

Una medición Android:

```text
Medicion(

    uuid = "EPSG-GTI-PROY-3A",

    nombre_dispositivo = "GTI-3A",

    tipo = "O3",

    valor = 333,

    contador = 30,

    rssi = -68
)
```

se almacena como:

```text
MEDICION

id_medicion = generado automáticamente

uuid = "EPSG-GTI-PROY-3A"

nombre_dispositivo = "GTI-3A"

tipo = "O3"

valor = 333

contador = 30

rssi = -68

fecha_hora = generada automáticamente
```

---

### Ejemplo con una medición física

Durante las pruebas físicas Android recibió, por ejemplo:

```text
uuid = "EPSG-GTI-PROY-3A"

nombre_dispositivo = "GTI-3A"

tipo = "O3"

valor = 1239

contador = 44

rssi = -54
```

La base de datos almacenará esos valores sin reinterpretarlos.

Por tanto:

```text
Arduino
    |
    | valor O3
    v
Android
    |
    | Medicion ya interpretada
    v
Backend
    |
    | validación
    v
Base de datos
```

---

### Correspondencia con SQL

La implementación SQL utilizará los siguientes tipos:

```text
id_medicion
    BIGINT UNSIGNED
    PRIMARY KEY
    AUTO_INCREMENT


uuid
    VARCHAR(36)
    NOT NULL


nombre_dispositivo
    VARCHAR(100)
    NOT NULL


tipo
    VARCHAR(30)
    NOT NULL


valor
    INT
    NOT NULL


contador
    TINYINT UNSIGNED
    NOT NULL


rssi
    SMALLINT
    NOT NULL


fecha_hora
    DATETIME
    NOT NULL
    DEFAULT CURRENT_TIMESTAMP
```

---

### Estructura SQL esperada

La tabla queda conceptualmente definida como:

```text
+--------------------+----------------------+------------------+
| Campo              | Tipo SQL             | Restricción      |
+--------------------+----------------------+------------------+
| id_medicion        | BIGINT UNSIGNED      | PK, AUTO_INCREMENT|
| uuid               | VARCHAR(36)          | NOT NULL         |
| nombre_dispositivo | VARCHAR(100)         | NOT NULL         |
| tipo               | VARCHAR(30)          | NOT NULL         |
| valor              | INT                  | NOT NULL         |
| contador           | TINYINT UNSIGNED     | NOT NULL         |
| rssi               | SMALLINT             | NOT NULL         |
| fecha_hora         | DATETIME             | NOT NULL         |
+--------------------+----------------------+------------------+
```

`fecha_hora` utiliza:

```text
DEFAULT CURRENT_TIMESTAMP
```

---

### Clave primaria

La única clave primaria es:

```text
id_medicion
```

No se utiliza como clave:

```text
uuid
```

porque un mismo dispositivo produce muchas mediciones.

Tampoco se utiliza:

```text
contador
```

porque el contador se repite cuando vuelve a cero.

Tampoco se utiliza la combinación:

```text
uuid + contador
```

porque después de cada ciclo completo del contador volvería a aparecer la misma
combinación.

---

### Restricciones lógicas

Antes de almacenar una medición, la lógica de negocio deberá comprobar:

```text
uuid no vacío

nombre_dispositivo no vacío

tipo válido

valor entero

0 <= contador <= 255

rssi entero
```

Para Sprint 0 el tipo válido necesario es:

```text
O3
```

La validación pertenece principalmente a la lógica de negocio.

La base de datos proporciona además las restricciones correspondientes a los
tipos y campos `NOT NULL`.

---

### Inserción de una medición

Conceptualmente:

```text
MedicionEntrada
        |
        v
validación
        |
        v
INSERT
        |
        v
MEDICION
```

Los campos introducidos son:

```text
uuid

nombre_dispositivo

tipo

valor

contador

rssi
```

Los campos:

```text
id_medicion

fecha_hora
```

son generados automáticamente por la base de datos.

---

### Obtención de la última medición

La aplicación web necesitará obtener la última medición almacenada.

Conceptualmente:

```text
MEDICION
    |
    v
ordenar por fecha_hora descendente
    |
    v
primera medición
```

Cuando sea necesario distinguir tipos de sensor:

```text
tipo
    |
    v
filtrar
    |
    v
ordenar por fecha_hora descendente
    |
    v
primera medición
```

Para Sprint 0:

```text
tipo = "O3"
```

---

### Consulta de mediciones por dispositivo

Las mediciones de un dispositivo pueden localizarse mediante:

```text
uuid
```

Ejemplo:

```text
EPSG-GTI-PROY-3A
```

Conceptualmente:

```text
uuid
    |
    v
MEDICION
    |
    v
mediciones correspondientes al dispositivo
```

---

### Consulta de varias mediciones

Para evitar devolver un número ilimitado de registros, las consultas que
devuelvan listas utilizarán un límite.

Conceptualmente:

```text
limit: N
    |
    v
listar mediciones
```

El límite deberá validarse antes de ejecutar la consulta.

---

### Pruebas de base de datos

La implementación deberá disponer de pruebas reproducibles.

Como mínimo se comprobará:

```text
creación correcta de la tabla

inserción de una medición válida

lectura de la medición insertada

almacenamiento de un valor positivo

almacenamiento de un valor negativo

almacenamiento del contador

almacenamiento del RSSI

generación automática de id_medicion

generación automática de fecha_hora

obtención de la última medición
```

Ejemplo de medición conocida para pruebas:

```text
uuid = "EPSG-GTI-PROY-3A"

nombre_dispositivo = "GTI-3A"

tipo = "O3"

valor = 333

contador = 30

rssi = -68
```

También deberá probarse un valor negativo:

```text
valor = -194
```

para confirmar que la base de datos no trata la concentración como un entero
sin signo.

---

### Flujo esperado durante la demostración

```text
Arduino
    |
    | medición ficticia
    v
Android
    |
    | Medicion
    v
REST
    |
    v
lógica de negocio
    |
    v
INSERT
    |
    v
MEDICION
    |
    v
SELECT última medición
    |
    v
Web
```

El valor mostrado en la web deberá coincidir con el valor introducido
originalmente en Arduino.

Ejemplo:

```text
Arduino = 333 ppb
        |
        v
Android = 333 ppb
        |
        v
Base de datos = 333
        |
        v
Web = 333 ppb
```

---

## Design Clarifications

- La base de datos utilizada durante Sprint 0 contiene una única tabla:

  ```text
  MEDICION
  ```

- Se evita añadir una tabla independiente `DISPOSITIVO` porque no es necesaria
  para la funcionalidad requerida actualmente.

- Si en fases posteriores fuera necesario almacenar información adicional sobre
  los dispositivos, la estructura podrá normalizarse añadiendo una tabla
  específica.

- La estructura actual prioriza simplicidad, trazabilidad y facilidad de prueba
  para Sprint 0.

- La base de datos no recibe datos BLE sin interpretar.

- La interpretación de:

  ```text
  Major

  Minor

  tipo numérico BLE

  int16
  ```

  pertenece al componente Android.

- La base de datos recibe:

  ```text
  uuid

  nombre_dispositivo

  tipo

  valor

  contador

  rssi
  ```

- El valor de O3 se almacena en:

  ```text
  ppb
  ```

- `valor` utiliza un tipo con signo porque una medición puede ser negativa.

- `contador` utiliza un rango lógico:

  ```text
  0..255
  ```

- El contador no es único.

- `id_medicion` es generado por la base de datos.

- `fecha_hora` es generada por el servidor/base de datos.

- Android no debe proporcionar `id_medicion`.

- Android no debe proporcionar `fecha_hora`.

- Para Sprint 0:

  ```text
  UUID = EPSG-GTI-PROY-3A

  nombre_dispositivo = GTI-3A

  tipo = O3
  ```

- La lógica de negocio será responsable de validar los datos antes de
  almacenarlos.

- El servidor REST no ejecutará directamente lógica de validación propia de la
  base de datos.

- La base de datos se limita a almacenamiento y consulta persistente.

- La estructura deberá permitir guardar tanto mediciones reales como
  mediciones generadas mediante la lógica fake.

- Una medición fake y una medición real tienen exactamente la misma estructura
  lógica una vez alcanzan el backend.

---

## General Rules

- **Database Engine:** MySQL o MariaDB compatible.

- **Naming:** Los nombres de tablas y columnas se mantendrán en minúsculas en
  la implementación SQL.

- La tabla utilizada será:

  ```text
  medicion
  ```

- **Primary Key:** Toda medición tendrá un identificador interno independiente
  de los datos recibidos.

- **Server-generated fields:** Los campos:

  ```text
  id_medicion

  fecha_hora
  ```

  no se recibirán desde clientes externos.

- **Signed values:** Los campos que puedan contener valores negativos no deben
  utilizar tipos `UNSIGNED`.

- **Counter:** `contador` debe admitir únicamente el rango representable por
  los 8 bits utilizados en Arduino:

  ```text
  0..255
  ```

- **Separation of Responsibilities:** La base de datos no realiza interpretación
  de protocolos BLE ni transformación de unidades.

- **Validation:** La validación semántica deberá realizarse antes de ejecutar
  operaciones de escritura en la base de datos.

- **Automatic Testing:** La creación, escritura y lectura de la base de datos
  deberán poder probarse de forma reproducible.

- **Design Consistency:** La implementación de:

  ```text
  src/database/
  ```

  deberá mantenerse sincronizada con:

  ```text
  doc/database_design.md
  ```

- Cualquier modificación futura de la estructura deberá actualizar primero
  este diseño o realizarse simultáneamente con su implementación.

- Los datos almacenados deberán ser compatibles con el objeto lógico:

  ```text
  Medicion
  ```

  utilizado por Android y por la lógica de negocio.