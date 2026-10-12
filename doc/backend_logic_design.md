# Backend Logic Component Design

## Component Design

### Objetivo del componente

El componente `backend_logic` implementa las reglas de negocio para validar, guardar y consultar las mediciones que llegan al servidor PBIO.

Se sitúa entre el servidor REST y la base de datos:

```text
Arduino --> iBeacon --> Android --> REST
                                    |
                                    v
                           backend_logic
                                    |
                                    v
                            MySQL / MariaDB
                                    |
                                    v
                       REST --> navegador web
```

La lógica de negocio **no conoce** las peticiones HTTP, las rutas REST ni el protocolo BLE. Recibe datos ya interpretados por Android y utiliza la conexión a base de datos proporcionada por la capa que la invoca.

---

### Correspondencia con los componentes existentes

El objeto lógico enviado por Android contiene:

```text
MedicionEntrada = (
    uuid: Text,
    nombre_dispositivo: Text,
    tipo: Text,
    valor: Z,
    contador: N,
    rssi: Z
)
```

El backend lo almacena en la tabla `medicion`, definida en `doc/database_design.md` y creada mediante `src/database/schema.sql`.

La base de datos añade automáticamente el identificador y la fecha:

```text
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

Las unidades del valor de O3 son **ppb**. No se envían ni se almacenan `Major`, `Minor` ni `TxPower`.

---

### Responsabilidades y límites

```text
Servidor REST
    |
    | MedicionEntrada
    v
LogicaMediciones
    |
    +--> validarMedicion()
    |
    +--> guardarMedicion()
    |
    +--> listarMediciones()
    |
    +--> obtenerUltimaMedicion()
    |
    +--> obtenerMedicionesDispositivo()
    |
    v
PDO con consultas SQL parametrizadas
    |
    v
Tabla medicion
```

- Android interpreta la trama iBeacon y construirá `MedicionEntrada`.
- REST convierte las peticiones y respuestas HTTP, sin implementar las reglas de negocio.
- `backend_logic` valida la entrada, ejecuta las operaciones y controla los errores de negocio/persistencia.
- MySQL/MariaDB almacena y devuelve los datos sin reinterpretar el protocolo BLE.
- La web consume REST; nunca accede directamente a MySQL.

---

### Lenguaje y estructura previstos

**Lenguaje:** PHP con PDO para MySQL/MariaDB.

Estructura prevista:

```text
doc/
    backend_logic_design.md

src/
    database/
        schema.sql
        test_database.sql
    backend_logic/
        LogicaMediciones.php
        tests/
            test_logica_mediciones.php
```

`LogicaMediciones` recibirá un objeto `PDO` por su constructor. Las credenciales y la creación de esa conexión quedarán fuera de la lógica de negocio. No se incluirán contraseñas reales en el repositorio.

---

### Clase `LogicaMediciones`

Responsabilidad: ofrecer operaciones independientes de HTTP para validar y manejar mediciones.

```text
             -------------- LogicaMediciones ----------------
             | conexion: ConexionBD
             |
conexion     --> LogicaMediciones() -->
             |
entrada      --> validarMedicion() --> B
             |
entrada      --> guardarMedicion() --> Medicion
             |
limite       --> listarMediciones() --> [Medicion]
             |
tipo         --> obtenerUltimaMedicion() --> Medicion?
             |
uuid, limite --> obtenerMedicionesDispositivo() --> [Medicion]
             |
             ------------------------------------------------
```

`ConexionBD` representa en el diseño la dependencia técnica que se implementará mediante `PDO`. `Medicion?` indica que puede no existir ninguna medición y, en ese caso, se devuelve `null`.

---

### `validarMedicion()`

```text
entrada: MedicionEntrada --> validarMedicion() --> B
```

**Responsabilidad:** comprobar que los campos recibidos cumplen el contrato del proyecto antes de almacenarlos.

Precondiciones: se recibe un conjunto de campos que pretende representar una `MedicionEntrada`.

Condiciones de validez:

```text
uuid:
    Text, no vacío, máximo 36 caracteres

