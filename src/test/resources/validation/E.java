package validation;

import ru.justagod.cutter.*;
import ru.justagod.cutter.invoke.*;

class E {

    void kek(Runnable a) {}

    void b() {
        kek(this::c);
    }

    @GradleSideOnly(GradleSide.SERVER)
    void c() {
    }
}
