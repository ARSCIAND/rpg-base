package com.rpg.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

// Abre la conexion con MySQL. Los datos de conexion se leen de config.txt
// (esta en la raiz del proyecto, junto al pom.xml, y no se sube a GitHub).
//
// Ejemplo de URL: jdbc:mysql://localhost:3306/rpg_grupal_db
//   jdbc           -> protocolo, le dice a Java que es una conexion JDBC
//   mysql          -> subprotocolo, con esto el DriverManager sabe que driver usar (mysql-connector-j)
//   localhost      -> host, la direccion del servidor donde esta MySQL (aqui es mi propio computador)
//   3306           -> puerto, la puerta por donde escucha MySQL (3306 es el de siempre)
//   rpg_grupal_db  -> esquema, el nombre exacto de la base de datos
public class ConexionDB {

    private static final String ARCHIVO_CONFIG = "config.txt";

    public static Connection getConnection() throws SQLException, DAOException {
        Map<String, String> config = leerConfig();
        String url = config.get("url");
        String usuario = config.get("usuario");
        String clave = config.get("clave");

        if (url == null || usuario == null || clave == null) {
            throw new DAOException("Falta url, usuario o clave en " + ARCHIVO_CONFIG);
        }
        return DriverManager.getConnection(url, usuario, clave);
    }

    private static Map<String, String> leerConfig() throws DAOException {
        File archivo = new File(ARCHIVO_CONFIG);
        if (!archivo.exists()) {
            throw new DAOException("No se encontro " + ARCHIVO_CONFIG + " en " + archivo.getAbsolutePath()
                    + ". Copia config.example.txt y pon tus datos.");
        }

        Map<String, String> config = new HashMap<>();
        try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.trim().isEmpty()) {
                    continue;
                }
                String[] partes = linea.split("=", 2);
                if (partes.length == 2) {
                    config.put(partes[0].trim(), partes[1].trim());
                }
            }
        } catch (FileNotFoundException e) {
            throw new DAOException("No se pudo abrir " + ARCHIVO_CONFIG);
        } catch (IOException e) {
            throw new DAOException("Error al leer " + ARCHIVO_CONFIG + ": " + e.getMessage());
        }
        return config;
    }
}
