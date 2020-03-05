package test7;
class Simple {
    @ru.justagod.cutter.GradleSideOnly(ru.justagod.cutter.GradleSide.SERVER)
    int server;
    @ru.justagod.cutter.GradleSideOnly(ru.justagod.cutter.GradleSide.CLIENT)
    int client;
}