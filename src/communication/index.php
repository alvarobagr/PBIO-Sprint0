<?php
/*
-----------------------------------------------------------------------------
Fichero: index.php
Descripcion: Servidor REST de PBIO Sprint 0.
             Recibe peticiones HTTP, utiliza la logica de negocio y devuelve
             respuestas JSON.
Copyright: Copyright (c) 2026 Álvaro Ballester Grau
Fecha: 12/10/2026
Autor: Álvaro Ballester Grau
Aportacion: Implementacion de los endpoints REST de consulta y registro
            de mediciones para el alojamiento Plesk.
-----------------------------------------------------------------------------
*/

// -------------------------------------------------------------------------
// codigo: N, datos: Text --> responderJSON() --> RespuestaHTTP
//
// Envia una respuesta HTTP en formato JSON y termina la ejecucion.
// -------------------------------------------------------------------------
function responderJSON(int $codigo, $datos): void
{
    http_response_code($codigo);

    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: no-store');

    echo json_encode(
        $datos,
        JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES
    );

    exit;
}


// -------------------------------------------------------------------------
// codigo: N, error: Text, mensaje: Text --> responderError()
//
// Devuelve errores con una estructura JSON uniforme.
// -------------------------------------------------------------------------
function responderError(
    int $codigo,
    string $error,
    string $mensaje
): void {
    responderJSON($codigo, [
        'error' => $error,
        'mensaje' => $mensaje
    ]);
}


// -------------------------------------------------------------------------
// --> peticionHTTPS() --> B
//
// Comprueba si la peticion utiliza HTTPS.
// -------------------------------------------------------------------------
function peticionHTTPS(): bool
{
    if (
        isset($_SERVER['HTTPS']) &&
        $_SERVER['HTTPS'] !== 'off' &&
        $_SERVER['HTTPS'] !== ''
    ) {
        return true;
    }

    if (
        isset($_SERVER['HTTP_X_FORWARDED_PROTO']) &&
        strtolower($_SERVER['HTTP_X_FORWARDED_PROTO']) === 'https'
    ) {
        return true;
    }

    return false;
}


// -------------------------------------------------------------------------
// --> crearLogicaMediciones() --> LogicaMediciones
//
// Carga los archivos privados y construye una conexion PDO.
// -------------------------------------------------------------------------
function crearLogicaMediciones(): array
{
    // index.php esta en httpdocs/api/.
    // pbio_backend esta al mismo nivel que httpdocs.

    $directorioPrivado =
        dirname(__DIR__, 2) . '/pbio_backend';

    $rutaConfiguracion =
        $directorioPrivado . '/config.local.php';

    $rutaLogica =
        $directorioPrivado . '/LogicaMediciones.php';

    if (
        !is_readable($rutaConfiguracion) ||
        !is_readable($rutaLogica)
    ) {
        throw new RuntimeException('ERROR_CONFIGURACION');
    }

    $config = require $rutaConfiguracion;

    if (!is_array($config)) {
        throw new RuntimeException('ERROR_CONFIGURACION');
    }

    $campos = [
        'host',
        'port',
        'dbname',
        'user',
        'password'
    ];

    foreach ($campos as $campo) {
        if (!array_key_exists($campo, $config)) {
            throw new RuntimeException('ERROR_CONFIGURACION');
        }
    }

    require_once $rutaLogica;

    $dsn =
        'mysql:host=' . $config['host'] .
        ';port=' . $config['port'] .
        ';dbname=' . $config['dbname'] .
        ';charset=utf8mb4';

    $conexion = new PDO(
        $dsn,
        $config['user'],
        $config['password'],
        [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false
        ]
    );

    return [
        new LogicaMediciones($conexion),
        $config
    ];
}


