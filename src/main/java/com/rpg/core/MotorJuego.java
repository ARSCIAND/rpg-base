package com.rpg.core;

import com.rpg.concurrency.GestorTurno;
import com.rpg.entities.Enemigo;
import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Gremio;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.PerfilJugador;
import com.rpg.entities.Personaje;
import com.rpg.entities.enums.RaridadItem;
import com.rpg.entities.enums.TipoEfecto;
import com.rpg.exceptions.CapacidadExcedidaException;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;
import com.rpg.persistencia.GestorArchivos;
import com.rpg.persistencia.Partida;

import java.util.List;
import java.util.Random;

/** Coordina el flujo del juego: crear heroe, combatir, torneo, bestiario y guardado. */
public class MotorJuego implements TurnoCallback {

    private static final String NOMBRE_GREMIO = "Guardianes del Alba";
    private static final int CURACION_POCION_VIDA = 30;
    private static final int SEGUNDOS_TURNO = 20;

    private final Consola consola;
    private final RegistroCombate registro = new RegistroCombate();
    private final Arena arena = new Arena(registro, new Random());
    private final Bestiario bestiario = new Bestiario();
    private final TorneoManager torneo = new TorneoManager(arena, registro);
    private final GestorTurno gestorTurno = new GestorTurno();
    private final GestorArchivos archivos = new GestorArchivos();
    private PerfilJugador perfil = new PerfilJugador();
    private Gremio gremio = new Gremio(NOMBRE_GREMIO);
    private String ultimoGuardado = "";

    // la cambia el hilo del temporizador; volatile para que el hilo principal vea el cambio
    private volatile boolean tiempoAgotado;

    public MotorJuego(Consola consola) {
        this.consola = consola;
        archivos.crearCarpeta();
    }

    // ---- opciones del menu principal

    public void combateRapido() {
        registro.limpiar();
        Personaje jugador = crearJugador();
        Enemigo enemigo = elegirEnemigo();
        registro.agregar("%n=== %s vs %s ===", jugador.getNombre(), enemigo.getNombre());

        boolean huyo = combatir(jugador, enemigo);

        // al terminar el combate se detienen los hilos de los efectos
        jugador.detenerEfectos();
        enemigo.detenerEfectos();

        cerrarCombate(jugador, enemigo, huyo);
        archivos.agregarAlHistorial(registro.getLineas());
        guardarProgreso(false);
    }

    public void jugarTorneo() {
        registro.limpiar();
        Personaje campeon = torneo.jugarTorneo();
        registro.agregar("%nCAMPEON DEL TORNEO: %s", campeon.getNombre());
        perfil.desbloquearLogro("Campeon del Torneo");
        archivos.agregarAlHistorial(registro.getLineas());
        guardarProgreso(false);
    }

    public void mostrarBestiario() {
        System.out.println("\n=== BESTIARIO ===");
        System.out.print(bestiario.describir());
        System.out.printf("Victorias: %d | Derrotas: %d | Logros: %s%n",
                perfil.getVictorias(), perfil.getDerrotas(), perfil.getLogros());
    }

    // ---- creacion de personajes

    private Personaje crearJugador() {
        List<Personaje> heroes = gremio.getMiembros();
        int clase;
        if (heroes.isEmpty()) {
            clase = consola.leerEntero("Clase (1. Guerrero, 2. Mago): ", 1, 2);
        } else {
            clase = consola.leerEntero("Clase (1. Guerrero, 2. Mago, 3. Heroe del gremio): ", 1, 3);
        }
        if (clase == 3) {
            return elegirHeroe(heroes);
        }
        String nombre = consola.leerNombre("Nombre del heroe: ");
        Personaje jugador = switch (clase) {
            case 1 -> new Guerrero(nombre);
            default -> new Mago(nombre);
        };
        entregarKitInicial(jugador);
        jugador.unirseAGremio(gremio);
        System.out.printf("%s se une al gremio %s (%d miembros).%n",
                jugador.getNombre(), gremio.getNombre(), gremio.getMiembros().size());
        return jugador;
    }

    // para seguir jugando con un heroe que ya existe (por ejemplo de una partida cargada)
    private Personaje elegirHeroe(List<Personaje> heroes) {
        for (int i = 0; i < heroes.size(); i++) {
            System.out.printf("  %d. %s (%s)%n", i + 1, heroes.get(i).getNombre(), heroes.get(i).getTipo());
        }
        Personaje heroe = heroes.get(consola.leerEntero("Heroe: ", 1, heroes.size()) - 1);
        heroe.restaurarVida();
        return heroe;
    }

