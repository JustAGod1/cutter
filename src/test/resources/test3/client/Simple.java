package test3.client;

class Simple {

    public static void main(String[] args) {
        Runnable r= new Runnable() {
            @Override
            public void run() {
                System.out.println("Hello");
            }
        };
    }

    class Simple2 {

    }
}