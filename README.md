# PBIO — Sprint 0

**Autor:** Álvaro Ballester Grau  
**Asignatura:** Proyecto de Aplicaciones de Biometría y Medio Ambiente (PBIO)  
**Titulación:** Grado en Tecnologías Interactivas — Universitat Politècnica de València

## 1. Objetivo

El proyecto registra mediciones de ozono (O3) obtenidas mediante un nodo Bluetooth Low Energy (BLE), las envía desde una aplicación Android a un servidor REST, las guarda en MySQL y las presenta en una página web.

Recorrido funcional:

```text
SparkFun nRF52840 Mini + sensor O3 (o medida ficticia en Arduino)
    ↓ publicidad BLE / iBeacon
Aplicación Android: decodifica UUID, Major y Minor
    ↓ envío manual HTTPS/JSON (POST con token)
Servidor REST PHP (capa communication)
    ↓
Lógica de negocio PHP (capa business_logic)
    ↓
Base de datos MySQL (tabla medicion)
    ↑
Consultas REST GET
    ↑
Proxy de lógica de negocio JavaScript (frontend_business_logic)
    ↑
Interfaz web (gui)
```

En el protocolo iBeacon, el byte alto de **Major** identifica el tipo de medida (14 = O3), el byte bajo transporta el contador y **Minor** transporta el valor en ppb codificado como entero de 16 bits con signo. La aplicación Android interpreta ese valor y permite enviar la última medición con un botón. También dispone de una medición ficticia de prueba.

## 2. Estructura del repositorio

```text
PBIO-Sprint0/
├── author.md
├── README.md
├── doc/
│   ├── arduino_design.md
│   ├── phone_design.md
│   ├── database_design.md
│   ├── business_logic_design.md
│   ├── communication_design.md
│   ├── frontend_business_logic_design.md
│   ├── gui_design.md
│   └── INSTRUCCIONES_DESPLIEGUE_WEB.md
└── src/
    ├── arduino/
    │   ├── leeryemitir/leeryemitir.ino
    │   └── tests/test_contrato_ibeacon.mjs
    ├── phone/                       # Proyecto Android / Gradle
    ├── database/
    │   ├── schema.sql
    │   └── test_database.sql
    ├── business_logic/
    │   ├── LogicaMediciones.php
    │   └── tests/test_logica_mediciones.php
    ├── communication/
    │   ├── index.php
    │   ├── .htaccess
    │   └── tests/test_rest_http.mjs
    ├── frontend_business_logic/
    │   ├── LogicaMedicionesCliente.js
    │   ├── package.json
    │   └── tests/test_logica_mediciones_cliente.mjs
    └── gui/
        ├── index.html
        ├── app.js
        ├── estilos.css
        └── tests/test_gui.mjs
```

Los documentos de `doc/` describen los componentes conforme a la especificación de diseño de la asignatura. La **lógica de negocio del servidor no depende de las rutas ni los códigos HTTP**. La GUI web consume únicamente la interfaz del proxy `frontend_business_logic`.

## 3. Requisitos de desarrollo

- **Firmware:** Arduino IDE, soporte de placas nRF52 y biblioteca Bluefruit compatible con la SparkFun nRF52840 Mini; placa y sensor para ensayos físicos.
- **Android:** Android Studio con SDK configurado; aplicación Java `com.example.abalgra1.pbio`, API mínima 26. Para probar BLE real hace falta un dispositivo Android con Bluetooth Low Energy y permisos concedidos.
- **Servidor:** alojamiento Plesk con PHP, PDO MySQL y base de datos MySQL.
- **Pruebas JavaScript y de contrato:** Node.js con soporte para `node --test` y `fetch` (recomendado Node.js 18 o posterior).

## 4. Configuración y ejecución de los componentes

### 4.1. Arduino

Abrir `src/arduino/leeryemitir/leeryemitir.ino` en Arduino IDE, seleccionar la placa/configuración correspondiente y compilar/cargar el firmware.

El firmware permite escoger entre medida real y ficticia mediante `USAR_MEDIDA_FICTICIA`; cuando se utiliza el modo ficticio, `MEDIDA_FICTICIA_PPM` define el valor. Ejemplo: `0.333` ppm se codifica como `333` ppb en Minor. Para validar el recorrido de extremo a extremo puede utilizarse ese modo de demostración.

### 4.2. Android

Abrir `src/phone/` como proyecto en Android Studio y sincronizar Gradle. En el archivo local `src/phone/local.properties`, conservar `sdk.dir` e incorporar la propiedad **privada** `PBIO_API_TOKEN` con el token del servidor de pruebas, sin comillas:

```properties
PBIO_API_TOKEN=VALOR_PRIVADO_CONFIGURADO_EN_PLESK
```

Gradle incorpora esa propiedad únicamente a la compilación `debug` por medio de `BuildConfig`. **No publicar `local.properties` ni distribuir la APK de depuración**: cualquier token incrustado en una APK puede extraerse. La versión `release` no lleva esa credencial de demostración.

Ejecutar la aplicación en un teléfono Android. Pulsar «Buscar nuestro dispositivo BTLE» para detectar el sensor, o «Cargar medición fake» para una prueba sin placa. El botón «Guardar y enviar al servidor» envía **una única medición por pulsación**; no envía automáticamente todos los anuncios BLE.

### 4.3. Base de datos

El esquema de `src/database/schema.sql` define la tabla `medicion` con las columnas `id_medicion`, `uuid`, `nombre_dispositivo`, `tipo`, `valor`, `contador`, `rssi` y `fecha_hora`.

