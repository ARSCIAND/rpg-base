package com.rpg.items;

import com.rpg.exceptions.CapacidadExcedidaException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.io.Serializable;

/** Contenedor generico con capacidad limitada: Inventario&lt;Pocion&gt; solo acepta pociones. */
public class Inventario<T> implements Iterable<T>, Serializable {

    private static final long serialVersionUID = 1L;

    private final List<T> elementos = new ArrayList<>();
    private final int capacidad;

    public Inventario(int capacidad) {
        if (capacidad <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser positiva");
        }
        this.capacidad = capacidad;
    }

    public void agregar(T elemento) throws CapacidadExcedidaException {
        if (elementos.size() >= capacidad) {
            throw new CapacidadExcedidaException(capacidad);
        }
        elementos.add(elemento);
    }

    public T obtener(int indice) {
        return elementos.get(indice);
    }

    public T quitar(int indice) {
        return elementos.remove(indice);
    }

    public int tamanio() {
        return elementos.size();
    }

    public boolean estaVacio() {
        return elementos.isEmpty();
    }

    // TALLER STREAMS: para recorrer el inventario con filter, map, flatMap...
    public Stream<T> stream() {
        return elementos.stream();
    }

    @Override
    public Iterator<T> iterator() {
        return elementos.iterator();
    }
}
