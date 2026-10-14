# Database Component Design

## Component Design

### 1. Objetivo y alcance

El componente `database` almacena las mediciones de ozono (O3) recibidas por la aplicación Android y validadas por la lógica de negocio. Su implementación de Sprint 0 está definida en `src/database/schema.sql` y utiliza una única tabla relacional llamada `medicion`.

El diseño representa **el esquema actual, ya desplegado y probado**. No introduce cambios en la estructura de MySQL ni anticipa modificaciones de un sprint posterior.

```text
Arduino (iBeacon)
       |
       v
Android (MedicionEntrada)
       |
       v
communication (REST)
       |
       v
business_logic (LogicaMediciones)
       |
       v
database (tabla medicion)
```

La base de datos no interpreta tramas Bluetooth, campos Major/Minor ni unidades físicas; almacena datos previamente interpretados. Para O3, `valor` se expresa en **ppb** y puede ser negativo.

### 2. Modelo de datos lógico

Se emplean los tipos abstractos definidos en `context/Software_Engineering_Spec.md`:

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
```

`MedicionEntrada` contiene los seis atributos que recibe la lógica de negocio para insertar una medición. Los atributos `id_medicion` y `fecha_hora` se generan en MySQL al guardar el registro. `fecha_hora: Text` representa la fecha en el objeto lógico devuelto por PHP; la columna de persistencia es de tipo SQL `DATETIME`.

### 3. Especificación relacional formal

La siguiente declaración sigue el formato obligatorio de `context/Database_Design_Spec.md` y se corresponde con el `CREATE TABLE` existente en `src/database/schema.sql`.

```text
====================================================================================
TABLE: medicion

DESCRIPTION: Almacena las mediciones de O3 interpretadas por Android,
             identificando el dispositivo, el valor, el contador, la señal
             de recepción y el instante de almacenamiento.

COLUMNS:

+ id_medicion        | BIGINT UNSIGNED  | NOT NULL | AUTO_INCREMENT
+ uuid               | VARCHAR(36)     | NOT NULL
+ nombre_dispositivo | VARCHAR(100)    | NOT NULL
+ tipo               | VARCHAR(30)     | NOT NULL
+ valor              | INT             | NOT NULL
+ contador           | TINYINT UNSIGNED| NOT NULL
+ rssi               | SMALLINT        | NOT NULL
+ fecha_hora         | DATETIME        | NOT NULL | CURRENT_TIMESTAMP

PRIMARY KEY: id_medicion

FOREIGN KEYS:

+ Ninguna. Sprint 0 utiliza una sola tabla y no define relaciones externas.

CONSTRAINTS:

