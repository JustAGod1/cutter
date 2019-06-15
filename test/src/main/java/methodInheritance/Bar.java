package methodInheritance;

import anno.Side;
import anno.SideOnly;
import kotlin.SinceKotlin;

public class Bar {

    @SideOnly(Side.SERVER)
    void baz() {}
}
