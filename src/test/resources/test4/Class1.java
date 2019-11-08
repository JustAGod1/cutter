package test4;

class Class1 {

    @anno.SideOnly(anno.Side.SERVER)
    public void server() {

    }

    public void server(int a) {

    }

    @anno.SideOnly(anno.Side.CLIENT)
    public void client() {

    }

    public void client(int a) {

    }
}