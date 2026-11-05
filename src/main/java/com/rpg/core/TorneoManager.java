package com.rpg.core;

import com.rpg.entities.Escuadron;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;

import java.util.List;

/** Torneo de 4 gladiadores: dos semifinales y una final. */
public class TorneoManager {

    private final Arena arena;
    private final RegistroCombate registro;

    // ganadores de cada semifinal, los escribe cada hilo
    private Personaje ganadorA;
    private Personaje ganadorB;

    public TorneoManager(Arena arena, RegistroCombate registro) {
        this.arena = arena;
        this.registro = registro;
    }

    /** @return el campeon del torneo */
    public Personaje jugarTorneo() {
        List<Personaje> gladiadores = crearGladiadores().getMiembros();

        // cada semifinal corre en su propio hilo
        Thread semifinalA = new Thread(() -> ganadorA = simularSemifinal(gladiadores.get(0), gladiadores.get(1), "[A] "));
        Thread semifinalB = new Thread(() -> ganadorB = simularSemifinal(gladiadores.get(2), gladiadores.get(3), "[B] "));

        System.out.println("Estado antes de start(): A=" + semifinalA.getState() + " B=" + semifinalB.getState());
        semifinalA.start();
        semifinalB.start();

        try {
            Thread.sleep(50);
            System.out.println("Estado durante la ejecucion: A=" + semifinalA.getState() + " B=" + semifinalB.getState());
            // el hilo principal espera a que terminen las dos semifinales
            semifinalA.join();
            semifinalB.join();
        } catch (InterruptedException e) {
            System.out.println("Se interrumpio el torneo");
            Thread.currentThread().interrupt();
        }
        System.out.println("Estado despues de join(): A=" + semifinalA.getState() + " B=" + semifinalB.getState());

        // solo despues del join se leen los ganadores
        return simularFinal(ganadorA, ganadorB);
    }

    public Personaje simularSemifinal(Personaje a, Personaje b, String etiqueta) {
        registro.agregar("%s=== SEMIFINAL: %s vs %s ===", etiqueta, a.getNombre(), b.getNombre());
        Personaje ganador = arena.duelo(a, b, etiqueta);
        registro.agregar("%sClasifica a la final: %s", etiqueta, ganador.getNombre());
        return ganador;
    }

    private Personaje simularFinal(Personaje a, Personaje b) {
        a.restaurarVida();
        b.restaurarVida();
        registro.agregar("=== FINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        return arena.duelo(a, b, "");
    }

    private Escuadron crearGladiadores() {
        Escuadron escuadron = new Escuadron("Gladiadores");
        escuadron.agregar(new Guerrero("Maximo"));
        escuadron.agregar(new Mago("Circe"));
        escuadron.agregar(new Guerrero("Aquiles"));
        escuadron.agregar(new Mago("Merlina"));
        return escuadron;
    }
}
