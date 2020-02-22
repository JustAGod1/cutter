package test9;

class Simple {

    public static void main(String[] args) {
        ClientInvoke a = () -> {
            System.out.println("Hello client lambda");
        };
        a.run();
        ClientInvoke b = new ClientInvoke() {
            public void run() {
                System.out.println("Hello client anonymous");
            }
        };
        b.run();

        ServerInvoke c = () -> {
            System.out.println("Hello server lambda");
        };
        c.run();
        ServerInvoke d = new ServerInvoke() {
            public void run() {
                System.out.println("Hello server anonymous");
            }
        };
        d.run();
    }
}