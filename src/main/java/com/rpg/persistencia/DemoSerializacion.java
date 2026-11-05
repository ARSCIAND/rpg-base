package com.rpg.persistencia;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

// Prueba de NotSerializableException: Sesion es Serializable pero tiene adentro un Reloj que no lo es.
// Se arregla poniendo el campo como transient.
public class DemoSerializacion {

    static class Reloj {
        long inicio = System.currentTimeMillis();
    }

    static class SesionMal implements Serializable {
        private static final long serialVersionUID = 1L;
        String jugador = "Thor";
        Reloj reloj = new Reloj();
    }

    static class SesionBien implements Serializable {
        private static final long serialVersionUID = 1L;
        String jugador = "Thor";
        transient Reloj reloj = new Reloj();
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        String archivo = "prueba_serializacion.dat";

        try (ObjectOutputStream salida = new ObjectOutputStream(new FileOutputStream(archivo))) {
            salida.writeObject(new SesionMal());
        } catch (NotSerializableException e) {
            System.out.println("Error: la clase " + e.getMessage() + " no es Serializable");
        }

        try (ObjectOutputStream salida = new ObjectOutputStream(new FileOutputStream(archivo))) {
            salida.writeObject(new SesionBien());
        }
        try (ObjectInputStream entrada = new ObjectInputStream(new FileInputStream(archivo))) {
            SesionBien leida = (SesionBien) entrada.readObject();
            System.out.println("Con transient si funciona. jugador = " + leida.jugador + ", reloj = " + leida.reloj);
        }
    }
}
