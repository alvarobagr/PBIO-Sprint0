# Despliegue de la interfaz web PBIO — Sprint 0

## Archivos locales del repositorio

```text
doc/
  frontend_business_logic_design.md
  gui_design.md
src/
  frontend_business_logic/
    LogicaMedicionesCliente.js
    tests/test_logica_mediciones_cliente.mjs
  gui/
    index.html
    app.js
    estilos.css
    tests/test_gui.mjs
```

## Pruebas locales

Instalar Node.js 18+ si no está instalado y situarse en la raíz del repositorio:

```powershell
node --test src/frontend_business_logic/tests/test_logica_mediciones_cliente.mjs src/gui/tests/test_gui.mjs
```

Las pruebas de proxy y GUI no realizan ninguna petición real ni insertan nada en MySQL.

## Publicación en Plesk

Usar el Administrador de archivos del dominio `abalgra1.upv.edu.es` para crear las carpetas de la web. No sobrescribir archivos anteriores:

```text
httpdocs/
  api/                         <-- existente; NO TOCAR
    index.php
    .htaccess
  pbio/                        <-- NUEVA
    gui/
      index.html
      app.js
      estilos.css
    frontend_business_logic/
      LogicaMedicionesCliente.js
```

No se suben los archivos de pruebas ni documentos al directorio público. Tampoco se sube `local.properties`, `config.local.php`, ni el token de POST.

Dirección final de la web:

`https://abalgra1.upv.edu.es/pbio/gui/`

La aplicación web y la API REST pertenecen al mismo origen, así que las consultas a `/api` funcionan sin ajustes de CORS.

## Comprobación visual y funcional

1. Abrir la URL anterior y comprobar la aparición de la última medición O3.
2. Comparar sus datos con `https://abalgra1.upv.edu.es/api/mediciones/ultima?tipo=O3`.
3. Pulsar «Actualizar ahora» y comprobar las cinco filas del historial.
4. Enviar una nueva medición desde Android y esperar hasta 15 segundos; comprobar que aparece en la web.
5. Si falla: abrir el inspector de red/consola y verificar que las rutas `.mjs`, `.js`, `.css` y `/api` se sirven sin error.
