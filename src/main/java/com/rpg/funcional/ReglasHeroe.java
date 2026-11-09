package com.rpg.funcional;

import com.rpg.model.Heroe;

import java.util.function.Function;
import java.util.function.Predicate;

// TALLER LAMBDAS: las reglas del registro de heroes guardadas como lambdas.
// Asi el Controller y la View las pasan como si fueran datos (filter, map, sorted...).
public final class ReglasHeroe {

    public static final int NIVEL_VETERANO = 3;
    public static final int NIVEL_LEYENDA = 5;

    // ---- Predicate<T>: recibe un heroe y responde true o false (el "guardia" que filtra)
    public static final Predicate<Heroe> ES_GUERRERO = heroe -> heroe.getTipo().equals("guerrero");
    public static final Predicate<Heroe> ES_MAGO = heroe -> heroe.getTipo().equals("mago");
    public static final Predicate<Heroe> ES_VETERANO = heroe -> heroe.getNivel() >= NIVEL_VETERANO;
    public static final Predicate<Heroe> ES_LEYENDA = heroe -> heroe.getNivel() >= NIVEL_LEYENDA;

    // ---- Function<T, R>: convierte un heroe en otra cosa (la "cinta transportadora")
    public static final Function<Heroe, String> NOMBRE = Heroe::getNombre;
    public static final Function<Heroe, String> FICHA_CORTA =
            heroe -> heroe.getNombre() + " (" + heroe.getTipo() + ", nivel " + heroe.getNivel() + ")";

    // ---- interfaz propia: dos parametros con nombre de negocio
    // poder = vida + 10 por cada nivel. Se usa para el ranking
    public static final CalculadoraPoder PODER = (vida, nivel) -> vida + nivel * 10;
    // al cargar un heroe en el juego, cada nivel despues del 1 le da 5 de vida extra
    public static final CalculadoraPoder VIDA_EN_COMBATE = (vida, nivel) -> vida + (nivel - 1) * 5;

    private ReglasHeroe() {
        // solo tiene constantes, no se crean objetos de esta clase
    }

    public static int poderDe(Heroe heroe) {
        return PODER.calcular(heroe.getVidaMaxima(), heroe.getNivel());
    }

    // devuelve un Predicate nuevo armado con el texto que escribio el usuario
    public static Predicate<Heroe> nombreContiene(String texto) {
        String buscado = texto.trim().toLowerCase();
        return heroe -> heroe.getNombre().toLowerCase().contains(buscado);
    }
}
