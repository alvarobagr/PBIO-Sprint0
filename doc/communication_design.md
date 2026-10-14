# Communication Component Design

## Component Design

### 1. Objetivo y límites del componente

El componente `communication` expone la interfaz HTTP/JSON del Sprint 0 para registrar y consultar mediciones del sensor de ozono. Su implementación se encuentra en `src/communication/index.php`, con reglas de enrutamiento de Apache en `src/communication/.htaccess`.

Es responsable de interpretar métodos, rutas, parámetros, cabeceras y cuerpos JSON; comprobar los requisitos de transporte y autorización; invocar `LogicaMediciones` de `src/business_logic/`; y convertir los resultados o errores en respuestas HTTP/JSON.

**No** interpreta anuncios iBeacon, no calcula concentraciones de O3, no contiene consultas SQL ni replica las validaciones de dominio de `LogicaMediciones`.

```text
Android (POST) / Frontend proxy (GET)
                  |
                  | HTTPS / JSON
                  v
         communication (PHP)
                  |
                  | operaciones de dominio
                  v
          business_logic (PHP)
                  |
                  | persistencia
                  v
             database (MySQL)
```

En la aplicación web, `src/gui/` utiliza `src/frontend_business_logic/`, que encapsula las peticiones GET; la GUI no se comunica directamente con este componente.

### 2. Tipos lógicos

Se usan los tipos abstractos de `context/Software_Engineering_Spec.md`. Los siguientes tipos de dominio son los mismos que en `doc/business_logic_design.md`:

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

Para describir exclusivamente la comunicación se definen:

```text
ErrorREST = (
    error: Text,
    mensaje: Text
)

RespuestaHTTP = (
    codigo: N,
    cuerpo_json: Text
)

ConfiguracionPrivada = (
    host: Text,
    port: Text,
    dbname: Text,
    user: Text,
    password: Text,
    api_token: Text
)

ContextoServicio = (
    logica: LogicaMediciones,
    config: ConfiguracionPrivada
)
```

`RespuestaHTTP` representa conceptualmente el código y el contenido JSON serializado que se envían al cliente. La implementación PHP escribe esa respuesta y finaliza la ejecución, por lo que sus funciones de respuesta se declaran `void` en código. `ConfiguracionPrivada` describe los campos esperados, **no** sus valores; `api_token` se consulta al registrar mediciones. La representación real del contexto en PHP es una colección con la instancia de negocio y la configuración.

`Medicion?` representa una medición o la ausencia de resultado (`null`); `[Medicion]` representa una colección de mediciones.

### 3. Interfaz HTTP pública

La API se publica en `/api/` sobre HTTPS, en el mismo dominio que la web:

| Método | Ruta | Invocación de negocio | Respuesta correcta |
|---|---|---|---|
| `GET` | `/api/mediciones?limit=5` | `listarMediciones(limite)` | `200` + `[Medicion]` |
| `GET` | `/api/mediciones/ultima?tipo=O3` | `obtenerUltimaMedicion(tipo)` | `200` + `Medicion` |
| `GET` | `/api/dispositivos/{uuid}/mediciones?limit=20` | `obtenerMedicionesDispositivo(uuid, limite)` | `200` + `[Medicion]` |
| `POST` | `/api/mediciones` | `guardarMedicion(entrada)` | `201` + `Medicion` |

**Consultas GET:** no necesitan token. El parámetro opcional `limit` utiliza `20` por defecto; si se proporciona debe ser un entero decimal entre `1` y `100`, sin caracteres adicionales. Para el Sprint 0, la consulta de última medición admite `tipo=O3`. Cuando una consulta de listado no tiene resultados devuelve `200` y `[]`; cuando no hay última medición del tipo indicado devuelve `404`.

**Registro POST:** requiere HTTPS, cabecera `X-PBIO-TOKEN` coincidente con la configuración privada, `Content-Type: application/json` y un objeto JSON con los seis campos de `MedicionEntrada`. El cuerpo no puede superar `16 KiB`. Android interpreta previamente la trama BLE: `valor` llega en ppb como entero con signo y el servidor no interpreta Major ni Minor.

Ejemplo de cuerpo POST, **sin claves ni credenciales**:

```json
{
  "uuid": "EPSG-GTI-PROY-3A",
  "nombre_dispositivo": "GTI-3A",
  "tipo": "O3",
  "valor": -32,
  "contador": 7,
  "rssi": -46
}
```

La base de datos genera `id_medicion` y `fecha_hora`; el componente REST no asigna esos valores.

### 4. Funciones de `src/communication/index.php`

Este componente está implementado con **funciones de módulo, no con una clase**. Se documentan a continuación todas las funciones que existen en su punto de entrada. Las firmas representan la intención lógica; los accesos a variables del entorno web, la serialización y el final de ejecución son detalles de PHP.

