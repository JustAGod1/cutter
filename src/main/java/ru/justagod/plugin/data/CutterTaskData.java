package ru.justagod.plugin.data;

import java.util.List;

public class CutterTaskData {
    public String name;
    public List<SideName> primalSides;
    public List<SideName> targetSides;

    public CutterTaskData(String name) {
        this.name = name;
    }
}
