package freemarker.log;

/**
 * @deprecated Don't use it, meant to be internal.
 */
// 2.4: Remove
public class CommonsLoggingLoggerFactory implements LoggerFactory {

    public Logger getLogger(String category) {
            return new CommonsLoggingLogger(
                            org.apache.commons.logging.LogFactory.getLog(category));
    }
    
    static private class CommonsLoggingLogger extends Logger {

            private final org.apache.commons.logging.Log logger;
            
            CommonsLoggingLogger(org.apache.commons.logging.Log logger) {
                    this.logger = logger;
            }
            
            public void debug(String message) {
                    logger.debug(message);
            }

            public void debug(String message, Throwable t) {
                    logger.debug(message, t);
            }

            public void info(String message) {
                    logger.info(message);
            }

            public void info(String message, Throwable t) {
                    logger.info(message, t);
            }

            public void warn(String message) {
                    logger.warn(message);
            }

            public void warn(String message, Throwable t) {
                    logger.warn(message, t);
            }

            public void error(String message) {
                    logger.error(message);
            }

            public void error(String message, Throwable t) {
                    logger.error(message, t);
            }

            public boolean isDebugEnabled() {
                    return logger.isDebugEnabled();
            }

            public boolean isInfoEnabled() {
                    return logger.isInfoEnabled();
            }

            public boolean isWarnEnabled() {
                    return logger.isWarnEnabled();
            }

            public boolean isErrorEnabled() {
                    return logger.isErrorEnabled();
            }

            public boolean isFatalEnabled() {
                    return logger.isFatalEnabled();
            }
            
    }
    
}
