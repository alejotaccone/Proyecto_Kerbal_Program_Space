# Reporte de Corrección: Distancia de Colisión 3D y Seguridad de API Key

## 1. Resumen Ejecutivo
1. Se reemplazó el cálculo geodésico superficial de distancia por una distancia euclídea tridimensional basada en coordenadas cartesianas ECEF en `GeoPosition`, corrigiendo además la aproximación vertical de `RogueDebris`.
2. Se eliminó la API Key hardcodeada en el código fuente, implementando una carga jerárquica y segura mediante archivo `config.properties` o variable de entorno `N2YO_API_KEY`, con soporte para modo local de contingencia y enmascaramiento de datos sensibles.
3. Se implementó una suite de pruebas de verificación manual en `src/test/GeoPositionCheck.java` sin dependencias externas, se verificó el arranque con y sin clave en CLI y GUI, y se documentó el procedimiento en `README.md`.

---

## 2. Problema 1: Distancia de Colisión y Cinemática Orbital

### Qué estaba mal y por qué
El método `GeoPosition.distanceTo()` utilizaba la fórmula de Haversine asumiendo el radio medio de la superficie terrestre ($R = 6371\text{ km}$) y luego sumaba de forma inconsistente el cuadrado de la diferencia de altitud a una distancia superficial calculada a cota cero.

Esto generaba dos errores físicos graves:
1. **Falsas colisiones por solapamiento vertical:** Dos objetos situados sobre las mismas coordenadas terrestres (latitud y longitud) pero a cotas orbitales radicalmente diferentes (por ejemplo, la ISS a 400 km de altitud y un satélite geoestacionario a 35.786 km) arrojaban una distancia casi nula en superficie con la fórmula previa, o distorsionaban el radio de proximidad.
2. **Distorsión del arco orbital:** A altitudes de órbita baja (400 km), el radio respecto al centro de la Tierra es de 6.791 km (no 6.371 km). Calcular la distancia sobre la esfera terrestre distorsionaba la separación horizontal real en aproximadamente un 6.6%, y en órbitas altas la distorsión superaba el 80%.

Adicionalmente, `RogueDebris` solo actualizaba sus coordenadas de latitud y longitud hacia el blanco en cada tick (`move()`), manteniendo fija su altitud de spawn. Si se migraba a distancia 3D sin modificarlo, la diferencia de altitud impedía que el debris ingresara al radio de impacto (< 100 km), haciendo que las crisis nunca se desencadenaran.

### Qué se cambió
- **`src/model/geometry/GeoPosition.java`**:
  - `distance3DTo(GeoPosition other)`: Convierte las coordenadas geodésicas $(\text{latitud}, \text{longitud}, \text{altitud})$ a coordenadas cartesianas tridimensionales centradas en la Tierra (ECEF) y calcula la distancia euclídea en el espacio.
  - `surfaceDistanceKm(GeoPosition other)`: Mantiene el algoritmo de Haversine a nivel del mar ($R = 6371\text{ km}$) para los casos donde se requiera medir la distancia sobre el suelo terrestre (ground-track).
  - `distanceTo(GeoPosition other)`: Redirige directamente a `distance3DTo(other)`, respetando el principio de Experto en Información para que todos los consumidores (`Radar`, `CrisisController`, `SpaceStation`) operen con distancia espacial real sin alterar sus firmas.
- **`src/model/spacecraft/RogueDebris.java`**:
  - `move()`: Se ajustó el algoritmo de persecución para que no solo aproxime latitud y longitud, sino que converja en altitud hacia la nave objetivo de manera proporcional a la distancia horizontal remanente.

### Fórmula explicada en lenguaje simple
La Tierra se modela en un espacio de tres dimensiones $(X, Y, Z)$ con origen en su centro.
1. Se calcula el radio total desde el centro del planeta:  
   $$r = 6371\text{ km} + \text{altitud en km}$$
2. Con la latitud y longitud (pasadas a radianes), se descompone ese radio en tres ejes:  
   - $X = r \cdot \cos(\text{lat}) \cdot \cos(\text{lon})$
   - $Y = r \cdot \cos(\text{lat}) \cdot \sin(\text{lon})$
   - $Z = r \cdot \sin(\text{lat})$
3. La distancia real en línea recta entre ambos objetos en el espacio es la hipotenusa 3D:  
   $$\text{distancia} = \sqrt{(X_2 - X_1)^2 + (Y_2 - Y_1)^2 + (Z_2 - Z_1)^2}$$

### Cambio de comportamiento en la simulación
- **Alertas de Radar (< 200 km):** Solo se disparan cuando dos objetos están verdaderamente cerca en el espacio exterior, eliminando falsos positivos entre objetos que se cruzan verticalmente a diferentes alturas.
- **Eventos de Crisis (< 100 km):** Las colisiones requieren convergencia espacial tridimensional completa.
- **Persistencia de Crisis:** Como `RogueDebris` ahora ajusta activamente su altitud mientras persigue a su blanco, las trayectorias de colisión continúan alcanzando la zona de peligro y disparando la ventana de evasión táctica.

---

## 3. Problema 2: Seguridad y Desacoplamiento de la API Key