nombre_dispositivo:
    Text, no vacío, máximo 100 caracteres

tipo:
    Text, "O3" para el Sprint 0

valor:
    Z, entre -32768 y 32767

contador:
    N, entre 0 y 255

rssi:
    Z, entre -32768 y 32767
```

Los límites de `valor` corresponden a la representación con signo de 16 bits utilizada por Arduino. El campo SQL `valor` es `INT` con signo, que puede almacenar esos valores.

Los valores de `contador` corresponden a un byte sin signo. `rssi` es un entero con signo compatible con el tipo SQL `SMALLINT`.

Postcondiciones:

- Devuelve `verdadero` si todos los campos cumplen el contrato.
- Devuelve `falso` en caso contrario.
- No modifica la base de datos ni realiza ninguna petición HTTP.

La implementación no transformará silenciosamente textos numéricos como `"333"` en el entero `333`: la entrada debe contener los tipos correctos. Los espacios al principio o al final de los identificadores se consideran entrada inválida, no se corrigen sin informar.

---

### `guardarMedicion()`

```text
entrada: MedicionEntrada --> guardarMedicion() --> Medicion
```

**Responsabilidad:** validar e insertar una nueva medición y devolver el registro persistido.

Precondiciones:

- Existe una conexión válida a MySQL/MariaDB.
- La tabla `medicion` existe.

Proceso:

```text
MedicionEntrada
      |
      v
validarMedicion()
      |
      +-- falso --> DATOS_INVALIDOS
      |
      v
INSERT parametrizado en medicion
      |
      v
obtener id_medicion generado
      |
      v
SELECT parametrizado por id_medicion
      |
      v
Medicion almacenada
```

Solo se insertan estos seis campos:

```text
uuid, nombre_dispositivo, tipo, valor, contador, rssi
```

`id_medicion` y `fecha_hora` los genera la base de datos.

Postcondiciones:

- Si la entrada es válida y la inserción tiene éxito, queda una nueva fila almacenada y se devuelve la `Medicion` completa.
- Si la entrada no es válida, no se ejecuta el `INSERT`.
- Si la base de datos falla, se informa de `ERROR_PERSISTENCIA`; no se informa falsamente de éxito.

No se elimina ni se sustituye una medición anterior por tener el mismo `uuid` y `contador`, ya que el contador se reinicia al completar 256 valores. Evitar el envío repetido del mismo anuncio BLE, cuando se implemente, corresponderá a la capa Android o a una política de ingestión explícita, no a una restricción `UNIQUE` sobre este contador.

---

### `listarMediciones()`

```text
limite: N --> listarMediciones() --> [Medicion]
```

**Responsabilidad:** devolver las mediciones más recientes con un máximo de registros controlado.

Precondiciones:

```text
1 <= limite <= 100
```

Proceso:

```text
limite
   |
   v
validar rango
   |
   v
SELECT ... FROM medicion
ORDER BY fecha_hora DESC, id_medicion DESC
LIMIT limite
   |
   v
