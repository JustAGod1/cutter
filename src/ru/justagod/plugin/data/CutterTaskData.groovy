package ru.justagod.plugin.data

class CutterTaskData {
    String name
    List<SideInfo> primalSides
    List<SideInfo> targetSides

    CutterTaskData(String name) {
        this.name = name
    }
}
