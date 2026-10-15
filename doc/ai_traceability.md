# Trazabilidad de prompts de IA — PBIO Sprint 0

**Autor:** Álvaro Ballester Grau  
**Fecha de documentación:** 15 de octubre de 2026  
**Ámbito:** evidencia documental de los prompts, comparación con diseños e implementación existente y verificación de pruebas disponibles.

## 1. Objetivo y procedencia

Se documentan **seis prompts originales en PDF facilitados por el autor**. Sus copias intactas se encuentran en [`doc/prompts/originales/`](prompts/originales/) y sus transcripciones legibles en [`doc/prompts/`](prompts/README.md). Se han asociado a los diseños y archivos existentes, según la rúbrica del Sprint 0 (apartado «Uso de herramientas IA», 10 %).

**Importante:** No se conocen las fechas exactas de creación ni el registro de ejecuciones históricas de cada prompt. Por ello, el contenido de los PDF se identifica como **solicitud original conservada**, no como prueba autónoma de que se ejecutara literalmente contra una IA ni de que esta generara una versión concreta del código. Este documento registra una **revisión comparativa efectuada el 15/10/2026** a partir de los originales y del ZIP `develop` previamente aportado; la versión de código inspeccionada procede del archivo de auditoría de fecha 14/10/2026. Las modificaciones documentales indicadas después en la conversación no se han vuelto a contrastar desde un nuevo ZIP.

## 2. Evolución del alcance

Los prompts conservados proponen inicialmente un sistema general de `CO2`, `TEMPERATURA` y `RUIDO`, con tablas `DISPOSITIVO` y `MEDICION`, además de listas y selección de dispositivos en la GUI. La implementación que se entrega en **Sprint 0** se especializó en **O3**: Arduino emite una concentración en ppb mediante iBeacon, Android interpreta la trama y envía JSON por HTTPS, PHP valida y almacena la medida en una única tabla `medicion`, y el frontend obtiene la última lectura y el histórico a través de un proxy JavaScript.

Estos cambios **no se ocultan ni se introducen retrospectivamente en los prompts originales**: el diseño definitivo se encuentra en los `doc/*_design.md` y tiene prioridad sobre los ejemplos históricos para evaluar el código vigente.

## 3. Matriz prompt → diseño → implementación → prueba

| Prompt original | Diseño definitivo | Implementación que se relaciona | Prueba / evidencia | Diferencia relevante |
|---|---|---|---|---|
| [01 — BD](prompts/01_database.md) | [`database_design.md`](database_design.md) | `src/database/schema.sql` | `src/database/test_database.sql` | Dos tablas con FK en origen → una tabla `medicion` sin FK en Sprint 0. |
| [02 — Lógica real](prompts/02_business_logic.md) | [`business_logic_design.md`](business_logic_design.md) | `src/business_logic/LogicaMediciones.php` | `src/business_logic/tests/test_logica_mediciones.php` | Operaciones de alta de dispositivo/tipos múltiples → validación e inserción de O3, consultas con límite. |
| [03 — REST](prompts/03_communication.md) | [`communication_design.md`](communication_design.md) | `src/communication/index.php`, `.htaccess` | `src/communication/tests/test_rest_http.mjs` | REST O3, límites, token y HTTPS; no incluye la ruta general `GET /api/dispositivos` del prompt original. |
| [04 — Teléfono](prompts/04_phone.md) | [`phone_design.md`](phone_design.md) | `src/phone/…/LogicaFakeTelefono.java`, `ClienteRestMediciones.java`, `MainActivity.java` | Tests JUnit de Android; integración manual documentada | La medida fake se crea localmente; el envío POST lo realiza otra clase. |
| [05 — Proxy web](prompts/05_frontend_business_logic.md) | [`frontend_business_logic_design.md`](frontend_business_logic_design.md) | `src/frontend_business_logic/LogicaMedicionesCliente.js` | `src/frontend_business_logic/tests/test_logica_mediciones_cliente.mjs` | Dos operaciones (`listarMediciones`, `obtenerUltimaMedicion`) en lugar de las cuatro del prompt antiguo. |
| [06 — UX](prompts/06_gui.md) | [`gui_design.md`](gui_design.md) | `src/gui/index.html`, `app.js`, `estilos.css` | `src/gui/tests/test_gui.mjs` | Panel O3 y 5 medidas recientes en lugar de tres tarjetas y selector de dispositivos. |

