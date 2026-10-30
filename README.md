# Simulador de Batalla RPG - Proyecto base

Este es el punto de partida del taller **Concurrencia e Hilos Aplicados al Simulador de Batalla RPG**,
continuacion del proyecto integrador "Simulador de Batalla RPG / Arena de Heroes" de la **Unidad 4.1**.
Es el simulador que ya construiste en los talleres 1 a 8, reunido en un solo proyecto y funcionando
de forma **100 % secuencial**: todo ocurre en un unico hilo, una cosa despues de otra.

## Objetivo

1. Leer el codigo y comprobar que reconoces cada concepto de los talleres anteriores.
2. Ejecutar el juego y ver donde la ejecucion secuencial se queda corta.
3. Identificar los puntos de extension (buscalos con el comentario `TALLER CONCURRENCIA`).

## Como compilar y ejecutar

Con Maven (JDK 21):

```
mvn -q compile
java -cp target/classes com.rpg.Main
```

Sin Maven, solo con `javac` (desde la carpeta del proyecto):

```
javac -encoding UTF-8 -d out $(find src/main/java -name "*.java")
java -cp out com.rpg.Main
```

En IntelliJ IDEA basta con abrir la carpeta como proyecto Maven y ejecutar `Main`.
El juego lee de `System.in`, asi que tambien puedes probarlo sin teclado:

```
printf '1\n1\nThor\n1\n1\n1\n1\n0\n' | java -cp out com.rpg.Main
```

El juego no escribe ningun archivo: el progreso se guarda en memoria (`MotorJuego.getUltimoGuardado()`),
solo cuando termina una pelea. El manejo de archivos se ve mas adelante en el curso.

## Estructura de paquetes

```
com.rpg
  Main                      menu principal por consola
  core/                     flujo del juego
    MotorJuego              crear heroe, bucle de combate, resultado, guardarProgreso()
    Arena                   reglas: ataque, especial, efectos por turno, duelo automatico
    TorneoManager           4 gladiadores, 2 semifinales y final
    Bestiario               Map de monstruos + contador de derrotas
    Consola                 lectura con validacion y reintento
    RegistroCombate         imprime y acumula el registro de combate
    FormatoCombate          barra de vida [#####-----] y resumen
    Accion                  enum con las acciones del jugador
  entities/                 modelo del dominio
    Personaje (abstracta), ICombatiente, Guerrero, Mago, Enemigo
    EstadoEfecto, Arma, Gremio, Escuadron, PerfilJugador
    enums/                  TipoPersonaje, EstadoPersonaje, RaridadItem, TipoEfecto
  items/                    Inventario<T>, Pocion
  exceptions/               JuegoException, SinManaException, CapacidadExcedidaException,
                            VidaInsuficienteException
  concurrency/              VACIO a proposito (solo package-info.java)
```

## Lo que ya debes tener (checklist)

| Taller | Concepto | Donde se ve |
|--------|----------|-------------|
| T1 | `switch` moderno (`->`), ternario, condiciones `&&` / `\|\|` | `MotorJuego.pedirAccion`, `Arena.atacar` (ternario del critico), `Arena.turnoAutomatico` |
| T1 | `equals` vs `==` en String; division decimal y casteos | `Bestiario.crearEnemigo` (`equalsIgnoreCase`), `Personaje.defender` y `calcularDanio` |
| T2 | Bucles `while` / `do-while` / `for`, `break` / `continue` | `MotorJuego.combatir`, `turnoJugador` (do-while), `Consola.leerEntero` (reintento con `continue`), `Personaje.procesarEfectos` |
| T3 | `String`: `trim`, `equalsIgnoreCase`, `format`, `StringBuilder` | `Consola.leerNombre`, `FormatoCombate.barraVida`, `RegistroCombate`, `MotorJuego.guardarProgreso` |
| T4 | Clases, objetos, constructores sobrecargados y `this(...)` | `Guerrero`, `Mago`, `Enemigo`, `Pocion`, `Personaje` |
| T5 | Encapsulamiento: atributos privados, getters/setters validados | `Personaje.setVida` (nunca < 0 ni > vida maxima), `setNombre`, `Arma` |
| T6 | Clase abstracta, interfaz, polimorfismo, genericos | `Personaje`, `ICombatiente`, `realizarAtaqueEspecial` en `Guerrero` / `Mago` / `Enemigo`, `Inventario<T>` |
| T7 | Composicion, agregacion, asociacion | `Personaje` crea su `Inventario` (composicion); `Gremio` y `Escuadron` (agregacion); `Personaje.equiparArma` y `unirseAGremio` (asociacion) |
| T8 | Enums, `Set`, `Map`, excepciones propias | `enums/`, `Personaje.habilidades` (`HashSet`), `PerfilJugador.logros` (`TreeSet`), `Bestiario` (`Map`), `exceptions/` capturadas en `Arena.intentarEspecial`, `MotorJuego.repartirBotin` y `entregarKitInicial` |

