# Business Logic Component Design

## Component Design

### 1. Objetivo y responsabilidad

El componente `business_logic` valida, almacena y consulta mediciones de ozono (O3). Recibe datos de dominio, aplica sus reglas de validación y utiliza la persistencia definida en `doc/database_design.md`.

Su implementación se encuentra en `src/business_logic/LogicaMediciones.php`. No necesita conocer el mecanismo empleado por otros componentes para invocar sus operaciones.

```text
Datos de dominio
       |
       v
LogicaMediciones
       |
       v
Persistencia relacional
       |
       v
Tabla medicion
```

### 2. Tipos de dominio

Se utiliza la notación lógica de `context/Software_Engineering_Spec.md`:

```text
MedicionEntrada = (
    uuid: Text,
    nombre_dispositivo: Text,
    tipo: Text,
    valor: Z,
    contador: N,
    rssi: Z
)

Medicion = (
    id_medicion: N,
    uuid: Text,
    nombre_dispositivo: Text,
    tipo: Text,
    valor: Z,
    contador: N,
    rssi: Z,
    fecha_hora: Text
)

MedicionBD = (
    id_medicion: N,
    uuid: Text,
    nombre_dispositivo: Text,
    tipo: Text,
    valor: Z,
    contador: N,
    rssi: Z,
    fecha_hora: Text
)
```

`MedicionBD` es una fila obtenida de la tabla `medicion` antes de normalizar la representación numérica. `Medicion?` denota una `Medicion` o ausencia de resultado (`null`). El valor de O3 se expresa en ppb y conserva el signo.

### 3. Clase `LogicaMediciones`

`ConexionBD` representa de forma abstracta una conexión a la persistencia relacional. En PHP se implementa mediante `PDO`, recibido por el constructor; la creación de la conexión y las credenciales quedan fuera de este componente.

```text
                              --------- LogicaMediciones ---------
                              |
                              | conexion: ConexionBD
                              |
                              | filaAMedicion(fila: MedicionBD) <--
                              | resultado: Medicion
                              |
                              |
conexion: ConexionBD       --> LogicaMediciones() -->
                              |
                              |
entrada: MedicionEntrada   --> validarMedicion() <--
valida: B                  <--
                              |
                              |
entrada: MedicionEntrada   --> guardarMedicion() -->
medicion: Medicion         <--
                              |
                              |
limite: N                  --> listarMediciones() <--
mediciones: [Medicion]     <--
                              |
                              |
tipo: Text                 --> obtenerUltimaMedicion() <--
medicion: Medicion?        <--
                              |
                              |
uuid: Text, limite: N      --> obtenerMedicionesDispositivo() <--
mediciones: [Medicion]     <--
                              |
                              ------------------------------------
```

Las operaciones públicas aparecen atravesando el límite de la clase y la conversión auxiliar `filaAMedicion()` permanece dentro de él, por ser privada. La flecha derecha `-->` indica modificación del estado o de la persistencia; `<--` indica consulta sin modificación.

### 4. Operaciones y contratos lógicos

#### `LogicaMediciones()`

```text
conexion: ConexionBD --> LogicaMediciones()
```

- **Precondición:** se proporciona una conexión válida a la base de datos.
- **Postcondición:** la instancia conserva la conexión para las operaciones posteriores. La conexión se configura para informar los errores de persistencia.
- **Efectos:** no inserta ni elimina mediciones.

#### `validarMedicion()`

```text
entrada: MedicionEntrada --> validarMedicion() --> B
```

- **Responsabilidad:** comprobar la presencia y validez de los seis campos requeridos.
- **Precondición:** `entrada` es un candidato a `MedicionEntrada`; puede contener datos incorrectos.
- **Postcondición:** devuelve `true` si cumple todas las condiciones indicadas y `false` en caso contrario. No escribe en la base de datos.

```text
uuid                 : Text no vacío, sin espacios extremos, longitud <= 36
nombre_dispositivo   : Text no vacío, sin espacios extremos, longitud <= 100
tipo                 : Text, exactamente "O3"
valor                : Z, -32768 <= valor <= 32767
contador             : N, 0 <= contador <= 255
rssi                 : Z, -32768 <= rssi <= 32767
```

No convierte cadenas numéricas en enteros ni modifica la entrada. Requiere los seis campos; la implementación actual no rechaza campos adicionales que no intervienen en el registro.

#### `guardarMedicion()`

```text
entrada: MedicionEntrada --> guardarMedicion() --> Medicion
```

- **Precondición:** existe la tabla `medicion` y la conexión está operativa.
- **Regla:** si `validarMedicion(entrada)` es falsa, se produce `DATOS_INVALIDOS` sin insertar ningún registro.
- **Proceso:** inserta los seis atributos de `MedicionEntrada`; recupera el identificador generado y consulta la fila recién creada.
- **Postcondición:** devuelve la `Medicion` persistida, incluyendo `id_medicion` y `fecha_hora` generados por la base de datos.
- **Error:** los fallos de persistencia producen `ERROR_PERSISTENCIA`.

```text
entrada
   |
   v
validarMedicion()
   |
   +-- false --> DATOS_INVALIDOS
   |
   v
INSERT en medicion
   |
   v
obtener id_medicion generado
   |
   v
SELECT por id_medicion
   |
   v
filaAMedicion()
   |
   v
Medicion
```

No se impone unicidad al par `uuid` y `contador`: el contador admite valores repetidos a lo largo del tiempo.

#### `listarMediciones()`

```text
limite: N --> listarMediciones() --> [Medicion]
```

- **Precondición:** `1 <= limite <= 100`.
- **Proceso:** selecciona hasta `limite` filas de `medicion` y convierte cada una en `Medicion`.
- **Postcondición:** devuelve la lista en orden `fecha_hora DESC, id_medicion DESC`; si no hay registros, devuelve `[]`.
- **Error:** un límite inválido produce `DATOS_INVALIDOS` y un fallo de persistencia produce `ERROR_PERSISTENCIA`.