// -------------------------------------------------------------------------
// --> leerLimite() --> N
//
// Obtiene y valida el parametro opcional limit.
// Valor por defecto: 20. Rango permitido: 1..100.
// -------------------------------------------------------------------------
function leerLimite(): int
{
    if (!array_key_exists('limit', $_GET)) {
        return 20;
    }

    $valor = $_GET['limit'];

    if (
        !is_string($valor) ||
        !preg_match('/^[0-9]{1,3}$/D', $valor)
    ) {
        throw new InvalidArgumentException('DATOS_INVALIDOS');
    }

    $limite = (int) $valor;

    if ($limite < 1 || $limite > 100) {
        throw new InvalidArgumentException('DATOS_INVALIDOS');
    }

    return $limite;
}


// -------------------------------------------------------------------------
// logica: LogicaMediciones --> consultarMediciones() --> [Medicion]
//
// Devuelve las ultimas mediciones almacenadas.
// -------------------------------------------------------------------------
function consultarMediciones(LogicaMediciones $logica): void
{
    $limite = leerLimite();

    $mediciones = $logica->listarMediciones($limite);

    responderJSON(200, $mediciones);
}


// -------------------------------------------------------------------------
// logica: LogicaMediciones --> consultarUltimaMedicion() --> Medicion?
//
// Devuelve la ultima medicion de O3.
// -------------------------------------------------------------------------
function consultarUltimaMedicion(LogicaMediciones $logica): void
{
    $tipo = $_GET['tipo'] ?? null;

    if (!is_string($tipo) || $tipo !== 'O3') {
        throw new InvalidArgumentException('DATOS_INVALIDOS');
    }

    $medicion = $logica->obtenerUltimaMedicion($tipo);

    if ($medicion === null) {
        responderError(
            404,
            'NO_ENCONTRADO',
            'No existen mediciones de este tipo'
        );
    }

    responderJSON(200, $medicion);
}


// -------------------------------------------------------------------------
// logica: LogicaMediciones, uuid: Text
//     --> consultarMedicionesDispositivo() --> [Medicion]
//
// Devuelve las mediciones asociadas a un dispositivo.
// -------------------------------------------------------------------------
function consultarMedicionesDispositivo(
    LogicaMediciones $logica,
    string $uuid
): void {
    $limite = leerLimite();

    $mediciones = $logica->obtenerMedicionesDispositivo(
        $uuid,
        $limite
    );

    responderJSON(200, $mediciones);
}


// -------------------------------------------------------------------------
// logica: LogicaMediciones, config: Config
//     --> registrarMedicion() --> Medicion
//
// Comprueba autorizacion, lee JSON y registra la medicion.
// -------------------------------------------------------------------------
function registrarMedicion(
    LogicaMediciones $logica,
    array $config
): void {
    // No permitir escrituras sin HTTPS.

    if (!peticionHTTPS()) {
        responderError(
            403,
            'HTTPS_REQUERIDO',
            'Esta operacion requiere HTTPS'
        );
    }

    // La clave se guarda exclusivamente en la configuracion privada.

    $claveEsperada = $config['api_token'] ?? '';

    if (
        !is_string($claveEsperada) ||
        strlen($claveEsperada) < 32
    ) {
        responderError(
            503,
            'API_NO_CONFIGURADA',
            'La escritura REST todavia no esta habilitada'
        );
    }

    $claveRecibida =
        $_SERVER['HTTP_X_PBIO_TOKEN'] ?? '';

    if (
        !is_string($claveRecibida) ||
        !hash_equals($claveEsperada, $claveRecibida)
    ) {
        responderError(
            401,
            'NO_AUTORIZADO',
            'No se ha proporcionado una autorizacion valida'
        );
    }

    // Comprobar el tipo de contenido.

    $tipoContenido = strtolower(trim(
        explode(';', $_SERVER['CONTENT_TYPE'] ?? '', 2)[0]
    ));

    if ($tipoContenido !== 'application/json') {
        responderError(
            415,
            'TIPO_CONTENIDO_INVALIDO',
            'Se requiere application/json'
        );
    }

    // Leer el cuerpo de la peticion.
    // Se limita a 16 KiB para evitar cuerpos excesivamente grandes.

    $cuerpo = file_get_contents(
        'php://input',
        false,
        null,
        0,
        16385
    );

    if ($cuerpo === false) {
        responderError(
            400,
            'JSON_INVALIDO',
            'No se pudo leer el cuerpo de la peticion'
        );
    }

    if (strlen($cuerpo) > 16384) {
        responderError(
            413,
            'CUERPO_DEMASIADO_GRANDE',
            'La peticion supera el tamano permitido'
        );
    }

    try {
        $entrada = json_decode(
            $cuerpo,
            true,
            512,
            JSON_THROW_ON_ERROR
        );
    } catch (JsonException $error) {
        responderError(
            400,
            'JSON_INVALIDO',
            'El cuerpo no contiene JSON valido'
        );
    }

    if (!is_array($entrada) || array_is_list($entrada)) {
        responderError(
            400,
            'DATOS_INVALIDOS',
            'Se requiere un objeto JSON con una medicion'
        );
    }

    // La validacion y el guardado pertenecen a la logica de negocio.

    $medicion = $logica->guardarMedicion($entrada);

    responderJSON(201, $medicion);
}