```text
codigo: N, datos: Text       --> responderJSON() --> RespuestaHTTP

codigo: N, error: Text,
mensaje: Text                --> responderError() --> RespuestaHTTP

                            --> peticionHTTPS() --> B

                            --> crearLogicaMediciones() --> ContextoServicio

                            --> leerLimite() --> N

logica: LogicaMediciones     --> consultarMediciones() --> RespuestaHTTP

logica: LogicaMediciones     --> consultarUltimaMedicion() --> RespuestaHTTP

logica: LogicaMediciones,
uuid: Text                   --> consultarMedicionesDispositivo() --> RespuestaHTTP

logica: LogicaMediciones,
config: ConfiguracionPrivada --> registrarMedicion() --> RespuestaHTTP

                            --> procesarPeticion() --> RespuestaHTTP
```

`datos: Text` expresa el contenido JSON lógico de la respuesta; en el código PHP `responderJSON()` recibe el dato estructurado y lo serializa. Las operaciones `consultar*()` y `registrarMedicion()` responden por el canal HTTP y terminan la petición, aunque su tipo de retorno en PHP sea `void`.

#### `responderJSON()` y `responderError()`

- `responderJSON(codigo, datos)` fija el estado HTTP, anuncia JSON UTF-8, deshabilita caché y devuelve la representación serializada.
- `responderError(codigo, error, mensaje)` construye un `ErrorREST` y delega la respuesta en `responderJSON()`.
- No se envían credenciales, trazas ni detalles internos de MySQL.

#### `peticionHTTPS()`

- Comprueba la indicación HTTPS proporcionada por el servidor web; la implementación también contempla `X-Forwarded-Proto` para el despliegue detrás de un proxy.
- Devuelve `true` o `false`. Una escritura sin HTTPS se rechaza con `403`.
- La confianza en cabeceras de proxy depende de que la infraestructura de despliegue esté configurada para ello; no se considera autenticación por sí misma.

#### `crearLogicaMediciones()`

- Localiza `pbio_backend/config.local.php` y `pbio_backend/LogicaMediciones.php` **fuera de `httpdocs`**.
- Lee y verifica los datos mínimos de conexión, crea una conexión `PDO` con MySQL y construye `LogicaMediciones` mediante inyección de esa conexión.
- Devuelve conceptualmente `ContextoServicio`, formado por la instancia de lógica y la configuración privada.
- La lógica de negocio no depende del protocolo HTTP; la capa de comunicación sí puede utilizar la lógica de negocio.

#### `leerLimite()`

- Consulta el parámetro opcional `limit`, con valor por defecto `20`.
- Valida que sea decimal entero entre `1` y `100`, incluidos los extremos.
- Si no es válido se comunica un error de entrada que se convierte en respuesta HTTP `400`.

#### `consultarMediciones()`

- Obtiene el límite con `leerLimite()`, invoca `LogicaMediciones.listarMediciones(limite)` y devuelve `200` con un array JSON.

#### `consultarUltimaMedicion()`

- Exige `tipo=O3`, invoca `LogicaMediciones.obtenerUltimaMedicion(tipo)` y devuelve `200` con un objeto JSON o `404` si el resultado es ausente.

#### `consultarMedicionesDispositivo()`

- Obtiene `uuid` de la ruta, lee el límite e invoca `LogicaMediciones.obtenerMedicionesDispositivo(uuid, limite)`.
- Devuelve `200` con una lista, que puede estar vacía. Las reglas de validez del UUID corresponden al componente de negocio.

#### `registrarMedicion()`

- Comprueba HTTPS, la configuración de la clave y la cabecera `X-PBIO-TOKEN` mediante comparación segura.
- Exige `application/json`, limita el cuerpo a `16 KiB` y comprueba que el JSON represente un objeto.
- Invoca `LogicaMediciones.guardarMedicion(entrada)`, que aplica las validaciones del dominio y realiza la persistencia.
- Devuelve `201` con la medición creada; un objeto inválido provoca `400`.

#### `procesarPeticion()`

- Lee método y ruta; reconoce exactamente las cuatro combinaciones descritas en la tabla de endpoints.
- Rechaza rutas desconocidas con `404` y métodos no admitidos con `405` y la cabecera `Allow` correspondiente.
- Construye el contexto mediante `crearLogicaMediciones()` y delega en la función de comunicación pertinente.
- El punto de entrada convierte `InvalidArgumentException` en `400` y registra errores inesperados sin exponer detalles privados, respondiendo `500`.

### 5. Contrato de errores HTTP

