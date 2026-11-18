package com.OverZone.Scardaoni.game;

/** Equipos de la partida. */
public final class Team {

    public static final int NONE = -1;
    public static final int BLUE = 0;
    public static final int RED = 1;

    private Team() { }

    public static String name(int team) {
        switch (team) {
            case BLUE: return "Azul";
            case RED: return "Rojo";
            default: return "-";
        }
    }
}
