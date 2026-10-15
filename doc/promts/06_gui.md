# Prompt original 06 — UX / interfaz web

> **Registro histórico.** Transcripción legible del PDF aportado por el autor; **no se ha adaptado ni corregido el contenido del prompt original**. La copia PDF exacta está en [`originales/06_gui.pdf`](originales/06_gui.pdf). Las diferencias con el Sprint 0 actual se explican en [`../ai_traceability.md`](../ai_traceability.md).

---

Quiero que me ayudes a hacer la UX de la web de mi proyecto PBIO.

La lógica fake del navegador ya la tenemos hecha, así que en este prompt quiero
trabajar solamente con la interfaz que ve y utiliza el usuario.

La arquitectura es:

usuario
  ->
UX
  ->
lógica fake
  ->
REST
  ->
lógica real
  ->
BBDD

Muy importante: no quiero fetch() directamente en el código de la UX.

Para obtener los datos tienes que utilizar las funciones de la lógica fake.


FUNCIONES QUE YA TENEMOS


Tenemos funciones como:

obtenerDispositivos()

obtenerMedicionesDispositivo(uuid)

obtenerUltimaMedicion(tipo)

obtenerMediciones()

Quiero que la interfaz utilice estas funciones.

No las vuelvas a programar.


QUÉ QUIERO VER


Quiero una sola página principal.


Arriba:

PBIO - MEDICIONES

Después:

Dispositivo:
[ GTI3A-2025 ▼ ]


Después quiero tres tarjetas:

CO2

último valor


TEMPERATURA

último valor


RUIDO

último valor


Y debajo:

HISTÓRICO DE MEDICIONES

con una tabla:

Tipo | Valor | Contador | RSSI | Fecha/hora


No quiero una web muy complicada.

Quiero que sea limpia, clara y que funcione bien.


AL ABRIR LA WEB


Quiero que primero salga:


"Cargando..."

Después llama a:

obtenerDispositivos()

Si hay dispositivos quiero que seleccione el primero y cargue sus mediciones.

Si no hay dispositivos:

"No hay dispositivos registrados."


SELECTOR


Quiero un selector para cambiar de dispositivo.

En pantalla se puede mostrar el nombre, por ejemplo:

GTI3A-2025

pero internamente utilizamos su UUID.

Cuando cambie de dispositivo quiero:

seleccionar dispositivo
  ->
obtenerMedicionesDispositivo(uuid)
  ->
actualizar tarjetas
  ->
actualizar histórico

No quiero que se recargue toda la página.


TARJETAS


Quiero tres:

CO2
TEMPERATURA
RUIDO


Cada una muestra el último valor de ese tipo.

Si hay fecha/hora también se puede mostrar.

Si no tenemos ninguna medición de ese tipo:

"Sin datos"

No pongas 0 para representar que no existen datos.


HISTÓRICO


La tabla tiene:

Tipo
Valor
Contador
RSSI
Fecha/hora

Las mediciones más recientes tienen que salir primero.

Si RSSI es null:

-

Si no hay mediciones:

"No existen mediciones para este dispositivo."


ERRORES Y ESTADOS


Quiero gestionar cosas como:

"Cargando..."

"No hay dispositivos registrados."

"No existen mediciones para este dispositivo."

"No se ha podido conectar con el servidor."


"Dispositivo no encontrado."

"Error al obtener los datos."

No quiero que aparezcan al usuario errores técnicos, stack traces ni objetos
raros de JavaScript.

Si quieres dejar información técnica para nosotros puedes utilizar console.


SEPARACIÓN


La UX puede:

- mostrar dispositivos;
- mostrar mediciones;
- actualizar la tabla;
- actualizar tarjetas;
- mostrar errores;
- reaccionar a cambios del usuario.

Pero no quiero que haga:

fetch()

URLs

rutas REST

acceso a BBDD

Eso pertenece a otras capas.


FUNCIONES DE INTERFAZ


Quiero una estructura más o menos así:

dispositivos: [Dispositivo]
-->
mostrarDispositivos()


mediciones: [Medicion]


