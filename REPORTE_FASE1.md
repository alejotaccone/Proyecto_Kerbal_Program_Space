# REPORTE FASE 1: Investigación y Auditoría Técnica

Proyecto: Kerbal Program Space  
Fecha: 2026-10-20  
Estado: FASE 1 COMPLETADA — Esperando aprobación para FASE 2.

---

## 1. Problema 1: Distancia de Colisión y Altitud en `GeoPosition`

### 1.1. Implementación actual del cálculo de distancia
* **Ubicación:** `src/model/geometry/GeoPosition.java` (líneas 41-59).
* **Código actual:**
```java
public double distanceTo(GeoPosition other) {
    if (other == null) return Double.MAX_VALUE;

    final int R = 6371; // Radio medio de la Tierra en km

    double latDistance = Math.toRadians(other.latitude - this.latitude);
    double lonDistance = Math.toRadians(other.longitude - this.longitude);

    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
             + Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(other.latitude))
             * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    double surfaceDistance = R * c;

    // Incorporar diferencia de altitud
    double altDifference = other.altitude - this.altitude;
    return Math.sqrt(surfaceDistance * surfaceDistance + altDifference * altDifference);
}
```

### 1.2. Hallazgo: ¿Qué sospecha se confirmó y cuál se descartó?
* **DESCARTADO:** La sospecha de que la distancia entre dos satélites sobre el mismo punto del mapa a distinta altitud (ej. 400 km vs 35.000 km) daría `~0 km` e ignoraría la altitud **es incorrecta**.  
  *Evidencia:* Las líneas 57 y 58 calculan `altDifference = other.altitude - this.altitude` y aplican Pitágoras `sqrt(surfaceDistance^2 + altDifference^2)`. Con idéntico lat/long, `surfaceDistance = 0`, por lo que la función devuelve exactamente `|alt2 - alt1| = 34.600 km`. No dispara falsas alarmas en satélites sobre la misma coordenada geográfica vertical.
* **CONFIRMADO (Error real de física orbital):**
  1. **Subestimación horizontal por radio terrestre al nivel del mar:** `surfaceDistance` multiplica la distancia angular `c` por $R = 6371\text{ km}$ (radio terrestre al nivel del mar). A altitudes de órbita baja (LEO, ~400 km), el radio es $r = R + h = 6771\text{ km}$ (un **6.28% mayor**). La distancia horizontal entre satélites en órbita se subestima sistemáticamente.
  2. **Aproximación cilíndrica vs euclidiana 3D:** La fórmula modela el espacio como un cilindro plano euclidiano ($\sqrt{\text{arco}^2 + \Delta h^2}$) en vez de calcular la distancia de cuerda 3D real entre vectores esféricos concéntricos.

### 1.3. Unidades de altitud y origen de datos
* **Unidad en el modelo:** Kilómetros (`km`). Documentado explícitamente en `GeoPosition.java:6` (`private double altitude; // en kilómetros`).
* **Origen de datos (`N2YOJsonParser.java:41`):**
  `double alt = json.contains("\"sataltitude\":") ? extractJsonDouble(json, "\"sataltitude\":") : 400.0;`
* Según la API de N2YO, el campo `sataltitude` se devuelve nativamente en **kilómetros**. Las unidades son coherentes y consistentes en todo el código.

### 1.4. Consumidores del cálculo de distancia
1. **`src/radar/Radar.java:32`**:
   `double distance = naveA.distanceTo(naveB);`  
   Umbral: `distance < 200.0 km`. Dispara alerta crítica de colisión.  
   *Necesidad:* **Distancia euclídea 3D**.
2. **`src/controller/CrisisController.java:48`**:
   `double dist = threat.distanceTo(target);`  
   Umbral: `dist < 100.0 km`. Desencadena el evento interactivo de colisión inminente (pausa, diálogo modal, daño/evasión).  
   *Necesidad:* **Distancia euclídea 3D**.
3. **`src/model/spacecraft/SpaceStation.java:137`**:
   `this.distanceTo(ship) <= 500.0`  
   Umbral: radio de acople y transferencia de combustible.  
   *Necesidad:* **Distancia euclídea 3D**.
4. **`src/model/spacecraft/OrbitalObject.java:49`**:
   Método puente que delega en `position.distanceTo(other.position)`.
5. **`src/gui/RadarPanel.java:191-209` (Visualización 2D):**
   No llama a `distanceTo()`. Proyecta en pantalla 2D mediante escala local plana en grados (`deltaLat * 111.0 km/deg`). No se ve afectado por el cálculo 3D.

