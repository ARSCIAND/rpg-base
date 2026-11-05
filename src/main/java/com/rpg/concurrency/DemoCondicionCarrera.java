package com.rpg.concurrency;

import com.rpg.entities.Guerrero;

import java.util.concurrent.locks.ReentrantLock;

// Prueba de la condicion de carrera: dos hilos le quitan vida al mismo tiempo.
// Cada hilo quita 1 de vida 20000 veces, asi que al final deberia quedar 100000 - 40000 = 60000.
public class DemoCondicionCarrera {

    static int vidaSinProteccion = 100000;

    static int vidaConLock = 100000;
    static ReentrantLock lock = new ReentrantLock();

    // igual que estaba recibirDanio antes del taller: lee, calcula y escribe sin proteccion
    static void danioSinProteccion() {
        int leida = vidaSinProteccion;
        Thread.yield();
        vidaSinProteccion = leida - 1;
    }

    // version con ReentrantLock. El unlock va en el finally, si no el lock se puede quedar tomado.
    // Lo usaria en vez de synchronized cuando necesito tryLock() (intentar sin quedarme esperando)
    // o poder cancelar la espera con interrupt(). Para algo simple synchronized es mas facil.
    static void danioConLock() {
        lock.lock();
        try {
            int leida = vidaConLock;
            Thread.yield();
            vidaConLock = leida - 1;
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // 1. sin proteccion
        Runnable sinProteccion = () -> {
            for (int i = 0; i < 20000; i++) {
                danioSinProteccion();
            }
        };
        ejecutarDosHilos(sinProteccion);
        System.out.println("Sin proteccion:  " + vidaSinProteccion);

        // 2. con el Personaje del juego (recibirDanio es synchronized)
        Guerrero guerrero = new Guerrero("Prueba");
        guerrero.setVidaMaxima(100000);
        guerrero.setVida(100000);
        Runnable conSynchronized = () -> {
            for (int i = 0; i < 20000; i++) {
                guerrero.recibirDanio(1);
            }
        };
        ejecutarDosHilos(conSynchronized);
        System.out.println("Con synchronized: " + guerrero.getVida());

        // 3. con ReentrantLock
        Runnable conLock = () -> {
            for (int i = 0; i < 20000; i++) {
                danioConLock();
            }
        };
        ejecutarDosHilos(conLock);
        System.out.println("Con ReentrantLock: " + vidaConLock);
    }

    static void ejecutarDosHilos(Runnable tarea) throws InterruptedException {
        Thread h1 = new Thread(tarea);
        Thread h2 = new Thread(tarea);
        h1.start();
        h2.start();
        h1.join();
        h2.join();
    }
}
