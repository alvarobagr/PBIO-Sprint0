# GUI Component Design

## Component Design

### Objetivo

`gui` muestra al usuario las lecturas de O3 y las últimas cinco mediciones recibidas por la API del proyecto PBIO.

### Pantalla principal: Panel de mediciones

- Cabecera: título «Sensor de ozono» y subtítulo del dispositivo GTI-3A.
- Tarjeta de última lectura: `valor` en **ppb** (puede ser negativo), `nombre_dispositivo`, `fecha_hora`, `contador`, `rssi`.
- Tabla con las últimas cinco mediciones: `id_medicion`, `fecha_hora`, `tipo`, `valor`, `contador` y `rssi`.
- Botón «Actualizar ahora».
- Mensaje de estado de la consulta, estado sin datos y estado de error.
- Actualización automática cada 15 segundos mientras la pestaña está visible.
- Diseño adaptable a ordenador y móvil.

### Arquitectura e interfaz

```text
Usuario
   |
   v
src/gui/index.html + app.js + estilos.css
   |
   v
src/frontend_business_logic/LogicaMedicionesCliente.js
   |
   v
Servidor /api --> lógica de negocio --> MySQL
```

La GUI **no** ejecuta `fetch`, `XMLHttpRequest` ni peticiones REST directas. Solo invoca estas firmas del proxy:

```text
limite: N --> listarMediciones() --> [Medicion]
tipo: Text --> obtenerUltimaMedicion() --> Medicion?
```

### Operaciones de presentación

```text
valor: Text|N|Z --> mostrarDato() --> Text
medicion: Medicion? --> presentarUltimaMedicion()
mediciones: [Medicion] --> presentarHistorial()
--> actualizarPanel()
--> inicializarInterfaz()
```

- `mostrarDato`: sustituye la ausencia de un dato por un guion.
- `presentarUltimaMedicion`: presenta los atributos de un registro o guiones si no existe.
- `presentarHistorial`: construye filas a partir de un listado; nunca interpreta campos de usuario como HTML.
- `actualizarPanel`: llama a las dos operaciones del proxy y comunica el estado al usuario.
- `inicializarInterfaz`: registra el botón y el refresco periódico.

### Archivos

```text
src/gui/index.html
src/gui/app.js
src/gui/estilos.css
src/gui/tests/test_gui.mjs
```

### Pruebas

- Comprobar que la GUI no contiene mecanismos de transporte HTTP.
- Comprobar la existencia del botón y los elementos de visualización.
- Comprobar la ausencia de credenciales privadas en los archivos de interfaz.

## Design Clarifications

- La fecha se presenta literalmente como la devuelve MySQL/PHP; no se inventa una zona horaria.
- La GUI utiliza `textContent` para mostrar los datos de la base de datos de forma segura.
- El diseño GUI es descriptivo: el agente v2 indica que su verificación visual queda fuera de alcance.
- Se publica bajo `/pbio/gui/` sin sobrescribir la página principal del dominio ni la carpeta `/api/`.

## General Rules

- **Programming Language:** HTML5, CSS3 y JavaScript ES Modules.
- **Function/Method Headers:** toda función relevante en JavaScript incluye su diseño lógico en un comentario delimitado por líneas discontinuas (`--------------------`).
- **Code Readability:** usar nombres claros, funciones separadas para datos y presentación, comentarios mínimos.
- **Automated Testing:** pruebas de acoplamiento y presencia de controles, más pruebas del proxy del que depende la GUI.