    private void entregarKitInicial(Personaje jugador) {
        List<Pocion> kit = List.of(
                new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN),
                new Pocion("Elixir de Regeneracion", 0, RaridadItem.RARO,
                        new EstadoEfecto("Regeneracion", TipoEfecto.CURACION_CONTINUA, 6, 3), false),
                new Pocion("Antidoto", 0, RaridadItem.COMUN, null, true));
        try {
            for (Pocion pocion : kit) {
                jugador.getInventario().agregar(pocion);
            }
        } catch (CapacidadExcedidaException e) {
            System.out.println("No se pudo entregar todo el kit: " + e.getMessage());
        }
    }

    private Enemigo elegirEnemigo() {
        List<String> nombres = bestiario.nombres();
        System.out.println("Rivales disponibles:");
        for (int i = 0; i < nombres.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, nombres.get(i));
        }
        while (true) {
            String texto = consola.leerLinea("Rival (numero o nombre): ");
            Enemigo enemigo = resolverRival(texto, nombres);
            if (enemigo != null) {
                return enemigo;
            }
            System.out.println("No existe ese rival.");
        }
    }

    private Enemigo resolverRival(String texto, List<String> nombres) {
        try {
            int numero = Integer.parseInt(texto);
            boolean valido = numero >= 1 && numero <= nombres.size();
            return valido ? bestiario.crearEnemigo(nombres.get(numero - 1)) : null;
        } catch (NumberFormatException e) {
            return bestiario.crearEnemigo(texto);
        }
    }

    // ---- bucle de combate

    /** @return true si el jugador huyo */
    private boolean combatir(Personaje jugador, Enemigo enemigo) {
        int ronda = 1;
        while (jugador.estaVivo() && enemigo.estaVivo()) {
            registro.agregar("%n-- Ronda %d --", ronda);
            if (arena.iniciarTurno(jugador) && turnoJugador(jugador, enemigo)) {
                return true;
            }
            arena.turnoAutomatico(enemigo, jugador);
            ronda++;
        }
        return false;
    }

    private boolean turnoJugador(Personaje jugador, Enemigo enemigo) {
        boolean turnoGastado;
        do {
            registro.agregar(FormatoCombate.resumen(jugador));
            registro.agregar(FormatoCombate.resumen(enemigo));
            Accion accion = pedirAccion();
            // mientras el jugador pensaba un veneno pudo haber matado a alguno
            if (!jugador.estaVivo() || !enemigo.estaVivo()) {
                return false;
            }
            if (accion == Accion.HUIR) {
                return true;
            }
            turnoGastado = switch (accion) {
                case ATACAR -> {
                    arena.atacar(jugador, enemigo);
                    yield true;
                }
                case ESPECIAL -> arena.intentarEspecial(jugador, enemigo);
                case POCION -> usarPocion(jugador);
                case TIEMPO_AGOTADO -> {
                    registro.agregar("%s se demoro mucho y pierde el turno.", jugador.getNombre());
                    yield true;
                }
                default -> {
                    mostrarEstado(jugador);
                    yield false;
                }
            };
        } while (!turnoGastado && enemigo.estaVivo());
        return false;
    }

    // Se inicia el temporizador y se espera la respuesta. El Scanner no se puede interrumpir,
    // entonces si el tiempo se acaba el turno se pierde aunque despues escriba algo.
    private Accion pedirAccion() {
        System.out.println("1. Atacar  2. Habilidad especial  3. Usar pocion  4. Ver estado  0. Huir  (" + SEGUNDOS_TURNO + " seg)");
        tiempoAgotado = false;
        gestorTurno.iniciar(SEGUNDOS_TURNO, this);
        try {
            while (true) {
                String texto = consola.leerLinea("Accion: ");
                if (tiempoAgotado) {
                    return Accion.TIEMPO_AGOTADO;
                }
                Integer opcion = consola.convertirEntero(texto, 0, 4);
                if (opcion != null) {
                    return switch (opcion) {
                        case 1 -> Accion.ATACAR;
                        case 2 -> Accion.ESPECIAL;
                        case 3 -> Accion.POCION;
                        case 4 -> Accion.ESTADO;
                        default -> Accion.HUIR;
                    };
                }
            }
        } finally {
            // si respondio a tiempo se cancela el temporizador
            gestorTurno.cancelar();
        }
    }

    @Override
    public void onTiempoAgotado() {
        tiempoAgotado = true;
        System.out.println("\nSe acabo el tiempo! Presiona Enter para continuar.");
    }

    private boolean usarPocion(Personaje jugador) {
        Inventario<Pocion> inventario = jugador.getInventario();
        if (inventario.estaVacio()) {
            System.out.println("No tienes pociones.");
            return false;
        }
        int numero = 1;
        for (Pocion pocion : inventario) {
            System.out.printf("  %d. %s%n", numero++, pocion.getNombre());
        }
        int eleccion = consola.leerEntero("Pocion (0 = cancelar): ", 0, inventario.tamanio());
        if (eleccion == 0) {
            return false;
        }
        registro.agregar(jugador.usarPocion(inventario.quitar(eleccion - 1)));
        return true;
    }

    private void mostrarEstado(Personaje jugador) {
        System.out.printf("Habilidades: %s | Arma: %s | Defensa: %d | Pociones: %d%n",
                jugador.getHabilidades(),
                jugador.getArma() != null ? jugador.getArma().getNombre() : "ninguna",
                jugador.getDefensa(), jugador.getInventario().tamanio());
        for (EstadoEfecto efecto : jugador.getEfectos()) {
            System.out.printf("Efecto activo: %s (%d ticks)%n", efecto.getNombre(), efecto.getTurnosRestantes());
        }
    }

    // ---- resultado del combate

    private void cerrarCombate(Personaje jugador, Enemigo enemigo, boolean huyo) {
        if (huyo) {
            registro.agregar("%s huye del combate.", jugador.getNombre());
        } else if (jugador.estaVivo()) {
            registro.agregar("%nVictoria de %s!", jugador.getNombre());
            perfil.registrarVictoria();
            bestiario.registrarEnemigoDerrotado(enemigo.getNombre());
            desbloquearLogros(jugador, enemigo);
            repartirBotin(jugador);
        } else {
            registro.agregar("%n%s ha caido ante %s.", jugador.getNombre(), enemigo.getNombre());
            perfil.registrarDerrota();
        }
    }

    private void desbloquearLogros(Personaje jugador, Enemigo enemigo) {
        boolean intacto = jugador.getVida() == jugador.getVidaMaxima();
        List<String> candidatos = List.of("Primera Victoria", "Cazador de " + enemigo.getNombre(),
                intacto ? "Intocable" : "");
        for (String logro : candidatos) {
            if (!logro.isEmpty() && perfil.desbloquearLogro(logro)) {
                registro.agregar("Logro desbloqueado: %s", logro);
            }
        }
    }

    private void repartirBotin(Personaje jugador) {
        try {
            jugador.getInventario().agregar(new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN));
            registro.agregar("Botin: Pocion de Vida.");
        } catch (CapacidadExcedidaException e) {
            registro.agregar("Botin perdido: %s", e.getMessage());
        }
    }

    // ---- guardado

    // Lo llama el hilo principal al terminar una pelea y el hilo daemon cada cierto tiempo.
    // synchronized para que no se guarden dos al mismo tiempo.
    public synchronized void guardarProgreso(boolean automatico) {
        StringBuilder contenido = new StringBuilder();
        contenido.append("=== PARTIDA RPG ===\n");
        contenido.append("Victorias: ").append(perfil.getVictorias()).append("\n");
        contenido.append("Derrotas: ").append(perfil.getDerrotas()).append("\n");
        contenido.append("Logros: ").append(String.join(", ", perfil.getLogros())).append("\n");
        contenido.append("Gremio ").append(gremio.getNombre()).append(": ")
                .append(gremio.getMiembros().size()).append(" miembros\n");
        contenido.append("Enemigos derrotados: ").append(bestiario.resumenDerrotados()).append("\n");
        contenido.append("\n--- Ultimo combate ---\n");
        contenido.append(registro.getTexto());
        ultimoGuardado = contenido.toString();
        archivos.escribirProgreso(ultimoGuardado);
        if (!automatico) {
            System.out.println("[Progreso guardado en partidas/progreso.txt]");
        }
    }

    public String getUltimoGuardado() {
        return ultimoGuardado;
    }

    public void guardarPartida() {
        Partida partida = new Partida(perfil, gremio, bestiario.getDerrotados());
        if (archivos.guardarPartida(partida)) {
            System.out.println("Partida guardada en partidas/partida.dat");
        }
    }

    public synchronized void cargarPartida() {
        Partida partida = archivos.cargarPartida();
        if (partida == null) {
            return;
        }
        perfil = partida.getPerfil();
        gremio = partida.getGremio();
        bestiario.cargarDerrotados(partida.getDerrotados());
        System.out.println("Partida cargada (guardada el " + partida.getFecha() + ")");
        System.out.println("Victorias: " + perfil.getVictorias() + " | Logros: " + perfil.getLogros());
        System.out.println("Campo transient despues de cargar: " + partida.getNota());
        // comprobar que los personajes siguen apuntando al mismo gremio
        for (Personaje heroe : gremio.getMiembros()) {
            System.out.println("  " + heroe.getNombre() + " sigue en el gremio: " + (heroe.getGremio() == gremio));
        }
    }

    public void mostrarHistorial() {
        List<String> lineas = archivos.leerHistorial();
        if (lineas.isEmpty()) {
            System.out.println("El historial esta vacio.");
            return;
        }
        System.out.println("\n=== HISTORIAL DE COMBATES ===");
        for (String linea : lineas) {
            System.out.println(linea);
        }
    }

    public void mostrarArchivos() {
        archivos.mostrarInfo();
    }

    public void borrarPartida() {
        if (!archivos.existePartida()) {
            System.out.println("No hay partida guardada.");
            return;
        }
        if (consola.confirmar("Seguro que quieres borrar la partida?")) {
            if (archivos.borrarPartida()) {
                System.out.println("Partida borrada.");
            }
        }
        if (consola.confirmar("Borrar tambien el historial?")) {
            if (archivos.borrarHistorial()) {
                System.out.println("Historial borrado.");
            } else {
                System.out.println("No se pudo borrar el historial.");
            }
        }
    }
}
