package com.brewstack.desktop;

public class AppState {
    private static Barista currentBarista;

    public static Barista getCurrentBarista() { return currentBarista; }
    public static void setCurrentBarista(Barista barista) { currentBarista = barista; }
}
