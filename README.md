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
├── api/
│   └── N2YOApiClient.java             # Cliente HTTP y parseador JSON para la API N2YO
├── engine/
│   ├── SimulationEngine.java          # Motor central: ciclo de ticks, eventos y estado
│   └── TelemetryLogger.java           # Registro de telemetría y monitor de segunda consola
├── gui/
│   └── MainGUI.java                   # Dashboard táctico Swing y canvas del RadarPanel
├── model/
│   ├── components/
│   │   ├── FuelTank.java              # Tanque de combustible, consumo y recarga
│   │   └── Kerbal.java                # Tripulante con rol y nivel de coraje
│   ├── geometry/
│   │   └── GeoPosition.java           # Coordenadas 3D y cálculo de distancia Haversine
│   └── spacecraft/
│       ├── Spacecraft.java            # Clase base abstracta
│       ├── SpaceStation.java          # Estación espacial base con soporte vital
│       ├── InternationalSpaceStation.java # Estación Espacial Internacional (ISS)
│       ├── ChinaSpaceStation.java     # Estación Espacial Tiangong (CSS)
│       ├── CrewShuttle.java           # Transbordador tripulado
│       ├── CargoShip.java             # Nave de carga
│       ├── ExplorationProbe.java      # Sonda de exploración solar
│       ├── SpaceDebris.java           # Basura espacial inerte
│       └── RogueDebris.java           # Basura hostil con vector de intercepción
└── radar/
    └── Radar.java                     # Cobertura de radar y detección de colisiones
```

---

## ⚙️ Requisitos

* **Java Development Kit (JDK):** Versión 11 o superior.
* **Conexión a Internet:** Para la sincronización de satélites en vivo con la API N2YO.
* **Sistema Operativo:** Windows, macOS o Linux (la consola secundaria de PowerShell es compatible con Windows).

---

## 🚀 Compilación y Ejecución

### 1. Compilar todo el proyecto

Desde la raíz del proyecto, ejecuta en la terminal:

```bash
# Crear la carpeta de binarios si no existe
mkdir bin

# Compilar todos los archivos fuente con codificación UTF-8
javac -encoding UTF-8 -d bin src/model/geometry/*.java src/model/components/*.java src/model/spacecraft/*.java src/radar/*.java src/api/*.java src/engine/*.java src/gui/*.java src/Main.java
```

### 2. Ejecutar la Interfaz Gráfica (Recomendado)

```bash
java -cp bin gui.MainGUI
```

### 3. Ejecutar en Modo Consola (CLI / Debug)

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

* Proyecto desarrollado para la simulación de tráfico y telemetría espacial.
* Datos orbitales en vivo provistos por [N2YO.com](https://www.n2yo.com/).
