# Validación de la entrega

Resultado final: **BUILD SUCCESSFUL**.

Comando ejecutado con JDK 17, SDK 35 y Gradle 8.9:

    gradle --no-daemon assembleDebug testDebugUnitTest lintDebug compileDebugAndroidTestJavaWithJavac

| Comprobación | Resultado |
|---|---|
| Compilación de la aplicación y generación del APK | Correcta |
| Pruebas unitarias de StockReportTest | 2 ejecutadas, 0 fallos, 0 errores |
| Compilación de RepositoryTest para Android | Correcta |
| Android Lint | Completado sin errores; conserva advertencias |
| Formato de los cambios con git diff --check | Correcto |
| XML de manifiesto y recursos | Analizados correctamente |

## Límites de la verificación

No había un teléfono ni un emulador conectado. Por tanto, RepositoryTest se
compiló, pero no se ejecutó: la migración y las transacciones requieren esa
comprobación en Android. Tampoco se ejecutaron pruebas de interfaz, lectura
con cámara, permisos, recepción de posición GPS, Google Maps ni guardado y
apertura del PDF mediante el selector de archivos de un dispositivo.

El README incluye el recorrido para esas comprobaciones. Ejecutar:

    ./gradlew connectedDebugAndroidTest

en un emulador o teléfono permite verificar la migración 2→3, los códigos
únicos y las operaciones de inventario con la prueba incluida.

Android Lint mantiene advertencias de localización de textos, compatibilidad,
estilo y otros aspectos del proyecto; no se añadió una línea base ni se
desactivaron comprobaciones para obtener el resultado.

## Cambios de configuración

Se aumentó targetSdk de 30 a 34 para resolver el error de target obsoleto
detectado por Lint. minSdk permanece en 30 (Android 11), compileSdk en 35.
La versión de la app es 1.1, código 2.

La actualización de la base se hace mediante ALTER TABLE y no elimina productos
ni movimientos. Para conservar datos de una instalación anterior, se debe
compilar usando la misma firma de esa instalación.