### Dónde estaba la key y antecedentes en git
- La clave `DPP6C4-6KSLTW-K5Z8BU-5THO` se encontraba hardcodeada en texto plano en la constante `DEFAULT_KEY` dentro de `src/api/N2YOApiClient.java` (línea 15).
- En el historial de git, la clave fue introducida en el commit `7df3020` ("Inicializar proyecto base: simulador de tráfico espacial...") y se propagó a lo largo de las ramas.
- **Acción requerida respecto a la clave:** Es **obligatorio rotar la clave** en el panel de N2YO. Al haber figurado en commits de git, si el repositorio tiene un remoto en GitHub público o compartido, la clave debe considerarse públicamente expuesta.

### Cómo se resolvió la configuración
Se implementó la clase `src/api/ApiConfig.java` que gestiona las credenciales bajo la siguiente prioridad:
1. Variable de entorno del sistema operativo: `N2YO_API_KEY`
2. Archivo de propiedades local en la raíz: `config.properties` (propiedad `n2yo.api.key`)
3. Ingreso interactivo por el usuario en la GUI (`txtApiKey`) o en la consola CLI (`Main`)

Medidas de protección aplicadas:
- Se eliminó el valor hardcodeado en `N2YOApiClient.DEFAULT_KEY` (ahora cadena vacía).
- Se agregó `config.properties` al archivo `.gitignore` para impedir que se suba accidentalmente a git.
- Se creó `config.properties.example` con la plantilla y comentarios de ayuda.
- Se implementó `ApiConfig.enmascararKey()`: en logs y consolas la clave se muestra enmascarada (ej. `DPP6...5THO`), evitando que se filtre en impresiones de pantalla o archivos de registro.
- Si no hay clave configurada o la red falla, la aplicación inicia en **Modo Local / Contingencia**, permitiendo probar la interfaz y las mecánicas sin arrojar excepciones ni congelar el hilo principal.

---

## 4. Resultado de las Pruebas

Se ejecutó la suite de verificación automatizada `src/test/GeoPositionCheck.java`:

| Caso de Prueba | Entrada | Valor Esperado | Valor Obtenido | Estado |
|---|---|---|---|---|
| **1. Mismo punto exacto** | Mismas coords geodésicas y cota (ISS 420 km) | 0.00 km | 0.00 km | **PASÓ** |
| **2. Misma vertical, distinta cota** | Lat: -34.60, Lng: -58.38; Alt 1: 400 km, Alt 2: 35.786 km | 35.386,00 km | 35.386,00 km | **PASÓ** |
| **3. Cuerda 3D vs Arco orbital** | Misma cota (420 km), $\Delta\text{lon} = 5^\circ$ | Cuerda 3D: 590,69 km (Arco: 590,88 km) | 590,6937 km | **PASÓ** |
| **4. Haversine superficie pura** | Nivel del mar ($R = 6371\text{ km}$), $\Delta\text{lon} = 5^\circ$ | 555,97 km | 555,9746 km | **PASÓ** |
| **5. Delegación polimórfica** | `pos1.distanceTo(pos2)` vs `pos1.distance3DTo(pos2)` | Coincidencia idéntica | Coincidencia idéntica | **PASÓ** |

### Pruebas de integración CLI / Entorno:
- **Ejecución sin clave:** El simulador detecta la ausencia de credenciales, emite mensaje de advertencia y levanta la estación en modo local sin excepciones.
- **Ejecución con variable de entorno (`$env:N2YO_API_KEY`):** Detecta la clave automáticamente y la muestra enmascarada.
- **Ejecución con `config.properties`:** Detecta la clave configurada en el archivo local, la enmascara y conecta fluidamente.

---

## 5. Observaciones Técnicas Adicionales

1. **Historial de Git:** Para preservar la integridad del historial y no alterar ramas remotas sin autorización explícita, los commits históricos antiguos mantienen los deltas donde figuraba la clave vieja. La rotación en el sitio web de N2YO invalida definitivamente la clave anterior sin requerir una reescritura destructiva del repositorio (`git filter-branch` / `BFG`).
2. **Cuota de la API N2YO:** Las cuentas gratuitas de N2YO permiten hasta 1.000 solicitudes por hora. Si se configuran intervalos de actualización muy cortos, se recomienda moderar la frecuencia de sincronización para evitar bloqueos por rate limit (HTTP 429 / 401).
3. **Consola Secundaria de Telemetría:** `TelemetryLogger.launchMonitorConsole()` invoca `powershell.exe` para abrir una ventana secundaria de monitoreo en Windows. En sistemas Linux/macOS la llamada falla de forma silenciosa y controlada, registrando el estado en los logs normales.

---

## 6. Comandos para Reproducir y Probar

### 1. Compilar todo el proyecto (incluyendo tests)
```bash
javac -encoding UTF-8 -d bin src/model/geometry/*.java src/model/components/*.java src/model/spacecraft/*.java src/radar/*.java src/api/*.java src/engine/*.java src/controller/*.java src/gui/*.java src/test/*.java src/Main.java
```

### 2. Ejecutar la suite de pruebas unitarias 3D
```bash
java -cp bin test.GeoPositionCheck
```

### 3. Probar CLI en Modo Local (sin clave)
```bash
java -cp bin Main
```
*(Presionar Enter al solicitar la API Key para continuar en modo local)*

### 4. Probar con archivo `config.properties`
```bash
# 1. Copiar la plantilla
cp config.properties.example config.properties

# 2. Configurar la clave dentro del archivo
# n2yo.api.key=TU_NUEVA_CLAVE

# 3. Ejecutar la GUI o CLI
java -cp bin gui.MainGUI
```
