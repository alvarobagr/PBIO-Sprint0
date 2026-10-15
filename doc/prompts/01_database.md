# Prompt original 01 — Base de datos

> **Registro histórico.** Transcripción legible del PDF aportado por el autor; **no se ha adaptado ni corregido el contenido del prompt original**. La copia PDF exacta está en [`originales/01_database.pdf`](originales/01_database.pdf). Las diferencias con el Sprint 0 actual se explican en [`../ai_traceability.md`](../ai_traceability.md).

---

Quiero que me ayudes a generar la base de datos de mi proyecto PBIO.

De momento quiero trabajar solamente con la base de datos.

No quiero que hagas todavía PHP, REST, Android ni la web.

Antes de crear nada mira si en el proyecto ya existe algún SQL que podamos
reutilizar.

La base de datos quiero hacerla en MySQL/MariaDB y tiene que poder ejecutarse
desde phpMyAdmin.

Quiero dos tablas:

DISPOSITIVO
MEDICION

La relación será:

DISPOSITIVO 1 ----- N MEDICIÓN

Es decir, un dispositivo puede tener muchas mediciones y cada medición pertenece
a un único dispositivo.


TABLA DISPOSITIVO


Quiero estos campos:

id_dispositivo
- INT UNSIGNED
- PRIMARY KEY
- AUTO_INCREMENT

uuid
- VARCHAR(36)
- NOT NULL
- UNIQUE

nombre
- VARCHAR(100)
- puede ser NULL

No quiero fecha_registro.

Un ejemplo de dispositivo sería:


id_dispositivo = 1
uuid = "EPSG-GTI-PROY-3A"
nombre = "GTI3A-2025"


TABLA MEDICIÓN


Quiero:

id_medicion
- BIGINT UNSIGNED
- PRIMARY KEY
- AUTO_INCREMENT

id_dispositivo
- INT UNSIGNED
- NOT NULL
- FOREIGN KEY a dispositivo.id_dispositivo

tipo
- VARCHAR(30)
- NOT NULL

valor
- INT
- NOT NULL

contador
- TINYINT UNSIGNED
- NOT NULL

rssi
- SMALLINT
- puede ser NULL

fecha_hora
- DATETIME
- NOT NULL
- DEFAULT CURRENT_TIMESTAMP


SIGNIFICADO


tipo puede ser de momento:

CO2
TEMPERATURA
RUIDO

valor puede ser negativo.

contador viene del beacon y puede ir de 0 a 255.

rssi es la intensidad con la que el móvil recibe la señal Bluetooth.
Puede ser negativo y también quiero permitir que sea NULL.

fecha_hora no la manda Android.
Quiero que la base de datos la ponga automáticamente cuando se guarda la
medición.


RELACIÓN


Quiero una FOREIGN KEY entre:

medicion.id_dispositivo

y

dispositivo.id_dispositivo

Utiliza InnoDB.

No quiero que se pueda borrar accidentalmente un dispositivo que todavía tenga
mediciones, así que utiliza ON DELETE RESTRICT.

No quiero una restricción UNIQUE con dispositivo + tipo + contador porque el
contador solo tiene 8 bits y después de 255 puede volver a empezar.


SQL


Genera un SQL completo que pueda pegar directamente en phpMyAdmin.

Quiero que:

1. cree la base de datos si no existe;


2. use utf8mb4;
3. cree primero dispositivo;
4. cree después medicion;
5. cree correctamente la FOREIGN KEY.

Pon algunos comentarios en español para separar las partes del SQL, pero no
hace falta comentar cada línea.


PRUEBAS


Quiero que también prepares pruebas para comprobar que la base de datos está
bien.

Como mínimo prueba:

1. Insertar un dispositivo.

2. Intentar repetir el mismo UUID y comprobar que no deja.

3. Insertar una medición.

4. Insertar varias mediciones del mismo dispositivo.

5. Guardar un valor negativo.

6. Guardar RSSI negativo.

7. Guardar una medición con RSSI NULL.

8. Probar contador = 0.

9. Probar contador = 255.

10. Comprobar que no acepta un contador fuera del rango de TINYINT UNSIGNED.

11. Intentar crear una medición con un dispositivo inexistente.

12. Intentar borrar un dispositivo que tenga mediciones.

13. Comprobar que fecha_hora se genera sola.

No estropees datos reales para hacer las pruebas.


ANTES DE HACERLO


Antes de cambiar o crear archivos:

1. Mira si ya existe SQL en el proyecto.
2. Dime qué podemos reutilizar.
3. Dime brevemente qué vas a generar.
4. Comprueba que no estás añadiendo tablas ni campos que yo no he pedido.

Después genera el SQL y las pruebas.

Si puedes ejecutar las pruebas, ejecútalas.

Al final dime:

- qué has creado;
- qué tablas hay;
- qué claves y restricciones has puesto;
- qué tests has hecho;
- cuáles pasan;
- cuáles fallan;
- y si has encontrado algún problema.

No añadas usuarios, login, nuevas tablas ni ninguna funcionalidad que no he
pedido.

Quiero únicamente la base de datos que hemos diseñado.
