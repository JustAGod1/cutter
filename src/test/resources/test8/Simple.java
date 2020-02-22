package test8;

class Simple {

    @anno.SideOnly(anno.Side.SERVER)
    void a(int par) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                System.out.println("Doesn't exist");
            }
        };
    }

    void b(long par) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                System.out.println("Exists");
            }
        };
    }
}