package validation;

import ru.justagod.cutter.*;
import ru.justagod.cutter.invoke.*;

class D {

    @GradleSideOnly(GradleSide.SERVER)
    int a;

    void b() {
        Invoke.server(() -> { a = 6;});
        a = 7;
        Invoke.client(() -> { a = 8;});
    }

    @GradleSideOnly(GradleSide.SERVER)
    void c() {
        a = 6;
        Invoke.client(() -> { a = 7; });
    }
}