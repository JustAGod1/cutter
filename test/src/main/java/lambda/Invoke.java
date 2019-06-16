package lambda;

public final class Invoke {

    public static void server(ServerInvoke r) {
        r.run();
    }

    public static void client(ClientInvoke r) {
        r.run();
    }
}
