package test4;

class Class1 {

    @ru.justagod.cutter.GradleSideOnly(ru.justagod.cutter.GradleSide.SERVER)
    public void server() {

    }

    public void server(int a) {

    }

    @ru.justagod.cutter.GradleSideOnly(ru.justagod.cutter.GradleSide.CLIENT)
    public void client() {

    }

    public void client(int a) {

    }
}