# REST Server Component Design

## Component Design

### Objetivo del componente

El componente `rest_server` proporciona una interfaz HTTP para comunicar la
aplicación Android y la aplicación web con la lógica de negocio del proyecto PBIO.

Su responsabilidad es recibir peticiones, interpretar sus parámetros, llamar
a las operaciones de negocio y devolver respuestas en formato JSON.

El servidor REST no accede directamente a las tablas mediante consultas SQL.
La persistencia se realiza a través de `LogicaMediciones`.

El flujo general es:

```text
Android
    |
    | HTTP POST + JSON
    v
Servidor REST
    |
    v
LogicaMediciones
    |
    v
MySQL / MariaDB
    |
    v
LogicaMediciones
    |
    v
Servidor REST
    |
    | HTTP + JSON
    v
Android / Web
```

---

### Alcance de Sprint 0

El servidor deberá permitir:

- Registrar una medición recibida desde Android.
- Consultar varias mediciones almacenadas.
- Consultar la última medición de un tipo.
- Consultar las mediciones de un dispositivo concreto.

La comunicación se realizará mediante HTTP y JSON.

El despliegue se realizará en el alojamiento Plesk proporcionado por la UPV.

La comunicación pública deberá utilizar HTTPS con un certificado válido.

---

### Lenguaje de programación

```text
Programming Language = PHP 8.2+
```

El servidor utiliza:

```text
PHP
PDO
MySQL / MariaDB
JSON
HTTP
```

No utiliza librerías externas ni frameworks REST para Sprint 0.

Se aprovecha la lógica de negocio implementada en:

```text
src/backend_logic/LogicaMediciones.php
```

---

### Separación de responsabilidades

```text
Android
    |
    | MedicionEntrada
    v
REST
    |
    | valida formato HTTP
    | interpreta JSON y parámetros
    | gestiona códigos de respuesta
    v
backend_logic
    |
    | valida reglas de negocio
    | guarda y consulta mediciones
    v
database
    |
    | almacena información
    v
MySQL
```

El servidor REST no interpreta:

```text
iBeacon
Major
Minor
TxPower
```

Android ya ha realizado esa interpretación.

---

### Tipos lógicos

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

ErrorREST = (
    error: Text,
    mensaje: Text
)
```

El valor de O3 se expresa en ppb.

Los campos `id_medicion` y `fecha_hora` son generados por la base de datos.

---

### Endpoints REST

La API se publicará bajo la ruta:

```text
/api/
```

Se definen cuatro operaciones:

| Método | Ruta | Responsabilidad |
|---|---|---|
| POST | /api/mediciones | Registrar una medición |
| GET | /api/mediciones | Listar mediciones |
| GET | /api/mediciones/ultima | Obtener última medición |
| GET | /api/dispositivos/{uuid}/mediciones | Consultar por UUID |

Las rutas están pensadas para un despliegue en el dominio UPV del proyecto.

La dirección definitiva utilizará HTTPS cuando el certificado esté configurado.

---

### registrarMedicion()

```text
entrada: MedicionEntrada --> registrarMedicion() --> Medicion
```

Método HTTP:

```text
POST
```

Ruta:

```text
/api/mediciones
```

Responsabilidad:

Recibir una medición enviada por Android y registrarla mediante la lógica
de negocio.

La petición debe utilizar:

```text
Content-Type: application/json
```

El cuerpo contiene:

```json
{
  "uuid": "EPSG-GTI-PROY-3A",
  "nombre_dispositivo": "GTI-3A",
  "tipo": "O3",
  "valor": 333,
  "contador": 30,
  "rssi": -68
}
```

Proceso:

```text
POST /api/mediciones
        |
        v
leer cuerpo JSON
        |
        v
comprobar formato JSON
        |
        v
obtener MedicionEntrada
        |
        v
guardarMedicion()
        |
        v
Medicion almacenada
        |
        v
