package com.rpg.concurrency;

import com.rpg.core.TurnoCallback;

// Cuenta regresiva del turno. Si llega a 0 avisa con el callback.
public class TemporizadorTurnoTask implements Runnable {

    private final int segundos;
    private final TurnoCallback callback;

    public TemporizadorTurnoTask(int segundos, TurnoCallback callback) {
        this.segundos = segundos;
        this.callback = callback;
    }

    @Override
    public void run() {
        for (int i = segundos; i > 0; i--) {
            if (i == 5) {
                System.out.println("\n[Quedan 5 segundos]");
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // el jugador respondio a tiempo, se cancela la cuenta
                Thread.currentThread().interrupt();
                return;
            }
        }
        callback.onTiempoAgotado();
    }
}
