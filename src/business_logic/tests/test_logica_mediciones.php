
<?php
/*
-----------------------------------------------------------------------------
Fichero: test_logica_mediciones.php
Descripción: Pruebas de integración de la lógica de negocio de PBIO con MySQL.
Copyright: Copyright (c) 2026 Álvaro Ballester Grau
Fecha: 10/10/2026
Autor: Álvaro Ballester Grau
Aportación: Comprobación reproducible de validación, inserción y consultas.
-----------------------------------------------------------------------------
*/

declare(strict_types=1);

// Este test se ejecuta desde una terminal, no desde el navegador.
if (PHP_SAPI !== 'cli') {
    http_response_code(403);
    exit('Este test debe ejecutarse desde la terminal.');
}

require_once dirname(__DIR__) . '/LogicaMediciones.php';


// -------------------------------------------------------------------------
// nombre: Text, condicion: B --> comprobar()
//
// Informa de una prueba correcta o detiene las pruebas ante un fallo.
// -------------------------------------------------------------------------
function comprobar(string $nombre, bool $condicion): void
{
    if (!$condicion) {
        throw new RuntimeException('ERROR - ' . $nombre);
    }

    echo 'OK - ' . $nombre . PHP_EOL;
}


// -------------------------------------------------------------------------
// operacion: Funcion --> rechazaDatosInvalidos() --> B
//
// Comprueba que una llamada inválida produce el error de negocio previsto.
// -------------------------------------------------------------------------
function rechazaDatosInvalidos(callable $operacion): bool
{
    try {
        $operacion();
        return false;
    } catch (InvalidArgumentException $error) {
        return $error->getMessage() === 'DATOS_INVALIDOS';
    }
}


$conexion = null;
$codigoSalida = 0;

