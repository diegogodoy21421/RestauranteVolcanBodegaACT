# Sistema de Bodega — Volcán Waffles & Café

Aplicación Android en Java, ampliada sobre la última versión de
https://github.com/iiooio18/RestauranteVolcanVfinal.

Esta entrega incorpora los ocho requisitos SHOULD HAVE y COULD HAVE del documento
«Casos de Usos - 2 (1).docx». Los productos y movimientos se guardan en SQLite.
El acceso conserva la demostración local: **admin / admin123**.
No incluye un servidor ni un nuevo sistema de usuarios o roles.

## Abrir y ejecutar

1. Descomprime el ZIP y abre la carpeta del proyecto en Android Studio.
2. Usa JDK 17 o superior, SDK Android 35 y acepta la sincronización de Gradle.
3. Ejecuta app en Android 11 o superior (API 30).
4. Inicia sesión con admin y admin123.

La primera sincronización requiere internet para descargar las dependencias.
El escáner utiliza ZXing Embedded 4.3.0. Los informes y el inventario funcionan
localmente. Google Maps necesita conexión para cargar mapas y rutas.
No se necesita una clave de Google Maps.
Configuración: compileSdk 35, targetSdk 34 y minSdk 30; versión 1.1 (código 2).

## Requisitos incorporados

| Código | Función | Dónde probarla |
|---|---|---|
| RF-19 | Informe con productos activos, stock actual, mínimo y disponibilidad | Menú → Informe de stock o Control de stock |
| RF-20 | Cámara y búsqueda manual por código de barras | Productos → Escanear / Código manual |
| RF-21 | Historial completo de entradas y salidas, usuario y fecha; filtros combinables | Menú → Historial de movimientos |
| RF-22 | Filtros combinados por categoría, nombre/SKU y estado | Informe de stock |
| RF-23 | Exportación real a PDF, con varias páginas si corresponde | Informe → Exportar informe a PDF |
| RF-24 | Ubicación fija de la empresa en Google Maps | Menú → Ubicación de la empresa y ruta |
| RF-25 | Consulta puntual de ubicación con permiso | Ubicación → Obtener mi ubicación |
| RF-26 | Ruta desde las coordenadas obtenidas hasta la empresa | Ubicación → Cómo llegar |

## Productos y cámara

Al crear o editar un producto puedes registrar **stock mínimo**, **código de barras**
y **ubicación física**. El código de barras es opcional y debe ser único cuando
se informa. Se guarda como texto para conservar ceros iniciales.

Los productos originales no traen un código comercial asignado: edita uno y
guarda el número impreso bajo el código del envase antes de intentar escanearlo.
El lector admite formatos 1D compatibles con ZXing (EAN, UPC, Code 128, etc.).
Puedes enfocar nuevamente hasta leerlo; Atrás cancela.

La ficha muestra SKU, nombre, categoría, stock, mínimo y ubicación física.
Si está activo, puedes registrar una entrada o salida: se conserva el usuario
de la sesión y el producto queda seleccionado.
Si está inactivo, se informa y no se ofrecen movimientos.
Si se deniega cámara, se ofrece búsqueda manual.

La edición conserva el stock; para cambiar existencias se utilizan entradas
y salidas, manteniendo trazabilidad.

## Informe y PDF

- **Sin stock**: stock igual a 0.
- **Crítico**: stock mayor que 0 y menor o igual al mínimo.
- **Disponible**: stock mayor que el mínimo.
- Los deshabilitados se excluyen del informe, conservando su historial.
- La exportación consulta el inventario y aplica los filtros seleccionados en ese
  momento; guarda una instantánea mientras se elige el destino.
- Android permite elegir nombre y carpeta para el PDF sin pedir acceso general
  a archivos. Cancelar no muestra una confirmación falsa.
- Si no hay coincidencias, se informa en pantalla y en el PDF.

## Empresa y ubicación

Dirección proporcionada: **Arauco 126, 1400099 Ovalle, Coquimbo, Chile**.
Se configura en LocationActivity.COMPANY_ADDRESS.

«Ver empresa» abre Google Maps con la dirección fija sin permiso GPS.
El Word permite abrir Google Maps externo. Si no está instalada su app,
la URL puede abrirse en el navegador.

La consulta pide permiso al pulsar un botón y acepta ubicación aproximada o
precisa. Con permiso preciso utiliza GPS si está habilitado; en otro caso
utiliza el proveedor de red, si está disponible.
Con servicios apagados ofrece abrir Ajustes. Si no obtiene una posición en
25 segundos, informa y permite reintentar. Se cancela la consulta al salir
de la pantalla. La ubicación no se guarda ni se consulta en segundo plano.

«Cómo llegar» envía a Google Maps las coordenadas actuales como origen y la
dirección de la empresa como destino, en modo automóvil. Con permiso denegado,
el mapa fijo sigue disponible y se ofrece abrir ajustes de permisos.
Si no hay Maps ni navegador, muestra un aviso.

## Conservación de datos

La base volcan_bodega.db pasa de versión 2 a 3 mediante ALTER TABLE.
Se conservan productos y movimientos; los productos existentes reciben mínimo 5,
código de barras vacío y ubicación física vacía. El mínimo se puede editar.
Se mantiene la migración anterior de versión 1 a 2 del repositorio de origen.
No desinstales ni borres datos si deseas conservar la información del dispositivo.

## Comprobaciones automatizadas

- StockReportTest: límites de estados, exclusión de inactivos, búsqueda por SKU,
  filtros simultáneos y resultados vacíos.
- RepositoryTest (instrumentación): migración 2→3, códigos únicos, entradas y salidas,
  rechazo por stock insuficiente, filtros e historial al deshabilitar.

Comandos:

    ./gradlew assembleDebug testDebugUnitTest lintDebug
    ./gradlew connectedDebugAndroidTest

La segunda orden requiere emulador o teléfono conectado.
Consulta VALIDACION.md para saber qué comprobaciones se ejecutaron aquí.

## Guion para demostrar

1. Crear un producto con stock 10, mínimo 5 y un código de barras real.
2. Escanearlo y comprobar la ficha. Registrar salida de 5: queda crítico.
3. En informe, combinar categoría, nombre y estado Crítico.
4. Guardar y abrir el PDF. Comprobar producto y cantidades.
5. Registrar salida de 5: queda Sin stock. Intentar otra unidad:
   debe rechazarse sin agregar movimiento.
6. En historial, filtrar por producto y admin; verificar entradas/salidas.
7. Deshabilitar: desaparece del informe, conserva historial y el escaneo avisa.
8. Abrir el mapa de Arauco 126 sin ubicación. Luego autorizar ubicación y pedir ruta.
9. Probar cámara denegada, ubicación denegada y servicios apagados.

Las imágenes del prototipo y README_CRUD_PRESENTACION.md son materiales
históricos del repositorio y no describen las nuevas pantallas.

## APK incluido

La carpeta APK incluye una compilación de depuración para probar en una instalación
nueva. Para actualizar una instalación existente y conservar datos, compila el
proyecto en tu Android Studio con la misma firma que utilizaste anteriormente.
