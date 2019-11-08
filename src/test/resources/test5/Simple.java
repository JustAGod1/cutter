package test5;

class Simple {

    @anno.SideOnly(anno.Side.SERVER)
    public void server() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                System.out.println("LOL");
            }
        };
    }
}