### 1.5. Movimiento de `RogueDebris` y riesgo de bloqueo de crisis
* **Ubicación:** `src/model/spacecraft/RogueDebris.java` (líneas 21-47).
* **CONFIRMADO:** `RogueDebris.move()` **solo calcula e interpola latitud y longitud**:
  ```java
  double deltaLat = targetLat - currentLat;
  double deltaLng = targetLng - currentLng;
  double dist = Math.sqrt(deltaLat * deltaLat + deltaLng * deltaLng);
  getPosition().setLatitude(currentLat + (deltaLat / dist) * step);
  getPosition().setLongitude(currentLng + (deltaLng / dist) * step);
  ```
  La altitud **jamás se modifica**.
* **Impacto en la simulación:**
  - Al crearse en `OrbitalEventSpawner.java:52`, se le asigna la altitud que la estación tenía en ese tick (`centro.getAltitude()`).
  - Sin embargo, la estación real (ISS/Tiangong) actualiza su altitud periódicamente vía N2YO (`SimulationEngine.syncWithN2YO()`). Si la estación varía su altitud a 420 km y el debris quedó congelado en 400 km, la diferencia en $z$ es de 20 km.
  - Si en el futuro o en otra condición la diferencia de altitud entre el blanco y el debris fuera $\ge 100\text{ km}$, **la distancia 3D jamás bajaría de 100 km y la crisis NUNCA se dispararía**.
  - **Conclusión:** Al pasar a distancia 3D, es obligatorio que `RogueDebris.move()` también aproxime su altitud hacia la del objetivo.

### 1.6. Casos concretos con números reales

* **Caso 1: Mismo punto lat/lon, distinta altitud (ISS 400 km vs GEO 35.786 km)**
  - Coordenadas A: (0°, 0°, 400 km)
  - Coordenadas B: (0°, 0°, 35.786 km)
  - Distancia actual `GeoPosition.distanceTo()`: **35.386,00 km**
  - Distancia real 3D ECEF: **35.386,00 km**
  - *Resultado:* El cálculo actual coincide exactamente porque el arco horizontal es 0. **No hay falsa alarma**.

* **Caso 2: Misma altitud orbital (400 km), separación angular de 1.5° (~167 km en superficie)**
  - Coordenadas A: (0°, 0°, 400 km)
  - Coordenadas B: (0°, 1.5°, 400 km)
  - Distancia actual `GeoPosition.distanceTo()`: **166,79 km**
  - Distancia real 3D ECEF: **177,26 km**
  - *Resultado:* El cálculo actual **subestima la distancia real en 10,47 km (un 5.9% de error)** debido a usar $R = 6371\text{ km}$ en lugar de $r = 6771\text{ km}$.

* **Caso 3: Falsa alarma real en el umbral de Radar (< 200 km)**
  - Coordenadas A: (0°, 0°, 400 km)
  - Coordenadas B: (0°, 1.7°, 450 km)
  - Distancia actual `GeoPosition.distanceTo()`: **195,53 km** $\rightarrow$ **DISPARA ALERTA CRÍTICA DE COLISIÓN (< 200 km)**.
  - Distancia real 3D ECEF: **207,74 km** $\rightarrow$ **NO DEBERÍA DISPARAR ALERTA ($\ge 200\text{ km}$)**.
  - *Resultado:* El error de 12.21 km convierte una situación orbital segura en una falsa alerta de colisión.

---

## 2. Problema 2: Exposición de API Key de N2YO

### 2.1. Ubicación de la API Key en el código
* **CONFIRMADO:** La API key está hardcodeada en texto plano en:
  - `src/api/N2YOApiClient.java` (línea 15):
    ```java
    public static final String DEFAULT_KEY = "DPP6C4-6KSLTW-K5Z8BU-5THO";
    ```
  - `src/gui/ControlPanel.java` (línea 82):
    ```java
    txtApiKey = crearCampoTexto(api.N2YOApiClient.DEFAULT_KEY);
    ```
    Se pre-rellena en un campo de texto visible en pantalla.
  - `src/Main.java` (línea 19):
    ```java
    apiKeyInput = api.N2YOApiClient.DEFAULT_KEY;
    ```
    Se utiliza como fallback si el usuario da Enter en la consola.

### 2.2. Presencia en logs y salidas
* **DESCARTE PARCIAL:** La key **no** se imprime directamente en `ConsolePanel`, ni en `TelemetryLogger`, ni en `System.out`.
* **RIESGO:** Viaja expuesta como parámetro de consulta en las URLs de las peticiones GET:
  `.../positions/{id}/{lat}/{lng}/{alt}/1/&apiKey=...`

### 2.3. Auditoría del historial de Git
* Se ejecutó `git log -S "DPP6C4-6KSLTW-K5Z8BU-5THO"`:
  - **Commit `88504b2`** (2026-08-11, autor `santisapiaa`): "Implementar API de Geolocalizacion" $\rightarrow$ **Aquí se introdujo la clave original**.
  - **Commit `b9e3991`** (2026-08-18, autor `alejotaccone`): "Implementar interfaz visual".
  - **Commit `cb97133`** (2026-09-14, autor `santisapiaa`): "Modularizar MainGUI en componentes UI con Alta Cohesion".
