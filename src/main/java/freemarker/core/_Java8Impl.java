package freemarker.core;

import java.lang.reflect.Method;

/**
 * Used internally only, might changes without notice!
 * Used for accessing functionality that's only present in Java 6 or later.
 */
// Compile this against Java 8
@SuppressWarnings("Since15") // For IntelliJ inspection
public class _Java8Impl implements _Java8 {
    
    public static final _Java8 INSTANCE = new _Java8Impl();

    private _Java8Impl() {
        // Not meant to be instantiated
    }    

    public boolean isDefaultMethod(Method method) {
        return method.isDefault();
    }

}
