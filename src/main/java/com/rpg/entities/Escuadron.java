package com.rpg.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Agregacion: disolver el escuadron no destruye a los personajes. */
public class Escuadron {

    private final String nombre;
    private final List<Personaje> miembros = new ArrayList<>();

    public Escuadron(String nombre) {
        this.nombre = nombre;
    }

    public boolean agregar(Personaje personaje) {
        if (personaje == null || miembros.contains(personaje)) {
            return false;
        }
        return miembros.add(personaje);
    }

    public boolean remover(Personaje personaje) {
        return miembros.remove(personaje);
    }

    public int contarVivos() {
        // TALLER STREAMS: antes era un for con un contador
        return (int) miembros.stream()
                .filter(Personaje::estaVivo)
                .count();
    }

    public void disolver() {
        miembros.clear();
    }

    public String getNombre() {
        return nombre;
    }

    public List<Personaje> getMiembros() {
        return Collections.unmodifiableList(miembros);
    }
}
