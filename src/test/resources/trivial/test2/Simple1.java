package test2;

@ru.justagod.cutter.GradleSideOnly(ru.justagod.cutter.GradleSide.SERVER)
public class Simple1 {
    public static void main() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                System.out.println("Hello");
            }
        };
    }
}