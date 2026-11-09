package com.rpg;

import com.rpg.view.HeroeUI;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        new HeroeUI(new Scanner(System.in)).iniciar();
    }
}