try {

    // ---------------------------------------------------------------------
    // DATOS DE CONEXION
    //
    // Se reciben desde variables de entorno para no guardar contraseñas
    // dentro de los archivos del repositorio.
    // ---------------------------------------------------------------------

    $host = getenv('PBIO_DB_HOST') ?: '127.0.0.1';
    $puerto = getenv('PBIO_DB_PORT') ?: '3306';
    $nombreBD = getenv('PBIO_DB_NAME');
    $usuario = getenv('PBIO_DB_USER') ?: 'root';
    $clave = getenv('PBIO_DB_PASSWORD');

    if ($nombreBD === false || trim($nombreBD) === '') {
        throw new RuntimeException(
            'Debes definir PBIO_DB_NAME con el nombre de tu base de datos.'
        );
    }

    if (!in_array('mysql', PDO::getAvailableDrivers(), true)) {
        throw new RuntimeException(
            'PHP no tiene habilitada la extension pdo_mysql.'
        );
    }

    $dsn = 'mysql:host=' . $host
        . ';port=' . $puerto
        . ';dbname=' . $nombreBD
        . ';charset=utf8mb4';

    $conexion = new PDO(
        $dsn,
        $usuario,
        $clave === false ? '' : $clave,
        [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false
        ]
    );

    echo 'Conexion a MySQL establecida.' . PHP_EOL;


    // ---------------------------------------------------------------------
    // VERIFICAR QUE LA TABLA ADMITE ROLLBACK
    // ---------------------------------------------------------------------

    $consultaMotor = $conexion->query(
        "SELECT ENGINE FROM information_schema.TABLES "
        . "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medicion'"
    );

    $motor = $consultaMotor->fetchColumn();

    if (!is_string($motor) || strcasecmp($motor, 'InnoDB') !== 0) {
        throw new RuntimeException(
            'La tabla medicion debe existir y utilizar InnoDB.'
        );
    }


    // ---------------------------------------------------------------------
    // INICIO DE LA TRANSACCION DE PRUEBAS
    // ---------------------------------------------------------------------

    $conexion->beginTransaction();

    $logica = new LogicaMediciones($conexion);


    // ---------------------------------------------------------------------
    // DATOS FICTICIOS CONOCIDOS
    // ---------------------------------------------------------------------

    $positiva = [
        'uuid' => 'EPSG-GTI-PROY-3A',
        'nombre_dispositivo' => 'GTI-3A',
        'tipo' => 'O3',
        'valor' => 333,
        'contador' => 30,
        'rssi' => -68
    ];

    $negativa = $positiva;
    $negativa['valor'] = -194;
    $negativa['contador'] = 63;
    $negativa['rssi'] = -54;


    // ---------------------------------------------------------------------
    // TEST 1 - MEDICION POSITIVA VALIDA
    // ---------------------------------------------------------------------

    comprobar(
        'Acepta medicion positiva',
        $logica->validarMedicion($positiva)
    );


    // ---------------------------------------------------------------------
    // TEST 2 - MEDICION NEGATIVA VALIDA
    // ---------------------------------------------------------------------

    comprobar(
        'Acepta medicion negativa',
        $logica->validarMedicion($negativa)
    );


    // ---------------------------------------------------------------------
    // TEST 3 - CONTADOR FUERA DE RANGO
    // ---------------------------------------------------------------------

    $invalida = $positiva;
    $invalida['contador'] = 256;

    comprobar(
        'Rechaza contador fuera de rango',
        !$logica->validarMedicion($invalida)
    );


    // ---------------------------------------------------------------------
    // TEST 4 - TIPO DESCONOCIDO
    // ---------------------------------------------------------------------

    $invalida = $positiva;
    $invalida['tipo'] = 'OTRO';

    comprobar(
        'Rechaza tipo desconocido',
        !$logica->validarMedicion($invalida)
    );


    // ---------------------------------------------------------------------
    // TEST 5 - VALOR NO ENTERO
    // ---------------------------------------------------------------------

    $invalida = $positiva;
    $invalida['valor'] = '333';

    comprobar(
        'Rechaza texto en lugar de entero',
        !$logica->validarMedicion($invalida)
    );


    // ---------------------------------------------------------------------
    // TEST 6 - VALOR FUERA DE INT16
    // ---------------------------------------------------------------------

    $invalida = $positiva;
    $invalida['valor'] = -32769;

    comprobar(
        'Rechaza valor fuera de int16',
        !$logica->validarMedicion($invalida)
    );


    // ---------------------------------------------------------------------
    // TEST 7 - GUARDADO INVALIDO
    // ---------------------------------------------------------------------

    comprobar(
        'guardarMedicion rechaza datos invalidos',
        rechazaDatosInvalidos(
            fn () => $logica->guardarMedicion($invalida)
        )
    );


    // ---------------------------------------------------------------------
    // TEST 8 - GUARDAR MEDICION POSITIVA
    // ---------------------------------------------------------------------

    $guardadaPositiva = $logica->guardarMedicion($positiva);

    comprobar(
        'Guarda 333 ppb con ID y fecha automaticos',
        $guardadaPositiva['valor'] === 333
        && $guardadaPositiva['contador'] === 30
        && $guardadaPositiva['id_medicion'] > 0
        && !empty($guardadaPositiva['fecha_hora'])
    );


    // ---------------------------------------------------------------------
    // TEST 9 - GUARDAR MEDICION NEGATIVA
    // ---------------------------------------------------------------------

    $guardadaNegativa = $logica->guardarMedicion($negativa);

    comprobar(
        'Guarda -194 ppb sin perder el signo',
        $guardadaNegativa['valor'] === -194
        && $guardadaNegativa['contador'] === 63
        && $guardadaNegativa['rssi'] === -54
    );


    // ---------------------------------------------------------------------
    // TEST 10 - LISTAR MEDICIONES
    // ---------------------------------------------------------------------

    $listado = $logica->listarMediciones(1);

    comprobar(
        'listarMediciones respeta el limite',
        count($listado) === 1
    );


    // ---------------------------------------------------------------------
    // TEST 11 - OBTENER ULTIMA MEDICION
    // ---------------------------------------------------------------------

    $ultima = $logica->obtenerUltimaMedicion('O3');

    comprobar(
        'Recupera la ultima medicion de O3',
        $ultima !== null
        && $ultima['id_medicion'] === $guardadaNegativa['id_medicion']
        && $ultima['valor'] === -194
    );


    // ---------------------------------------------------------------------
    // TEST 12 - CONSULTAR POR UUID
    // ---------------------------------------------------------------------

    $delDispositivo = $logica->obtenerMedicionesDispositivo(
        'EPSG-GTI-PROY-3A',
        2
    );

    comprobar(
        'Consulta mediciones por UUID',
        count($delDispositivo) === 2
        && $delDispositivo[0]['uuid'] === 'EPSG-GTI-PROY-3A'
    );


    // ---------------------------------------------------------------------
    // TEST 13 - LIMITE INCORRECTO
    // ---------------------------------------------------------------------

    comprobar(
        'Rechaza limite incorrecto',
        rechazaDatosInvalidos(
            fn () => $logica->listarMediciones(101)
        )
    );


    echo PHP_EOL;
    echo 'TODAS LAS PRUEBAS HAN PASADO.' . PHP_EOL;

} catch (Throwable $error) {

    fwrite(
        STDERR,
        'FALLO: ' . $error->getMessage() . PHP_EOL
    );

    $codigoSalida = 1;

} finally {

    // ---------------------------------------------------------------------
    // DESHACER LOS DATOS DE PRUEBA
    // ---------------------------------------------------------------------

    if ($conexion instanceof PDO && $conexion->inTransaction()) {

        try {

            $conexion->rollBack();

            echo 'ROLLBACK ejecutado: datos de prueba descartados.'
                . PHP_EOL;

        } catch (Throwable $error) {

            fwrite(
                STDERR,
                'FALLO al deshacer la prueba: '
                . $error->getMessage()
                . PHP_EOL
            );

            $codigoSalida = 1;
        }
    }
}

exit($codigoSalida);
