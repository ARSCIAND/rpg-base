package com.rpg.funcional;

// TALLER LAMBDAS: interfaz funcional propia.
// Podria usar BiFunction<Integer, Integer, Integer>, pero con este nombre se entiende de una vez
// que es una regla del juego (como el ejemplo de CalculadoraImpuestos de la guia).
// @FunctionalInterface hace que el compilador se queje si alguien agrega un segundo metodo abstracto.
@FunctionalInterface
public interface CalculadoraPoder {

    // el unico metodo abstracto (regla SAM). La lambda es la "llave" que lo implementa
    int calcular(int vidaMaxima, int nivel);

    // los metodos default no rompen la regla SAM porque ya tienen cuerpo
    default String describir(int vidaMaxima, int nivel) {
        return "vida " + vidaMaxima + " + nivel " + nivel + " = " + calcular(vidaMaxima, nivel);
    }
}
