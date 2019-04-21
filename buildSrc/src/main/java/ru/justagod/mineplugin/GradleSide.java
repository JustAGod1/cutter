package ru.justagod.mineplugin;

/**
 * Created by JustAGod on 08.03.2018.
 */
@SuppressWarnings("unused")
public enum GradleSide {
    SERVER("server"), CLIENT("client");

    final String postfix;

    GradleSide(String postfix) {
        this.postfix = postfix;
    }
}
