package com.mastfrog.bits.unsafe;

import java.lang.reflect.Field;
import sun.misc.Unsafe;

/**
 *
 * @author Tim Boudreau
 */
final class UnsafeUtils {
    public static final Unsafe UNSAFE;
    static {
        try {
            UNSAFE = getUnsafe();
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new Error(ex);
        }
    }
    
    @SuppressWarnings("restriction")
    private static Unsafe getUnsafe() throws NoSuchFieldException, IllegalAccessException {
        Field singletonInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
        singletonInstanceField.setAccessible(true);
        return (Unsafe) singletonInstanceField.get(null);
    }

    private UnsafeUtils() {
        throw new AssertionError();
    }
}
