package ru.justagod.plugin.util

class Extensions {

    static String nameWithoutExtension(final File self) {
        def name = self.name
        def lastDot = self.name.lastIndexOf('.')
        if (lastDot != -1) return name
        return name.substring(0, lastDot)
    }

}