#### `obtenerUltimaMedicion()`

```text
tipo: Text --> obtenerUltimaMedicion() --> Medicion?
```

- **Precondición:** `tipo = "O3"` durante Sprint 0.
- **Proceso:** consulta la primera fila de `medicion` para ese tipo, ordenada por `fecha_hora DESC, id_medicion DESC`.
- **Postcondición:** devuelve la `Medicion` más reciente o `null` si no existe ninguna.
- **Error:** un tipo inválido produce `DATOS_INVALIDOS` y un fallo de persistencia produce `ERROR_PERSISTENCIA`.

#### `obtenerMedicionesDispositivo()`

```text
uuid: Text, limite: N --> obtenerMedicionesDispositivo() --> [Medicion]
```

- **Precondiciones:** `uuid` es un texto no vacío, sin espacios extremos y de longitud máxima 36; `1 <= limite <= 100`.
- **Proceso:** consulta la tabla `medicion`, filtrada por `uuid`, con orden `fecha_hora DESC, id_medicion DESC`.
- **Postcondición:** devuelve hasta `limite` registros de ese dispositivo, o `[]` si no los hay.
- **Error:** los parámetros inválidos producen `DATOS_INVALIDOS` y un fallo de persistencia produce `ERROR_PERSISTENCIA`.

#### `filaAMedicion()` (privada)

```text
fila: MedicionBD --> filaAMedicion() --> Medicion
```

- **Responsabilidad:** transformar una fila de la tabla en un valor del dominio `Medicion`.
- **Precondición:** la fila contiene las ocho columnas esperadas.
- **Postcondición:** convierte `id_medicion`, `valor`, `contador` y `rssi` a enteros; conserva los atributos textuales y la fecha. No modifica la base de datos.

### 5. Relación con el diseño de la base de datos

Según `doc/database_design.md` y `src/database/schema.sql`, la entidad persistida es la tabla **`medicion`**:

```text
medicion (
    id_medicion, uuid, nombre_dispositivo, tipo,
    valor, contador, rssi, fecha_hora
)
```

Correspondencia entre campos lógicos y columnas:

| Campo lógico | Columna SQL | Tipo SQL | Origen |
| --- | --- | --- | --- |
| `id_medicion: N` | `id_medicion` | `BIGINT UNSIGNED` | Autoincremento |
| `uuid: Text` | `uuid` | `VARCHAR(36)` | Entrada validada |
| `nombre_dispositivo: Text` | `nombre_dispositivo` | `VARCHAR(100)` | Entrada validada |
| `tipo: Text` | `tipo` | `VARCHAR(30)` | Entrada validada |
| `valor: Z` | `valor` | `INT` | Entrada validada |
| `contador: N` | `contador` | `TINYINT UNSIGNED` | Entrada validada |
| `rssi: Z` | `rssi` | `SMALLINT` | Entrada validada |
| `fecha_hora: Text` | `fecha_hora` | `DATETIME` | Valor predeterminado de la BD |

La clave primaria es `id_medicion`. No hay tablas adicionales ni claves foráneas en el esquema vigente. Las escrituras se realizan mediante consultas parametrizadas y las consultas de lectura convierten sus filas mediante `filaAMedicion()`.

### 6. Pruebas automatizadas

El archivo `src/business_logic/tests/test_logica_mediciones.php` contiene pruebas de integración que verifican:

- Aceptación de valores de O3 positivos y negativos; rechazo de tipo, contador y valores inválidos.
- Inserciones con identificador y fecha generados, conservando los valores originales.
- Listados limitados, consulta de la última medición y filtrado por dispositivo.
- Detección de parámetros incorrectos.

Las pruebas de escritura se ejecutan dentro de una transacción que finaliza con `ROLLBACK`, con una tabla compatible con transacciones. La ejecución efectiva y su resultado deben comprobarse por separado en el entorno con PHP y MySQL.

## Design Clarifications

- El diseño describe el esquema actualmente implementado en Sprint 0; si cambia la base de datos, se revisará antes de dar el diseño por definitivo.
- Se mantiene una única tabla `medicion`, tal como figura en `doc/database_design.md` y `src/database/schema.sql`.
- `tipo` tiene como único valor permitido `"O3"` en la implementación actual.
- `valor` se almacena en ppb con signo, y `contador` no es un identificador único.
- El identificador y la fecha son generados por la base de datos.
- Las consultas se ordenan primero por fecha y después por identificador para resolver empates.
- La dependencia de persistencia se recibe desde fuera de la clase; no se guardan credenciales en `LogicaMediciones.php`.
- Los errores de negocio y persistencia se expresan como `DATOS_INVALIDOS` y `ERROR_PERSISTENCIA`.

## General Rules

- **Programming Language:** PHP, con PDO para la persistencia MySQL/MariaDB.
- **Function/Method Headers:** cada función o método debe incluir su firma lógica en un bloque de comentarios delimitado por líneas discontinuas (`--------------------`).
- **Code Readability:** nombres claros y código autoexplicativo, evitando comentarios internos innecesarios.
- **Automated Testing:** crear y mantener pruebas unitarias o de integración para todos los métodos críticos; los fallos deben poder detectarse automáticamente.
- **File Headers:** cada fichero de código debe identificar nombre, descripción, copyright, fecha, autor y aportación.
- **Design Correspondence:** mantener el diseño sincronizado con `src/business_logic/LogicaMediciones.php` y con la estructura declarada en `doc/database_design.md`.
- **Persistence Safety:** utilizar parámetros enlazados y validar las entradas antes de realizar escrituras.