package ru.justagod.cutter.invoke;

public class Invoke {

    public static void client(InvokeClient block) {
        block.run();
    }

    public static void server(InvokeServer block) {
        block.run();
    }

    public static <T>T serverValue(InvokeServerValue<T> block) {
        return block.run();
    }

    public static <T>T clientValue(InvokeClientValue<T> block) {
        return block.run();
    }

}
