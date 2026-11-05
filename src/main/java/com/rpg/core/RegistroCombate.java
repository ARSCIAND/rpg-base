package com.rpg.core;

import java.util.ArrayList;
import java.util.List;

/** Imprime cada evento del combate y lo guarda para incluirlo en el guardado de la partida. */
// synchronized porque ahora varios hilos escriben aqui (por ejemplo las dos semifinales)
public class RegistroCombate {

    private final List<String> lineas = new ArrayList<>();

    public synchronized void agregar(String linea) {
        System.out.println(linea);
        for (String parte : linea.split("\\r?\\n")) {
            lineas.add(parte);
        }
    }

    public void agregar(String formato, Object... argumentos) {
        agregar(String.format(formato, argumentos));
    }

    public synchronized void limpiar() {
        lineas.clear();
    }

    public synchronized List<String> getLineas() {
        return new ArrayList<>(lineas);
    }

    public synchronized String getTexto() {
        StringBuilder texto = new StringBuilder();
        for (String linea : lineas) {
            texto.append(linea).append("\n");
        }
        return texto.toString();
    }
}