// -------------------------------------------------------------------------
// --> procesarPeticion() --> RespuestaHTTP
//
// Identifica metodo y ruta y ejecuta el endpoint correspondiente.
// -------------------------------------------------------------------------
function procesarPeticion(): void
{
    $metodo = $_SERVER['REQUEST_METHOD'] ?? 'GET';

    $ruta = parse_url(
        $_SERVER['REQUEST_URI'] ?? '/api/',
        PHP_URL_PATH
    );

    if (!is_string($ruta)) {
        responderError(400, 'RUTA_INVALIDA', 'Ruta no valida');
    }

    $ruta = rtrim($ruta, '/');

    // Comprobar si existe una ruta reconocida.

    $esLista = $ruta === '/api/mediciones';

    $esUltima = $ruta === '/api/mediciones/ultima';

    $esDispositivo = preg_match(
        '#^/api/dispositivos/([^/]+)/mediciones$#D',
        $ruta,
        $coincidencias
    ) === 1;

    if (!$esLista && !$esUltima && !$esDispositivo) {
        responderError(
            404,
            'RUTA_NO_ENCONTRADA',
            'La ruta solicitada no existe'
        );
    }

    // Rechazar metodos no permitidos.

    if ($esLista) {
        if ($metodo !== 'GET' && $metodo !== 'POST') {
            header('Allow: GET, POST');
            responderError(405, 'METODO_NO_PERMITIDO', 'Metodo no permitido');
        }
    } elseif ($metodo !== 'GET') {
        header('Allow: GET');
        responderError(405, 'METODO_NO_PERMITIDO', 'Metodo no permitido');
    }

    // Cargar la logica de negocio y la configuracion privada.

    [$logica, $config] = crearLogicaMediciones();

    // GET /api/mediciones
    // POST /api/mediciones

    if ($esLista) {
        if ($metodo === 'GET') {
            consultarMediciones($logica);
        }

        registrarMedicion($logica, $config);
    }

    // GET /api/mediciones/ultima?tipo=O3

    if ($esUltima) {
        consultarUltimaMedicion($logica);
    }

    // GET /api/dispositivos/{uuid}/mediciones

    if ($esDispositivo) {
        $uuid = rawurldecode($coincidencias[1]);

        consultarMedicionesDispositivo(
            $logica,
            $uuid
        );
    }
}


// -------------------------------------------------------------------------
// PUNTO DE ENTRADA
// -------------------------------------------------------------------------

try {

    procesarPeticion();

} catch (InvalidArgumentException $error) {

    responderError(
        400,
        'DATOS_INVALIDOS',
        'Los datos proporcionados no son validos'
    );

} catch (Throwable $error) {

    error_log(
        'PBIO REST: ' . get_class($error)
    );

    responderError(
        500,
        'ERROR_INTERNO',
        'No se pudo completar la operacion'
    );
}