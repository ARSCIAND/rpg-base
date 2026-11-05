package com.rpg.concurrency;

import com.rpg.core.TurnoCallback;

// Crea un hilo nuevo para cada turno (un Thread no se puede iniciar dos veces).
public class GestorTurno {

    private Thread hilo;

    public void iniciar(int segundos, TurnoCallback callback) {
        hilo = new Thread(new TemporizadorTurnoTask(segundos, callback));
        hilo.start();
    }

    public void cancelar() {
        if (hilo != null) {
            hilo.interrupt();
        }
    }
}
