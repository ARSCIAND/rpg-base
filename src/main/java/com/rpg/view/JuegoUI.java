package com.rpg.view;

import com.rpg.concurrency.AutoGuardadoDaemon;
import com.rpg.controller.HeroeController;
import com.rpg.core.Consola;
import com.rpg.core.MotorJuego;
import com.rpg.entities.Personaje;
import com.rpg.model.Heroe;

import java.util.List;
import java.util.Scanner;

// Menu del simulador de batalla. Antes estaba en Main, lo pase aqui para que Main solo arranque la View.
public class JuegoUI {

    private final Consola consola;
    private final MotorJuego motor;
    private final HeroeController heroeController;

    public JuegoUI(Scanner scanner, HeroeController heroeController) {
        this.consola = new Consola(scanner);
        this.motor = new MotorJuego(consola);
        this.heroeController = heroeController;

        // hilo de auto-guardado cada 10 segundos. setDaemon(true) tiene que ir antes de start()
        Thread autoGuardado = new Thread(new AutoGuardadoDaemon(motor, 10000));
        autoGuardado.setDaemon(true);
        autoGuardado.start();
    }

    public void iniciar() {
        int opcion;
        do {
            mostrarMenu();
            opcion = consola.leerEntero("Opcion: ", 0, 9);
            switch (opcion) {
                case 1 -> motor.combateRapido();
                case 2 -> motor.jugarTorneo();
                case 3 -> motor.mostrarBestiario();
                case 4 -> motor.mostrarHistorial();
                case 5 -> motor.guardarPartida();
                case 6 -> motor.cargarPartida();
                case 7 -> motor.mostrarArchivos();
                case 8 -> motor.borrarPartida();
                case 9 -> cargarHeroeDesdeBD();
                default -> System.out.println("Volviendo al registro de heroes.");
            }
        } while (opcion != 0);
    }

    // guardado final antes de salir. Como guardarProgreso es synchronized, si el daemon
    // esta escribiendo se espera a que termine y el archivo no queda a medias
    public void cerrar() {
        motor.guardarProgreso(false);
    }

    private void mostrarMenu() {
        System.out.println("\n=== SIMULADOR DE BATALLA RPG ===");
        System.out.println("1. Combate rapido");
        System.out.println("2. Torneo");
        System.out.println("3. Ver bestiario");
        System.out.println("4. Ver historial de combates");
        System.out.println("5. Guardar partida");
        System.out.println("6. Cargar partida");
        System.out.println("7. Ver archivos");
        System.out.println("8. Borrar partida");
        System.out.println("9. Cargar heroe desde BD");
        System.out.println("0. Volver");
    }

    private void cargarHeroeDesdeBD() {
        List<Heroe> heroes = heroeController.listar();
        if (heroes.isEmpty()) {
            System.out.println(heroeController.getMensaje());
            return;
        }
        System.out.println("Heroes guardados:");
        heroes.forEach(heroe -> System.out.println("  " + heroe));
        int id = consola.leerEntero("Id del heroe: ", 1, Integer.MAX_VALUE);
        Personaje personaje = heroeController.cargarPersonaje(id);
        System.out.println(heroeController.getMensaje());
        if (personaje != null) {
            motor.combateConHeroeBD(personaje);
        }
    }
}
