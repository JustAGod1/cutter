package lambda;

public class Foo {

    public static void foo() {
        Invoke.server(() -> System.out.println(""));
        Invoke.server(System.out::println);
        Invoke.server(new ServerInvoke() {
            @Override
            public void run() {
                System.out.println();
            }
        });
        Invoke.client(() -> System.out.println("Lol"));
    }
}
