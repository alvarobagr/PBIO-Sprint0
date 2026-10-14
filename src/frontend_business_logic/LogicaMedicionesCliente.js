/*
-----------------------------------------------------------------------------
Fichero: LogicaMedicionesCliente.js
Descripción: Proxy de lógica de negocio del cliente para consultar mediciones.
Copyright: Copyright (c) 2026 Álvaro Ballester Grau
Fecha: 13/10/2026
Autor: Álvaro Ballester Grau
Aportación: Encapsulación de peticiones REST sin exponer HTTP a la GUI.
             Corrección de la invocación de fetch en el navegador.
-----------------------------------------------------------------------------
*/

export class LogicaMedicionesCliente {

    // -------------------------------------------------------------------------
    // mecanismo_consulta: MecanismoConsulta --> LogicaMedicionesCliente()
    //
    // Permite sustituir el mecanismo de consulta para pruebas automatizadas.
    // Usa una función intermedia para conservar el contexto de fetch.
    // -------------------------------------------------------------------------
    constructor(
        peticion = (url, opciones) => globalThis.fetch(url, opciones)
    ) {

        if (typeof peticion !== "function") {
            throw new TypeError(
                "No hay un mecanismo de consulta disponible."
            );
        }

        this._peticion = peticion;
    }


    // -------------------------------------------------------------------------
    // limite: N --> listarMediciones() --> [Medicion]
    //
    // Consulta las mediciones recientes a través del servidor, sin exponer
    // detalles del transporte a la interfaz gráfica.
    // -------------------------------------------------------------------------
    async listarMediciones(limite) {

        if (!Number.isInteger(limite) || limite < 1 || limite > 100) {
            throw new RangeError(
                "El límite debe estar entre 1 y 100."
            );
        }

        const respuesta = await this._peticion(
            `/api/mediciones?limit=${limite}`,
            {
                method: "GET",
                headers: {
                    Accept: "application/json"
                },
                cache: "no-store"
            }
        );

        if (!respuesta.ok) {
            throw new Error(
                "No se pudieron consultar las mediciones."
            );
        }

        const mediciones = await respuesta.json();

        if (!Array.isArray(mediciones)) {
            throw new Error(
                "El servidor devolvió un listado no válido."
            );
        }

        return mediciones;
    }


    // -------------------------------------------------------------------------
    // tipo: Text --> obtenerUltimaMedicion() --> Medicion?
    //
    // Consulta la última medición del tipo solicitado. Si no existe,
    // devuelve null, igual que la operación equivalente del backend.
    // -------------------------------------------------------------------------
    async obtenerUltimaMedicion(tipo) {

        if (typeof tipo !== "string" || tipo.trim() === "") {
            throw new TypeError(
                "Debes indicar un tipo de medición."
            );
        }

        const respuesta = await this._peticion(
            `/api/mediciones/ultima?tipo=${encodeURIComponent(tipo)}`,
            {
                method: "GET",
                headers: {
                    Accept: "application/json"
                },
                cache: "no-store"
            }
        );

        if (respuesta.status === 404) {
            return null;
        }

        if (!respuesta.ok) {
            throw new Error(
                "No se pudo consultar la última medición."
            );
        }

        const medicion = await respuesta.json();

        if (
            medicion === null ||
            typeof medicion !== "object" ||
            Array.isArray(medicion)
        ) {
            throw new Error(
                "El servidor devolvió una medición no válida."
            );
        }

        return medicion;
    }
}