+ PRIMARY KEY (id_medicion).
+ id_medicion es autoincremental y no admite NULL.
+ Todas las columnas son NOT NULL.
+ contador utiliza TINYINT UNSIGNED (rango representable: 0..255).
+ valor utiliza INT con signo y rssi utiliza SMALLINT con signo.
+ fecha_hora tiene DEFAULT CURRENT_TIMESTAMP.
+ No existe restriccion UNIQUE sobre uuid, contador ni su combinacion.
+ El esquema vigente no declara restricciones CHECK adicionales.
====================================================================================
```

### 4. Campos y responsabilidades

| Columna | Origen | Uso en Sprint 0 |
| --- | --- | --- |
| `id_medicion` | MySQL | Identificador único generado con `AUTO_INCREMENT`. |
| `uuid` | Android, tras interpretar el iBeacon | Identifica el dispositivo. Valor utilizado: `EPSG-GTI-PROY-3A`. |
| `nombre_dispositivo` | Android | Nombre lógico del dispositivo, actualmente `GTI-3A`. |
| `tipo` | Android | Tipo de medida: `O3`. Android interpreta previamente el código BLE `14`. |
| `valor` | Android | Medida de O3 expresada en ppb; admite valores negativos. |
| `contador` | Arduino, interpretado por Android | Contador de 8 bits (0..255); puede repetirse. |
| `rssi` | Android | Intensidad de la señal BLE recibida, expresada en dBm. |
| `fecha_hora` | MySQL | Momento de inserción mediante `DEFAULT CURRENT_TIMESTAMP`. |

La tabla no contiene una clave externa hacia una tabla de dispositivos. Un mismo valor de `uuid` puede aparecer en tantas filas como mediciones se reciban.

### 5. Validaciones del dominio frente a restricciones SQL

Las siguientes reglas son responsabilidad de `business_logic`, según `doc/business_logic_design.md`; **no son restricciones `CHECK` implementadas en el `schema.sql` actual**:

```text
uuid: Text                 no vacio, sin espacios extremos, longitud <= 36
nombre_dispositivo: Text   no vacio, sin espacios extremos, longitud <= 100
tipo: Text                 exactamente "O3"
valor: Z                   -32768 <= valor <= 32767
contador: N                0 <= contador <= 255
rssi: Z                    -32768 <= rssi <= 32767
```

MySQL impone sus tipos de columna, `NOT NULL`, la clave primaria y el valor predeterminado de la fecha. La validación semántica ocurre antes de la inserción mediante `LogicaMediciones::validarMedicion()`.

### 6. Correspondencia con las operaciones de negocio

La implementación `src/business_logic/LogicaMediciones.php` utiliza la tabla `medicion` de esta forma:

| Operación lógica | Acceso a la tabla |
| --- | --- |
| `guardarMedicion(entrada)` | `INSERT` de `uuid`, `nombre_dispositivo`, `tipo`, `valor`, `contador` y `rssi`; obtiene la fila por su `id_medicion`. |
| `listarMediciones(limite)` | `SELECT` con orden `fecha_hora DESC, id_medicion DESC` y límite de registros. |
| `obtenerUltimaMedicion(tipo)` | `SELECT` filtrado por `tipo`, con el mismo orden y `LIMIT 1`. |
| `obtenerMedicionesDispositivo(uuid, limite)` | `SELECT` filtrado por `uuid`, ordenado por fecha e identificador. |

`id_medicion` permite distinguir mediciones del mismo dispositivo incluso cuando `contador` se repite. El segundo criterio de ordenación, `id_medicion DESC`, resuelve los empates de `fecha_hora` cuando varias inserciones ocurren en el mismo segundo.

### 7. Pruebas de la base de datos

Archivo: `src/database/test_database.sql`.

El script comprueba inserciones positivas y negativas (`333` y `-194` ppb), los valores extremos de `contador` (`0` y `255`), la generación automática de `id_medicion` y `fecha_hora`, la consulta de la última medición y la ausencia de valores `NULL` en campos obligatorios.

La prueba comienza con `START TRANSACTION` y termina con `ROLLBACK`, pensado para deshacer las filas insertadas durante las comprobaciones en un motor transaccional. Un `ROLLBACK` **no garantiza recuperar los valores consumidos de `AUTO_INCREMENT`**.

`src/database/schema.sql` contiene `DROP TABLE IF EXISTS medicion` antes de `CREATE TABLE`. Por tanto, **no debe ejecutarse de nuevo sobre la base de datos desplegada con mediciones que deban conservarse**. El script de creación está destinado a inicialización controlada o a una base de datos de pruebas.

## Design Clarifications

- **Alcance de Sprint 0:** se mantiene deliberadamente la tabla actual de ocho columnas para conservar la compatibilidad con Android, REST, lógica de negocio y GUI ya probados.
- **Identificación del dispositivo:** el campo vigente se llama `uuid`; su valor de ejemplo es `EPSG-GTI-PROY-3A`. No se cambia a `id_dispositivo` en este sprint.
- **Campos existentes:** `nombre_dispositivo`, `contador` y `rssi` permanecen en MySQL durante Sprint 0, aunque su utilidad se revisará después.
- **Mejora propuesta para el siguiente sprint, no implementada:** estudiar el renombrado de `uuid` a `id_dispositivo` (conservando su identificador textual) y la eliminación de `nombre_dispositivo`, `contador` y `rssi`. Antes de aplicarla habrá que sincronizar SQL, PHP, JSON, Android, GUI, diseños y tests.
- **Una tabla:** no existen tablas `dispositivo` o `tipo_medida` en la implementación actual, ni claves foráneas.
- **Sin reinterpretación BLE:** la conversión de Major/Minor y el significado de `tipo` se resuelven antes de la persistencia.

## General Rules

- **Programming Language:** SQL para MySQL/MariaDB; los scripts de implementación son `schema.sql` y `test_database.sql`.
- **Function/Method Headers:** si se añaden procedimientos, funciones o métodos, cada uno deberá incorporar su firma lógica en comentarios delimitados por líneas discontinuas (`--------------------`); actualmente no se definen funciones almacenadas en la base de datos.
- **Code Readability:** utilizar nombres descriptivos, mantener sentencias SQL legibles y añadir comentarios únicamente cuando aclaren decisiones relevantes.
- **Automated Testing:** mantener pruebas reproducibles de creación, inserción, consulta, tipos y restricciones de los campos; ejecutar las comprobaciones con protección de los datos reales.
- **Design Correspondence:** todo cambio de columnas o restricciones deberá actualizar conjuntamente `doc/database_design.md`, `src/database/schema.sql`, `src/business_logic/LogicaMediciones.php` y las pruebas afectadas.
- **Separation of Responsibilities:** la base de datos almacena los datos y aplica sus restricciones SQL; la validación de dominio pertenece a `business_logic`, y el transporte pertenece a `communication`.
- **Deployment Safety:** no ejecutar scripts destructivos sobre producción sin copia de seguridad y procedimiento de migración validado.
