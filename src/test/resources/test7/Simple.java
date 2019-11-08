class Simple {
    @anno.SideOnly(anno.Side.SERVER)
    int server;
    @anno.SideOnly(anno.Side.CLIENT)
    int client;
}