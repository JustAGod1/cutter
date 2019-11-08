package test1;

class Simple {
    @anno.SideOnly(anno.Side.CLIENT)
    private static void client() {

    }

    @anno.SideOnly(anno.Side.SERVER)
    private static void server() {

    }
}