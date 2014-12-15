package freemarker.log;

/**
 * Don't use this class; it's only public to work around Google App Engine Java
 * compliance issues. FreeMarker developers only: treat this class as package-visible.
 */
public class _Log4jOverSLF4JTester {

    private static final String MDC_KEY = _Log4jOverSLF4JTester.class.getName();

    /**
     * Returns if Log4j-over-SLF4J is actually working. Sometimes the API classes are present, but there's no SLF4J
     * implementation around.
     */
    public static final boolean test() {
        org.apache.log4j.MDC.put(MDC_KEY, "");
        try {
            return org.slf4j.MDC.get(MDC_KEY) != null;
        } finally {
            org.apache.log4j.MDC.remove(MDC_KEY);
        }
    }

}
