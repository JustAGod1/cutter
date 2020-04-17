package test10;

class Simple {

    public static void main(String[] args) {
        if (SideUtil.isServer()) {
            System.out.println("Server code");
        }
        if (SideUtil.isServer) {
            System.out.println("Server code");
        }
        if (SideUtil.isClient()) {
            System.out.println("Client code");
        }
        if (SideUtil.isClient) {
            System.out.println("Client code");
        }
        if (SideUtil.isClient && SideUtil.isServer) {
            System.out.println("Dead code");
        }
        if (SideUtil.isClient() && SideUtil.isServer()) {
            System.out.println("Dead code");
        }
        if (SideUtil.isClient || SideUtil.isServer) {
            System.out.println("code");
        }
        if (SideUtil.isClient() || SideUtil.isServer()) {
            System.out.println("code");
        }
    }
}

class SideUtil {

    public static boolean isServer = true;
    public static boolean isClient = true;
    public static boolean isServer() {
        return true;
    }

    public static boolean isClient() {
        return true;
    }
}