package com.rpg.core;

// TALLER LAMBDAS: tiene un solo metodo abstracto, asi que tambien se puede pasar como lambda
@FunctionalInterface
public interface TurnoCallback {

    void onTiempoAgotado();
}
