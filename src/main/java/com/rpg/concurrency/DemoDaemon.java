package com.rpg.concurrency;

// Prueba de hilo daemon: el main termina a los 2 segundos y la JVM cierra
// aunque el hilo daemon tenga un while(true).
public class DemoDaemon {

    public static void main(String[] args) throws InterruptedException {
        Thread daemon = new Thread(() -> {
            while (true) {
                System.out.println("daemon sigue vivo");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        daemon.setDaemon(true);
        daemon.start();

        // setDaemon despues de start() lanza IllegalThreadStateException
        try {
            daemon.setDaemon(false);
        } catch (IllegalThreadStateException e) {
            System.out.println("Error esperado: no se puede cambiar setDaemon despues de start()");
        }

        Thread.sleep(2000);
        System.out.println("Termina main, el daemon sigue vivo? " + daemon.isAlive());
    }
}
