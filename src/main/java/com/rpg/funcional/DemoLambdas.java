package com.rpg.funcional;

import com.rpg.core.Bestiario;
import com.rpg.entities.Enemigo;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Personaje;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

// Unidad 6.1: Expresiones Lambda. Se ejecuta solo (clic derecho -> Run 'DemoLambdas.main()')
// y no necesita la base de datos. Sigue el orden de la guia, con objetos del juego.
public class DemoLambdas {

    public static void main(String[] args) {
        claseAnonimaVsLambda();
        lasCuatroInterfaces();
        interfazPropia();
        pipeline();
    }

    // 1. lo mismo escrito con clase anonima (antes de Java 8) y con lambda
    private static void claseAnonimaVsLambda() {
        titulo("1. Clase anonima vs lambda");

        Runnable anonima = new Runnable() {
            @Override
            public void run() {
                System.out.println("Grito de batalla desde una clase anonima");
            }
        };
        // (parametros) -> cuerpo. Runnable no recibe nada, por eso los parentesis van vacios
        Runnable lambda = () -> System.out.println("Grito de batalla desde una lambda");

        anonima.run();
        lambda.run();
        // en IntelliJ, Alt + Enter sobre la clase anonima ofrece "Replace with lambda"
    }

    // 2. Consumer, Supplier, Function y Predicate de java.util.function
    private static void lasCuatroInterfaces() {
        titulo("2. Las cuatro interfaces de java.util.function");

        // Consumer<T>: recibe y no devuelve nada (el "agujero negro"). Metodo accept
        Consumer<String> anunciar = texto -> System.out.println("[Arena] " + texto);
        List.of("Thor", "Merlin", "Xena").forEach(anunciar);

        // Supplier<T>: no recibe nada y entrega algo (la "fabrica"). Metodo get
        Bestiario bestiario = new Bestiario();
        List<String> nombres = bestiario.nombres();
        Random random = new Random();
        Supplier<Enemigo> rivalAleatorio = () -> bestiario.crearEnemigo(nombres.get(random.nextInt(nombres.size())));
        System.out.println("Rival sorpresa: " + rivalAleatorio.get().getNombre());

        // Function<T, R>: recibe un T y devuelve un R (la "cinta transportadora"). Metodo apply
        Function<Personaje, String> estado = p -> p.getNombre() + " " + p.getVida() + "/" + p.getVidaMaxima();
        Function<String, Integer> largo = String::length;   // referencia a metodo, igual a texto -> texto.length()
        Personaje thor = new Guerrero("Thor");
        System.out.println("Estado: " + estado.apply(thor));
        System.out.println("Letras de 'Bootcamp': " + largo.apply("Bootcamp"));

        // Predicate<T>: recibe un T y devuelve boolean (el "guardia"). Metodo test
        Predicate<Integer> esCritico = danio -> danio >= 15;
        System.out.println("Un golpe de 20 es critico? " + esCritico.test(20));
        System.out.println("Un golpe de 8 es critico? " + esCritico.test(8));
        // los Predicate se combinan con and, or y negate
        Predicate<Integer> esNormal = esCritico.negate().and(danio -> danio > 0);
        System.out.println("Un golpe de 8 es normal? " + esNormal.test(8));
    }

    // 3. interfaz funcional propia (ver CalculadoraPoder y ReglasHeroe)
    private static void interfazPropia() {
        titulo("3. Interfaz funcional propia con @FunctionalInterface");

        // un solo contrato y varios comportamientos
        CalculadoraPoder suma = (vida, nivel) -> vida + nivel;
        CalculadoraPoder multiplicacion = (vida, nivel) -> vida * nivel;
        System.out.println("Suma: " + suma.calcular(10, 5));                     // 15
        System.out.println("Multiplicacion: " + multiplicacion.calcular(10, 5)); // 50

        // las reglas que usa el juego de verdad
        System.out.println("Poder de Thor (150 de vida, nivel 3): " + ReglasHeroe.PODER.describir(150, 3));
        System.out.println("Vida en combate de Thor: " + ReglasHeroe.VIDA_EN_COMBATE.describir(150, 3));
    }

    // 4. las cuatro interfaces juntas en un pipeline
    private static void pipeline() {
        titulo("4. Pipeline: Supplier -> Predicate -> Function -> Consumer");

        Bestiario bestiario = new Bestiario();
        List<String> nombres = bestiario.nombres();
        Random random = new Random(7);   // con semilla para que salga siempre lo mismo

        Supplier<Enemigo> generarEnemigo = () -> bestiario.crearEnemigo(nombres.get(random.nextInt(nombres.size())));
        Predicate<Enemigo> esPeligroso = enemigo -> enemigo.getAtaque() >= 10;
        Function<Enemigo, String> extraerFicha = enemigo -> enemigo.getNombre() + " (ATK " + enemigo.getAtaque() + ")";
        Consumer<String> alertar = ficha -> System.out.println("Cuidado con: " + ficha);

        Stream.generate(generarEnemigo)   // Supplier
                .limit(6)                 // sin limit, generate no terminaria nunca
                .filter(esPeligroso)      // Predicate
                .map(extraerFicha)        // Function
                .forEach(alertar);        // Consumer
    }

    private static void titulo(String texto) {
        System.out.println("\n=== " + texto + " ===");
    }
}
