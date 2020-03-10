package test6;

class Simple {

    @ru.justagod.cutter.GradleSideOnly(ru.justagod.cutter.GradleSide.SERVER)
    public void a() {
        Runnable a = () -> System.out.println("LOL");
    }

}