package ru.justagod.plugin.util

@SuppressWarnings("unused")
class Extensions {

    static String nameWithoutExtension(final File self) {
        def name = self.name
        def lastDot = self.name.lastIndexOf('.')
        if (lastDot == -1) return name
        return name.substring(0, lastDot)
    }

    static String extension(final File self) {
        def name = self.name
        def lastDot = self.name.lastIndexOf('.')
        if (lastDot == -1) return null
        return name.substring(lastDot + 1)
    }

}
