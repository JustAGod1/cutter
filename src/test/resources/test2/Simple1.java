package test2;

@anno.SideOnly(anno.Side.SERVER)
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