**Atención: `schema.sql` ejecuta `DROP TABLE IF EXISTS medicion`. No utilizarlo en la base de datos de Plesk que ya contiene las medidas reales; eliminaría sus registros.** Utilizarlo únicamente al preparar una base nueva o con copia de seguridad y autorización para reinicializarla.

El archivo `src/database/test_database.sql` contiene pruebas SQL que utilizan transacciones y terminan con `ROLLBACK`.

### 4.4. Servidor PHP y Plesk

La implementación fuente del repositorio está separada en `src/communication/` y `src/business_logic/`. En el alojamiento utilizado para la demostración, los archivos se organizan del siguiente modo:

```text
Directorio privado del alojamiento/
├── pbio_backend/
│   ├── config.local.php               # Configuración privada, no subir a Git
│   └── LogicaMediciones.php
└── httpdocs/
    ├── api/
    │   ├── index.php
    │   └── .htaccess
    └── pbio/
        ├── frontend_business_logic/
        │   └── LogicaMedicionesCliente.js
        └── gui/
            ├── index.html
            ├── app.js
            └── estilos.css
```

`config.local.php` guarda exclusivamente en Plesk los datos de conexión a MySQL y el token de autorización. **Nunca se incluye en el repositorio.** La ruta del backend privado del alojamiento es distinta del nombre de la carpeta fuente `src/business_logic/`.

Los puntos de entrada de la API son:

| Operación | Ruta | Descripción |
|---|---|---|
| GET | `/api/mediciones?limit=5` | Lista las últimas mediciones. |
| GET | `/api/mediciones/ultima?tipo=O3` | Consulta la última medición O3. |
| GET | `/api/dispositivos/EPSG-GTI-PROY-3A/mediciones?limit=5` | Filtra por UUID. |
| POST | `/api/mediciones` | Registra una medición JSON; requiere HTTPS y cabecera `X-PBIO-Token`. |

El servidor devuelve JSON y usa la lógica de negocio para validar, guardar y consultar medidas.

### 4.5. Página web

URL de la demostración:

`https://abalgra1.upv.edu.es/pbio/gui/`

La página muestra la última medida de O3 y las cinco lecturas más recientes. Ofrece un botón «Actualizar ahora» y refresco automático cada 15 segundos mientras la pestaña está visible. El cliente JavaScript consulta la API del mismo origen; las consultas GET no necesitan el token de POST.

Para publicar o actualizar la web, seguir `doc/INSTRUCCIONES_DESPLIEGUE_WEB.md`. No sobrescribir `httpdocs/api/` al actualizar los archivos del frontend.

## 5. Pruebas automatizadas

Ejecutar los comandos siguientes **desde la raíz de `PBIO-Sprint0`**, salvo que se indique lo contrario.

**Pruebas de contrato Arduino (Node.js):**

```powershell
node --test src/arduino/tests/test_contrato_ibeacon.mjs
```

**Pruebas del frontend y GUI (Node.js, sin conexión a MySQL):**

```powershell
node --test src/frontend_business_logic/tests/test_logica_mediciones_cliente.mjs src/gui/tests/test_gui.mjs
```

**Pruebas HTTP REST (Node.js, contra el servidor HTTPS publicado):**

```powershell
node --test src/communication/tests/test_rest_http.mjs
```

Este último conjunto utiliza consultas GET y verifica el rechazo de un POST sin token; no requiere credenciales y no está diseñado para insertar medidas.

**Pruebas unitarias Android (PowerShell, desde `src/phone/`):**

```powershell
cd src/phone
.\gradlew.bat test
cd ../..
```

**Comprobación de sintaxis PHP (requiere PHP CLI):**

```powershell
php -l src/business_logic/LogicaMediciones.php
php -l src/business_logic/tests/test_logica_mediciones.php
php -l src/communication/index.php
```

**Pruebas de integración PHP/MySQL:** `src/business_logic/tests/test_logica_mediciones.php`. Requieren PHP CLI, extensión PDO MySQL y variables de entorno `PBIO_DB_HOST`, `PBIO_DB_PORT`, `PBIO_DB_NAME`, `PBIO_DB_USER` y `PBIO_DB_PASSWORD`. Su ejecución debe realizarse en un entorno autorizado con conexión a la base de datos; el test utiliza transacciones para revertir los cambios. En Plesk se ejecutó mediante un lanzador privado del alojamiento que no forma parte del repositorio.

Las pruebas automatizadas no sustituyen la demostración física del nodo ni la prueba de envío de Android.

## 6. Demostración del Sprint 0

1. Compilar/cargar el firmware Arduino, seleccionando una medida ficticia controlada si se desea.
2. Abrir la aplicación Android y comprobar la recepción BLE, UUID, tipo, contador y valor en ppb.
3. Pulsar «Guardar y enviar al servidor» y comprobar la confirmación HTTP 201.
4. Abrir `/api/mediciones/ultima?tipo=O3` y verificar que devuelve el nuevo registro.
5. Abrir `/pbio/gui/` y comprobar que el valor y el historial coinciden con el servidor; utilizar «Actualizar ahora» si es necesario.

## 7. Seguridad y observaciones

- Mantener fuera del repositorio `local.properties`, `config.local.php`, `.env`, contraseñas y tokens.
- No compartir una APK `debug` que lleve la credencial incorporada. Este mecanismo de token está destinado exclusivamente a la demostración académica.
- No ejecutar `schema.sql` contra una base de datos con mediciones que se deban conservar.
- Los cambios futuros del modelo de datos deben reflejarse conjuntamente en SQL, lógica de negocio, API, aplicaciones y documentos de diseño.
