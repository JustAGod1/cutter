package ru.justagod.test;

import ru.justagod.mineplugin.GradleSide;
import ru.justagod.mineplugin.GradleSideOnly;

@GradleSideOnly(GradleSide.SERVER)
public interface Server {

    @GradleSideOnly(GradleSide.SERVER)
    public void kek();


}
