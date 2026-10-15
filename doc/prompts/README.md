# Prompts de IA originales — PBIO Sprint 0

**Autor del proyecto:** Álvaro Ballester Grau  
**Revisión documental:** 15 de octubre de 2026

Esta carpeta conserva los **seis prompts aportados por el autor en formato PDF**, junto con una transcripción Markdown para facilitar la revisión en GitHub. Se conserva el contenido original: los ejemplos y requisitos que mencionan `CO2`, `TEMPERATURA` y `RUIDO`, dos tablas SQL y otras operaciones pertenecen a una **fase anterior de diseño**, no a la aplicación O3 finalmente entregada.

## Documentos

| Nº | Tema | Transcripción Markdown | PDF original (sin modificar) |
|---|---|---|---|
| 01 | Base de datos | [`01_database.md`](01_database.md) | [`originales/01_database.pdf`](originales/01_database.pdf) |
| 02 | Lógica de negocio real | [`02_business_logic.md`](02_business_logic.md) | [`originales/02_business_logic.pdf`](originales/02_business_logic.pdf) |
| 03 | Servidor REST | [`03_communication.md`](03_communication.md) | [`originales/03_communication.pdf`](originales/03_communication.pdf) |
| 04 | Lógica fake del teléfono | [`04_phone.md`](04_phone.md) | [`originales/04_phone.pdf`](originales/04_phone.pdf) |
| 05 | Lógica fake del navegador | [`05_frontend_business_logic.md`](05_frontend_business_logic.md) | [`originales/05_frontend_business_logic.pdf`](originales/05_frontend_business_logic.pdf) |
| 06 | UX / interfaz web | [`06_gui.md`](06_gui.md) | [`originales/06_gui.pdf`](originales/06_gui.pdf) |

## Cómo interpretar estas evidencias

1. **Originales:** Los PDF de `originales/` son los mismos archivos que aportó el autor, sin alterar. Las versiones `.md` son transcripciones del texto extraído; cuando haya dudas de maquetación, prevalece el PDF original.
2. **Diseño final:** Los documentos `doc/*_design.md` describen el Sprint 0 actualmente implementado (O3, una tabla `medicion`, REST/HTTPS, proxy web). No hay que regenerar ni sustituir el código funcional a partir de los prompts antiguos.
3. **Trazabilidad:** [`doc/ai_traceability.md`](../ai_traceability.md) relaciona los prompts con diseños, archivos de implementación, pruebas y cambios de alcance.
4. **Alcance de la prueba:** Estos PDF acreditan la existencia de prompts escritos; por sí solos **no demuestran qué salida concreta produjo una IA ni que ese mismo prompt se usara literalmente para generar cada archivo**. La comprobación de correspondencia entre diseño e implementación se documenta separadamente.

## Precaución de entrega

El SQL descrito en el primer prompt era una propuesta histórica de dos tablas. **No ejecutar ese prompt para recrear la base de datos actual** ni ejecutar `src/database/schema.sql` sobre la base de datos Plesk que ya contiene medidas: el script existente incluye `DROP TABLE IF EXISTS medicion`. No incluir secretos como `local.properties` ni `config.local.php` en el repositorio.