respuesta HTTP 201
```

La validación de campos de negocio corresponde a `LogicaMediciones`.

El servidor REST interpreta el formato de la petición y transforma las
excepciones de negocio en respuestas HTTP.

Respuesta correcta:

```text
HTTP 201 Created
```

```json
{
  "id_medicion": 1,
  "uuid": "EPSG-GTI-PROY-3A",
  "nombre_dispositivo": "GTI-3A",
  "tipo": "O3",
  "valor": 333,
  "contador": 30,
  "rssi": -68,
  "fecha_hora": "2026-10-10 15:00:00"
}
```

La fecha y el identificador anteriores son ejemplos ilustrativos.
Los valores reales serán generados por MySQL.

Si los datos no son válidos:

```text
HTTP 400 Bad Request
```

Si el contenido no es JSON:

```text
HTTP 415 Unsupported Media Type
```

Si ocurre un error interno de persistencia:

```text
HTTP 500 Internal Server Error
```

No se devolverán contraseñas ni detalles internos de MySQL.

---

### consultarMediciones()

```text
limite: N --> consultarMediciones() --> [Medicion]
```

Método HTTP:

```text
GET
```

Ruta:

```text
/api/mediciones
```

Parámetro opcional:

```text
limit: N
```

Si se omite, se utilizará un límite predeterminado de 20 registros.

El rango admitido es:

```text
1 <= limit <= 100
```

Ejemplo:

```text
GET /api/mediciones?limit=10
```

Proceso:

```text
petición GET
      |
      v
leer limit
      |
      v
comprobar formato entero
      |
      v
listarMediciones(limit)
      |
      v
[Medicion]
      |
      v
JSON
```

Respuesta correcta:

```text
HTTP 200 OK
```

```json
[
  {
    "id_medicion": 2,
    "uuid": "EPSG-GTI-PROY-3A",
    "nombre_dispositivo": "GTI-3A",
    "tipo": "O3",
    "valor": -194,
    "contador": 63,
    "rssi": -54,
    "fecha_hora": "2026-10-10 15:01:00"
  },
  {
    "id_medicion": 1,
    "uuid": "EPSG-GTI-PROY-3A",
    "nombre_dispositivo": "GTI-3A",
    "tipo": "O3",
    "valor": 333,
    "contador": 30,
    "rssi": -68,
    "fecha_hora": "2026-10-10 15:00:00"
  }
]
```

Si no existen mediciones:

```json
[]
```

Se devolverá igualmente:

```text
HTTP 200 OK
```

---

### consultarUltimaMedicion()

```text
tipo: Text --> consultarUltimaMedicion() --> Medicion?
```

Método HTTP:

```text
GET
```

Ruta:

```text
/api/mediciones/ultima
```

Parámetro:

```text
tipo: Text
```

Para Sprint 0:

```text
tipo = "O3"
```

Ejemplo:

```text
GET /api/mediciones/ultima?tipo=O3
```

Responsabilidad:

Obtener la última medición almacenada del tipo solicitado.

Proceso:

```text
GET /api/mediciones/ultima
        |
        v
obtener tipo
        |
        v
obtenerUltimaMedicion(tipo)
        |
        +---- no existe ----> HTTP 404
        |
        v
Medicion
        |
        v
HTTP 200 + JSON
```

Respuesta correcta:

```text
HTTP 200 OK
```

```json
{
  "id_medicion": 2,
  "uuid": "EPSG-GTI-PROY-3A",
  "nombre_dispositivo": "GTI-3A",
  "tipo": "O3",
  "valor": -194,
  "contador": 63,
  "rssi": -54,
  "fecha_hora": "2026-10-10 15:01:00"
}
```

Esta operación será utilizada principalmente por la aplicación web.

Si no existe ninguna medición del tipo indicado:

```text
HTTP 404 Not Found
```

Si el tipo es inválido:

```text
HTTP 400 Bad Request
```

---

### consultarMedicionesDispositivo()

```text
uuid: Text, limite: N
    -->
consultarMedicionesDispositivo() --> [Medicion]
```

Método HTTP:

```text
GET
```

Ruta:

```text
/api/dispositivos/{uuid}/mediciones
```

Ejemplo:

```text
GET /api/dispositivos/EPSG-GTI-PROY-3A/mediciones?limit=20
```

Responsabilidad:

Obtener las mediciones asociadas a un dispositivo concreto.

El UUID se obtiene de la ruta.

El parámetro `limit` es opcional y utiliza el mismo rango que la consulta
general de mediciones.

Proceso:

```text
GET por dispositivo
        |
        v
obtener UUID
        |
        v
obtener limit
        |
        v
obtenerMedicionesDispositivo(uuid, limit)
        |
        v
[Medicion]
        |
        v
HTTP 200 + JSON
```

Si el dispositivo no tiene mediciones:

```json
[]
```

No es necesario crear una tabla independiente de dispositivos.

---

### Clase o módulo REST

La implementación inicial utilizará un punto de entrada:

```text
index.php
```

Responsabilidad:

- Recibir las peticiones HTTP.
- Identificar el método.
- Identificar la ruta.
- Obtener los parámetros.
- Leer el cuerpo JSON cuando corresponda.
- Invocar las operaciones de `LogicaMediciones`.
- Transformar resultados y errores en respuestas HTTP.

No debe duplicar las reglas de negocio ya implementadas.

Diseño lógico:

```text
               -------------- REST Server ---------------

