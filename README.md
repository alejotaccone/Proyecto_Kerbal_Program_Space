# 🚀 Kerbal Program Space — Centro de Control y Simulador de Tráfico Orbital

![Java](https://img.shields.io/badge/Java-11%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Swing](https://img.shields.io/badge/GUI-Java%20Swing%20%2F%20AWT-blue?style=for-the-badge)
![API](https://img.shields.io/badge/API-N2YO%20NORAD%20REST-green?style=for-the-badge)

**Kerbal Program Space** es un simulador de monitoreo, telemetría y tráfico orbital en tiempo real desarrollado en Java puro (sin dependencias externas). El sistema se conecta en vivo a la **API REST de N2YO** (red de seguimiento NORAD) para rastrear satélites y estaciones espaciales reales en órbita terrestre, simulando dinámicas de vuelo, consumo de combustible, maniobras de evasión y alertas de colisión.

---

## 📌 Características Principales

* 🌐 **Integración en Vivo con N2YO (NORAD):** Obtiene coordenadas geográficas reales (latitud, longitud y altitud) en tiempo real de satélites como la Estación Espacial Internacional (ISS), Tiangong (CSS), Telescopio Hubble (HST), satélites NOAA y restos orbitales.
* 📡 **Dashboard Táctico y Radar 2D Animado (`MainGUI`):**
  * Pantalla de radar con barrido rotativo continuo a ~33 FPS y efecto de resplandor fosforescente.
  * Proyección matemática en tiempo real de coordenadas geodésicas a píxeles en pantalla.
  * *Blips* polimórficos de colores según el tipo de objeto (estaciones, tripulados, carga, sondas y chatarra).
  * Interactividad: click directo sobre cualquier satélite en el radar para inspeccionar sus datos.
* 🛸 **Modelo de Naves Polimórfico:**
  * **Estaciones Espaciales (`ISS` / `Tiangong`):** Soporte vital (oxígeno, energía con paneles solares, control térmico) y puerto de acople/recarga de combustible.
  * **Transbordadores Tripulados (`CrewShuttle`):** Gestión de tripulantes Kerbals (Pilotos, Ingenieros, Científicos con estadísticas de coraje).
  * **Naves de Carga (`CargoShip`):** Capacidad y gestión de carga útil en toneladas con capacidad de eyección de emergencia.
  * **Sondas Científicas (`ExplorationProbe`):** Paneles solares desplegables para recarga autónoma en órbita.
  * **Basura Espacial Pasiva (`SpaceDebris`):** Objetos inertes a la deriva con nivel de riesgo cinético.
  * **Anomalías Cinéticas Hostiles (`RogueDebris`):** Fragmentos con vector de persecución activo que amenazan con impactar naves aliadas.
* ⚡ **Sistema de Eventos de Crisis:** Detección de trayectorias de colisión (< 100 km) con ventana de decisión interactiva (forzar evasión o aceptar impacto).
* 🧵 **Arquitectura Asíncrona sin Congelamiento (Zero Lag):** Las llamadas de red HTTP corren en hilos `SwingWorker` y `CompletableFuture` en segundo plano, garantizando animaciones fluidas a 30 FPS.
* 🖥️ **Doble Modo de Ejecución:** Interfaz Gráfica Táctica (`MainGUI`) y Consola Interactiva CLI (`Main`) con monitor secundario en PowerShell.

---

## 📂 Estructura del Proyecto

```text
src/
├── Main.java                          # Punto de entrada por consola interactiva (CLI)
├── Img/                               # Recursos gráficos de las naves (JPG / JFIF)
├── api/                               # Integración externa REST
│   ├── N2YOApiClient.java             # Cliente HTTP para la API N2YO
│   └── N2YOJsonParser.java            # Parseador de respuestas JSON (SRP)
├── controller/                        # Capa Controlador (Patrón MVC — Iteración 4)
│   ├── SimulationController.java      # Controlador principal: timers, eventos de UI y orquestación
│   └── CrisisController.java          # Controlador de crisis: lógica de colisiones y órdenes tácticas
├── engine/                            # Lógica del motor y telemetría (Modelo)
│   ├── SimulationEngine.java          # Motor central: ticks, estado de objetos y física
│   ├── OrbitalEventSpawner.java       # Generador estocástico de eventos y basura espacial
│   ├── TelemetryLogger.java           # Registro y formateo telemétrico
│   └── ExternalMonitorLauncher.java   # Lanzador de la consola secundaria PowerShell
├── gui/                               # Capa Vista (Patrón MVC)
│   ├── MainGUI.java                   # Ventana principal JFrame (ensambla paneles especialistas)
│   ├── ControlPanel.java              # Panel de controles, configuración y leyenda (WEST)
│   ├── RadarPanel.java                # Pantalla animada del radar táctico 2D (CENTER)
│   ├── ShipMonitorPanel.java          # Consola lateral de telemetría e imagen de nave (EAST)
│   ├── HeaderBarPanel.java            # Barra superior con estado de tick y reloj (NORTH)
│   ├── ConsolePanel.java              # Terminal de logs telemétricos en tiempo real (SOUTH)
│   ├── CrisisDialogView.java          # Vista modal táctica para toma de decisiones y alertas
│   ├── CrisisDialogHandler.java       # Fachada de compatibilidad hacia CrisisController
│   └── ShipImageLoader.java           # Servicio de carga y caché de imágenes en memoria
├── model/                             # Capa Modelo (Dominio y Entidades)
│   ├── components/
│   │   ├── FuelTank.java              # Tanque de combustible, consumo y recarga
│   │   ├── PercentageGauge.java       # Indicador porcentual acotado (0%-100%)
│   │   ├── Kerbal.java                # Tripulante espacial con aptitudes
│   │   └── KerbalRole.java            # Enumeración de roles especializados
│   ├── geometry/
│   │   └── GeoPosition.java           # Coordenadas 3D y cálculo de distancia Haversine
│   └── spacecraft/
│       ├── OrbitalObject.java         # Clase abstracta base de objetos en órbita
│       ├── Spacecraft.java            # Vehículos propulsados activos
│       ├── SpaceStation.java          # Estación espacial con soporte vital
│       ├── CrewShuttle.java           # Transbordador tripulado
│       ├── CargoShip.java             # Nave de transporte de carga
│       ├── ExplorationProbe.java      # Sonda de investigación solar
│       ├── SpaceDebris.java           # Basura espacial pasiva inerte
│       ├── RogueDebris.java           # Fragmento hostil con vector de persecución
│       ├── SpacecraftFactory.java     # Factoría para instanciación polimórfica (OCP)
│       ├── SpacecraftInfo.java        # Metadata e identificación de nave
│       ├── FuelCarrier.java           # Interfaz para portadores de combustible (ISP)
│       ├── EvasiveCapable.java        # Interfaz de capacidad evasiva (ISP)
│       └── RadarRenderable.java       # Interfaz de dibujo en radar (ISP)
└── radar/
    └── Radar.java                     # Cobertura de radar y detección de colisiones
```

---

## ⚙️ Requisitos

* **Java Development Kit (JDK):** Versión 11 o superior.
* **Conexión a Internet:** Para la sincronización de satélites en vivo con la API N2YO.
* **Sistema Operativo:** Windows, macOS o Linux (la consola secundaria de PowerShell es compatible con Windows).

---

## 🔑 Configuración de API Key (N2YO)

Para obtener datos orbitales en vivo desde N2YO.com se requiere una API Key gratuita. El sistema admite múltiples métodos de configuración (en orden de prioridad):

1. **Archivo `config.properties` (Recomendado):**
   Copia el archivo de ejemplo y coloca tu clave:
   ```bash
   cp config.properties.example config.properties
   ```
   Dentro de `config.properties`:
   ```properties
   n2yo.api.key=TU_API_KEY_AQUI
   ```
   *(Nota: `config.properties` está ignorado en `.gitignore` para proteger tus credenciales).*

2. **Variable de entorno del sistema:**
   * PowerShell: `$env:N2YO_API_KEY="TU_API_KEY_AQUI"`
   * Bash / Linux: `export N2YO_API_KEY="TU_API_KEY_AQUI"`

3. **Ingreso manual en ejecución:**
   Tanto la GUI (`MainGUI`) como la consola (`Main`) disponen de campos interactivos para ingresar o cambiar la clave en caliente.

Si no se proporciona ninguna clave, la aplicación opera en **Modo Local de Contingencia**, simulando órbitas sin arrojar errores ni bloquear la interfaz.

---

## 🚀 Compilación y Ejecución

### 1. Compilar todo el proyecto

Desde la raíz del proyecto, ejecuta en la terminal:

```bash
# Crear la carpeta de binarios si no existe
mkdir bin

# Compilar todos los archivos fuente con codificación UTF-8 (incluye controller/)
javac -encoding UTF-8 -d bin src/model/geometry/*.java src/model/components/*.java src/model/spacecraft/*.java src/radar/*.java src/api/*.java src/engine/*.java src/controller/*.java src/gui/*.java src/test/*.java src/Main.java
```

### 2. Ejecutar las Pruebas de Distancia 3D

```bash
java -cp bin test.GeoPositionCheck
```

### 3. Ejecutar la Interfaz Gráfica (Recomendado)

```bash
java -cp bin gui.MainGUI
```

### 4. Ejecutar en Modo Consola (CLI / Debug)

```bash
java -cp bin Main
```

---

## 🎮 Controles del Dashboard Gráfico

1. **Iniciar Simulación:** Presiona `[>> INICIAR SIMULACIÓN]` para conectar con N2YO y comenzar el rastreo en vivo.
2. **Selección de Estación Objetivo:** Alterna entre la **ISS (NORAD 25544)** y la **Tiangong (NORAD 48274)** usando el combo superior y presionando `[R] CAMBIAR ESTACIÓN`.
3. **Monitoreo de Naves:** Haz click en cualquier punto/blip del radar o selecciónala en el menú desplegable para ver su foto real y ficha técnica en el panel derecho.
4. **Generar Crisis:** Pulsa `[!] Generar Anomalía` para inyectar una trayectoria hostil de *Rogue Debris* y poner a prueba los sistemas de evasión.

---

## 👥 Autores y Créditos
* Los derechos son reservados para Alejo Taccone y Santiago Sapia, cualquier copia de este   producto sera demandado en legales.
* Proyecto desarrollado para la simulación de tráfico y telemetría espacial.
* Datos orbitales en vivo provistos por [N2YO.com](https://www.n2yo.com/).
* © 2026 Alejo Taccone - Santiago Sapia. Todos los derechos reservados.

* <span style="color:red">**ADVERTENCIA**</span>: Queda estrictamente prohibida la reproducción, distribución, modificación o comunicación pública, total o parcial, de este contenido sin la autorización previa y por escrito del titular de los derechos de autor. Cualquier uso no autorizado constituirá una violación de los derechos de propiedad intelectual y podrá dar lugar a acciones legales civiles y penales.