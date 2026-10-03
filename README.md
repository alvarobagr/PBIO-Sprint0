# PBIO - Sprint 0

Proyecto de la asignatura PBIO del Grado en Tecnologías Interactivas.

## Objetivo

El sistema permite enviar una medición desde una placa mediante Bluetooth Low
Energy/iBeacon, recibirla en una aplicación Android, enviarla mediante una API
REST al backend, almacenarla en una base de datos y mostrarla posteriormente
en una aplicación web.

Arquitectura general:

Arduino / SparkFun
        ↓
BLE / iBeacon
        ↓
Android
        ↓
API REST
        ↓
Lógica de negocio
        ↓
Base de datos
        ↓
Aplicación web

## Estructura del repositorio

- `doc/`: diseños de los componentes.
- `prompts/`: especificaciones utilizadas para generar código mediante IA.
- `src/arduino/`: código de la placa.
- `src/phone/`: aplicación Android.
- `src/database/`: scripts de base de datos.
- `src/backend_logic/`: lógica de negocio del backend.
- `src/rest_server/`: servidor REST.
- `src/browser/`: aplicación web.

## Aplicación Android

Package:

`com.example.abalgra1.pbio`

Minimum SDK:

API 26

## Ejecución

Las instrucciones concretas de despliegue se completarán conforme se integren
los distintos componentes del sistema.

## Tests

Los tests automáticos de cada componente se encuentran junto al código
correspondiente.

Android:

`src/phone/app/src/test/`

Las instrucciones completas de ejecución de todos los tests se actualizarán
durante el desarrollo.