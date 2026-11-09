# Simulador de Batalla RPG

Simulador de combates por turnos en consola, hecho en Java durante los talleres del modulo.
Empezo como un juego secuencial y en cada taller le fuimos agregando algo nuevo: programacion orientada
a objetos, colecciones, hilos, manejo de archivos, conexion a **MySQL** con **MVC + DAO** y, en el ultimo
taller, **expresiones lambda** y **Stream API**.

Todo lo necesario para instalarlo, ejecutarlo y revisar cada taller esta en este README.

| | |
|---|---|
| Lenguaje | Java 21 |
| Gestor de dependencias | Maven (`mysql-connector-j 8.4.0`) |
| Base de datos | MySQL 8.4 LTS (`rpg_grupal_db`) |
| IDE usado | IntelliJ IDEA |
| Clase principal | `com.rpg.Main` |

## Contenido

1. [Que hace el proyecto](#1-que-hace-el-proyecto)
2. [Requisitos](#2-requisitos)
3. [Instalacion y ejecucion paso a paso](#3-instalacion-y-ejecucion-paso-a-paso)
4. [Como usarlo](#4-como-usarlo)
5. [Estructura del proyecto](#5-estructura-del-proyecto)
6. [Arquitectura](#6-arquitectura)
7. [Talleres y checklists](#7-talleres-y-checklists)
8. [Problemas comunes](#8-problemas-comunes)
9. [Notas](#9-notas)
10. [Autores](#10-autores)

---

## 1. Que hace el proyecto

**Registro de heroes (guardado en MySQL)**
- Insertar, listar, buscar por tipo, actualizar nivel y eliminar heroes.
- Las validaciones estan en el Controller y la tabla tiene restricciones `CHECK` y `UNIQUE`.
- Todas las consultas usan `PreparedStatement`, asi que no se puede hacer inyeccion SQL.
- Estadisticas, ranking por poder y filtros hechos con Stream API y `Predicate`.

**Simulador de batalla**
- Combate rapido contra los monstruos del bestiario, con pociones, efectos de veneno o curacion y logros.
- Torneo de 4 gladiadores con las semifinales en paralelo (hilos y `join`).
- Temporizador de turno y auto-guardado en segundo plano con un hilo daemon.
- Guardar, cargar y borrar la partida (serializacion) e historial de combates en un `.txt`.
- Cargar un heroe de la base de datos para pelear con el.

---

## 2. Requisitos

| Herramienta | Version | Para que |
|-------------|---------|----------|
| JDK | 21 | Compilar y ejecutar. IntelliJ lo puede descargar solo. |
| MySQL Server | 8.x (lo probamos con 8.4.13 LTS) | Guardar los heroes. Debe escuchar en el puerto `3306`. |
| IntelliJ IDEA | Community o Ultimate | Abrir y ejecutar el proyecto. Ya trae Maven. |
| DBeaver (u otro cliente SQL) | Cualquiera | Ejecutar el script `schema.sql`. |

El driver de MySQL no hay que descargarlo a mano: esta en el `pom.xml` y Maven lo baja al cargar el proyecto.

---

## 3. Instalacion y ejecucion paso a paso

Si el equipo ya tiene MySQL funcionando, se puede pasar directo al paso 3.3.

### 3.1 Descargar el proyecto

```bash
git clone https://github.com/ARSCIAND/rpg-base.git
```

O en GitHub: **Code -> Download ZIP** y descomprimirlo.

### 3.2 Instalar MySQL en Windows (sin Docker)

1. Entrar a <https://dev.mysql.com/downloads/mysql/>.
2. En **Select Version** elegir una **8.4.x LTS** y en el sistema operativo dejar **Microsoft Windows**.
3. Descargar **Windows (x86, 64-bit), MSI Installer**. En la pagina de Oracle se puede dar
   **No thanks, just start my download** para no crear cuenta.
4. Abrir el `.msi`, elegir **Typical** y luego **Install**.
5. Al final dejar marcado **Run MySQL Configurator** y dar **Finish**.
6. En el Configurator:

| Pantalla | Que hacer |
|----------|-----------|
| Data Directory | Dejar la ruta que viene |
| Type and Networking | *Development Computer*, TCP/IP marcado, puerto **3306** |
| Accounts and Roles | Escribir la **clave de root** y anotarla (se usa en el paso 3.4) |
| Windows Service | Dejar marcado que arranque con Windows (el servicio se llama **MySQL84**) |
| Server File Permissions | Dejar la primera opcion |
| Sample Databases | No marcar nada |
| Apply Configuration | **Execute** y esperar a que todos los pasos queden en verde |

Con eso MySQL queda corriendo como servicio y se prende solo cada vez que se enciende el equipo.

### 3.3 Crear la base de datos

El script [`schema.sql`](schema.sql) crea la base `rpg_grupal_db`, la tabla `heroes` y tres heroes de
prueba (Thor, Merlin y Xena).

**Con DBeaver:**

1. **Base de datos -> Nueva conexion de base de datos -> MySQL** (driver *MySQL 8 and later*).
2. Host `localhost`, puerto `3306`, usuario `root` y la clave de root.
3. En la pestana **Driver properties** (si pide descargar el driver, aceptar) poner
   `allowPublicKeyRetrieval` en **true**. Sin esto MySQL 8.4 rechaza la conexion con
   *Public Key Retrieval is not allowed*.
4. **Probar conexion** -> debe decir *Conectado* -> **Finalizar**.
5. Clic derecho en la conexion -> **Editor SQL -> Nuevo script SQL**, pegar todo `schema.sql` y ejecutarlo
   con **Alt + X** (Ctrl + Enter solo ejecuta una sentencia).
6. DBeaver avisa del `DROP TABLE`: se acepta, la tabla todavia no existe. El mensaje
   *Unknown table 'rpg_grupal_db.heroes'* que sale en la pestana Salida tambien es normal.
7. Refrescar con **F5**: tiene que aparecer `rpg_grupal_db` con la tabla `heroes`.

**Con la consola de MySQL** (si esta en el PATH):

```bash
mysql -u root -p < schema.sql
```

### 3.4 Archivo de conexion `config.txt`

El programa no tiene la clave escrita en el codigo: la lee de un archivo `config.txt` que va en la raiz
del proyecto, al lado del `pom.xml`.

1. Copiar `config.example.txt` y renombrar la copia como `config.txt`
   (en IntelliJ: clic derecho -> **Copy**, clic derecho en la carpeta `rpg-base` -> **Paste**).
2. Cambiar la clave por la de root:

```properties
url=jdbc:mysql://localhost:3306/rpg_grupal_db?allowPublicKeyRetrieval=true&useSSL=false
usuario=root
clave=su_clave
```

`config.txt` esta en el `.gitignore`, por eso no aparece en el repositorio y cada quien crea el suyo.

### 3.5 Abrir en IntelliJ IDEA

1. **File -> Open** y elegir la carpeta `rpg-base` (la que tiene el `pom.xml`).
2. Cuando salga el aviso *"Maven build scripts found"*, dar **Load Maven Project**
   (o clic derecho en `pom.xml` -> **Add as Maven Project** / **Maven -> Reload project**).
3. Esperar a que termine la barra de abajo. En **External Libraries** debe aparecer
   `Maven: com.mysql:mysql-connector-j:8.4.0`.
4. En **File -> Project Structure -> Project** revisar que el SDK sea **21**.

### 3.6 Ejecutar

1. Abrir `src/main/java/com/rpg/Main.java`.
2. Dar clic en el triangulo verde al lado de `main` -> **Run 'Main.main()'**.
3. En la consola escribir `2` y Enter. Si salen Thor, Merlin y Xena, la conexion con MySQL funciona.

Tambien se puede ejecutar sin IntelliJ si se tiene Maven instalado:

```bash
mvn -q compile exec:java -Dexec.mainClass=com.rpg.Main
```

### 3.7 Prueba rapida de todo

| Opcion | Que se deberia ver |
|--------|--------------------|
| `2` | Los 3 heroes de la tabla |
| `1` | Insertar un heroe nuevo (por ejemplo `Artemisa`, `mago`, `85`, `4`) |
| `7` | Estadisticas: total, guerreros y magos, vida promedio, el mas poderoso, veteranos |
| `8` | Ranking por poder (poder = vida + nivel x 10) |
| `9` | Filtros: veteranos, guerreros veteranos, novatos, magos o nombre que contiene un texto |
| `6` y luego `9` | Cargar un heroe de la BD y pelear con el. Merlin (nivel 5) entra con 110 de vida |
| `0` | Salir |

---

## 4. Como usarlo

El programa arranca en el menu del registro de heroes:

```
=== REGISTRO DE HEROES ===
1. Insertar heroe
2. Listar heroes
3. Buscar heroes por tipo
4. Actualizar nivel
5. Eliminar heroe
6. Ir al simulador de batalla
7. Estadisticas de heroes (Streams)
8. Ranking por poder (Streams)
9. Filtrar heroes (Lambdas / Predicate)
0. Salir
```

La opcion 6 abre el simulador de batalla:

```
=== SIMULADOR DE BATALLA RPG ===
1. Combate rapido
2. Torneo
3. Ver bestiario
4. Ver historial de combates
5. Guardar partida
6. Cargar partida
7. Ver archivos
8. Borrar partida
9. Cargar heroe desde BD
0. Volver
```

Las partidas y el historial se guardan en la carpeta `partidas/`, que se crea sola al ejecutar.

### Demos que se ejecutan por separado

Cada una tiene su propio `main` y se corre con el triangulo verde. Ninguna necesita MySQL.

| Clase | Que muestra |
|-------|-------------|
| `funcional/DemoLambdas` | Unidad 6.1: clase anonima vs lambda, `Consumer`, `Supplier`, `Function`, `Predicate`, interfaz funcional propia y un pipeline con las cuatro |
| `funcional/DemoStreams` | Unidad 6.2: origenes, `filter`, `map`, `flatMap`, `anyMatch`, evaluacion perezosa, `toList` y que un stream no se reutiliza |
| `concurrency/DemoCondicionCarrera` | La condicion de carrera y como se arregla con `synchronized` y `ReentrantLock` |
| `concurrency/DemoDaemon` | Diferencia entre hilo de usuario e hilo daemon |
| `persistencia/DemoSerializacion` | Serializacion, `transient` y `serialVersionUID` |

---

## 5. Estructura del proyecto

```
rpg-base/
  pom.xml                  version de Java y dependencia mysql-connector-j
  schema.sql               crea rpg_grupal_db, la tabla heroes y los datos de prueba
  config.example.txt       plantilla de la conexion (se copia como config.txt)
  src/main/java/com/rpg/
    Main.java              solo abre la View
    view/                  HeroeUI (menu de heroes), JuegoUI (menu del simulador)
    controller/            HeroeController: validaciones, mensajes y consultas con Streams
    model/                 Heroe, el POJO de la tabla heroes
    dao/                   ConexionDB, HeroeDAO, DAOException (el unico paquete que usa java.sql)
    funcional/             CalculadoraPoder, ReglasHeroe, DemoLambdas, DemoStreams
    core/                  MotorJuego, Arena, TorneoManager, Bestiario, Consola, RegistroCombate...
    entities/              Personaje, Guerrero, Mago, Enemigo, Gremio, Escuadron, enums
    items/                 Inventario<T>, Pocion
    exceptions/            excepciones propias del juego
    concurrency/           temporizador de turno, efectos continuos, auto-guardado daemon
    persistencia/          GestorArchivos, Partida (archivos y serializacion)
```

---

## 6. Arquitectura

El registro de heroes esta hecho con **MVC + DAO**. Cada capa solo habla con la siguiente:

```
View (HeroeUI)  ->  Controller (HeroeController)  ->  DAO (HeroeDAO)  ->  MySQL
  consola             validaciones y Streams            JDBC                rpg_grupal_db
```

- **View:** solo lee del teclado e imprime. No tiene nada de SQL.
- **Controller:** valida los datos, decide que mensaje mostrar y procesa las listas con Stream API.
- **DAO:** es el unico que usa `java.sql`. Si algo falla convierte la `SQLException` en `DAOException`.
- **Conexion:** `ConexionDB` lee la url, el usuario y la clave de `config.txt`.

Este es el recorrido completo de la opcion "Insertar heroe":

```
 Usuario
   |  escribe: Thor, guerrero, 150, 3
   v
+-----------------------------+
| VIEW  -  HeroeUI            |   solo Scanner y System.out
| insertar()                  |   lee los datos y se los pasa al controller
+-----------------------------+
   |  controller.insertar("Thor", "guerrero", 150, 3)
   v
+-----------------------------+
| CONTROLLER - HeroeController|   valida: nombre no vacio, tipo guerrero/mago,
| insertar(...)               |   vida > 0, nivel >= 1
|                             |   si algo esta mal devuelve el mensaje y no llama al DAO
+-----------------------------+
   |  dao.existeNombre("Thor")         -> si ya existe: "Ya existe un heroe llamado Thor."
   |  dao.insertar(new Heroe(...))     (Heroe es el POJO del paquete model)
   v
+-----------------------------+
| DAO  -  HeroeDAO            |   unica capa con JDBC
| insertar(Heroe)             |   1. ConexionDB.getConnection()  (lee config.txt)
|                             |   2. con.prepareStatement("INSERT ... VALUES (?, ?, ?, ?)")
|                             |   3. setString / setInt en cada ?
|                             |   4. executeUpdate()
|                             |   5. try-with-resources cierra PreparedStatement y Connection
+-----------------------------+
   |  INSERT INTO heroes ...
   v
+-----------------------------+
| BD  -  MySQL                |   rpg_grupal_db.heroes
|                             |   UNIQUE(nombre), CHECK(tipo), CHECK(nivel >= 1)
+-----------------------------+

 Vuelta:
 BD -> DAO: 1 fila afectada (si falla, SQLException -> el DAO lanza DAOException)
 DAO -> Controller: termina bien o lanza DAOException
 Controller -> View: "Heroe Thor guardado en la base de datos."  o  "Error: ..."
 View -> Usuario: imprime el mensaje
```


---

## 7. Talleres y checklists

Resumen de lo que se trabajo en cada taller y donde esta en el codigo. Debajo estan los checklists
completos de cada guia, marcados.

| Taller | Temas | Donde esta |
|--------|-------|------------|
| 1 a 8 | Decisiones, bucles, `String`, POO, encapsulamiento, herencia, interfaces, genericos, relaciones entre clases, `Set`, `Map`, enums y excepciones | `core/`, `entities/`, `items/`, `exceptions/` |
| Concurrencia | `Thread`, `Runnable`, `interrupt`, `join`, hilo daemon, `synchronized` | `concurrency/`, `TorneoManager`, `Personaje` |
| Manejo de archivos | `File`, `BufferedWriter`, `BufferedReader`, serializacion, `try-with-resources` | `persistencia/` |
| JDBC y MVC | Conexion a MySQL, `PreparedStatement`, CRUD, DAO, MVC | `view/`, `controller/`, `model/`, `dao/`, `schema.sql` |
| Lambdas (6.1) | `Consumer`, `Supplier`, `Function`, `Predicate`, `@FunctionalInterface`, referencias a metodos | `funcional/`, `HeroeController`, `HeroeUI` |
| Stream API (6.2) | `filter`, `map`, `flatMap`, `sorted`, `anyMatch`, `toList`, `Collectors` | `HeroeController`, `funcional/DemoStreams` |

Los cambios de los ultimos talleres se pueden encontrar en el codigo buscando los comentarios
`TALLER JDBC`, `TALLER LAMBDAS` y `TALLER STREAMS`.

### Lo que ya debes tener (checklist)

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

### Punto de partida del taller de concurrencia

> Esta seccion describe como estaba el proyecto **antes** de los talleres de concurrencia y archivos.
> Hoy esos modulos ya estan hechos (ver los checklists marcados mas abajo).

#### Que NO tenia aun (taller de concurrencia)

Estos cuatro modulos van en el paquete `com.rpg.concurrency` (y en los puntos marcados del motor):

1. **Temporizador de turno**: hoy el jugador puede tardar lo que quiera en elegir su accion
   (`MotorJuego.pedirAccion` bloquea esperando el teclado).
2. **Efectos DoT / HoT con interrupcion**: hoy `Personaje.procesarEfectos` aplica los efectos turno a turno;
   no hay tareas independientes ni forma de cancelarlas a mitad de camino.
3. **Semifinales en paralelo con `join`**: `TorneoManager.jugarTorneo` simula una semifinal despues de otra.
4. **Auto-guardado daemon**: `MotorJuego.guardarProgreso` solo se invoca manualmente al terminar una pelea
   (y guarda en memoria; llevarlo a disco es parte del taller de archivos).

#### Que NO tenia aun (taller de archivos)

Estos modulos van en el paquete `com.rpg.persistencia` (y en los puntos marcados con `TALLER ARCHIVOS`):

1. **Log de combate en texto**: `RegistroCombate` solo acumula y muestra el texto; no se guarda ni se lee
   desde un archivo con `BufferedWriter` / `BufferedReader`.
2. **Inspeccion y borrado de archivos**: no hay uso de `File` (existe, tamano, ruta) ni borrado con
   `Files.deleteIfExists`.
3. **Guardar y cargar partida**: `MotorJuego.guardarProgreso` deja un `String` en memoria; falta guardar el
   estado con `Serializable` + `ObjectOutputStream` / `ObjectInputStream`, y una forma de cargarlo desde `Main`.
4. **Control de la serializacion**: `transient` para lo que no debe guardarse y `serialVersionUID` en cada clase.

#### Advertencia: condicion de carrera en la vida

`Personaje.recibirDanio` y `Personaje.curar` modifican la vida con operaciones de leer, calcular y escribir
que **no estan protegidas**. Hoy funcionan porque solo hay un hilo. Cuando varios hilos toquen la vida del
mismo personaje al mismo tiempo (por ejemplo un veneno y un ataque), se podran perder actualizaciones.
Parte del taller consiste en reproducir el problema y resolverlo.

El problema no es solo la vida: tambien comparten estado mutable el campo `estado` (`setVida` lo pasa a
`MUERTO`, y `curar` lo consulta con `estaVivo()`) y el mapa `efectos`, que `aplicarEfecto` y `quitarEfecto`
modifican mientras `procesarEfectos` lo recorre (puede lanzar `ConcurrentModificationException`). Cuando una
operacion toca varias variables dependientes, la unidad 4.2 recomienda `synchronized`.

### Checklist del taller: lo que debes realizar

Marca cada punto cuando lo tengas funcionando. El orden sigue las secciones de la guia del taller.
Las clases nuevas van en `com.rpg.concurrency` (y `TurnoCallback` en `com.rpg.core`).
Mas abajo hay un checklist aparte para el taller de archivos.

#### Antes de empezar

- [x] El proyecto compila y ejecutas `Main` sin errores.
- [x] Jugaste un combate rapido y un torneo completo, y localizaste los 5 comentarios `TALLER CONCURRENCIA`.
- [x] Repasaste la tabla "Lo que ya debes tener": si algun concepto no lo reconoces, repasalo antes de seguir.

#### Seccion 1: Temporizador de turno (`Thread`, `Runnable`, `sleep`)

- [x] Crear la interfaz `TurnoCallback` con el metodo `void onTiempoAgotado()`.
- [x] Crear `TemporizadorTurnoTask` que implemente `Runnable` y reciba por constructor la duracion en segundos y el `TurnoCallback`.
- [x] En `run()`, hacer una cuenta regresiva con un `for` o `while` decreciente, un segundo por iteracion.
- [x] Usar `Thread.sleep(1000)` dentro de un `try-catch` que capture `InterruptedException`.
- [x] Al llegar a 0, llamar a `callback.onTiempoAgotado()`.
- [x] Crear `GestorTurno`, que cree el `Thread` con el `Runnable` (o con una lambda) y llame a `.start()`.
- [x] Integrarlo en `MotorJuego.pedirAccion`: si el tiempo se agota antes de que el jugador elija, el turno se pierde.
- [x] Si el jugador responde a tiempo, cancelar el temporizador con `.interrupt()` (sin banderas booleanas manuales).

#### Seccion 2: Efectos continuos DoT / HoT (`interrupt`, `InterruptedException`)

- [x] Crear `EfectoContinuoTask` (`Runnable`) con nombre del efecto e intervalo en milisegundos.
- [x] En `run()`, usar `while (!Thread.currentThread().isInterrupted())` y aplicar el dano o la curacion sobre el `Personaje`.
- [x] En el `catch (InterruptedException e)`: limpiar el estado del personaje, llamar a `Thread.currentThread().interrupt()` y salir del bucle con `break`.
- [x] Cada efecto corre en su propio hilo; no se usa `Thread.stop()` ni banderas booleanas.
- [x] Reemplazar la logica por turnos de `Personaje.procesarEfectos` por hilos que se cancelan con `.interrupt()`.
- [x] Al usar un antidoto, al recibir una purificacion o al terminar el combate, el hilo del efecto se interrumpe y termina limpio.

#### Seccion 3: Torneo en paralelo (`join`)

- [x] En `TorneoManager.jugarTorneo`, crear dos `Thread`, uno por semifinal, cada uno con su `Runnable`.
- [x] Llamar a `.start()` en ambos para que corran al mismo tiempo.
- [x] En el hilo principal, llamar a `.join()` sobre la semifinal A y luego sobre la B.
- [x] Proteger los `join()` con `try-catch` de `InterruptedException`.
- [x] Consultar los ganadores **solo despues** de que ambos `join()` hayan retornado, y entonces lanzar la final.
- [x] Imprimir `Thread.getState()` de cada semifinal antes de `start()`, durante la ejecucion y despues del `join()`, y relacionar lo que ves con el ciclo de vida del hilo (`NEW`, `RUNNABLE`, `TIMED_WAITING`, `TERMINATED`).

#### Seccion 4: Auto-guardado en segundo plano (hilo daemon)

- [x] Crear `AutoGuardadoDaemon` con un bucle `while (true)` y `Thread.sleep(intervalo)` entre guardados.
- [x] Hacer que el guardado registre periodicamente las estadisticas de los personajes y el historial de la partida **en memoria** (por ejemplo, un `String` o una lista con la ultima copia). No uses archivos todavia.
- [x] Llamar a `setDaemon(true)` **antes** de `.start()` (si no, `IllegalThreadStateException`).
- [x] Comprobar que la aplicacion cierra de inmediato al salir, sin esperar al hilo de guardado.

#### Seccion 5: Reto integrador

- [x] Los cuatro modulos funcionan juntos: temporizador de turno, efectos DoT/HoT, torneo con `join` y auto-guardado daemon.
- [x] Cumples la estructura esperada: `com.rpg.core` (`MotorJuego`, `TorneoManager`, `TurnoCallback`), `com.rpg.entities` (`Personaje`, `EstadoEfecto`), `com.rpg.concurrency` (`TemporizadorTurnoTask`, `EfectoContinuoTask`, `AutoGuardadoDaemon`) y `Main`.
- [x] El combate y el torneo se juegan completos, sin bloqueos ni excepciones sin controlar.

#### Seccion 6: Depuracion y condiciones de carrera

- [x] Reproduciste la condicion de carrera: dos efectos DoT (o un efecto y un ataque) sobre el mismo `Personaje` pierden actualizaciones de vida.
- [x] La corrigiste en `Personaje.recibirDanio` y `curar` con `synchronized` (mecanismo principal, visto en la unidad 4.2), y puedes explicar por que lo elegiste.
- [x] Protegiste tambien el acceso a `estado` y `efectos` (ver la advertencia de arriba).
- [x] Puedes explicar por que `AtomicInteger.addAndGet(-danio)` no basta aqui: `setVida` acota la vida a `0..vidaMaxima` y cambia `estado`. (Reto extra: resolverlo con `updateAndGet` o un bucle con `compareAndSet`.)
- [x] Ningun `catch (InterruptedException)` esta vacio ni solo imprime la traza.
- [x] Nunca llamas a `.start()` dos veces sobre el mismo `Thread`: para cada turno o combate se crea un `Thread` nuevo.
- [x] Nunca llamas a `run()` directamente: usas siempre `start()`, que es lo que crea el hilo nuevo.
- [x] Todos los `Thread.sleep()` estan dentro de `try-catch` que gestiona `InterruptedException`.
- [x] Todo hilo de auto-guardado o monitoreo usa `setDaemon(true)` antes de `.start()` (ver "Investigacion obligatoria").
- [x] El `join()` del torneo espera a ambas semifinales antes de leer la memoria compartida de los ganadores.

### Checklist del taller de archivos: lo que debes realizar

Las clases nuevas van en `com.rpg.persistencia`. Los puntos de extension estan marcados con `TALLER ARCHIVOS`
en `MotorJuego`, `RegistroCombate`, `Main` y las clases del dominio.

#### Antes de empezar

- [x] El proyecto compila y ejecutas `Main` sin errores.
- [x] Localizaste los comentarios `TALLER ARCHIVOS` y entiendes que el guardado actual es solo en memoria.

#### Seccion 1: Clase `File`

- [x] Crear un `File` con una ruta relativa y otro con una ruta absoluta (`getAbsolutePath()`), y explicar la diferencia.
- [x] Usar `exists()`, `length()` y `getName()` para mostrar informacion de un archivo de partida o de log.
- [x] Usar `createNewFile()` y manejar su `IOException`.
- [x] Crear la carpeta `partidas/` si no existe (`mkdirs()`), sin fallar si ya existe.

#### Seccion 2: Escritura (`BufferedWriter` + `FileWriter`)

- [x] Volcar el log de `RegistroCombate` a un `.txt` con `BufferedWriter` + `FileWriter`.
- [x] Escribir con `newLine()` en vez de concatenar saltos de linea a mano.
- [x] Usar `FileWriter(file, true)` para **agregar** (append) sin borrar los combates anteriores, y comprobar la diferencia con el modo por defecto, que sobrescribe.
- [x] Hacer `flush()` / cerrar el escritor para que el contenido llegue al disco.

#### Seccion 3: Lectura (`BufferedReader` + `FileReader`)

- [x] Leer el log con `BufferedReader` + `FileReader` usando `while ((linea = lector.readLine()) != null)`.
- [x] Mostrar por consola el historial leido (por ejemplo, una opcion nueva en el menu de `Main`).
- [x] Capturar `FileNotFoundException` cuando el archivo aun no existe y mostrar un mensaje claro (sin tildes).

#### Seccion 4: Borrado

- [x] Borrar un archivo con `File.delete()` y revisar el `boolean` que devuelve.
- [x] Borrar con `Files.deleteIfExists(Path)` y explicar la diferencia con `File.delete()`.
- [x] Ofrecer en el menu una opcion para borrar la partida guardada, confirmando antes con el jugador.

#### Seccion 5: Serializacion (`Serializable`, `ObjectOutputStream`, `ObjectInputStream`)

- [x] Hacer `Serializable` las clases del dominio que forman la partida: `Personaje` (y `Guerrero`, `Mago`, `Enemigo`), `Inventario`, `Pocion`, `Arma`, `Gremio`, `EstadoEfecto`, `PerfilJugador`.
- [x] Declarar `serialVersionUID` en cada clase serializable y explicar para que sirve.
- [x] Marcar con `transient` al menos un campo que no deba guardarse y comprobar que vuelve con su valor por defecto al cargar.
- [x] Guardar la partida con `ObjectOutputStream.writeObject(...)` en un `.dat`.
- [x] Cargar la partida con `ObjectInputStream.readObject()`, con el cast al tipo correcto.
- [x] Provocar y resolver una `NotSerializableException` por un objeto anidado que no es `Serializable`.
- [x] Comprobar que despues de cargar, el `Gremio` y sus `Personaje` siguen relacionados (el ciclo `Personaje` - `Gremio` se serializa bien).
- [x] Agregar una opcion "Cargar partida" y comprobar que el juego continua con el estado guardado.

#### Seccion 6: `try-with-resources` y excepciones

- [x] Todos los flujos (`BufferedWriter`, `BufferedReader`, `ObjectOutputStream`, `ObjectInputStream`) se abren con `try-with-resources` (`AutoCloseable`); ningun `close()` manual.
- [x] Manejas `IOException`, `FileNotFoundException` (la mas especifica va primero), `ClassNotFoundException` y `NotSerializableException` con mensajes utiles.
- [x] Ningun `catch` esta vacio ni solo imprime la traza.
- [x] El juego no se cae si el archivo no existe, esta corrupto o no se puede escribir.

#### Seccion 7: Integracion con concurrencia (si ya hiciste el taller anterior)

- [x] `AutoGuardadoDaemon` escribe a disco (ya no solo en memoria) cada cierto intervalo.
- [x] Protegiste la escritura y la lectura del mismo archivo con `synchronized` (sobre un mismo objeto candado o metodos de una clase compartida), para que el daemon y el hilo principal no lo accedan a la vez.
- [x] Comprobaste que, al cerrar el juego, el hilo daemon no deja un archivo a medio escribir.

### Investigacion obligatoria

Dos temas de la guia no se explican en las presentaciones de las unidades 4.1 y 4.2. Debes investigarlos
por tu cuenta y dejar evidencia en el proyecto:

- [x] **Hilos daemon.** Demuestra con una prueba que la JVM cierra aunque el hilo daemon siga vivo, y que
  `setDaemon(true)` despues de `start()` lanza `IllegalThreadStateException`. Anota en un comentario la
  diferencia entre hilo de usuario y hilo daemon.
- [x] **`ReentrantLock`.** Reescribe `Personaje.recibirDanio` con `lock()` / `unlock()` dentro de `try-finally`
  y explica en 2 o 3 lineas cuando lo preferirias a `synchronized`. Si el `unlock()` no esta en el `finally`,
  el lock puede quedar tomado para siempre.

### Investigacion obligatoria (taller de archivos)

- [x] **`FileWriter` vs `Files.writeString`.** Explica en 2 o 3 lineas la diferencia (flujo con buffer que
  abres y cierras tu, frente a una sola llamada de NIO que escribe todo el texto) y cuando usarias cada uno.
- [x] **Seguridad de `Serializable`.** Explica por que no se debe deserializar (`readObject`) datos que no
  son de confianza: un archivo manipulado puede ejecutar codigo o consumir recursos al reconstruir objetos.
  Anota que alternativas existen (por ejemplo JSON o un formato de texto propio).

### Adaptaciones respecto a la guia

Este proyecto sigue la guia del taller, con estas diferencias a proposito:

- **Auto-guardado en memoria.** La guia habla de guardar "en el almacenamiento". En el taller de concurrencia
  el guardado es en memoria (un `String` o una lista con la ultima copia): ese es el estado de la partida.
  El taller de archivos lo lleva a disco (ver la seccion 7 del checklist de archivos).
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

### Taller JDBC y MVC: Registro de Heroes (ejercicio grupal)

Los heroes se guardan en MySQL (`rpg_grupal_db`, tabla `heroes`) y se manejan desde un menu MVC + DAO.

El juego arranca en el menu del registro de heroes. La opcion 6 abre el simulador de batalla y ahi la
opcion 9 "Cargar heroe desde BD" crea un `Guerrero` o `Mago` con los datos de la tabla y lo usa en un
combate rapido. El diagrama del flujo View -> Controller -> DAO -> BD esta en la seccion [Arquitectura](#6-arquitectura).

```
com.rpg
  Main                 solo hace new HeroeUI(...).iniciar()
  view/                HeroeUI (menu de heroes), JuegoUI (menu del simulador, antes estaba en Main)
  controller/          HeroeController (validaciones y mensajes)
  model/               Heroe (POJO)
  dao/                 ConexionDB, HeroeDAO, DAOException (lo unico que usa java.sql)
```

### Taller Lambdas y Stream API (Unidad 6.1 y 6.2)

Las lambdas y los streams se usan dentro del mismo juego, sobre los heroes que vienen de MySQL.
Todo lo nuevo se encuentra buscando los comentarios `TALLER LAMBDAS` y `TALLER STREAMS`.

En el menu del registro de heroes hay tres opciones nuevas:

- **7. Estadisticas de heroes:** total, guerreros y magos (`filter` + `count`), vida promedio
  (`mapToInt` + `average`), el mas poderoso (`max`), veteranos en mayuscula (`filter` + `map` + `toList`),
  si hay alguna leyenda (`anyMatch`) y todos en una linea (`Collectors.joining`).
- **8. Ranking por poder:** `sorted` con `Comparator.comparingInt(...).reversed()` y `map` a texto.
- **9. Filtrar heroes:** cada opcion arma un `Predicate<Heroe>` (`and`, `negate` o uno creado con el texto
  que escribe el usuario) y el Controller solo hace `filter(condicion)`.

Ademas hay dos demos que se ejecutan solas y **no necesitan la base de datos**, una por guia:
`funcional/DemoLambdas` y `funcional/DemoStreams`.

```
com.rpg.funcional
  CalculadoraPoder     interfaz funcional propia (@FunctionalInterface, un metodo + uno default)
  ReglasHeroe          Predicate, Function y CalculadoraPoder del registro de heroes
  DemoLambdas          guia 6.1 paso a paso
  DemoStreams          guia 6.2 paso a paso
```

#### Checklist Unidad 6.1: Expresiones Lambda

- [x] Clase anonima vs lambda (`DemoLambdas.claseAnonimaVsLambda`)
- [x] Partes de una lambda: parametros, `->` y cuerpo (todas las de `ReglasHeroe`)
- [x] `Consumer<T>` con `accept` / `forEach` (`HeroeUI.imprimirHeroe`, `DemoLambdas`)
- [x] `Supplier<T>` con `get` (`HeroeController.fechaActual`, rival aleatorio en `DemoLambdas`)
- [x] `Function<T, R>` con `apply` (`ReglasHeroe.FICHA_CORTA`, `HeroeController.FABRICA_PERSONAJES`)
- [x] `Predicate<T>` con `test`, `and` y `negate` (`ReglasHeroe`, `HeroeUI.filtrar`)
- [x] Las cuatro juntas en un pipeline `Stream.generate -> filter -> map -> forEach` (`DemoLambdas.pipeline`)
- [x] Interfaz propia con `@FunctionalInterface` (`CalculadoraPoder`), usada en el ranking y al cargar
      un heroe en combate: ahora el nivel le suma vida (`ReglasHeroe.VIDA_EN_COMBATE`)
- [x] Referencias a metodo y a constructor (`Heroe::getNombre`, `System.out::println`, `Guerrero::new`)
- [x] `TurnoCallback` marcada con `@FunctionalInterface`

#### Checklist Unidad 6.2: Stream API

- [x] Imperativo (`for`) vs declarativo (`stream`) y la lista original no cambia (`DemoStreams`)
- [x] Origenes: `Stream.of`, `Arrays.stream`, `Stream.builder`, `lista.stream()` (`DemoStreams.origenes`)
- [x] `filter` (`HeroeController.filtrar`, `Escuadron.contarVivos`, `Bestiario.crearEnemigo`)
- [x] `map` (`HeroeController.estadisticas`, `rankingPorPoder`)
- [x] `flatMap` con habilidades e inventarios de un escuadron (`DemoStreams.flatMap`, `Inventario.stream()`)
- [x] `anyMatch` y cortocircuito (`HeroeController.estadisticas`, `DemoStreams`)
- [x] Evaluacion perezosa: sin operacion terminal no se ejecuta nada (`DemoStreams.evaluacionPerezosa`)
- [x] De vuelta a una lista: `toList()` y `collect(Collectors.toList())`
- [x] Un stream no se reutiliza: `IllegalStateException` (`DemoStreams.streamYaConsumido`)
- [x] Caso real: origen -> filtra -> extrae -> guarda en lista, con datos de MySQL
- [x] Ciclos `for` del juego pasados a streams o `forEach` (`Escuadron`, `Bestiario`, `MotorJuego`, las View)

Nota: no todos los `for` se cambiaron. En `MotorJuego.entregarKitInicial` el `agregar` lanza una excepcion
verificada (`CapacidadExcedidaException`) y una lambda de `forEach` no la deja pasar, y en el ranking se
necesita el numero del puesto. Ahi el `for` normal es mas claro.

---

## 8. Problemas comunes

| Mensaje | Que hacer |
|---------|-----------|
| `No se encontro config.txt en ...` | Falta crear `config.txt` al lado del `pom.xml` (paso 3.4). El mensaje dice en que carpeta lo busco. |
| `Access denied for user 'root'` | La clave de `config.txt` no es la de root. |
| `Communications link failure` | MySQL esta apagado o no usa el puerto 3306. En Windows: `Win + R` -> `services.msc` -> iniciar **MySQL84**. |
| `Public Key Retrieval is not allowed` | Falta `?allowPublicKeyRetrieval=true&useSSL=false` al final de la url (en DBeaver, la propiedad del paso 3.3). |
| `Unknown database 'rpg_grupal_db'` | Falta ejecutar `schema.sql` (paso 3.3). |
| `No suitable driver found` | Maven no cargo las dependencias: clic derecho en `pom.xml` -> **Maven -> Reload project**. |
| Errores en `.toList()` o en `switch` con `->` | El proyecto no esta usando JDK 21 (paso 3.5). |

---

## 9. Notas

- Los mensajes de la consola no llevan tildes a proposito, para que se vean bien en cualquier terminal.
- El azar del combate (criticos, habilidades de los enemigos) cambia en cada ejecucion, por eso dos
  combates iguales pueden terminar distinto.
- Si se quiere usar MySQL en Docker en vez de instalado, el codigo no cambia: solo la url, el usuario y la
  clave de `config.txt`.

---

## 10. Autores

- _Nombre del integrante_
- _Nombre del integrante_

Repositorio: <https://github.com/ARSCIAND/rpg-base>
