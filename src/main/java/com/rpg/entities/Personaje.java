package com.rpg.entities;

import com.rpg.concurrency.EfectoContinuoTask;
import com.rpg.entities.enums.EstadoPersonaje;
import com.rpg.entities.enums.TipoPersonaje;
import com.rpg.exceptions.SinManaException;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Los metodos que tocan vida, estado o efectos son synchronized porque ahora los usan
// varios hilos a la vez (el principal y los hilos de los efectos).
public abstract class Personaje implements ICombatiente, Serializable {

    private static final long serialVersionUID = 1L;

    private static final int VIDA_BASE = 100;
    private static final int ATAQUE_BASE = 10;
    private static final int DEFENSA_BASE = 5;
    private static final int CAPACIDAD_INVENTARIO = 3;
    private static final int DANIO_MINIMO = 1;
    private static final double PORCENTAJE_TOTAL = 100.0;
    private static final int INTERVALO_EFECTO_MS = 1200;

    private String nombre;
    private final TipoPersonaje tipo;
    private int vidaMaxima;
    private int vida;
    private final int ataqueBase;
    private final int defensaBase;
    private EstadoPersonaje estado = EstadoPersonaje.VIVO;

    private Arma arma;                  // asociacion 0..1
    private Gremio gremio;              // asociacion bidireccional 0..1
    private final Inventario<Pocion> inventario = new Inventario<>(CAPACIDAD_INVENTARIO); // composicion
    private final Set<String> habilidades = new HashSet<>();
    private final Map<String, EstadoEfecto> efectos = new LinkedHashMap<>();

    // transient porque Thread no es Serializable (sin esto sale NotSerializableException)
    private transient Map<String, Thread> hilosEfectos = new HashMap<>();

    protected Personaje(String nombre, TipoPersonaje tipo) {
        this(nombre, tipo, VIDA_BASE, ATAQUE_BASE, DEFENSA_BASE);
    }

    protected Personaje(String nombre, TipoPersonaje tipo, int vidaMaxima, int ataqueBase, int defensaBase) {
        validarNombre(nombre);
        if (vidaMaxima <= 0 || ataqueBase < 0 || defensaBase < 0) {
            throw new IllegalArgumentException("Estadisticas invalidas para " + nombre);
        }
        this.nombre = nombre;
        this.tipo = tipo;
        this.vidaMaxima = vidaMaxima;
        this.vida = vidaMaxima;
        this.ataqueBase = ataqueBase;
        this.defensaBase = defensaBase;
    }

