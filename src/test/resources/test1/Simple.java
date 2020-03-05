package test1;

class Simple {
    @ru.justagod.cutter.GradleSideOnly(ru.justagod.cutter.GradleSide.CLIENT)
    private static void client() {

    }

    @ru.justagod.cutter.GradleSideOnly(ru.justagod.cutter.GradleSide.SERVER)
    private static void server() {

    }
}