> Los enlaces a diseños y código hacen referencia a rutas dentro del repositorio `PBIO-Sprint0`, no a archivos externos. La tabla identifica **correspondencia y diferencias**, no equivalencia literal con las especificaciones antiguas.

## 4. Revisión por componente

### P01. Base de datos — prompt histórico

- **Petición original:** MySQL/MariaDB, dos tablas (`dispositivo` y `medicion`), relación uno-a-muchos, `uuid` único, FK, marca de fecha automática y pruebas de restricciones.
- **Diseño/resultado actual:** `doc/database_design.md` y `src/database/schema.sql` utilizan **una tabla** `medicion` con ocho campos (`id_medicion`, `uuid`, `nombre_dispositivo`, `tipo`, `valor`, `contador`, `rssi`, `fecha_hora`); el identificador del dispositivo viaja como texto. Los valores `O3` pueden ser negativos.
- **Decisión documentada:** se mantuvo el esquema desplegado y probado, sin migración de tablas antes de la entrega.
- **Verificación disponible:** `src/database/test_database.sql` define pruebas SQL. No se ejecutó sobre Plesk durante esta revisión; hacerlo sin aislar datos reales sería inadecuado.

### P02. Lógica de negocio real — prompt histórico

- **Petición original:** funciones para registrar o buscar dispositivos, registrar medidas, obtener dispositivos y consultar medidas de varios tipos; capa independiente de HTTP, uso de PDO y tests.
- **Diseño/resultado actual:** `src/business_logic/LogicaMediciones.php` define `validarMedicion`, `guardarMedicion`, `listarMediciones`, `obtenerUltimaMedicion` y `obtenerMedicionesDispositivo`. Utiliza PDO y errores de dominio sin gestionar rutas o códigos HTTP.
- **Adaptación:** no hay alta separada de dispositivos porque la persistencia actual no tiene tabla `dispositivo`; el único tipo admitido por el contrato es `O3`.
- **Verificación disponible:** `src/business_logic/tests/test_logica_mediciones.php` y sintaxis de PHP; pruebas de integración con MySQL sujetas a configuración del entorno.

### P03. Servidor REST — prompt histórico

- **Petición original:** controlar endpoints GET y POST, traducir errores a HTTP y JSON, evitar SQL en rutas y añadir tests automáticos.
- **Diseño/resultado actual:** GET `/api/mediciones?limit=N`; GET `/api/mediciones/ultima?tipo=O3`; GET `/api/dispositivos/{uuid}/mediciones?limit=N`; POST `/api/mediciones`. La ruta POST exige token `X-PBIO-TOKEN` y HTTPS; la lógica de persistencia reside en `business_logic`.
- **Adaptación:** el servidor final no implementa `GET /api/dispositivos`; un UUID sin medidas devuelve `[]` con HTTP 200, en vez del comportamiento 404 del prompt antiguo.
- **Verificación disponible:** `src/communication/tests/test_rest_http.mjs` contiene 11 casos HTTP, incluidos GET, validación de límites y rechazo de POST sin token. Esos 11 tests se habían comunicado como superados en una ejecución anterior; **no se han vuelto a ejecutar en esta revisión**, que no realiza inserciones sobre Plesk.

### P04. Lógica fake del teléfono — prompt histórico

- **Petición original:** enviar la medición mediante POST/JSON desde Android sin acceso a MySQL ni bloqueo de la interfaz; probar la serialización y errores de red.
- **Diseño/resultado actual:** `LogicaFakeTelefono.crearMedicionFake()` crea una medida `O3` reproducible. `ClienteRestMediciones.generarJSON()` y `enviarMedicion()` encapsulan el JSON y POST HTTPS en un ejecutor; `MainActivity` coordina el botón de envío.
- **Adaptación:** el método conceptual original `registrarMedicion()` no está implementado literalmente en `LogicaFakeTelefono`: se dividieron las responsabilidades entre generación de datos falsos y cliente REST. La UI no realiza la petición de red directamente.
- **Verificación disponible:** tests JUnit de Android para clases de utilidad y lógica fake. No se debe afirmar que están automatizados todos los casos de timeout/errores solicitados por aquel prompt sin nuevas pruebas.

### P05. Lógica fake del navegador — prompt histórico

