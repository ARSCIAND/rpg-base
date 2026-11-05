package com.rpg;

import com.rpg.concurrency.AutoGuardadoDaemon;
import com.rpg.core.Consola;
import com.rpg.core.MotorJuego;

import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            Consola consola = new Consola(scanner);
            MotorJuego motor = new MotorJuego(consola);

            // hilo de auto-guardado cada 10 segundos. setDaemon(true) tiene que ir antes de start()
            Thread autoGuardado = new Thread(new AutoGuardadoDaemon(motor, 10000));
            autoGuardado.setDaemon(true);
            autoGuardado.start();

            ejecutarMenu(consola, motor);

            // guardado final antes de salir. Como guardarProgreso es synchronized, si el daemon
            // esta escribiendo se espera a que termine y el archivo no queda a medias
            motor.guardarProgreso(false);
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada finalizada. Hasta luego.");
        }
    }

    private static void ejecutarMenu(Consola consola, MotorJuego motor) {
        int opcion;
        do {
            mostrarMenu();
            opcion = consola.leerEntero("Opcion: ", 0, 8);
            switch (opcion) {
                case 1 -> motor.combateRapido();
                case 2 -> motor.jugarTorneo();
                case 3 -> motor.mostrarBestiario();
                case 4 -> motor.mostrarHistorial();
                case 5 -> motor.guardarPartida();
                case 6 -> motor.cargarPartida();
                case 7 -> motor.mostrarArchivos();
                case 8 -> motor.borrarPartida();
                default -> System.out.println("Hasta luego.");
            }
        } while (opcion != 0);
    }

    private static void mostrarMenu() {
        System.out.println("\n=== SIMULADOR DE BATALLA RPG ===");
        System.out.println("1. Combate rapido");
        System.out.println("2. Torneo");
        System.out.println("3. Ver bestiario");
        System.out.println("4. Ver historial de combates");
        System.out.println("5. Guardar partida");
        System.out.println("6. Cargar partida");
        System.out.println("7. Ver archivos");
        System.out.println("8. Borrar partida");
        System.out.println("0. Salir");
    }
}
