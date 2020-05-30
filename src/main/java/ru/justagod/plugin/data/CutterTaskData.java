package ru.justagod.plugin.data;

import java.util.List;

public class CutterTaskData {
    public String name;
    public String archiveName;
    public boolean removeAnnotations = true;
    public List<SideName> primalSides;
    public List<SideName> targetSides;


    public CutterTaskData(String name) {
        this.name = name;
    }


    @Override
    public String toString() {
        return "CutterTaskData{" +
                "name='" + name + '\'' +
                ", archiveName='" + archiveName + '\'' +
                ", removeAnnotations=" + removeAnnotations +
                ", primalSides=" + primalSides +
                ", targetSides=" + targetSides +
                '}';
    }
}
