package com.rpg.funcional;

import com.rpg.core.Bestiario;
import com.rpg.entities.Escuadron;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;
import com.rpg.entities.enums.RaridadItem;
import com.rpg.exceptions.CapacidadExcedidaException;
import com.rpg.items.Pocion;
import com.rpg.model.Heroe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Unidad 6.2: Stream API. Tambien se ejecuta solo y sin base de datos.
// Origen -> operaciones intermedias -> operacion terminal, como la cinta de la fabrica.
public class DemoStreams {

    public static void main(String[] args) {
        imperativoVsDeclarativo();
        origenes();
        filterYMap();
        flatMap();
        anyMatchYCortocircuito();
        evaluacionPerezosa();
        deVueltaALista();
        streamYaConsumido();
        casoReal();
    }

    private static void imperativoVsDeclarativo() {
        titulo("1. El como (for) vs el que (stream)");
        List<Integer> danios = List.of(12, 7, 20, 3, 15, 8);

        // imperativo: le digo paso a paso COMO hacerlo
        List<Integer> paresFor = new ArrayList<>();
        for (int danio : danios) {
            if (danio % 2 == 0) {
                paresFor.add(danio);
            }
        }
        // declarativo: le digo QUE quiero
        List<Integer> paresStream = danios.stream()
                .filter(danio -> danio % 2 == 0)
                .toList();

        System.out.println("Con for:    " + paresFor);
        System.out.println("Con stream: " + paresStream);
        System.out.println("La lista original no cambio: " + danios);   // inmutabilidad
    }

    private static void origenes() {
        titulo("2. Origenes (la materia prima)");
        Stream.of("Espada", "Arco", "Baston").forEach(arma -> System.out.println("Stream.of -> " + arma));

        String[] rangos = {"Bronce", "Plata", "Oro"};
        Arrays.stream(rangos).forEach(rango -> System.out.println("Arrays.stream -> " + rango));

        Stream<String> gritos = Stream.<String>builder().add("Por el gremio!").add("A la carga!").build();
        gritos.forEach(grito -> System.out.println("builder -> " + grito));

        new Bestiario().nombres().stream().forEach(nombre -> System.out.println("lista.stream() -> " + nombre));
    }

    private static void filterYMap() {
        titulo("3. filter y map");
        List<String> monstruos = new Bestiario().nombres();

        // filter usa un Predicate: deja pasar solo lo que da true
        List<String> conA = monstruos.stream()
                .filter(nombre -> nombre.contains("a"))
                .toList();
        // map usa una Function: 1 a 1, incluso puede cambiar el tipo (String -> Integer)
        List<String> enMayuscula = monstruos.stream().map(String::toUpperCase).toList();
        List<Integer> largos = monstruos.stream().map(String::length).toList();

        System.out.println("Con la letra a: " + conA);
        System.out.println("En mayuscula:   " + enMayuscula);
        System.out.println("Largo:          " + largos);
    }

    private static void flatMap() {
        titulo("4. flatMap (el aplanador)");
        Escuadron escuadron = crearEscuadron();

        // cada personaje tiene un Set de habilidades: Stream<Set<String>> -> Stream<String>
        List<String> habilidades = escuadron.getMiembros().stream()
                .flatMap(personaje -> personaje.getHabilidades().stream())
                .distinct()
                .sorted()
                .toList();
        System.out.println("Habilidades del escuadron: " + habilidades);

        // cada personaje tiene un inventario: se "desempaquetan" todas las pociones en un solo flujo
        List<String> pociones = escuadron.getMiembros().stream()
                .flatMap(personaje -> personaje.getInventario().stream())
                .map(Pocion::getNombre)
                .toList();
        System.out.println("Pociones de todos juntas: " + pociones);

        // el ejemplo de la guia: List<List<T>> con List::stream
        List<List<String>> botinPorCofre = List.of(List.of("Oro", "Gema"), List.of("Pergamino"), List.of("Oro"));
        System.out.println("Botin aplanado: " + botinPorCofre.stream().flatMap(List::stream).toList());
    }

