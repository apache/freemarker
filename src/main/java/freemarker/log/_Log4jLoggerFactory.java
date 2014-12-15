/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.log;

import org.apache.log4j.Level;

/**
 * Don't use this class; it's only public to work around Google App Engine Java
 * compliance issues. FreeMarker developers only: treat this class as package-visible.
 */
public class _Log4jLoggerFactory implements LoggerFactory
{
    public Logger getLogger(String category)
    {
        return new Log4jLogger(org.apache.log4j.Logger.getLogger(category));
    }

    private static class Log4jLogger
    extends
        Logger
    {
        private final org.apache.log4j.Logger logger;
        
        Log4jLogger(org.apache.log4j.Logger logger)
        {
            this.logger = logger;
        }
        
        public void debug(String message)
        {
            logger.debug(message);
        }

        public void debug(String message, Throwable t)
        {
            logger.debug(message, t);
        }

        public void error(String message)
        {
            logger.error(message);
        }

        public void error(String message, Throwable t)
        {
            logger.error(message, t);
        }

        public void info(String message)
        {
            logger.info(message);
        }

        public void info(String message, Throwable t)
        {
            logger.info(message, t);
        }

        public void warn(String message)
        {
            logger.warn(message);
        }

        public void warn(String message, Throwable t)
        {
            logger.warn(message, t);
        }

        public boolean isDebugEnabled()
        {
            return logger.isDebugEnabled();
        }

        public boolean isInfoEnabled()
        {
            return logger.isInfoEnabled();
        }

        public boolean isWarnEnabled()
        {
            return logger.isEnabledFor(Level.WARN);
        }

        public boolean isErrorEnabled()
        {
            return logger.isEnabledFor(Level.ERROR);
        }

        public boolean isFatalEnabled()
        {
            return logger.isEnabledFor(Level.FATAL);
        }
    }
}
