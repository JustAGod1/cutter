package ru.justagod.test;

import ru.justagod.mineplugin.GradleSide;
import ru.justagod.mineplugin.GradleSideOnly;

public class Lol implements Server{

    @GradleSideOnly(GradleSide.SERVER)
    public String a;

    @Override
    @GradleSideOnly(GradleSide.CLIENT)
    public void kek() {
        Runnable a = () -> {
            Runnable b = new Runnable() {
                @Override
                public void run() {

                }
            };
        };
    }

    @GradleSideOnly(GradleSide.CLIENT)
    public static class Huy {

    }
}
