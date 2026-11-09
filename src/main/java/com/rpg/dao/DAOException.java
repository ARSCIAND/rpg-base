package com.rpg.dao;

// Error de la capa de datos. Asi el Controller no necesita conocer SQLException.
public class DAOException extends Exception {

    public DAOException(String mensaje) {
        super(mensaje);
    }

    public DAOException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
