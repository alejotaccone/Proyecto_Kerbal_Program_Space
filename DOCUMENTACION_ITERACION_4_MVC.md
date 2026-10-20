# 🚀 Documentación de la Iteración 4 — Patrón MVC (Modelo - Vista - Controlador)

---

## 📌 1. ¿Qué es MVC y por qué lo aplicamos?

Hasta la Iteración 3, nuestro proyecto ya tenía mejoras importantes de diseño como **GRASP** (Alta Cohesión, Experto en Información, Bajo Acoplamiento) y **SOLID** (Responsabilidad Única, Abierto/Cerrado, Segregación de Interfaces).

Sin embargo, en la parte gráfica teníamos un problema clásico de las aplicaciones de escritorio: **la ventana principal (`MainGUI`) hacía de todo**. 
No solo armaba los botones y la pantalla, sino que también:
- Manejaba los temporizadores de la simulación.
- Coordinaba los hilos de fondo (`SwingWorker`) para llamar a la API.
- Tomaba decisiones sobre qué hacer cuando el usuario presionaba un botón.
- Calculaba qué datos mostrar y cuándo repintar el radar.

Para resolver esto y cumplir con los objetivos de la **Iteración 4**, implementamos el patrón de arquitectura **MVC (Modelo - Vista - Controlador)**.

### En palabras simples, ¿cómo se reparten las tareas ahora?

1. **El Modelo (M):** Es el corazón del sistema. Sabe cómo vuelan las naves, cuánta nafta les queda, si chocan o no, y se conecta con la API de N2YO. **No sabe nada de botones, ventanas ni colores**.
2. **La Vista (V):** Son las ventanas y paneles que ve el operador en pantalla. Solo se preocupa por verse bien, ubicar los componentes y capturar los clics del usuario. **No calcula física ni toma decisiones de negocio**.
3. **El Controlador (C):** Es el intermediario o director de orquesta. Cuando el usuario hace clic en un botón, la Vista le avisa al Controlador. El Controlador le pide al Modelo que haga la acción (por ejemplo, evadir o recargar) y luego le dice a la Vista cómo tiene que actualizarse la pantalla con los nuevos datos.

---

## 🔍 2. ¿Qué se modificó exactamente? (Seguimiento clase por clase)

### A. Nuevas clases creadas en el paquete `controller/`

#### 1. `SimulationController.java` (Controlador Principal)
- **¿Qué hace?** Es el cerebro operativo del simulador gráfico.
- **Responsabilidades:**
  - Recibe los eventos de todos los botones de `ControlPanel` (Iniciar simulación, Detener, Cambiar estación, Evasión, Habilidad especial, Recargar combustible, Generar crisis).
  - Escucha los clics sobre el radar interactivo para seleccionar naves automáticamente.
  - Administra los dos temporizadores del sistema: el reloj de ticks (cada 10 segundos) y la animación del radar táctico (a ~33 cuadros por segundo).
  - Ejecuta las peticiones a la API en segundo plano con `SwingWorker` para que la pantalla nunca se congele.
  - Sincroniza los datos del `SimulationEngine` con los paneles visuales.

#### 2. `CrisisController.java` (Controlador de Colisiones y Crisis)
- **¿Qué hace?** Gestiona la lógica de decisiones cuando una basura hostil (*Rogue Debris*) se acerca peligrosamente a una nave.
- **Responsabilidades:**
  - Detecta si hay una amenaza a menos de 100 km de distancia.
  - Pausa la simulación temporalmente.
  - Le pide a la Vista (`CrisisDialogView`) que le pregunte al Comandante si quiere forzar la evasión o ignorar la alerta.
  - Según la respuesta del usuario, ejecuta la maniobra de evasión en el Modelo o aplica la destrucción de la nave en el motor.
  - Reanuda la simulación una vez resuelta la emergencia.

---

### B. Mejoras en la capa visual `gui/` (La Vista)

#### 3. `MainGUI.java` (Refactorizada y simplificada)
- **Antes:** Tenía **465 líneas de código**. Tenía mezclada la construcción de la ventana con la lógica de conexión a la API, timers de simulación, gestión de eventos y toma de decisiones.
- **Ahora:** Quedó reducida a **~140 líneas**. Es una Vista pura con **Alta Cohesión**:
  - Solo se encarga de crear la ventana, colocar los 5 paneles tácticos y configurar el diseño (*Composite UI*).
  - Al iniciar, crea su `SimulationController` y le delega el control.
  - Expone métodos *getter* limpios para que el controlador pueda leer los datos ingresados y actualizar la pantalla.

