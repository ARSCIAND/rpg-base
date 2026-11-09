package com.rpg.controller;

import com.rpg.dao.DAOException;
import com.rpg.dao.HeroeDAO;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;
import com.rpg.funcional.ReglasHeroe;
import com.rpg.model.Heroe;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// Recibe lo que escribe el usuario en la View, lo valida y le pide los datos al DAO.
// Aqui se decide que mensaje se muestra. No tiene nada de JDBC ni de consola.
public class HeroeController {

    private static final int LARGO_MAXIMO_NOMBRE = 30;

    private final HeroeDAO dao = new HeroeDAO();

    // mensaje para cuando una consulta no devuelve nada (lista vacia o error)
    private String mensaje = "";

    // TALLER LAMBDAS: Supplier, no recibe nada y entrega la fecha y hora solo cuando se le pide (get)
    private final Supplier<String> fechaActual =
            () -> LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

    // TALLER LAMBDAS: Function con referencias a constructores. Del tipo de la tabla sale el
    // Personaje del juego, sin if/else: "guerrero" -> new Guerrero(nombre), "mago" -> new Mago(nombre)
    private static final Map<String, Function<String, Personaje>> FABRICA_PERSONAJES = Map.of(
            "guerrero", Guerrero::new,
            "mago", Mago::new);

    public String insertar(String nombre, String tipo, int vidaMaxima, int nivel) {
        nombre = nombre.trim();
        tipo = tipo.trim().toLowerCase();

        if (nombre.isEmpty()) {
            return "El nombre no puede estar vacio.";
        }
        if (nombre.length() > LARGO_MAXIMO_NOMBRE) {
            return "El nombre puede tener maximo " + LARGO_MAXIMO_NOMBRE + " letras.";
        }
        if (!esTipoValido(tipo)) {
            return "Tipo no valido, debe ser guerrero o mago.";
        }
        if (vidaMaxima <= 0) {
            return "La vida maxima debe ser mayor que 0.";
        }
        if (nivel < 1) {
            return "El nivel debe ser 1 o mayor.";
        }

        try {
            if (dao.existeNombre(nombre)) {
                return "Ya existe un heroe llamado " + nombre + ".";
            }
            dao.insertar(new Heroe(nombre, tipo, vidaMaxima, nivel));
            return "Heroe " + nombre + " guardado en la base de datos.";
        } catch (DAOException e) {
            return "Error: " + e.getMessage();
        }
    }

    public List<Heroe> listar() {
        try {
            List<Heroe> heroes = dao.listar();
            mensaje = "No hay heroes registrados.";
            return heroes;
        } catch (DAOException e) {
            mensaje = "Error: " + e.getMessage();
            return new ArrayList<>();
        }
    }

    public List<Heroe> buscarPorTipo(String tipo) {
        tipo = tipo.trim().toLowerCase();
        if (!esTipoValido(tipo)) {
            mensaje = "Tipo no valido, debe ser guerrero o mago.";
            return new ArrayList<>();
        }
        try {
            List<Heroe> heroes = dao.buscarPorTipo(tipo);
            mensaje = "No hay heroes de tipo " + tipo + ".";
            return heroes;
        } catch (DAOException e) {
            mensaje = "Error: " + e.getMessage();
            return new ArrayList<>();
        }
    }

    public String actualizarNivel(int id, int nivel) {
        if (nivel < 1) {
            return "El nivel debe ser 1 o mayor.";
        }
        try {
            int filas = dao.actualizarNivel(id, nivel);
            if (filas == 0) {
                return "No existe un heroe con ese id.";
            }
            return "Nivel actualizado a " + nivel + ".";
        } catch (DAOException e) {
            return "Error: " + e.getMessage();
        }
    }

    public String eliminar(int id) {
        try {
            int filas = dao.eliminar(id);
            if (filas == 0) {
                return "No existe un heroe con ese id.";
            }
            return "Heroe eliminado.";
        } catch (DAOException e) {
            return "Error: " + e.getMessage();
        }
    }