## Que NO tiene aun (lo haras en el taller de concurrencia)

Estos cuatro modulos van en el paquete `com.rpg.concurrency` (y en los puntos marcados del motor):

1. **Temporizador de turno**: hoy el jugador puede tardar lo que quiera en elegir su accion
   (`MotorJuego.pedirAccion` bloquea esperando el teclado).
2. **Efectos DoT / HoT con interrupcion**: hoy `Personaje.procesarEfectos` aplica los efectos turno a turno;
   no hay tareas independientes ni forma de cancelarlas a mitad de camino.
3. **Semifinales en paralelo con `join`**: `TorneoManager.jugarTorneo` simula una semifinal despues de otra.
4. **Auto-guardado daemon**: `MotorJuego.guardarProgreso` solo se invoca manualmente al terminar una pelea
   (y guarda en memoria, sin archivos).

### Advertencia: condicion de carrera en la vida

`Personaje.recibirDanio` y `Personaje.curar` modifican la vida con operaciones de leer, calcular y escribir
que **no estan protegidas**. Hoy funcionan porque solo hay un hilo. Cuando varios hilos toquen la vida del
mismo personaje al mismo tiempo (por ejemplo un veneno y un ataque), se podran perder actualizaciones.
Parte del taller consiste en reproducir el problema y resolverlo.

El problema no es solo la vida: tambien comparten estado mutable el campo `estado` (`setVida` lo pasa a
`MUERTO`, y `curar` lo consulta con `estaVivo()`) y el mapa `efectos`, que `aplicarEfecto` y `quitarEfecto`
modifican mientras `procesarEfectos` lo recorre (puede lanzar `ConcurrentModificationException`). Cuando una
operacion toca varias variables dependientes, la unidad 4.2 recomienda `synchronized`.

## Checklist del taller: lo que debes realizar

Marca cada punto cuando lo tengas funcionando. El orden sigue las secciones de la guia del taller.
Las clases nuevas van en `com.rpg.concurrency` (y `TurnoCallback` en `com.rpg.core`).

### Antes de empezar

- [ ] El proyecto compila y ejecutas `Main` sin errores.
- [ ] Jugaste un combate rapido y un torneo completo, y localizaste los 5 comentarios `TALLER CONCURRENCIA`.
- [ ] Repasaste la tabla "Lo que ya debes tener": si algun concepto no lo reconoces, repasalo antes de seguir.

### Seccion 1: Temporizador de turno (`Thread`, `Runnable`, `sleep`)

