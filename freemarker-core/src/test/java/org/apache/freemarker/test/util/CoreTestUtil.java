package org.apache.freemarker.test.util;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Version;

public final class CoreTestUtil {

    private CoreTestUtil() {
        // Not meant to be instantiated
    }

    /**
     * Returns the closes FreeMarker version number that doesn't exit yet (so it's illegal).
     */
    public static Version getClosestFutureVersion() {
        Version v = Configuration.getVersion();
        return new Version(v.getMajor(), v.getMinor(), v.getMicro() + 1);
    }
}
