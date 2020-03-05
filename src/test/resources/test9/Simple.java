package test9;

import ru.justagod.cutter.*;
import ru.justagod.cutter.invoke.*;

class Simple {

    public static void main(String[] args) {
        InvokeClient a = () -> {
            System.out.println("Hello client lambda");
        };
        a.run();
        InvokeClient b = new InvokeClient() {
            public void run() {
                System.out.println("Hello client anonymous");
            }
        };
        b.run();

        InvokeServer c = () -> {
            System.out.println("Hello server lambda");
        };
        c.run();
        InvokeServer d = new InvokeServer() {
            public void run() {
                System.out.println("Hello server anonymous");
            }
        };
        d.run();
    }
}