- [ ] Crear la interfaz `TurnoCallback` con el metodo `void onTiempoAgotado()`.
- [ ] Crear `TemporizadorTurnoTask` que implemente `Runnable` y reciba por constructor la duracion en segundos y el `TurnoCallback`.
- [ ] En `run()`, hacer una cuenta regresiva con un `for` o `while` decreciente, un segundo por iteracion.
- [ ] Usar `Thread.sleep(1000)` dentro de un `try-catch` que capture `InterruptedException`.
- [ ] Al llegar a 0, llamar a `callback.onTiempoAgotado()`.
- [ ] Crear `GestorTurno`, que cree el `Thread` con el `Runnable` (o con una lambda) y llame a `.start()`.
- [ ] Integrarlo en `MotorJuego.pedirAccion`: si el tiempo se agota antes de que el jugador elija, el turno se pierde.
- [ ] Si el jugador responde a tiempo, cancelar el temporizador con `.interrupt()` (sin banderas booleanas manuales).

### Seccion 2: Efectos continuos DoT / HoT (`interrupt`, `InterruptedException`)

- [ ] Crear `EfectoContinuoTask` (`Runnable`) con nombre del efecto e intervalo en milisegundos.
- [ ] En `run()`, usar `while (!Thread.currentThread().isInterrupted())` y aplicar el dano o la curacion sobre el `Personaje`.
- [ ] En el `catch (InterruptedException e)`: limpiar el estado del personaje, llamar a `Thread.currentThread().interrupt()` y salir del bucle con `break`.
- [ ] Cada efecto corre en su propio hilo; no se usa `Thread.stop()` ni banderas booleanas.
- [ ] Reemplazar la logica por turnos de `Personaje.procesarEfectos` por hilos que se cancelan con `.interrupt()`.
- [ ] Al usar un antidoto, al recibir una purificacion o al terminar el combate, el hilo del efecto se interrumpe y termina limpio.

### Seccion 3: Torneo en paralelo (`join`)

- [ ] En `TorneoManager.jugarTorneo`, crear dos `Thread`, uno por semifinal, cada uno con su `Runnable`.
- [ ] Llamar a `.start()` en ambos para que corran al mismo tiempo.
- [ ] En el hilo principal, llamar a `.join()` sobre la semifinal A y luego sobre la B.
- [ ] Proteger los `join()` con `try-catch` de `InterruptedException`.
- [ ] Consultar los ganadores **solo despues** de que ambos `join()` hayan retornado, y entonces lanzar la final.
- [ ] Imprimir `Thread.getState()` de cada semifinal antes de `start()`, durante la ejecucion y despues del `join()`, y relacionar lo que ves con el ciclo de vida del hilo (`NEW`, `RUNNABLE`, `TIMED_WAITING`, `TERMINATED`).

### Seccion 4: Auto-guardado en segundo plano (hilo daemon)

- [ ] Crear `AutoGuardadoDaemon` con un bucle `while (true)` y `Thread.sleep(intervalo)` entre guardados.
- [ ] Hacer que el guardado registre periodicamente las estadisticas de los personajes y el historial de la partida **en memoria** (por ejemplo, un `String` o una lista con la ultima copia). No uses archivos todavia.
- [ ] Llamar a `setDaemon(true)` **antes** de `.start()` (si no, `IllegalThreadStateException`).
- [ ] Comprobar que la aplicacion cierra de inmediato al salir, sin esperar al hilo de guardado.

### Seccion 5: Reto integrador

- [ ] Los cuatro modulos funcionan juntos: temporizador de turno, efectos DoT/HoT, torneo con `join` y auto-guardado daemon.
- [ ] Cumples la estructura esperada: `com.rpg.core` (`MotorJuego`, `TorneoManager`, `TurnoCallback`), `com.rpg.entities` (`Personaje`, `EstadoEfecto`), `com.rpg.concurrency` (`TemporizadorTurnoTask`, `EfectoContinuoTask`, `AutoGuardadoDaemon`) y `Main`.
- [ ] El combate y el torneo se juegan completos, sin bloqueos ni excepciones sin controlar.

### Seccion 6: Depuracion y condiciones de carrera