| Situación | Código HTTP | Identificador representativo |
|---|---|---|
| Consulta correcta | `200` | — |
| Registro correcto | `201` | — |
| Datos o JSON inválidos | `400` | `DATOS_INVALIDOS`, `JSON_INVALIDO` |
| Token ausente o incorrecto | `401` | `NO_AUTORIZADO` |
| Escritura sin HTTPS | `403` | `HTTPS_REQUERIDO` |
| Ruta desconocida o última medición inexistente | `404` | `RUTA_NO_ENCONTRADA`, `NO_ENCONTRADO` |
| Método no permitido | `405` | `METODO_NO_PERMITIDO` |
| Cuerpo superior a 16 KiB | `413` | `CUERPO_DEMASIADO_GRANDE` |
| Content-Type incorrecto | `415` | `TIPO_CONTENIDO_INVALIDO` |
| Error interno o de persistencia | `500` | `ERROR_INTERNO` |
| Token de escritura no configurado correctamente | `503` | `API_NO_CONFIGURADA` |

Formato de error:

```json
{
  "error": "DATOS_INVALIDOS",
  "mensaje": "Los datos proporcionados no son validos"
}
```

### 6. Estructura del repositorio y despliegue

Estructura lógica del código fuente:

```text
PBIO-Sprint0/
├── doc/
│   ├── communication_design.md
│   ├── business_logic_design.md
│   └── database_design.md
└── src/
    ├── communication/
    │   ├── index.php
    │   └── .htaccess
    ├── business_logic/
    │   ├── LogicaMediciones.php
    │   └── tests/
    └── database/
        ├── schema.sql
        └── test_database.sql
```

Despliegue real de Plesk (las rutas del servidor **no** se renombran por cambiar los nombres del repositorio):

```text
Home directory/
├── httpdocs/
│   ├── api/
│   │   ├── index.php
│   │   └── .htaccess
│   └── pbio/
│       ├── gui/
│       └── frontend_business_logic/
└── pbio_backend/
    ├── config.local.php      (privado; excluido de Git)
    └── LogicaMediciones.php  (privado)
```

`.htaccess` utiliza `RewriteBase /api/` y dirige las rutas REST a `index.php`. El archivo `config.local.php` incluye credenciales y clave de escritura solo en el servidor y no forma parte del repositorio. El servidor REST despliega el PHP de `src/communication/` en `httpdocs/api/`; el código de `src/business_logic/` se copia a `pbio_backend/`.

### 7. Pruebas de comunicación

Se han comprobado manualmente en Plesk las tres rutas GET y el POST autorizado con respuesta `201`; también se ha verificado el envío desde Android y su visualización posterior en la web.

Las **pruebas HTTP automatizadas aún están pendientes**. Se deberán añadir bajo `src/communication/tests/`, preferentemente sobre un entorno de pruebas independiente, y cubrir como mínimo: GET de lista y última medición; filtro por UUID; POST válido; token ausente/erróneo; JSON incorrecto; tipo de contenido; límites inválidos; métodos no permitidos y errores sin información sensible. Las pruebas que creen registros no deben alterar los datos de producción.

## Design Clarifications

- `communication` se ocupa de HTTP/JSON y de autorizar la escritura; `business_logic` aplica reglas de dominio y utiliza la tabla `medicion` descrita en `database_design.md`.
- Los contratos de dominio compartidos con `business_logic` son `MedicionEntrada` y `Medicion`. Los tipos de transporte (`RespuestaHTTP`, `ErrorREST`) pertenecen exclusivamente a este diseño.
- En el repositorio los nombres siguen `communication` y `business_logic`; en el alojamiento Plesk se conservan `api` y `pbio_backend` porque son rutas de despliegue independientes.
- El token de demostración no se publica en Git ni se incluye en este documento. La inclusión de una clave en una APK de depuración no la convierte en secreto seguro para distribución pública.
- Las peticiones GET de la web se canalizan mediante el proxy `frontend_business_logic`; el navegador consulta la API del mismo origen sin CORS público.
- La futura modificación del esquema MySQL requerirá revisar conjuntamente `database_design.md`, `business_logic_design.md` y este contrato si cambian los campos de `Medicion`.

## General Rules

- **Programming Language:** PHP 8.2 o posterior para el servidor REST; Apache `.htaccess` para las reglas de reescritura en Plesk.
- **Function/Method Headers:** cada función debe documentar su diseño lógico en un bloque de comentarios delimitado por líneas discontinuas (`--------------------`).
- **Code Readability:** nombres claros y código autoexplicativo, evitando comentarios adicionales innecesarios.
- **Automated Testing:** generar pruebas unitarias o de integración para todas las funciones importantes y rutas HTTP; mantenerlas independientes de la base de datos de producción.
- **Architecture:** las dependencias son `communication → business_logic → database`; ninguna función del negocio depende de HTTP ni de `src/communication/`.
- **Security:** no publicar tokens, contraseñas, trazas SQL ni ficheros privados; requerir HTTPS y token para el POST.
- **Consistency:** mantener la correspondencia entre `doc/communication_design.md` y `src/communication/`, y actualizar ambas partes cuando cambie el contrato.