metodo: Text,
ruta: Text,
parametros: [Text]
            --> procesarPeticion() --> RespuestaHTTP

entrada: MedicionEntrada
            --> registrarMedicion() --> Medicion

limite: N
            --> consultarMediciones() --> [Medicion]

tipo: Text
            --> consultarUltimaMedicion() --> Medicion?

uuid: Text,
limite: N
            --> consultarMedicionesDispositivo() --> [Medicion]

codigo: N,
datos: Any
            --> responderJSON()

               ------------------------------------------
```

Las funciones HTTP se implementarán en la capa REST.

La clase `LogicaMediciones` seguirá siendo independiente de HTTP.

---

### Conexión con la lógica de negocio

El servidor utilizará:

```text
LogicaMediciones.php
```

La conexión se realizará mediante PDO.

El servidor no contendrá directamente las credenciales.

Estas permanecerán en:

```text
pbio_backend/config.local.php
```

Este archivo pertenece al despliegue privado del servidor.

No se subirá a GitHub.

---

### Organización del repositorio

```text
PBIO-Sprint0/
|
+-- doc/
|   +-- database_design.md
|   +-- backend_logic_design.md
|   +-- rest_server_design.md
|
+-- src/
    +-- database/
    |   +-- schema.sql
    |   +-- test_database.sql
    |
    +-- backend_logic/
    |   +-- LogicaMediciones.php
    |   +-- ejecutar_test.php
    |   +-- tests/
    |       +-- test_logica_mediciones.php
    |
    +-- rest_server/
        +-- index.php
        +-- .htaccess
        +-- tests/
            +-- test_rest.php
```

Los archivos de configuración privada no formarán parte del repositorio.

---

### Organización en Plesk

El despliegue previsto será:

```text
Home directory/
|
+-- httpdocs/
|   +-- api/
|       +-- index.php
|       +-- .htaccess
|
+-- pbio_backend/
    +-- config.local.php
    +-- LogicaMediciones.php
    +-- ejecutar_test.php
    +-- tests/
        +-- test_logica_mediciones.php
```

`httpdocs` contiene únicamente los archivos que deben ser accesibles
a través de HTTP.

La lógica de negocio, las pruebas privadas y las credenciales permanecerán
fuera de `httpdocs`.

La configuración de rutas mediante `.htaccess` se comprobará en Plesk
antes de considerarla definitiva.

---

### Seguridad

La aplicación Android enviará las mediciones al servidor mediante HTTPS.

Antes de permitir el registro público de mediciones, el servidor deberá
disponer de un certificado TLS válido y de un mecanismo de autorización
para las operaciones de escritura.

Para el Sprint 0 se prevé una clave de acceso de demostración enviada
mediante una cabecera HTTP, cuya verificación pertenecerá a la capa REST.

La clave del servidor se almacenará en configuración privada y no en el
repositorio.

Una clave incluida en una aplicación Android no se considera un secreto
inextraíble; este mecanismo solo ofrecerá protección básica para la demo.
Un despliegue de producción requeriría controles de autenticación y
protección adicionales.

El servidor deberá:

- Aceptar JSON válido en las operaciones de registro.
- Rechazar métodos HTTP no admitidos.
- No mostrar contraseñas ni errores internos SQL.
- Utilizar las consultas parametrizadas del backend.
- No permitir acceso HTTP a los archivos privados.
- No habilitar indiscriminadamente CORS para cualquier origen.

La web del mismo dominio podrá consultar la API sin necesidad de habilitar
CORS para otros dominios.

---

### Gestión de errores HTTP

Se definen los siguientes resultados:

| Situación | Código HTTP |
|---|---|
| Consulta correcta | 200 OK |
| Medición registrada | 201 Created |
| Datos inválidos o JSON incorrecto | 400 Bad Request |
| Falta autorización para escritura | 401 Unauthorized |
| Acceso denegado | 403 Forbidden |
| Ruta o medición no encontrada | 404 Not Found |
| Método no permitido | 405 Method Not Allowed |
| Tipo de contenido incorrecto | 415 Unsupported Media Type |
| Error interno de persistencia | 500 Internal Server Error |

Los errores tendrán una estructura uniforme:

```json
{
  "error": "DATOS_INVALIDOS",
  "mensaje": "Los datos proporcionados no son validos"
}
```

La descripción será comprensible para el cliente.

Los mensajes técnicos internos no se mostrarán en las respuestas públicas.

---

### Pruebas REST

Se prepararán pruebas reproducibles para comprobar la comunicación HTTP.

Las pruebas deberán comprobar:

1. Registro de una medición válida mediante POST.
2. Respuesta HTTP 201 al registrar correctamente.
3. Rechazo de un JSON mal formado.
4. Rechazo de campos inválidos.
5. Rechazo de una petición no autorizada.
6. Obtención de varias mediciones mediante GET.
7. Respeto del parámetro `limit`.
8. Obtención de la última medición de O3.
9. Consulta de mediciones por UUID.
10. Respuesta correcta cuando no existen mediciones.
11. Rechazo de métodos HTTP no admitidos.
12. Respuestas JSON válidas.
13. Ausencia de información sensible en los errores.

Las pruebas que inserten registros mediante HTTP se ejecutarán
preferentemente contra una base de datos de pruebas independiente.

No se presupone que una petición HTTP pueda revertirse mediante una
transacción abierta en otra petición.

---

### Demostración final

El profesor podrá introducir una medición ficticia conocida en Arduino.

Ejemplo:

```text
Valor ficticio = 333 ppb
```

El flujo esperado será:

```text
Arduino
   |
   | iBeacon con Minor = 333
   v