    // Convierte la fila de la BD en un Guerrero o un Mago del juego.
    // Devuelve null si no existe, y el mensaje queda en getMensaje()
    public Personaje cargarPersonaje(int id) {
        try {
            Heroe heroe = dao.buscarPorId(id);
            if (heroe == null) {
                mensaje = "No existe un heroe con ese id.";
                return null;
            }
            Personaje personaje = FABRICA_PERSONAJES.get(heroe.getTipo()).apply(heroe.getNombre());
            // la vida sale de la tabla y el nivel le suma vida extra (interfaz propia CalculadoraPoder)
            int vida = ReglasHeroe.VIDA_EN_COMBATE.calcular(heroe.getVidaMaxima(), heroe.getNivel());
            personaje.setVidaMaxima(vida);
            personaje.setVida(vida);
            mensaje = heroe.getNombre() + " (nivel " + heroe.getNivel() + ") cargado desde la base de datos con "
                    + vida + " de vida.";
            return personaje;
        } catch (DAOException e) {
            mensaje = "Error: " + e.getMessage();
            return null;
        }
    }

    // ================= TALLER STREAMS =================
    // La BD solo entrega la lista (dao.listar) y aqui se procesa con Stream API.
    // Patron de la guia: origen (heroes.stream()) -> intermedias (filter, map, sorted) -> terminal

    // filtra con cualquier Predicate que le pase la View (veteranos, guerreros, nombre que contiene...)
    public List<Heroe> filtrar(Predicate<Heroe> condicion) {
        List<Heroe> heroes = listar();
        if (heroes.isEmpty()) {
            return heroes;
        }
        mensaje = "Ningun heroe cumple esa condicion.";
        return heroes.stream()
                .filter(condicion)
                .toList();
    }

    // ranking de mayor a menor poder (poder = vida + nivel * 10)
    public List<String> rankingPorPoder() {
        List<Heroe> heroes = listar();
        return heroes.stream()
                .sorted(Comparator.comparingInt(ReglasHeroe::poderDe).reversed())
                .map(heroe -> String.format("%-15s %-9s poder %d  (%s)", heroe.getNombre(), heroe.getTipo(),
                        ReglasHeroe.poderDe(heroe),
                        ReglasHeroe.PODER.describir(heroe.getVidaMaxima(), heroe.getNivel())))
                .toList();
    }

    public String estadisticas() {
        List<Heroe> heroes = listar();
        if (heroes.isEmpty()) {
            return mensaje;
        }

        long guerreros = heroes.stream().filter(ReglasHeroe.ES_GUERRERO).count();
        long magos = heroes.stream().filter(ReglasHeroe.ES_MAGO).count();

        // mapToInt pasa a un IntStream, que ya trae average() y sum()
        double vidaPromedio = heroes.stream()
                .mapToInt(Heroe::getVidaMaxima)
                .average()
                .orElse(0);

        Heroe masPoderoso = heroes.stream()
                .max(Comparator.comparingInt(ReglasHeroe::poderDe))
                .orElseThrow();

        // filter + map + toList: "toma los heroes, deja los veteranos, saca el nombre en mayuscula"
        List<String> veteranos = heroes.stream()
                .filter(ReglasHeroe.ES_VETERANO)
                .map(ReglasHeroe.NOMBRE)
                .map(String::toUpperCase)
                .toList();

        // anyMatch hace cortocircuito: para apenas encuentra una leyenda
        boolean hayLeyenda = heroes.stream().anyMatch(ReglasHeroe.ES_LEYENDA);

        // collect(Collectors.joining) es otra forma de terminar el stream: arma un solo String
        String todos = heroes.stream()
                .map(ReglasHeroe.FICHA_CORTA)
                .collect(Collectors.joining(", "));

        StringBuilder texto = new StringBuilder();
        texto.append("Reporte generado el ").append(fechaActual.get()).append('\n');
        texto.append("Total de heroes: ").append(heroes.size())
                .append(" (").append(guerreros).append(" guerreros, ").append(magos).append(" magos)\n");
        texto.append(String.format("Vida maxima promedio: %.1f%n", vidaPromedio));
        texto.append("Mas poderoso: ").append(ReglasHeroe.FICHA_CORTA.apply(masPoderoso))
                .append(" con poder ").append(ReglasHeroe.poderDe(masPoderoso)).append('\n');
        texto.append("Veteranos (nivel ").append(ReglasHeroe.NIVEL_VETERANO).append(" o mas): ")
                .append(veteranos.isEmpty() ? "ninguno" : veteranos).append('\n');
        texto.append("Hay alguna leyenda (nivel ").append(ReglasHeroe.NIVEL_LEYENDA).append(" o mas)? ")
                .append(hayLeyenda ? "si" : "no").append('\n');
        texto.append("Todos: ").append(todos);
        return texto.toString();
    }

    public String getMensaje() {
        return mensaje;
    }

    private boolean esTipoValido(String tipo) {
        return FABRICA_PERSONAJES.containsKey(tipo);   // solo hay fabrica para guerrero y mago
    }
}
