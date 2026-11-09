package com.rpg.dao;

import com.rpg.model.Heroe;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Toda la parte JDBC de la tabla heroes. Ninguna otra clase usa java.sql.
// executeUpdate -> INSERT, UPDATE, DELETE (devuelve las filas afectadas)
// executeQuery  -> SELECT (devuelve un ResultSet con los datos)
public class HeroeDAO {

    public void insertar(Heroe heroe) throws DAOException {
        String sql = "INSERT INTO heroes (nombre, tipo, vida_maxima, nivel) VALUES (?, ?, ?, ?)";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, heroe.getNombre());
            ps.setString(2, heroe.getTipo());
            ps.setInt(3, heroe.getVidaMaxima());
            ps.setInt(4, heroe.getNivel());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("No se pudo guardar el heroe: " + e.getMessage(), e);
        }
    }

    public List<Heroe> listar() throws DAOException {
        String sql = "SELECT id, nombre, tipo, vida_maxima, nivel FROM heroes ORDER BY id";
        List<Heroe> heroes = new ArrayList<>();
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                heroes.add(crearHeroe(rs));
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudieron listar los heroes: " + e.getMessage(), e);
        }
        return heroes;
    }

    public List<Heroe> buscarPorTipo(String tipo) throws DAOException {
        String sql = "SELECT id, nombre, tipo, vida_maxima, nivel FROM heroes WHERE tipo = ? ORDER BY id";
        List<Heroe> heroes = new ArrayList<>();
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tipo);
            // el ResultSet va en su propio try porque primero hay que poner el parametro
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    heroes.add(crearHeroe(rs));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo buscar por tipo: " + e.getMessage(), e);
        }
        return heroes;
    }

    // la necesito para cargar un heroe en el juego. Devuelve null si no existe
    public Heroe buscarPorId(int id) throws DAOException {
        String sql = "SELECT id, nombre, tipo, vida_maxima, nivel FROM heroes WHERE id = ?";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return crearHeroe(rs);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo buscar el heroe: " + e.getMessage(), e);
        }
        return null;
    }

    // para que el Controller revise que el nombre no este repetido antes de insertar
    public boolean existeNombre(String nombre) throws DAOException {
        String sql = "SELECT id FROM heroes WHERE nombre = ?";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo revisar el nombre: " + e.getMessage(), e);
        }
    }

    public int actualizarNivel(int id, int nivel) throws DAOException {
        String sql = "UPDATE heroes SET nivel = ? WHERE id = ?";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, nivel);
            ps.setInt(2, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("No se pudo actualizar el nivel: " + e.getMessage(), e);
        }
    }

    public int eliminar(int id) throws DAOException {
        String sql = "DELETE FROM heroes WHERE id = ?";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("No se pudo eliminar el heroe: " + e.getMessage(), e);
        }
    }

    // pasa la fila actual del ResultSet a un objeto Heroe
    private Heroe crearHeroe(ResultSet rs) throws SQLException {
        return new Heroe(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("tipo"),
                rs.getInt("vida_maxima"),
                rs.getInt("nivel"));
    }
}
