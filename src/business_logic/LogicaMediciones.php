<?php
/*
-----------------------------------------------------------------------------
Fichero: LogicaMediciones.php
Descripción: Lógica de negocio para validar, almacenar y consultar las
             mediciones recibidas por el sistema PBIO Sprint 0.
Copyright: Copyright (c) 2026 Álvaro Ballester Grau
Fecha: 10/10/2026
Autor: Álvaro Ballester Grau
Aportación: Implementación de las operaciones de negocio independientes
            de HTTP mediante PDO y consultas SQL parametrizadas.
-----------------------------------------------------------------------------
*/

declare(strict_types=1);

final class LogicaMediciones
{
    private PDO $conexion;

    // -------------------------------------------------------------------------
    // conexion: ConexionBD --> LogicaMediciones()
    //
    // Recibe la conexión existente a MySQL/MariaDB. No contiene credenciales.
    // -------------------------------------------------------------------------
    public function __construct(PDO $conexion)
    {
        $this->conexion = $conexion;
        $this->conexion->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $this->conexion->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
    }

    // -------------------------------------------------------------------------
    // entrada: MedicionEntrada --> validarMedicion() --> B
    //
    // Comprueba los seis campos del contrato Android-backend sin escribir
    // en la base de datos ni modificar los datos recibidos.
    // -------------------------------------------------------------------------
    public function validarMedicion($entrada): bool
    {
        if (!is_array($entrada)) {
            return false;
        }

        $campos = [
            'uuid',
            'nombre_dispositivo',
            'tipo',
            'valor',
            'contador',
            'rssi'
        ];

        foreach ($campos as $campo) {
            if (!array_key_exists($campo, $entrada)) {
                return false;
            }
        }

        if (!is_string($entrada['uuid']) ||
            $entrada['uuid'] === '' ||
            trim($entrada['uuid']) !== $entrada['uuid'] ||
            strlen($entrada['uuid']) > 36) {
            return false;
        }

        if (!is_string($entrada['nombre_dispositivo']) ||
            $entrada['nombre_dispositivo'] === '' ||
            trim($entrada['nombre_dispositivo']) !== $entrada['nombre_dispositivo'] ||
            strlen($entrada['nombre_dispositivo']) > 100) {
            return false;
        }

        if (!is_string($entrada['tipo']) || $entrada['tipo'] !== 'O3') {
            return false;
        }

        if (!is_int($entrada['valor']) ||
            $entrada['valor'] < -32768 ||
            $entrada['valor'] > 32767) {
            return false;
        }

        if (!is_int($entrada['contador']) ||
            $entrada['contador'] < 0 ||
            $entrada['contador'] > 255) {
            return false;
        }

        if (!is_int($entrada['rssi']) ||
            $entrada['rssi'] < -32768 ||
            $entrada['rssi'] > 32767) {
            return false;
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // entrada: MedicionEntrada --> guardarMedicion() --> Medicion
    //
    // Valida, inserta una nueva medición y recupera la fila ya almacenada.
    // Excepciones: DATOS_INVALIDOS o ERROR_PERSISTENCIA.
    // -------------------------------------------------------------------------
    public function guardarMedicion($entrada): array
    {
        if (!$this->validarMedicion($entrada)) {
            throw new InvalidArgumentException('DATOS_INVALIDOS');
        }

        try {
            $sql = 'INSERT INTO medicion
                    (uuid, nombre_dispositivo, tipo, valor, contador, rssi)
                    VALUES
                    (:uuid, :nombre_dispositivo, :tipo, :valor, :contador, :rssi)';

            $sentencia = $this->conexion->prepare($sql);
            $sentencia->bindValue(':uuid', $entrada['uuid'], PDO::PARAM_STR);
            $sentencia->bindValue(':nombre_dispositivo', $entrada['nombre_dispositivo'], PDO::PARAM_STR);
            $sentencia->bindValue(':tipo', $entrada['tipo'], PDO::PARAM_STR);
            $sentencia->bindValue(':valor', $entrada['valor'], PDO::PARAM_INT);
            $sentencia->bindValue(':contador', $entrada['contador'], PDO::PARAM_INT);
            $sentencia->bindValue(':rssi', $entrada['rssi'], PDO::PARAM_INT);
            $sentencia->execute();

            $id = $this->conexion->lastInsertId();

            if ($id === false || $id === '0') {
                throw new RuntimeException('ERROR_PERSISTENCIA');
            }

            $consulta = $this->conexion->prepare(
                'SELECT id_medicion, uuid, nombre_dispositivo, tipo,
                        valor, contador, rssi, fecha_hora
                 FROM medicion
                 WHERE id_medicion = :id_medicion'
            );

            $consulta->bindValue(':id_medicion', $id, PDO::PARAM_INT);
            $consulta->execute();

            $fila = $consulta->fetch();

            if ($fila === false) {
                throw new RuntimeException('ERROR_PERSISTENCIA');
            }

            return $this->filaAMedicion($fila);

        } catch (PDOException $error) {
            throw new RuntimeException('ERROR_PERSISTENCIA', 0, $error);
        }
    }

    // -------------------------------------------------------------------------
    // limite: N --> listarMediciones() --> [Medicion]
    //
    // Recupera como máximo 100 mediciones, ordenadas por fecha e ID.
    // -------------------------------------------------------------------------
    public function listarMediciones($limite): array
    {
        if (!is_int($limite) || $limite < 1 || $limite > 100) {
            throw new InvalidArgumentException('DATOS_INVALIDOS');
        }

        try {
            $sentencia = $this->conexion->prepare(
                'SELECT id_medicion, uuid, nombre_dispositivo, tipo,
                        valor, contador, rssi, fecha_hora
                 FROM medicion
                 ORDER BY fecha_hora DESC, id_medicion DESC
                 LIMIT :limite'
            );

            $sentencia->bindValue(':limite', $limite, PDO::PARAM_INT);
            $sentencia->execute();

            return array_map(
                [$this, 'filaAMedicion'],
                $sentencia->fetchAll()
            );

        } catch (PDOException $error) {
            throw new RuntimeException('ERROR_PERSISTENCIA', 0, $error);
        }
    }

    // -------------------------------------------------------------------------
    // tipo: Text --> obtenerUltimaMedicion() --> Medicion?
    //
    // Obtiene la última medición del tipo O3; devuelve null si no existe.
    // -------------------------------------------------------------------------
    public function obtenerUltimaMedicion($tipo): ?array
    {
        if (!is_string($tipo) || $tipo !== 'O3') {
            throw new InvalidArgumentException('DATOS_INVALIDOS');
        }

        try {
            $sentencia = $this->conexion->prepare(
                'SELECT id_medicion, uuid, nombre_dispositivo, tipo,
                        valor, contador, rssi, fecha_hora
                 FROM medicion
                 WHERE tipo = :tipo
                 ORDER BY fecha_hora DESC, id_medicion DESC
                 LIMIT 1'
            );

            $sentencia->bindValue(':tipo', $tipo, PDO::PARAM_STR);
            $sentencia->execute();

            $fila = $sentencia->fetch();

            return $fila === false
                ? null
                : $this->filaAMedicion($fila);

        } catch (PDOException $error) {
            throw new RuntimeException('ERROR_PERSISTENCIA', 0, $error);
        }
    }

    // -------------------------------------------------------------------------
    // uuid: Text, limite: N --> obtenerMedicionesDispositivo() --> [Medicion]
    //
    // Recupera las últimas mediciones del UUID solicitado.
    // -------------------------------------------------------------------------
    public function obtenerMedicionesDispositivo($uuid, $limite): array
    {
        if (!is_string($uuid) ||
            $uuid === '' ||
            trim($uuid) !== $uuid ||
            strlen($uuid) > 36 ||
            !is_int($limite) ||
            $limite < 1 ||
            $limite > 100) {
            throw new InvalidArgumentException('DATOS_INVALIDOS');
        }

        try {
            $sentencia = $this->conexion->prepare(
                'SELECT id_medicion, uuid, nombre_dispositivo, tipo,
                        valor, contador, rssi, fecha_hora
                 FROM medicion
                 WHERE uuid = :uuid
                 ORDER BY fecha_hora DESC, id_medicion DESC
                 LIMIT :limite'
            );

            $sentencia->bindValue(':uuid', $uuid, PDO::PARAM_STR);
            $sentencia->bindValue(':limite', $limite, PDO::PARAM_INT);
            $sentencia->execute();

            return array_map(
                [$this, 'filaAMedicion'],
                $sentencia->fetchAll()
            );

        } catch (PDOException $error) {
            throw new RuntimeException('ERROR_PERSISTENCIA', 0, $error);
        }
    }

    // -------------------------------------------------------------------------
    // fila: MedicionBD --> filaAMedicion() --> Medicion
    //
    // Convierte los valores numéricos obtenidos de SQL a enteros PHP.
    // No interpreta tramas BLE ni transforma las unidades de medida.
    // -------------------------------------------------------------------------
    private function filaAMedicion(array $fila): array
    {
        return [
            'id_medicion' => (int) $fila['id_medicion'],
            'uuid' => $fila['uuid'],
            'nombre_dispositivo' => $fila['nombre_dispositivo'],
            'tipo' => $fila['tipo'],
            'valor' => (int) $fila['valor'],
            'contador' => (int) $fila['contador'],
            'rssi' => (int) $fila['rssi'],
            'fecha_hora' => $fila['fecha_hora']
        ];
    }
}