    private static void anyMatchYCortocircuito() {
        titulo("5. anyMatch y el cortocircuito");
        List<Integer> vidas = List.of(40, 0, 75, 90, 120);

        // peek solo sirve para ver por donde va. Despues del 0 ya no revisa los demas
        boolean hayCaido = vidas.stream()
                .peek(vida -> System.out.println("  revisando vida " + vida))
                .anyMatch(vida -> vida == 0);
        System.out.println("Hay algun personaje caido? " + hayCaido);
    }

    private static void evaluacionPerezosa() {
        titulo("6. Evaluacion perezosa (lazy)");
        Stream<String> sinTerminal = Stream.of("Goblin", "Ogro")
                .map(nombre -> {
                    System.out.println("  procesando " + nombre);
                    return nombre.toUpperCase();
                });
        System.out.println("Ya arme el stream pero no se ha procesado nada...");
        System.out.println("Ahora la operacion terminal: " + sinTerminal.toList());
    }

    private static void deVueltaALista() {
        titulo("7. De vuelta a una coleccion");
        List<String> nuevo = Stream.of("Thor", "Xena").toList();                           // Java 16+
        List<String> clasico = Stream.of("Thor", "Xena").collect(Collectors.toList());     // antes de Java 16
        String unido = Stream.of("Thor", "Xena", "Merlin").collect(Collectors.joining(" | "));
        System.out.println("toList(): " + nuevo);
        System.out.println("collect(Collectors.toList()): " + clasico);
        System.out.println("Collectors.joining: " + unido);
    }

    private static void streamYaConsumido() {
        titulo("8. Los streams se consumen");
        Stream<String> heroes = Stream.of("Thor", "Merlin");
        heroes.forEach(nombre -> System.out.println("  " + nombre));
        try {
            heroes.forEach(nombre -> System.out.println("  " + nombre));   // segunda vez: error
        } catch (IllegalStateException e) {
            System.out.println("Reutilizarlo lanza IllegalStateException: " + e.getMessage());
        }
    }

    // el mismo tipo de consulta que hace HeroeController, pero con heroes en memoria
    private static void casoReal() {
        titulo("9. Caso real: toma los heroes, filtra los veteranos, extrae el nombre y guardalo en una lista");
        List<Heroe> heroes = List.of(
                new Heroe(1, "Thor", "guerrero", 150, 3),
                new Heroe(2, "Merlin", "mago", 90, 5),
                new Heroe(3, "Xena", "guerrero", 130, 2));

        List<String> veteranos = heroes.stream()   // 1. origen
                .filter(ReglasHeroe.ES_VETERANO)   // 2. filtra
                .map(Heroe::getNombre)             // 3. extrae
                .toList();                         // 4. guarda en una lista
        System.out.println("Veteranos: " + veteranos);
    }

    private static Escuadron crearEscuadron() {
        Personaje thor = new Guerrero("Thor");
        Personaje merlin = new Mago("Merlin");
        Personaje xena = new Guerrero("Xena");
        thor.aprenderHabilidad("Bloqueo");
        merlin.aprenderHabilidad("Teletransporte");
        xena.aprenderHabilidad("Bloqueo");
        try {
            thor.getInventario().agregar(new Pocion("Pocion de Vida", 30, RaridadItem.COMUN));
            merlin.getInventario().agregar(new Pocion("Pocion de Mana", 20, RaridadItem.RARO));
            merlin.getInventario().agregar(new Pocion("Antidoto", 0, RaridadItem.COMUN, null, true));
        } catch (CapacidadExcedidaException e) {
            System.out.println(e.getMessage());
        }
        Escuadron escuadron = new Escuadron("Vanguardia");
        escuadron.agregar(thor);
        escuadron.agregar(merlin);
        escuadron.agregar(xena);
        return escuadron;
    }

    private static void titulo(String texto) {
        System.out.println("\n=== " + texto + " ===");
    }
}
