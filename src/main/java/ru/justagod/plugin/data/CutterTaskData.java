package ru.justagod.plugin.data;

import ru.justagod.plugin.processing.model.InvokeClass;

import java.util.ArrayList;
import java.util.List;

public class CutterTaskData {
    public String name;
    public List<SideName> primalSides;
    public List<SideName> targetSides;
    public List<InvokeClass> invokeClasses = new ArrayList<>();

    public CutterTaskData(String name) {
        this.name = name;
    }
}
