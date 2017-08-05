package freemarker.template;

import freemarker.core.Environment;
import freemarker.log.Logger;

/**
 * Default {@link AttemptExceptionReporter} implementation, factored out from {@link AttemptExceptionReporter} so that
 * we can have static field.
 */
class LoggingAttemptExceptionReporter implements AttemptExceptionReporter {
    
    private static final Logger LOG = Logger.getLogger("freemarker.runtime");
    
    private final boolean logAsWarn;
    
    public LoggingAttemptExceptionReporter(boolean logAsWarn) {
        this.logAsWarn = logAsWarn;
    }

    public void report(TemplateException te, Environment env) {
        String message = "Error executing FreeMarker template part in the #attempt block";
        if (!logAsWarn) {
            LOG.error(message, te);
        } else {
            LOG.warn(message, te);
        }
    }
    
}