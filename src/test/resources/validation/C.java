package validation;

import ru.justagod.cutter.*;

class C1 {

    C2 a;

    C2 b() {
        return null;
    }

    void c(C2 f) {

    }

}


@GradleSideOnly(GradleSide.SERVER)
class C2 {

}