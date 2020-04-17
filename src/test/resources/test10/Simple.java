package test10;

class Simple {

    public static void main(String[] args) {
        boolean flag = Runtime.getRuntime().availableProcessors() > 0;
        if (flag || SideUtil.isServer()) {
            System.out.println("Server code");
        } else if (flag || SideUtil.isClient()) {
            System.out.println("Client code");
        } else {
            System.out.println("Both");
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