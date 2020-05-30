package validation;

import ru.justagod.cutter.*;

class A {

    void a() {
        b();
    }

    @GradleSideOnly(GradleSide.SERVER)
    void b() {

    }
}