- [ ] Reproduciste la condicion de carrera: dos efectos DoT (o un efecto y un ataque) sobre el mismo `Personaje` pierden actualizaciones de vida.
- [ ] La corrigiste en `Personaje.recibirDanio` y `curar` con `synchronized` (mecanismo principal, visto en la unidad 4.2), y puedes explicar por que lo elegiste.
- [ ] Protegiste tambien el acceso a `estado` y `efectos` (ver la advertencia de arriba).
- [ ] Puedes explicar por que `AtomicInteger.addAndGet(-danio)` no basta aqui: `setVida` acota la vida a `0..vidaMaxima` y cambia `estado`. (Reto extra: resolverlo con `updateAndGet` o un bucle con `compareAndSet`.)
- [ ] Ningun `catch (InterruptedException)` esta vacio ni solo imprime la traza.
- [ ] Nunca llamas a `.start()` dos veces sobre el mismo `Thread`: para cada turno o combate se crea un `Thread` nuevo.
- [ ] Nunca llamas a `run()` directamente: usas siempre `start()`, que es lo que crea el hilo nuevo.
- [ ] Todos los `Thread.sleep()` estan dentro de `try-catch` que gestiona `InterruptedException`.
- [ ] Todo hilo de auto-guardado o monitoreo usa `setDaemon(true)` antes de `.start()` (ver "Investigacion obligatoria").
- [ ] El `join()` del torneo espera a ambas semifinales antes de leer la memoria compartida de los ganadores.

## Investigacion obligatoria

Dos temas de la guia no se explican en las presentaciones de las unidades 4.1 y 4.2. Debes investigarlos
por tu cuenta y dejar evidencia en el proyecto:

- [ ] **Hilos daemon.** Demuestra con una prueba que la JVM cierra aunque el hilo daemon siga vivo, y que
  `setDaemon(true)` despues de `start()` lanza `IllegalThreadStateException`. Anota en un comentario la
  diferencia entre hilo de usuario y hilo daemon.
- [ ] **`ReentrantLock`.** Reescribe `Personaje.recibirDanio` con `lock()` / `unlock()` dentro de `try-finally`
  y explica en 2 o 3 lineas cuando lo preferirias a `synchronized`. Si el `unlock()` no esta en el `finally`,
  el lock puede quedar tomado para siempre.

## Adaptaciones respecto a la guia

Este proyecto sigue la guia del taller, con estas diferencias a proposito:

- **Auto-guardado en memoria.** La guia habla de guardar "en el almacenamiento". Aqui el guardado es en
  memoria (un `String` o una lista con la ultima copia) porque el manejo de archivos se ve mas adelante.
- **Reemplazo de `Personaje.procesarEfectos`.** La guia pide que cada efecto DoT/HoT corra en su propio hilo;
  este taller ademas te pide sustituir la logica por turnos actual por esos hilos.
- **Mecanismo de sincronizacion.** La guia sugiere `synchronized`, `AtomicInteger` o `ReentrantLock`. Aqui el
  mecanismo principal es `synchronized` (unidad 4.2); `ReentrantLock` y los hilos daemon pasan a la seccion
  "Investigacion obligatoria" porque las presentaciones no los cubren.
- **Nombre del temporizador.** La guia usa `TemporizadorTurno` en la tabla y los pasos de la seccion 1, pero
  `TemporizadorTurnoTask` en la estructura de la seccion 5. Usa `TemporizadorTurnoTask`.
- **Nombre del paquete.** El esqueleto de la guia declara `package com.rpg.concurrencia;`. En este proyecto el
  paquete es `com.rpg.concurrency`: ajusta la linea `package` si copias el esqueleto.
- **Detalles de `EfectoContinuoTask` que la guia sugiere.** Usa un `finally` para liberar recursos y consulta
  `Thread.currentThread().isInterrupted()` donde no haya un `sleep()` que lance `InterruptedException`.

## Notas

- Los mensajes de consola no llevan tildes a proposito, para que se vean bien en cualquier terminal.
- Para repetir el juego con resultados distintos no hay semilla fija: el azar (criticos, habilidades de los
  enemigos) cambia en cada ejecucion.