Android
   |
   | valor = 333
   v
POST /api/mediciones
   |
   v
Servidor REST
   |
   v
guardarMedicion()
   |
   v
MySQL
   |
   | valor = 333
   v
GET /api/mediciones/ultima?tipo=O3
   |
   v
Aplicación web
   |
   v
333 ppb
```

La medición mostrada en la web deberá coincidir con la enviada por Arduino.

---

## Design Clarifications

- El componente REST utiliza PHP 8.2 o posterior.

- Se despliega en Plesk, utilizando el dominio proporcionado por la UPV.

- La API utilizará la ruta base `/api/`.

- Se definen cuatro endpoints principales de registro y consulta.

- El servidor utiliza la lógica de negocio existente en `LogicaMediciones.php`.

- No se duplicarán las reglas de validación del backend.

- REST transforma peticiones HTTP en llamadas a la lógica de negocio.

- REST transforma resultados y excepciones en respuestas JSON y códigos HTTP.

- La tabla utilizada continúa siendo `medicion`.

- No se crea una tabla independiente `dispositivo`.

- Android enviará el valor de O3 ya interpretado, en ppb y con signo.

- REST no interpreta tramas BLE.

- Las credenciales de MySQL permanecerán fuera de `httpdocs`.

- La versión pública de la API deberá utilizar HTTPS.

- Las operaciones de escritura requerirán protección básica antes de
  habilitarse públicamente.

- El servidor REST se probará independientemente de Android.

- Después de validar REST se implementará su cliente en Android.

- La aplicación web consultará los datos a través de REST y no mediante
  conexiones SQL directas desde el navegador.

---

## General Rules

- **Programming Language:** PHP.

- **HTTP Interface:** Métodos GET y POST.

- **Data Format:** JSON.

- **Database Access:** A través de `LogicaMediciones`.

- **File Headers:** Cada archivo incluirá:

  ```text
  nombre
  descripcion
  copyright
  fecha
  autor
  aportacion
  ```

- **Author:** Álvaro Ballester Grau.

- **Function/Method Headers:** Cada función incorporará su firma lógica y
  descripción mediante comentarios delimitados por líneas discontinuas.

- **Separation of Responsibilities:** REST no implementará reglas de negocio
  que pertenezcan a `backend_logic`.

- **Error Handling:** Las respuestas deberán utilizar códigos HTTP y JSON
  coherentes.

- **Security:** Nunca se incluirán contraseñas de MySQL en archivos públicos
  o en GitHub.

- **Compatibility:** El código deberá ser compatible con PHP 8.2 y el
  alojamiento Plesk utilizado.

- **Automated Testing:** Las rutas REST deberán disponer de pruebas
  reproducibles.

- **Design Consistency:** La implementación deberá mantenerse sincronizada
  con este documento.

- **Repository Consistency:** El diseño:

  ```text
  doc/rest_server_design.md
  ```

  deberá coincidir con el código:

  ```text
  src/rest_server/
  ```

- **Integration:** La comunicación con Android y la web se implementará
  después de validar las rutas REST.