[Medicion]
```

Postcondiciones:

- Devuelve una lista ordenada de la más reciente a la más antigua (fecha y, en caso de empate, identificador).
- Si no hay registros, devuelve `[]`.
- Si `limite` no cumple el rango, produce `DATOS_INVALIDOS`.

Se utiliza `fecha_hora DESC, id_medicion DESC` para seleccionar la fecha más reciente y resolver los empates cuando varias mediciones tienen el mismo segundo.

---

### `obtenerUltimaMedicion()`

```text
tipo: Text --> obtenerUltimaMedicion() --> Medicion?
```

**Responsabilidad:** consultar la última medición almacenada del tipo indicado.

Precondición:

```text
tipo = "O3" para Sprint 0
```

Consulta lógica:

```text
SELECT ... FROM medicion
WHERE tipo = tipo_solicitado
ORDER BY fecha_hora DESC, id_medicion DESC
LIMIT 1
```

Postcondiciones:

- Devuelve la `Medicion` más reciente del tipo indicado.
- Si no existe ninguna, devuelve `null`; la capa REST decidirá si responde con HTTP 404.
- Un tipo inválido produce `DATOS_INVALIDOS`.

Esta es la operación prevista para que la web muestre la última medición recibida.

---

### `obtenerMedicionesDispositivo()`

```text
uuid: Text, limite: N --> obtenerMedicionesDispositivo() --> [Medicion]
```

**Responsabilidad:** listar las mediciones de un dispositivo concreto.

Precondiciones:

```text
uuid: Text no vacío, máximo 36 caracteres
1 <= limite <= 100
```

Consulta lógica:

```text
SELECT ... FROM medicion
WHERE uuid = uuid_solicitado
ORDER BY fecha_hora DESC, id_medicion DESC
LIMIT limite
```

Postcondiciones:

- Devuelve únicamente registros asociados al `uuid` solicitado.
- Ordena por `fecha_hora DESC, id_medicion DESC`.
- Si no encuentra mediciones, devuelve `[]`.
- Entradas inválidas producen `DATOS_INVALIDOS`.

---

### Tipos de resultado y errores

```text
ErrorNegocio = {
    DATOS_INVALIDOS,
    ERROR_PERSISTENCIA
}

ResultadoGuardar = Medicion | ErrorNegocio
ResultadoListar = [Medicion] | ErrorNegocio
ResultadoUltima = Medicion? | ErrorNegocio
```

En PHP se utilizarán excepciones apropiadas para separar entradas inválidas de errores de base de datos. No se devolverán mensajes SQL internos al cliente HTTP.

Correspondencia HTTP **prevista para la futura capa REST**, no implementada aquí:

```text
DATOS_INVALIDOS       --> 400 Bad Request
ERROR_PERSISTENCIA    --> 500 Internal Server Error
Última no encontrada --> 404 Not Found
Guardado correcto     --> 201 Created
Consultas correctas   --> 200 OK
```

El código HTTP y la generación de JSON pertenecen exclusivamente a `rest_server`.

---

### Consultas a la base de datos

Las operaciones emplearán consultas preparadas y parámetros enlazados mediante `PDO`, por ejemplo:

```sql
INSERT INTO medicion
    (uuid, nombre_dispositivo, tipo, valor, contador, rssi)
VALUES
    (:uuid, :nombre_dispositivo, :tipo, :valor, :contador, :rssi);
```

Para las consultas de lectura, los valores de filtro se enlazarán como parámetros. El `LIMIT` se validará como entero entre 1 y 100 antes de aplicarse a la consulta.

Se configurará `PDO` para lanzar excepciones ante errores SQL y se usará el modo de lectura asociativa (`PDO::FETCH_ASSOC`) al recuperar mediciones.

---

### Ejemplo completo de la demostración

En Arduino se establece una medición ficticia conocida:

```text
valor = 333 ppb
```

Android interpreta el beacon y prepara:

```text
MedicionEntrada = (
    uuid = "EPSG-GTI-PROY-3A",
    nombre_dispositivo = "GTI-3A",
    tipo = "O3",
    valor = 333,
    contador = 30,
    rssi = -68
)
```

El servidor REST entrega los seis campos a `guardarMedicion()`, que valida y guarda la medición. Después, `obtenerUltimaMedicion("O3")` permite recuperar el valor `333` para mostrarlo en la web.

El RSSI de una captura física no tiene por qué coincidir con `-68`, utilizado aquí como dato ficticio reproducible.

```text
Arduino 333 ppb
      |
      v
Android: MedicionEntrada(valor=333)
      |
      v
REST: recibir JSON
      |
      v
backend_logic: validar + guardar
      |
      v
medicion.valor = 333
      |
      v
backend_logic: obtenerUltimaMedicion("O3")
      |
      v
REST: devolver JSON
      |
      v