    private static void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
    }

    // ---- combate

    public abstract String realizarAtaqueEspecial(Personaje objetivo) throws SinManaException;

    public void alIniciarTurno() {
    }

    public int calcularDanio() {
        double bruto = ataqueBase + tipo.getBonificadorAtaque();
        if (arma != null) {
            bruto += arma.getDanioEfectivo();
        }
        return (int) Math.round(bruto);
    }

    @Override
    public void defender(int danoRecibido) {
        double factorMitigacion = PORCENTAJE_TOTAL / (PORCENTAJE_TOTAL + getDefensa());
        int danioFinal = (int) Math.round(danoRecibido * factorMitigacion);
        recibirDanio(Math.max(DANIO_MINIMO, danioFinal));
    }

    // ---- vida

    // Use synchronized y no AtomicInteger porque aqui cambian juntas la vida y el estado
    // (setVida pone MUERTO si llega a 0). AtomicInteger solo protege el numero.
    public synchronized void recibirDanio(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("El danio no puede ser negativo");
        }
        setVida(vida - cantidad);
    }

    public synchronized int curar(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La curacion no puede ser negativa");
        }
        if (!estaVivo()) {
            return 0;
        }
        int vidaAntes = vida;
        setVida(vida + cantidad);
        return vida - vidaAntes;
    }

    public synchronized void restaurarVida() {
        detenerEfectos();
        estado = EstadoPersonaje.VIVO;
        vida = vidaMaxima;
    }

    @Override
    public synchronized boolean estaVivo() {
        return estado != EstadoPersonaje.MUERTO;
    }

    // ---- efectos DoT / HoT (cada efecto corre en su propio hilo)

    public synchronized void aplicarEfecto(EstadoEfecto efecto) {
        if (!estaVivo()) {
            return;
        }
        // si ya tenia el mismo efecto, se cancela el hilo viejo
        Thread viejo = getHilos().remove(efecto.getNombre());
        if (viejo != null) {
            viejo.interrupt();
        }
        EstadoEfecto copia = efecto.copia();
        efectos.put(copia.getNombre(), copia);
        if (copia.esDanio() && estado == EstadoPersonaje.VIVO) {
            estado = EstadoPersonaje.ENVENENADO;
        }
        Thread hilo = new Thread(new EfectoContinuoTask(this, copia, INTERVALO_EFECTO_MS));
        hilo.setDaemon(true);
        getHilos().put(copia.getNombre(), hilo);
        hilo.start();
    }

    public synchronized boolean quitarEfecto(String nombreEfecto) {
        if (efectos.remove(nombreEfecto) == null) {
            return false;
        }
        Thread hilo = getHilos().remove(nombreEfecto);
        if (hilo != null) {
            hilo.interrupt();
        }
        normalizarEstado();
        return true;
    }

    public synchronized void quitarEfectosDanio() {
        for (String nombreEfecto : new ArrayList<>(efectos.keySet())) {
            if (efectos.get(nombreEfecto).esDanio()) {
                quitarEfecto(nombreEfecto);
            }
        }
    }

    // se llama al terminar el combate
    public synchronized void detenerEfectos() {
        for (Thread hilo : getHilos().values()) {
            hilo.interrupt();
        }
        getHilos().clear();
        efectos.clear();
        normalizarEstado();
    }

    // Lo llama el hilo del efecto cada cierto tiempo. Devuelve false cuando el efecto ya no debe seguir.
    public synchronized boolean aplicarTick(EstadoEfecto efecto) {
        if (efectos.get(efecto.getNombre()) != efecto || !estaVivo()) {
            return false;
        }
        if (efecto.esDanio()) {
            recibirDanio(efecto.getValorPorTick());
            System.out.printf("   [%s] %s sufre %d de dano (%d/%d)%n",
                    efecto.getNombre(), nombre, efecto.getValorPorTick(), vida, vidaMaxima);
        } else {
            int curado = curar(efecto.getValorPorTick());
            System.out.printf("   [%s] %s recupera %d de vida (%d/%d)%n",
                    efecto.getNombre(), nombre, curado, vida, vidaMaxima);
        }
        efecto.consumirTurno();
        if (!estaVivo()) {
            System.out.println(nombre + " cae derrotado por " + efecto.getNombre());
            return false;
        }
        if (efecto.haTerminado()) {
            efectos.remove(efecto.getNombre());
            normalizarEstado();
            System.out.println("   " + nombre + ": termina " + efecto.getNombre());
            return false;
        }
        return true;
    }

    // limpieza cuando interrumpen el hilo del efecto
    public synchronized void finalizarEfecto(EstadoEfecto efecto) {
        if (efectos.get(efecto.getNombre()) == efecto) {
            efectos.remove(efecto.getNombre());
            getHilos().remove(efecto.getNombre());
        }
        normalizarEstado();
    }

    // despues de cargar una partida el campo transient queda en null, por eso se crea aqui
    private Map<String, Thread> getHilos() {
        if (hilosEfectos == null) {
            hilosEfectos = new HashMap<>();
        }
        return hilosEfectos;
    }

    private boolean tieneEfectoDanio() {
        for (EstadoEfecto efecto : efectos.values()) {
            if (efecto.esDanio()) {
                return true;
            }
        }
        return false;
    }

    private void normalizarEstado() {
        if (estado == EstadoPersonaje.ENVENENADO && !tieneEfectoDanio()) {
            estado = EstadoPersonaje.VIVO;
        }
    }

    // ---- aturdimiento

    public synchronized void aturdir() {
        if (estaVivo()) {
            estado = EstadoPersonaje.ATURDIDO;
        }
    }

    public synchronized boolean consumirAturdimiento() {
        boolean estabaAturdido = estado == EstadoPersonaje.ATURDIDO;
        if (estabaAturdido) {
            estado = tieneEfectoDanio() ? EstadoPersonaje.ENVENENADO : EstadoPersonaje.VIVO;
        }
        return estabaAturdido;
    }

    // ---- inventario, arma, gremio, habilidades

    public synchronized String usarPocion(Pocion pocion) {
        int curado = curar(pocion.getCuracionReal());
        if (pocion.getEfecto() != null) {
            aplicarEfecto(pocion.getEfecto());
        }
        if (pocion.isPurifica()) {
            quitarEfectosDanio();
        }
        return String.format("%s usa %s (+%d de vida)", nombre, pocion.getNombre(), curado);
    }

    public void equiparArma(Arma nuevaArma) {
        this.arma = nuevaArma;
    }

    public void desequiparArma() {
        this.arma = null;
    }

    public void unirseAGremio(Gremio nuevoGremio) {
        if (nuevoGremio == null || nuevoGremio == gremio) {
            return;
        }
        if (gremio != null) {
            gremio.removerMiembro(this);
        }
        gremio = nuevoGremio;
        nuevoGremio.agregarMiembro(this);
    }

    public void abandonarGremio() {
        Gremio anterior = gremio;
        if (anterior == null) {
            return;
        }
        gremio = null;
        anterior.removerMiembro(this);
    }

    public boolean aprenderHabilidad(String habilidad) {
        return habilidades.add(habilidad);
    }

    // ---- getters y setters validados

    public synchronized void setVida(int nuevaVida) {
        vida = Math.max(0, Math.min(nuevaVida, vidaMaxima));
        if (vida == 0) {
            estado = EstadoPersonaje.MUERTO;
        }
    }

    public void setNombre(String nombre) {
        validarNombre(nombre);
        this.nombre = nombre;
    }

    public synchronized void setVidaMaxima(int vidaMaxima) {
        if (vidaMaxima <= 0) {
            throw new IllegalArgumentException("La vida maxima debe ser positiva");
        }
        this.vidaMaxima = vidaMaxima;
        this.vida = Math.min(vida, vidaMaxima);
    }

    public int getDefensa() {
        return defensaBase + tipo.getBonificadorDefensa();
    }

    public String getNombre() {
        return nombre;
    }

    public TipoPersonaje getTipo() {
        return tipo;
    }

    public synchronized int getVida() {
        return vida;
    }

    public synchronized int getVidaMaxima() {
        return vidaMaxima;
    }

    public synchronized EstadoPersonaje getEstado() {
        return estado;
    }

    public Arma getArma() {
        return arma;
    }

    public Gremio getGremio() {
        return gremio;
    }

    public Inventario<Pocion> getInventario() {
        return inventario;
    }

    public Set<String> getHabilidades() {
        return Collections.unmodifiableSet(habilidades);
    }

    // devuelvo una copia para que no falle si un hilo cambia el mapa mientras se recorre
    public synchronized List<EstadoEfecto> getEfectos() {
        return new ArrayList<>(efectos.values());
    }
}
