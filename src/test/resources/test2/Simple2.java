package test2;

@anno.SideOnly(anno.Side.CLIENT)
public class Simple2 {
    public static void main() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                System.out.println("Hello");
            }
        };
    }
}