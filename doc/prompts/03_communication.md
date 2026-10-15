# Prompt original 03 — Servidor REST

> **Registro histórico.** Transcripción legible del PDF aportado por el autor; **no se ha adaptado ni corregido el contenido del prompt original**. La copia PDF exacta está en [`originales/03_communication.pdf`](originales/03_communication.pdf). Las diferencias con el Sprint 0 actual se explican en [`../ai_traceability.md`](../ai_traceability.md).

---

Quiero que me ayudes a generar el servidor REST de mi proyecto PBIO.

La lógica de negocio real ya la tenemos hecha y la base de datos también.

En este prompt quiero hacer solamente la capa REST.

La arquitectura es:

cliente
  ->
REST
  ->
lógica de negocio
  ->
BBDD

No quiero que las rutas REST hagan directamente consultas SQL.

Tampoco quiero que vuelvas a programar las funciones de negocio dentro de las
rutas.

Quiero que el REST reciba la petición, saque los datos, llame a la función de
negocio que corresponda y devuelva una respuesta JSON.


FUNCIONES DE NEGOCIO QUE YA EXISTEN


Tenemos:

obtenerORegistrarDispositivo()

registrarMedicion()

obtenerMediciones()

obtenerUltimaMedicion()

obtenerMedicionesDispositivo()

obtenerDispositivos()

Quiero que el REST utilice estas funciones.

No las dupliques.


POST /api/mediciones


Quiero esta ruta:

POST /api/mediciones

Android enviará algo así:

{
    "uuid": "EPSG-GTI-PROY-3A",
    "nombre_dispositivo": "GTI3A-2025",
    "tipo": "CO2",
    "valor": 123,
    "contador": 27,
    "rssi": -68
}

RSSI puede faltar.

No se envía:

id_medicion
id_dispositivo
fecha_hora

Quiero que:

1. leas el JSON;
2. compruebes que sea válido;
3. prepares los datos;
4. llames a registrarMedicion();
5. devuelvas la medición creada en JSON.

Si funciona:

HTTP 201

Si el JSON es incorrecto:

HTTP 400

Si la lógica dice que los datos no son válidos:

HTTP 400

Si ocurre un error interno:


HTTP 500


GET /api/mediciones


Quiero:

GET /api/mediciones

Tiene que llamar a:

obtenerMediciones()

y devolver el resultado en JSON.

Si todo funciona:

HTTP 200


GET ÚLTIMA MEDICIÓN


Quiero:

GET /api/mediciones/ultima?tipo=CO2

También tiene que funcionar con:

TEMPERATURA
RUIDO

Tiene que llamar a:

obtenerUltimaMedicion(tipo)

Si falta tipo:

400

Si el tipo es incorrecto:

400


Si no existe ninguna medición:

404

Si existe:

200


MEDICIONES DE UN DISPOSITIVO


Quiero:

GET /api/dispositivos/{uuid}/mediciones

Tiene que llamar a:

obtenerMedicionesDispositivo(uuid)

Si el dispositivo no existe:

404

Si el dispositivo existe pero no tiene mediciones:

200

y devolver:

[]

Si tiene mediciones:

200

y devolver la lista.


DISPOSITIVOS


Quiero:

GET /api/dispositivos


Tiene que llamar a:

obtenerDispositivos()

y devolver los dispositivos en JSON.


RESPUESTAS


Quiero que toda la API devuelva JSON.

En errores quiero algo parecido a:

{
    "error": true,
    "mensaje": "Descripción del error"
}

Usa códigos HTTP correctamente:

200
201
400
404
405
500

Si se utiliza un método que no corresponde con una ruta:

405


IMPORTANTE


No quiero reglas de negocio complejas dentro del REST.

Por ejemplo, registrarMedicion() ya valida el tipo, contador, etc.

La capa REST puede comprobar que la petición tenga un JSON válido y que existan
los datos necesarios para llamar a la función.

Pero no quiero tener la misma lógica repetida en dos sitios.


ERRORES


La lógica de negocio puede devolver o lanzar errores como:

DatosMedicionInvalidos

TipoMedicionNoValido

DispositivoNoEncontrado

MedicionNoEncontrada

Quiero que los traduzcas a HTTP.

Por ejemplo:

DatosMedicionInvalidos -> 400

TipoMedicionNoValido -> 400

DispositivoNoEncontrado -> 404

MedicionNoEncontrada -> 404

errores inesperados -> 500

No enseñes al cliente:

stack traces
SQL
passwords
errores internos de PHP
rutas internas


ORGANIZACIÓN


Intenta separar:

- rutas;
- lectura de parámetros;
- llamada a lógica de negocio;
- respuestas JSON;
- gestión de errores.


No quiero todas las cosas mezcladas en un único archivo enorme si no hace falta.

Si el código que nos dio el profesor ya tiene alguna parte REST que se pueda
reutilizar, utilízala.


COMENTARIOS


Quiero comentarios en español.

No pongas comentarios obvios en cada línea.

Quiero cabeceras en las funciones importantes usando nuestra notación.

Ejemplo:

// cuerpo: Text --> procesarRegistroMedicion() --> Medicion

Otro:

// tipo: Text --> procesarUltimaMedicion() --> Medicion

Otro:

// codigo: N, datos: Text --> enviarRespuestaJSON()

Usamos:

N = naturales
Z = enteros
R = reales
B = booleanos
Text = texto
[Tipo] = lista


TESTS


Quiero tests específicos del REST.

Aquí sí quiero probar las rutas.

Como mínimo:


1. POST correcto devuelve 201.

2. La respuesta del POST es JSON.

3. JSON incorrecto devuelve 400.

4. Falta UUID devuelve 400.

5. Tipo incorrecto devuelve 400.

6. Contador incorrecto devuelve 400.

7. GET /api/mediciones devuelve 200.

8. GET /api/mediciones devuelve una lista JSON.

9. Última medición devuelve 200 si existe.

10. Última medición sin tipo devuelve 400.

11. Última medición inexistente devuelve 404.

12. GET /api/dispositivos devuelve 200.

13. Mediciones de dispositivo existente devuelve 200.

14. Dispositivo inexistente devuelve 404.

15. Dispositivo sin mediciones devuelve [].

16. Método HTTP incorrecto devuelve 405.

17. Un error inesperado devuelve 500 sin enseñar información interna.

18. Comprobar Content-Type application/json.


ANTES DE PROGRAMAR


Primero:

1. Mira el backend que ya existe.
2. Localiza la lógica de negocio.
3. Mira si hay algún código REST del profesor que podamos reutilizar.
4. Dime qué archivos vas a crear o modificar.


5. Comprueba que no estás metiendo SQL dentro de las rutas.
6. Comprueba que no estás duplicando la lógica del negocio.

Después implementa el REST y los tests.

Al final dime:

- qué rutas funcionan;
- qué archivos has creado;
- qué archivos has modificado;
- qué funciones de negocio utilizas;
- qué códigos HTTP utilizas;
- qué tests has hecho;
- cuáles pasan;
- cuáles fallan;
- y qué problemas has encontrado.

No quiero que en este prompt hagas Android, web, UX, BBDD ni lógica del negocio.

Solo quiero el servidor REST.
