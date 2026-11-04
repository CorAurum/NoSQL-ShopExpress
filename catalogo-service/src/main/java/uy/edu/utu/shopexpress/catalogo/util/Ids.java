package uy.edu.utu.shopexpress.catalogo.util;

import java.util.UUID;

public final class Ids {

    private Ids() {
    }

    /** Genera identificadores legibles con prefijo, p. ej. usr_3f9a1c2b7d4e. */
    public static String nuevo(String prefijo) {
        return prefijo + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
