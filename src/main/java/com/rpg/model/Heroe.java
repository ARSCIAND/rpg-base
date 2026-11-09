package com.rpg.model;

// POJO que representa una fila de la tabla heroes
public class Heroe {

    private int id;
    private String nombre;
    private String tipo;
    private int vidaMaxima;
    private int nivel;

    public Heroe(String nombre, String tipo, int vidaMaxima, int nivel) {
        setNombre(nombre);
        setTipo(tipo);
        setVidaMaxima(vidaMaxima);
        setNivel(nivel);
    }

    // este se usa cuando el heroe viene de la base de datos y ya tiene id
    public Heroe(int id, String nombre, String tipo, int vidaMaxima, int nivel) {
        this(nombre, tipo, vidaMaxima, nivel);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
        this.nombre = nombre.trim();
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        if (!"guerrero".equals(tipo) && !"mago".equals(tipo)) {
            throw new IllegalArgumentException("El tipo debe ser guerrero o mago");
        }
        this.tipo = tipo;
    }

    public int getVidaMaxima() {
        return vidaMaxima;
    }

    public void setVidaMaxima(int vidaMaxima) {
        if (vidaMaxima <= 0) {
            throw new IllegalArgumentException("La vida maxima debe ser mayor que 0");
        }
        this.vidaMaxima = vidaMaxima;
    }

    public int getNivel() {
        return nivel;
    }

    public void setNivel(int nivel) {
        if (nivel < 1) {
            throw new IllegalArgumentException("El nivel debe ser 1 o mayor");
        }
        this.nivel = nivel;
    }

    @Override
    public String toString() {
        return String.format("%-4d %-15s %-9s vida: %-4d nivel: %d", id, nombre, tipo, vidaMaxima, nivel);
    }
}
