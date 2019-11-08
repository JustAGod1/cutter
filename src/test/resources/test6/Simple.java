package test6;

class Simple {

    @anno.SideOnly(anno.Side.SERVER)
    public void a() {
        Runnable a = () -> System.out.println("LOL");
    }

}