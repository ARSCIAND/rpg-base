package com.rpg.view;

import com.rpg.controller.HeroeController;
import com.rpg.funcional.ReglasHeroe;
import com.rpg.model.Heroe;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Predicate;

// Menu del registro de heroes. Solo lee del teclado e imprime,
// todo lo demas se lo pide al HeroeController (aqui no hay nada de base de datos).
public class HeroeUI {

    private final Scanner scanner;
    private final HeroeController controller = new HeroeController();
    private final JuegoUI juego;

    // TALLER LAMBDAS: Consumer, recibe un heroe y solo lo imprime (no devuelve nada)
    private final Consumer<Heroe> imprimirHeroe = heroe -> System.out.println(heroe);

    public HeroeUI(Scanner scanner) {
        this.scanner = scanner;
        this.juego = new JuegoUI(scanner, controller);
    }

    public void iniciar() {
        try {
            int opcion;
            do {
                mostrarMenu();
                opcion = leerEntero("Opcion: ");
                switch (opcion) {
                    case 1 -> insertar();
                    case 2 -> listar();
                    case 3 -> buscarPorTipo();
                    case 4 -> actualizarNivel();
                    case 5 -> eliminar();
                    case 6 -> juego.iniciar();
                    case 7 -> System.out.println(controller.estadisticas());
                    case 8 -> rankingPorPoder();
                    case 9 -> filtrar();
                    case 0 -> System.out.println("Hasta luego.");
                    default -> System.out.println("Opcion no valida.");
                }
            } while (opcion != 0);
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada finalizada. Hasta luego.");
        }
        juego.cerrar();
    }

    private void mostrarMenu() {
        System.out.println("\n=== REGISTRO DE HEROES ===");
        System.out.println("1. Insertar heroe");
        System.out.println("2. Listar heroes");
        System.out.println("3. Buscar heroes por tipo");
        System.out.println("4. Actualizar nivel");
        System.out.println("5. Eliminar heroe");
        System.out.println("6. Ir al simulador de batalla");
        System.out.println("7. Estadisticas de heroes (Streams)");
        System.out.println("8. Ranking por poder (Streams)");
        System.out.println("9. Filtrar heroes (Lambdas / Predicate)");
        System.out.println("0. Salir");
    }

    private void insertar() {
        System.out.print("Nombre: ");
        String nombre = scanner.nextLine();
        System.out.print("Tipo (guerrero / mago): ");
        String tipo = scanner.nextLine();
        int vida = leerEntero("Vida maxima: ");
        int nivel = leerEntero("Nivel: ");
        System.out.println(controller.insertar(nombre, tipo, vida, nivel));
    }

    private void listar() {
        mostrarLista(controller.listar());
    }

    private void buscarPorTipo() {
        System.out.print("Tipo (guerrero / mago): ");
        mostrarLista(controller.buscarPorTipo(scanner.nextLine()));
    }

    private void actualizarNivel() {
        int id = leerEntero("Id del heroe: ");
        int nivel = leerEntero("Nuevo nivel: ");
        System.out.println(controller.actualizarNivel(id, nivel));
    }

    private void eliminar() {
        int id = leerEntero("Id del heroe a eliminar: ");
        System.out.print("Seguro? (s/n): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("s")) {
            System.out.println(controller.eliminar(id));
        } else {
            System.out.println("No se elimino nada.");
        }
    }

    private void mostrarLista(List<Heroe> heroes) {
        if (heroes.isEmpty()) {
            System.out.println(controller.getMensaje());
            return;
        }
        System.out.println("ID   NOMBRE          TIPO");
        heroes.forEach(imprimirHeroe);
    }

    private void rankingPorPoder() {
        List<String> ranking = controller.rankingPorPoder();
        if (ranking.isEmpty()) {
            System.out.println(controller.getMensaje());
            return;
        }
        System.out.println("=== RANKING POR PODER ===");
        // aqui no puedo usar una lambda que cambie una variable local (tiene que ser "efectivamente final"),
        // por eso el puesto sale del for con indice
        for (int i = 0; i < ranking.size(); i++) {
            System.out.println((i + 1) + ". " + ranking.get(i));
        }
    }

    // cada opcion arma un Predicate distinto y el Controller solo hace filter(condicion)
    private void filtrar() {
        System.out.println("1. Veteranos (nivel " + ReglasHeroe.NIVEL_VETERANO + " o mas)");
        System.out.println("2. Guerreros veteranos");
        System.out.println("3. Novatos (menos de nivel " + ReglasHeroe.NIVEL_VETERANO + ")");
        System.out.println("4. Magos");
        System.out.println("5. Nombre que contiene...");
        int opcion = leerEntero("Filtro: ");
        Predicate<Heroe> condicion = switch (opcion) {
            case 1 -> ReglasHeroe.ES_VETERANO;
            case 2 -> ReglasHeroe.ES_GUERRERO.and(ReglasHeroe.ES_VETERANO);   // and() une dos Predicate
            case 3 -> ReglasHeroe.ES_VETERANO.negate();                       // negate() lo invierte
            case 4 -> ReglasHeroe.ES_MAGO;
            case 5 -> {
                System.out.print("Texto a buscar: ");
                yield ReglasHeroe.nombreContiene(scanner.nextLine());
            }
            default -> null;
        };
        if (condicion == null) {
            System.out.println("Filtro no valido.");
            return;
        }
        mostrarLista(controller.filtrar(condicion));
    }

    // vuelve a preguntar hasta que escriban un numero
    private int leerEntero(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String texto = scanner.nextLine().trim();
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                System.out.println("\"" + texto + "\" no es un numero, intenta de nuevo.");
            }
        }
    }
}