* La clave ha estado presente en el repositorio desde hace más de dos meses.

### 2.4. Estado del repositorio remoto en GitHub
* **Remoto:** `origin https://github.com/alejotaccone/Proyecto_Kerbal_Program_Space.git`
* **Visibilidad comprobada en la API de GitHub:**
  - `visibility: public`
  - `private: false`
* **CONFIRMADO:** **El repositorio es público**. La clave `DPP6C4-6KSLTW-K5Z8BU-5THO` está públicamente expuesta en GitHub.
* **ACCIÓN REQUERIDA DEL USUARIO:** Debes ingresar a [n2yo.com](https://www.n2yo.com/) y **rotar / regenerar la API Key** inmediatamente.

### 2.5. Estado del `.gitignore`
* El archivo `.gitignore` actual solo contiene:
  ```gitignore
  nave_monitor.txt
  monitor.ps1
  ```
* No protege `config.properties`, ni variables locales, ni binarios compilados `.class`.

---

## 3. Plan de Corrección Propuesto (FASE 2)

### Para el Problema 1 (Distancia 3D):
1. **Refactorizar `GeoPosition.java`:**
   - Crear el método `distance3DTo(GeoPosition other)` convirtiendo a coordenadas cartesianas ECEF:
     $$r = 6371.0 + \text{alt}$$
     $$x = r \cdot \cos(\text{lat}) \cdot \cos(\text{lon})$$
     $$y = r \cdot \cos(\text{lat}) \cdot \sin(\text{lon})$$
     $$z = r \cdot \sin(\text{lat})$$
     $$d_{3D} = \sqrt{\Delta x^2 + \Delta y^2 + \Delta z^2}$$
   - Renombrar o mantener el cálculo actual como `surfaceDistanceKm(GeoPosition other)` para distancias puras sobre la superficie terrestre.
   - Hacer que `GeoPosition.distanceTo()` use la distancia 3D real por defecto, manteniendo Experto en Información.
2. **Actualizar consumidores:**
   - `Radar.java`, `CrisisController.java` y `SpaceStation.java` consumirán la distancia 3D real.
3. **Ajustar `RogueDebris.java`:**
   - Modificar `RogueDebris.move()` para que el vector de intercepción calcule también $\Delta\text{alt}$ y converja suavemente en altitud hacia su objetivo, garantizando que las crisis sigan ocurriendo a pesar del cambio a 3D.

### Para el Problema 2 (Seguridad API Key):
1. **Eliminar la clave hardcodeada:**
   - Reemplazar `DEFAULT_KEY` por una cadena vacía o placeholder neutro.
2. **Mecanismo de resolución de configuración:**
   - Crear una clase utilitaria simple `api.ApiConfig` que busque la key en este orden:
     1. Variable de entorno del sistema `N2YO_API_KEY`.
     2. Archivo local `config.properties` en la raíz del proyecto (`n2yo.api.key=...`).
     3. Entrada manual desde la GUI o CLI.
3. **Gestión segura de archivos:**
   - Agregar `config.properties` al `.gitignore`.
   - Crear un archivo plantilla `config.properties.example` con instrucciones y sin claves reales.
4. **Manejo de errores:**
   - Si no se detecta ninguna clave, la GUI y la consola informarán al usuario de forma clara sin lanzar excepciones ni crashear.
   - Enmascarar la clave en cualquier log o representación textual.

---

## 4. Riesgos y Cambios de Comportamiento Esperados

| Componente | Comportamiento Anterior | Comportamiento Nuevo | Impacto / Riesgo |
| :--- | :--- | :--- | :--- |
| **Radar (< 200 km)** | Falsas alertas de colisión cuando dos satélites a altitudes distintas estaban cerca del umbral angular. | Detección física rigurosa. Menor cantidad de falsas alertas entre satélites a órbitas desfasadas. | Ningún riesgo; mayor fidelidad de simulación. |
| **Crisis (< 100 km)** | Podía fallar si la ISS cambiaba de altitud y el debris quedaba en otra cota. | El debris perseguirá en latitud, longitud y altitud. Las crisis ocurrirán con precisión geométrica real. | Se requiere verificar que la velocidad vertical del debris sea armónica con la horizontal. |
| **Inicio de la app** | Se conectaba automáticamente con la clave quemada. | Si el usuario no configuró la variable ni el `config.properties`, requerirá que ingrese la clave o la defina en el archivo. | Los usuarios nuevos deberán copiar `config.properties.example` a `config.properties`. |

---
**FIN DE FASE 1**  
Esperando confirmación del usuario para proceder con la FASE 2.