#### 4. `CrisisDialogView.java` (Nueva clase de Vista)
- **¿Qué hace?** Extrae toda la parte visual que antes estaba mezclada dentro de `CrisisDialogHandler`.
- Se encarga exclusivamente de dibujar los cuadros de diálogo modales con estilo cibernético oscuro:
  - La ventana de alerta de impacto con los botones tácticos de decisión.
  - La ventana de confirmación cuando una estación es destruida, mostrando su imagen y causa del impacto.
- No borra objetos del motor ni calcula combustible; solo muestra la ventana y devuelve la opción elegida por el operador.

#### 5. `CrisisDialogHandler.java` (Mantenida por compatibilidad)
- Se adaptó para que actúe como una **fachada de compatibilidad hacia atrás**. Si alguna parte del código o documentación vieja la llama, delega la llamada directamente al nuevo `CrisisController`.

---

## 📊 3. Mapa de la Arquitectura MVC en el Proyecto

| Capa | Paquetes Java | Clases Principales | ¿De qué se encarga? |
| :--- | :--- | :--- | :--- |
| **MODELO (Model)** | `model/`<br>`engine/`<br>`radar/`<br>`api/` | `SimulationEngine`<br>`OrbitalObject` (y todas sus subclases)<br>`Radar`<br>`FuelTank`, `Kerbal`<br>`N2YOApiClient`, `N2YOJsonParser` | Datos de las naves, física orbital, cálculo de combustible, detección matemática de distancias y conexión HTTP con NORAD. |
| **VISTA (View)** | `gui/` | `MainGUI`<br>`ControlPanel`<br>`RadarPanel`<br>`ShipMonitorPanel`<br>`HeaderBarPanel`<br>`ConsolePanel`<br>`CrisisDialogView`<br>`ShipImageLoader` | Dibujar la interfaz, animación del barrido fosforescente, paneles de control, mostrar fotos de naves y renderizar ventanas emergentes. |
| **CONTROLADOR (Controller)** | `controller/` | `SimulationController`<br>`CrisisController` | Conectar la Vista con el Modelo. Escucha clics, timers y eventos, ejecuta la lógica en el motor y refresca la interfaz con los resultados. |

---

## 📈 4. Diagrama de Clases Actualizado

El archivo interactivo **`Diagrama de Clases (1).html`** fue actualizado con el nuevo diseño:
- **Pestaña 3 ("Interfaz Gráfica"):** Muestra la nueva clase `SimulationController` conectada a `MainGUI`, a los paneles y a `CrisisController`.
- **Pestaña 4 ("Arquitectura General"):** Muestra claramente la separación en tres niveles: Capa Vista, Capa Controlador y Capa Modelo.

Para verlo, simplemente hacé doble clic sobre `Diagrama de Clases (1).html` en el navegador.

---

## 🛠️ 5. Cómo Compilar y Ejecutar

Para compilar el proyecto con el nuevo paquete `controller`:

```bash
# 1. Compilar todo el proyecto a la carpeta bin/
javac -encoding UTF-8 -d bin src/model/geometry/*.java src/model/components/*.java src/model/spacecraft/*.java src/radar/*.java src/api/*.java src/engine/*.java src/controller/*.java src/gui/*.java src/Main.java

# 2. Ejecutar el Dashboard Gráfico (Modo Recomendado)
java -cp bin gui.MainGUI

# 3. O ejecutar el modo Consola CLI interactivo
java -cp bin Main
```

---

## ✅ Conclusión para la Entrega de la Iteración 4
- **Se implementó el patrón arquitectónico MVC en su totalidad.**
- **Se eliminó el Bad Smell de God Class en `MainGUI`**, reduciendo su tamaño en más de un 70%.
- **No se rompió ninguna funcionalidad previa**: la conexión con N2YO, el radar táctico a 33 FPS, la evasión de basura espacial y la recarga en estación siguen funcionando con total fluidez.

