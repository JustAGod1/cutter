package validation;

import ru.justagod.cutter.*;

class B {

    @GradleSideOnly(GradleSide.SERVER)
    int c;

    void a() {
        c = 9;
    }
}