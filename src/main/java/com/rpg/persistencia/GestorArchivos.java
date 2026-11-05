package com.rpg.persistencia;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

// Maneja todos los archivos del juego dentro de la carpeta partidas/
// Los metodos son synchronized porque el hilo de auto-guardado y el hilo principal
// pueden usar los archivos al mismo tiempo.
//
// FileWriter vs Files.writeString:
// Con BufferedWriter + FileWriter abro el archivo, escribo linea por linea y lo cierro (sirve para ir
// agregando, como el historial). Files.writeString escribe todo un String de una vez en una sola
// llamada, sirve cuando el texto ya esta completo y es corto.
//
// Seguridad de Serializable:
// No se debe hacer readObject de archivos que no sean de confianza, porque un archivo modificado puede
// crear objetos que ejecuten codigo o que consuman toda la memoria. Es mas seguro guardar en JSON o en
// un archivo de texto propio y leer los datos uno por uno.
public class GestorArchivos {

    private File carpeta = new File("partidas");
    private File archivoPartida = new File(carpeta, "partida.dat");
    private File archivoHistorial = new File(carpeta, "historial_combates.txt");
    private File archivoProgreso = new File(carpeta, "progreso.txt");

    public synchronized void crearCarpeta() {
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }
        try {
            if (archivoHistorial.createNewFile()) {
                System.out.println("Se creo el archivo " + archivoHistorial.getName());
            }
        } catch (IOException e) {
            System.out.println("No se pudo crear el historial: " + e.getMessage());
        }
    }

    public synchronized void mostrarInfo() {
        File relativo = new File("partidas/partida.dat");
        File absoluto = new File(relativo.getAbsolutePath());
        System.out.println("\n=== ARCHIVOS ===");
        System.out.println("Ruta relativa: " + relativo.getPath());
        System.out.println("Ruta absoluta: " + absoluto.getPath());
        mostrarArchivo(archivoPartida);
        mostrarArchivo(archivoHistorial);
        mostrarArchivo(archivoProgreso);
    }

    private void mostrarArchivo(File archivo) {
        System.out.println(archivo.getName() + " | existe: " + archivo.exists() + " | tamano: " + archivo.length() + " bytes");
    }

    // ---- escritura

    // true = append, agrega al final sin borrar los combates anteriores
    public synchronized void agregarAlHistorial(List<String> lineas) {
        crearCarpeta();
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(archivoHistorial, true))) {
            escritor.write("----------------------------------------");
            escritor.newLine();
            for (String linea : lineas) {
                escritor.write(linea);
                escritor.newLine();
            }
            escritor.flush();
        } catch (IOException e) {
            System.out.println("Error al escribir el historial: " + e.getMessage());
        }
    }

    // sin el true se sobrescribe, asi el archivo solo tiene el ultimo progreso
    public synchronized void escribirProgreso(String texto) {
        crearCarpeta();
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(archivoProgreso))) {
            for (String linea : texto.split("\\r?\\n")) {
                escritor.write(linea);
                escritor.newLine();
            }
            escritor.flush();
        } catch (IOException e) {
            System.out.println("Error al escribir el progreso: " + e.getMessage());
        }
    }

    // ---- lectura

    public synchronized List<String> leerHistorial() {
        List<String> lineas = new ArrayList<>();
        try (BufferedReader lector = new BufferedReader(new FileReader(archivoHistorial))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                lineas.add(linea);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Todavia no existe el historial de combates.");
        } catch (IOException e) {
            System.out.println("Error al leer el historial: " + e.getMessage());
        }
        return lineas;
    }

    // ---- borrado

    // Files.deleteIfExists no falla si el archivo no existe y si falla por otra razon lanza IOException
    public synchronized boolean borrarPartida() {
        try {
            return Files.deleteIfExists(archivoPartida.toPath());
        } catch (IOException e) {
            System.out.println("No se pudo borrar la partida: " + e.getMessage());
            return false;
        }
    }

    // File.delete solo devuelve true o false, no dice por que fallo
    public synchronized boolean borrarHistorial() {
        return archivoHistorial.delete();
    }

    public synchronized boolean existePartida() {
        return archivoPartida.exists();
    }

    // ---- serializacion

    public synchronized boolean guardarPartida(Partida partida) {
        crearCarpeta();
        try (ObjectOutputStream salida = new ObjectOutputStream(new FileOutputStream(archivoPartida))) {
            salida.writeObject(partida);
            return true;
        } catch (NotSerializableException e) {
            System.out.println("No se pudo guardar, esta clase no es Serializable: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error al guardar la partida: " + e.getMessage());
        }
        return false;
    }

    public synchronized Partida cargarPartida() {
        try (ObjectInputStream entrada = new ObjectInputStream(new FileInputStream(archivoPartida))) {
            return (Partida) entrada.readObject();
        } catch (FileNotFoundException e) {
            System.out.println("No hay ninguna partida guardada.");
        } catch (ClassNotFoundException e) {
            System.out.println("La partida tiene una clase que no existe: " + e.getMessage());
        } catch (IOException | ClassCastException e) {
            System.out.println("El archivo de la partida esta danado: " + e.getMessage());
        }
        return null;
    }
}