-->
mostrarUltimasMediciones()


mediciones: [Medicion]
-->
mostrarHistorico()


mensaje: Text
-->
mostrarError()


estado: Text
-->
mostrarEstado()


No quiero una sola función enorme que haga todo.


SEGURIDAD


Para datos que vienen del servidor quiero que utilices textContent siempre que
sea posible.

No quiero innerHTML con nombres, UUID, tipos, valores o mensajes recibidos del
servidor.

Si necesitas crear elementos utiliza createElement().

Quiero evitar que un dato externo pueda interpretarse como HTML o JavaScript.


DISEÑO


Haz un CSS sencillo.

Quiero que se vea bien pero no hace falta una interfaz espectacular.

Tiene que ser:

- clara;


- ordenada;
- responsive;
- cómoda de utilizar.

Quiero distinguir bien:

selector

tarjetas

histórico

errores


No metas librerías grandes si no hacen falta.


ACCESIBILIDAD


Utiliza labels correctamente.

Que se pueda leer bien.

Que los controles sean utilizables con teclado.

No utilices solamente colores para indicar que ha ocurrido un error.


COMENTARIOS


Quiero comentarios en español.

No pongas comentarios para cada línea.

Las funciones importantes tienen que llevar cabeceras con nuestra notación.

Por ejemplo:

// dispositivos: [Dispositivo] --> mostrarDispositivos()

Otro:

// mediciones: [Medicion] --> mostrarUltimasMediciones()


Otro:

// mediciones: [Medicion] --> mostrarHistorico()

Otro:

// mensaje: Text --> mostrarError()


Usamos:

N = naturales
Z = enteros
R = reales
B = booleanos
Text = texto

[Tipo] = lista


TESTS


Quiero tests automáticos para la interfaz.

No quiero depender siempre del servidor real.

Puedes simular los datos que devuelve la lógica fake.

Quiero probar como mínimo:

1. Mostrar varios dispositivos.

2. Seleccionar el primer dispositivo.

3. Mostrar que no hay dispositivos.

4. Cambiar de dispositivo.

5. Mostrar último CO2.

6. Mostrar última temperatura.

7. Mostrar último ruido.

8. Mostrar "Sin datos" si no hay CO2.


9. Mostrar "Sin datos" si no hay temperatura.

10. Mostrar "Sin datos" si no hay ruido.

11. Mostrar varias filas en el histórico.

12. Mostrar "-" si RSSI es null.

13. Mostrar mensaje si no hay mediciones.

14. Mostrar estado "Cargando...".

15. Mostrar error de conexión.

16. Mostrar error general.

17. Comprobar que cambiar de dispositivo no recarga toda la página.

18. Comprobar que si un dato contiene HTML se muestra como texto y no se ejecuta.

19. Comprobar que la UX no utiliza fetch() directamente.


PRUEBAS MANUALES


Al final también quiero que me digas cómo probar:

- abrir web;
- ver carga;
- ver dispositivos;
- seleccionar GTI3A-2025;
- ver CO2;
- ver temperatura;
- ver ruido;
- ver histórico;
- cambiar de dispositivo;
- probar uno sin datos;
- probar RSSI null;
- probar servidor apagado;
- ver cómo queda en una pantalla pequeña.


ANTES DE PROGRAMAR


Primero:

1. Mira los archivos web que ya tenemos.
2. Localiza la lógica fake.
3. Dime qué funciones fake vas a utilizar.
4. Dime qué partes de la interfaz existente podemos reutilizar.
5. Dime qué problemas ves.
6. Dime qué archivos vas a cambiar.
7. Comprueba que no estás haciendo fetch() directamente.

Después implementa la UX.

Al acabar dime:

- qué archivos has creado;
- cuáles has modificado;
- cómo está organizada la interfaz;
- qué funciones de lógica fake utiliza;
- cómo gestionas carga y errores;
- qué tests has creado;
- cuáles pasan;
- cuáles fallan;
- y qué pruebas tengo que hacer yo.

No hagas backend, REST, BBDD, Android ni lógica fake nueva.

Quiero solamente la UX del navegador.
