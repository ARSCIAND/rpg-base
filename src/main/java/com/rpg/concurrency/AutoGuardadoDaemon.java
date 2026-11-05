package com.rpg.concurrency;

import com.rpg.core.MotorJuego;

// Guarda el progreso cada cierto tiempo en segundo plano.
// Hilo de usuario: la JVM espera a que termine antes de cerrar.
// Hilo daemon: la JVM cierra aunque siga vivo. Por eso este hilo se marca como daemon,
// si no el while(true) no dejaria cerrar el juego.
public class AutoGuardadoDaemon implements Runnable {

    private final MotorJuego motor;
    private final int intervaloMs;

    public AutoGuardadoDaemon(MotorJuego motor, int intervaloMs) {
        this.motor = motor;
        this.intervaloMs = intervaloMs;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(intervaloMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            motor.guardarProgreso(true);
        }
    }
}
