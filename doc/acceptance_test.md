# PBIO Sprint 0 — Criterios de aceptación y evidencias de pruebas

**Autor:** Álvaro Ballester Grau  
**Fecha:** 14/10/2026  
**Estado del sistema:** base de datos del Sprint 0 sin modificaciones; se mantiene la tabla `medicion` de ocho columnas.  
**Propósito:** definir resultados observables y formas reproducibles de verificarlos para cada componente y para el recorrido completo.

## 1. Alcance y condiciones

- Las pruebas se relacionan con los componentes y diseños de `doc/` y `src/`.
- Una prueba de sintaxis o de contrato estático no demuestra, por sí sola, el funcionamiento del hardware ni de la red.
- Las consultas REST de pruebas utilizan el servidor de demostración; los tests `GET` y el `POST` sin token no deben insertar datos.
- No publicar contraseñas, `config.local.php`, `local.properties`, tokens ni APK de depuración con credenciales.
- No ejecutar `src/database/schema.sql` sobre la base de datos que contiene medidas reales: recrea la tabla.

## 2. Criterios de aceptación por componente

| ID | Componente | Entrada o condición de prueba | Resultado esperado | Comprobación reproducible |
|---|---|---|---|---|
| CA-01 | Arduino (`src/arduino`) | Firmware con UUID `EPSG-GTI-PROY-3A`, tipo O3 = 14 y medida positiva o negativa | El Major codifica tipo y contador; el Minor codifica ppb como entero de 16 bits con signo; emisión iBeacon | `node --test src/arduino/tests/test_contrato_ibeacon.mjs` (8 pruebas de contrato estático y ejemplos de codificación). Prueba física BLE adicional |
| CA-02 | Android (`src/phone`) | Trama BLE válida del dispositivo del proyecto | La app interpreta UUID, tipo, contador y valor con signo y los muestra en pantalla | Desde `src/phone`: `.\gradlew.bat test`; comprobación física con placa y teléfono |
| CA-03 | Android, medición fake | Pulsar «Cargar medición fake» | Se muestra O3 = 333 ppb, UUID `EPSG-GTI-PROY-3A`, contador 30 y RSSI -68 | Test `LogicaFakeTelefonoTest.java` mediante Gradle; inspección de la pantalla |
| CA-04 | Base de datos (`src/database`) | Insertar mediciones 333 y -194 ppb, contador 0 y 255 | Guarda valores y fecha, permite consultar última medida; la prueba revierte cambios | Ejecutar `src/database/test_database.sql` en una base de datos de pruebas o sesión autorizada y comprobar los mensajes `OK` y el `ROLLBACK` |
| CA-05 | Lógica de negocio (`src/business_logic`) | Medición válida/incorrecta; guardar, listar y consultar | Acepta dominio válido, rechaza entradas fuera de rango y consulta la tabla `medicion` sin lógica HTTP | `src/business_logic/tests/test_logica_mediciones.php`, con PDO MySQL y variables `PBIO_DB_*`, en entorno autorizado (transacción y rollback) |
| CA-06 | Comunicación REST (`src/communication`) | GET con límites válidos e inválidos; POST sin token | JSON correcto, filtrado por UUID, 400 para entradas inválidas, 404 para rutas inexistentes y 401 para POST sin autorización | `node --test src/communication/tests/test_rest_http.mjs` (11 pruebas HTTP contra HTTPS de Plesk) |
| CA-07 | Proxy del frontend (`src/frontend_business_logic`) | `listarMediciones(5)` y `obtenerUltimaMedicion("O3")` con respuestas simuladas | Lista válida, `null` cuando no hay medida, preserva negativos y comunica errores | `node --test src/frontend_business_logic/tests/test_logica_mediciones_cliente.mjs` (6 pruebas, sin red) |
| CA-08 | GUI web (`src/gui`) | Datos de última medición e historial; pulsar «Actualizar ahora» | Muestra valor O3 y cinco registros más recientes; la GUI utiliza el proxy y no llama HTTP directamente | `node --test src/gui/tests/test_gui.mjs` (3 pruebas estáticas); abrir la URL de la web y comparar con API REST |

## 3. Criterio de aceptación de extremo a extremo (obligatorio en la rúbrica)

**CA-09 — Medida ficticia fijada en el firmware y visible en la web.**

1. Configurar `USAR_MEDIDA_FICTICIA` en el firmware y definir una medida conocida, por ejemplo `MEDIDA_FICTICIA_PPM = 0.333` (333 ppb). Compilar y cargar en el Arduino.
2. Iniciar el escaneo BLE en Android; comprobar el UUID y que el valor interpretado es **333 ppb**.
3. Pulsar «Guardar y enviar al servidor» una sola vez; resultado esperado: **HTTP 201**.
4. Consultar `https://abalgra1.upv.edu.es/api/mediciones/ultima?tipo=O3`: el nuevo registro debe contener **valor = 333**, el mismo UUID y una fecha generada por MySQL.
5. Abrir `https://abalgra1.upv.edu.es/pbio/gui/` y pulsar «Actualizar ahora»: la última lectura mostrada debe ser **333 ppb**, coincidiendo con la respuesta REST.

**Criterio de éxito:** el mismo valor ficticio introducido en Arduino llega sin alteración a Android, MySQL y la web. Si entra otra medida después, repetir la consulta inmediatamente o identificar el registro de prueba por fecha e ID.

**Estado de evidencia:** el recorrido con valores recibidos físicamente por Android y guardados/consultados en la web se ha comprobado (incluidos 119 ppb y -32 ppb). La demostración específica del **mismo valor ficticio fijado en el firmware** queda pendiente de ejecutar como ensayo final ante la entrega; no se debe presentar como realizada sin su evidencia.

## 4. Comandos generales de verificación

Desde la raíz `PBIO-Sprint0`:

```powershell
node --test src/arduino/tests/test_contrato_ibeacon.mjs src/communication/tests/test_rest_http.mjs src/frontend_business_logic/tests/test_logica_mediciones_cliente.mjs src/gui/tests/test_gui.mjs
php -l src/business_logic/LogicaMediciones.php
php -l src/business_logic/tests/test_logica_mediciones.php
php -l src/communication/index.php
cd src/phone
.\gradlew.bat test
cd ../..
```

**Evidencia disponible (14/10/2026):** 28/28 pruebas Node.js correctas, tres validaciones PHP de sintaxis correctas y compilación/pruebas locales de Android con `BUILD SUCCESSFUL`. Estas evidencias no sustituyen los tests de integración PHP/MySQL ni la prueba física de la medida ficticia.

## 5. Cobertura pendiente declarada

- Tests instrumentados específicos de `ClienteRestMediciones.java` (sin modificar el cliente que ya funciona).
- Tests automatizados seguros de POST autorizado y otros fallos del cuerpo JSON, preferentemente en un entorno de pruebas aislado y no contra producción.
- Evidencia de una ejecución final de CA-09 con el valor ficticio introducido en el **código Arduino**, no solo con el botón fake de Android.

La simplificación futura de la tabla `medicion` (por ejemplo, `uuid` a `id_dispositivo` y eliminación de campos no esenciales) queda **fuera del Sprint 0**. Requerirá coordinar SQL, PHP, REST, Android, web, diseños y nuevas pruebas en un sprint posterior.
