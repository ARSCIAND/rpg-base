package com.rpg.concurrency;

import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Personaje;

// Un efecto (veneno, quemadura, regeneracion) que corre en su propio hilo.
// Se cancela con interrupt() cuando se usa un antidoto o termina el combate.
public class EfectoContinuoTask implements Runnable {

    private final Personaje objetivo;
    private final EstadoEfecto efecto;
    private final int intervaloMs;

    public EfectoContinuoTask(Personaje objetivo, EstadoEfecto efecto, int intervaloMs) {
        this.objetivo = objetivo;
        this.efecto = efecto;
        this.intervaloMs = intervaloMs;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(intervaloMs);
                } catch (InterruptedException e) {
                    objetivo.finalizarEfecto(efecto);
                    Thread.currentThread().interrupt();
                    break;
                }
                if (!objetivo.aplicarTick(efecto)) {
                    break;
                }
            }
        } finally {
            objetivo.finalizarEfecto(efecto);
        }
    }
}