- **Petición original:** funciones como `obtenerDispositivos()`, `obtenerMedicionesDispositivo(uuid)`, `obtenerUltimaMedicion(tipo)` y `obtenerMediciones()` que encapsulen `fetch` para la UX.
- **Diseño/resultado actual:** `LogicaMedicionesCliente.js` implementa `listarMediciones(limite)` y `obtenerUltimaMedicion(tipo)`; utiliza peticiones GET encapsuladas y un mecanismo inyectable para simular respuestas en tests.
- **Adaptación:** la GUI no necesita selector ni todas las funciones previstas en el prompt original. Las dos firmas del proxy se alinean con métodos del diseño backend.
- **Verificación disponible:** seis pruebas automatizadas sobre el proxy en `test_logica_mediciones_cliente.mjs`.

### P06. Interfaz web — prompt histórico

- **Petición original:** una página con selector de dispositivo, tres tarjetas (`CO2`, `TEMPERATURA`, `RUIDO`), histórico, errores accesibles y prohibición de `fetch()` directo en la interfaz.
- **Diseño/resultado actual:** panel sencillo para el último dato O3 y las cinco lecturas recientes, sin selector de dispositivo; `src/gui/app.js` consume exclusivamente el proxy `LogicaMedicionesCliente`.
- **Adaptación:** se simplificó la interfaz conforme al alcance O3 del Sprint 0, conservando la separación de capas.
- **Verificación disponible:** tres pruebas de estructura de la GUI en `src/gui/tests/test_gui.mjs`; no equivalen a todos los 19 escenarios funcionales propuestos en el PDF antiguo.

## 5. Verificaciones realizadas en esta revisión documental

**Referencia del código:** exportación ZIP de `develop` recibida para auditoría final el 14/10/2026; no se ha aplicado ningún cambio de implementación con estos documentos.

| Comprobación | Estado | Alcance |
|---|---|---|
| Tests de contrato Arduino, lógica de frontend y GUI: `node --test ...` | **17/17 superados** (15/10/2026) | 8 firmware, 6 proxy, 3 GUI; no requieren Plesk. |
| Sintaxis de `src/business_logic/LogicaMediciones.php` | **Correcta** (15/10/2026) | `php -l`, no ejecuta consultas SQL. |
| Sintaxis de `src/business_logic/tests/test_logica_mediciones.php` | **Correcta** (15/10/2026) | `php -l`, no ejecuta integración SQL. |
| Sintaxis de `src/communication/index.php` | **Correcta** (15/10/2026) | `php -l`, no contacta Plesk. |
| Tests REST externos | **11/11 declarados como superados previamente** | No ejecutados en esta revisión. |
| Integración Android real, MySQL y demostración completa | **Experiencias previas comunicadas por el autor** | No repetidas ni medidas automáticamente ahora. |
| Correspondencia exacta de cada prompt original con el código generado | **No verificable retrospectivamente** | Los prompts son históricos; se registran diferencias y archivo final. |

Los resultados de pruebas de esta sección se refieren al **código inspeccionado**, no a una nueva ejecución de los prompts contra un modelo IA.

## 6. Revisión responsable del uso de IA

- Los prompts muestran instrucciones de **alcance por componente**, consultas previas al código, separación de capas, cabeceras lógicas y tests de aceptación.
- Se ha revisado su correspondencia **con los diseños e implementación actuales** y se han documentado cambios significativos, en lugar de fingir correspondencia literal.
- Se conserva autoría y contenido de las seis fuentes PDF, y se presentan transcripciones para poder consultarlas en el repositorio.
- No se incluyen tokens, credenciales ni configuración privada. El despliegue funcional existente no se modifica.
- **Límite de evidencia:** no se conserva en estos PDF la conversación completa en que se hubiera solicitado al modelo implementar cada prompt ni la respuesta exacta del modelo. Para acreditar ese criterio de la rúbrica de manera íntegra se necesitarían esos historiales o evidencias equivalentes auténticas; no se inventan.

## 7. Relación con aceptación del Sprint 0

Consultar [`doc/acceptance_test.md`](acceptance_test.md) para los criterios de aceptación incorporados posteriormente. La prueba presencial de la rúbrica requiere fijar una medida ficticia **en el firmware Arduino**, recibirla en Android, enviarla por REST, almacenarla y comprobar el mismo valor en la web; los tests estáticos, por sí solos, no sustituyen esa demostración.

**Conclusión:** los seis prompts originales están conservados y se ha documentado de manera verificable su relación con las decisiones de arquitectura actuales. La implementación final debe evaluarse contra los diseños definitivos, no contra los ejemplos previos de los PDF.
