package ru.justagod.model.factory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Baz {

    String value();

    Lol kek();

    enum Lol {
        A, B, C
    }
}
