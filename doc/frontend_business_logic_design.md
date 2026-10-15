# Frontend Business Logic Component Design

## Component Design

### Objetivo

`frontend_business_logic` proporciona a la interfaz web operaciones de dominio para consultar mediciones del sensor O3. Encapsula las peticiones a `communication` y no expone rutas HTTP, cabeceras ni códigos de transporte a `gui`.

### Contrato de dominio compartido con `business_logic`

Los nombres, tipos de entrada y tipos de salida coinciden con las operaciones equivalentes de `LogicaMediciones` documentadas en `doc/business_logic_design.md`. El cliente solo implementa las dos operaciones que utiliza la GUI.

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

### Interfaz pública del proxy

```text
limite: N --> listarMediciones() --> [Medicion]
tipo: Text --> obtenerUltimaMedicion() --> Medicion?
```
```text
                                         -------- LogicaMedicionesCliente --------
                                         |
                                         | mecanismo_consulta: MecanismoConsulta
                                         |
                                         |
limite: N                      --> listarMediciones() <--
mediciones: [Medicion]         <--
                                         |
                                         |
tipo: Text                --> obtenerUltimaMedicion() <--
medicion: Medicion?       <--
                                         |
                                         ------------------------------------------------

```

`Medicion?` representa una medición o la ausencia de mediciones (`null`). El diseño lógico no expresa la naturaleza asíncrona de JavaScript porque es un detalle técnico de implementación.

### Reglas y condiciones de las operaciones

**`listarMediciones()`**

```text
limite: N --> listarMediciones() --> [Medicion]
```

- Precondición: `1 <= limite <= 100`.
- Resultado: lista de registros recientes; la persistencia y el orden de consulta corresponden al backend.
- Implementación proxy: consulta pública `GET /api/mediciones?limit=...`.

**`obtenerUltimaMedicion()`**

```text
tipo: Text --> obtenerUltimaMedicion() --> Medicion?
```

- Precondición: `tipo` debe ser texto no vacío. En el Sprint 0 se usa `O3`.
- Resultado: última medición del tipo o `null` si no existe.
- Implementación proxy: consulta pública `GET /api/mediciones/ultima?tipo=...`.

### Implementación y dependencias

```text
src/gui/app.js
      |
      v
src/frontend_business_logic/LogicaMedicionesCliente.js
      |
      v
src/communication/ (servidor REST desplegado en /api)
      |
      v
src/business_logic/ --> src/database/
```

Los métodos de dominio no aceptan ni devuelven tipos HTTP. Las consultas de red son un detalle privado del proxy; el mecanismo `fetch` se puede sustituir por una función de prueba para ejecutar tests sin red.

### Pruebas automatizadas

Archivo: `src/frontend_business_logic/tests/test_logica_mediciones_cliente.mjs`.

- El listado devuelve datos y conserva valores negativos.
- Las firmas públicas se invocan con los parámetros diseñados.
- La ausencia de mediciones devuelve `null`.
- Los límites inválidos se rechazan sin contactar el servidor.
- Los errores de servidor y los datos de respuesta no válidos se comunican a la GUI.

## Design Clarifications

- El frontend implementa un subconjunto de la lógica de negocio: solo las dos operaciones que necesita la GUI actual.
- El constructor de JavaScript recibe opcionalmente un mecanismo de consulta para pruebas; es una decisión técnica de inyección de dependencias y no añade operaciones de dominio al contrato que debe coincidir con el backend.
- El backend genera `id_medicion` y `fecha_hora`. El cliente no modifica ni recalcula `valor`.
- Las consultas GET públicas no necesitan el token reservado para POST desde Android.

## General Rules

- **Programming Language:** JavaScript con módulos ES (`.js` en la implementación del proxy y `.mjs` en las pruebas automatizadas).
- **Function/Method Headers:** cada método relevante incluye su diseño lógico en un bloque de comentarios delimitado por líneas discontinuas (`--------------------`).
- **Code Readability:** nombres claros, código autoexplicativo y comentarios solo para documentar interfaces y decisiones relevantes.
- **Automated Testing:** implementar pruebas unitarias de todos los métodos públicos clave, sin acceso real a red ni a MySQL.