Web: 333 ppb
```

---

### Pruebas automáticas previstas

Las pruebas de `backend_logic` se implementarán en `src/backend_logic/tests/` y deberán producir un resultado reproducible, así como un código de salida distinto de cero si falla una comprobación.

Se comprobarán como mínimo:

1. `validarMedicion()` acepta una medición positiva válida (`333`).
2. `validarMedicion()` acepta una medición negativa válida (`-194`).
3. Rechaza `uuid`, nombre o tipo vacíos.
4. Rechaza tipos distintos de `O3` durante Sprint 0.
5. Rechaza `valor` fuera del rango `-32768..32767`.
6. Rechaza `contador` menor que 0 o mayor que 255.
7. Rechaza valores no enteros en `valor`, `contador` y `rssi`.
8. `guardarMedicion()` conserva exactamente los seis campos y obtiene ID/fecha automáticos.
9. `listarMediciones()` respeta el límite y ordena por fecha descendente y desempata por ID.
10. `obtenerUltimaMedicion("O3")` devuelve la última fila del tipo O3.
11. `obtenerMedicionesDispositivo()` filtra por UUID.
12. Se comprueban correctamente los resultados vacíos (`null` o `[]`).
13. La entrada no se concatena directamente en consultas SQL.

Las pruebas que escriban en MySQL utilizarán una **base de pruebas** o una transacción que termine en `ROLLBACK`, sin borrar los datos de uso normal. Se documentará cómo ejecutarlas desde la línea de comandos de PHP.

---

## Design Clarifications

- El diseño está sincronizado con la **tabla única `medicion`** adoptada para Sprint 0.
- No se crea una tabla adicional `dispositivo`.
- La lógica de negocio es independiente de `Android`, `BluetoothLeScanner`, iBeacon y HTTP.
- `tipo` llega como texto lógico (`"O3"`), no como el número BLE `14`.
- `valor` llega ya expresado en ppb y con signo.
- `contador` se representa en el rango `0..255`, pero no es una clave única.
- `id_medicion` y `fecha_hora` los genera la base de datos.
- Las operaciones de lectura usan `fecha_hora DESC, id_medicion DESC` para seleccionar la medición más reciente y resolver empates.
- Las consultas SQL se implementarán con `PDO` y parámetros, sin concatenar datos externos.
- Las credenciales no se introducirán en `LogicaMediciones.php` ni se publicarán en Git.
- La validación de los tipos es estricta para evitar conversiones implícitas imprevistas.
- La interfaz REST, sus rutas, el JSON de respuesta y los códigos de estado se diseñarán en `doc/rest_server_design.md`.
- El envío automático de mediciones desde Android todavía está pendiente: actualmente su aplicación BLE muestra las lecturas y ofrece una medición fake.
- Las pruebas de ejecución del código PHP solo se darán por completadas cuando se implemente y ejecute el componente.

---

## General Rules

- **Programming Language:** PHP.
- **Database Access:** PDO con MySQL/MariaDB y sentencias preparadas.
- **File Headers:** Cada archivo de código incluirá nombre, descripción, copyright, fecha, autor y aportación.
- **Author:** Álvaro Ballester Grau.
- **Function/Method Headers:** Cada operación incluirá en comentarios su firma lógica, descripción, entradas, salidas y condiciones relevantes, respetando la notación propia del proyecto.
- **Names:** Mantener exactamente los nombres diseñados: `validarMedicion`, `guardarMedicion`, `listarMediciones`, `obtenerUltimaMedicion` y `obtenerMedicionesDispositivo`.
- **Separation of Responsibilities:** `rest_server` se ocupa de HTTP; `backend_logic` se ocupa de reglas y consultas; `database` define el esquema persistente.
- **Validation:** No realizar escrituras si la entrada es inválida.
- **Error Handling:** Diferenciar datos incorrectos de fallos de persistencia; evitar exponer credenciales o detalles internos SQL.
- **Automated Testing:** Añadir pruebas que puedan repetirse y detecten fallos automáticamente.
- **Repository Consistency:** Mantener `doc/backend_logic_design.md` y `src/backend_logic/` sincronizados.
- **Design Before Code:** Registrar en este documento cualquier nueva operación antes de implementarla.