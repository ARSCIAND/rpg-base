package com.rpg.persistencia;

import com.rpg.entities.Gremio;
import com.rpg.entities.PerfilJugador;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// Todo lo que se guarda en el archivo .dat
public class Partida implements Serializable {

    // serialVersionUID es la version de la clase. Si cambia, Java no deja cargar las partidas viejas
    // (InvalidClassException). Si no lo pongo, Java lo calcula solo y cambia con cualquier modificacion.
    private static final long serialVersionUID = 1L;

    private PerfilJugador perfil;
    private Gremio gremio;
    private HashMap<String, Integer> derrotados;
    private LocalDateTime fecha;

    // transient: no se guarda, al cargar vuelve como null
    private transient String nota = "partida de esta sesion";

    public Partida(PerfilJugador perfil, Gremio gremio, Map<String, Integer> derrotados) {
        this.perfil = perfil;
        this.gremio = gremio;
        this.derrotados = new HashMap<>(derrotados);
        this.fecha = LocalDateTime.now();
    }

    public PerfilJugador getPerfil() {
        return perfil;
    }

    public Gremio getGremio() {
        return gremio;
    }

    public Map<String, Integer> getDerrotados() {
        return derrotados;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getNota() {
        return nota;
    }
}
