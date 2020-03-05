package test5;

class Simple {

    @ru.justagod.cutter.GradleSideOnly(ru.justagod.cutter.GradleSide.SERVER)
    public void server() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                System.out.println("LOL");
            }
        };
    }
}