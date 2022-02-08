/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package freemarker.log;

import java.util.logging.Level;

/**
 * Don't use this class; it's only public to work around Google App Engine Java
 * compliance issues. FreeMarker developers only: treat this class as package-visible.
 */
public class _JULLoggerFactory implements LoggerFactory {
    @Override
    public Logger getLogger(String category) {
        return new JULLogger(java.util.logging.Logger.getLogger(category));
    }

    private static class JULLogger
    extends
        Logger {
        private final java.util.logging.Logger logger;
        
        JULLogger(java.util.logging.Logger logger) {
            this.logger = logger;
        }
        
        @Override
        public void debug(String message) {
            logger.log(Level.FINE, message);
        }

        @Override
        public void debug(String message, Throwable t) {
            logger.log(Level.FINE, message, t);
        }

        @Override
        public void error(String message) {
            logger.log(Level.SEVERE, message);
        }

        @Override
        public void error(String message, Throwable t) {
            logger.log(Level.SEVERE, message, t);
        }

        @Override
        public void info(String message) {
            logger.log(Level.INFO, message);
        }

        @Override
        public void info(String message, Throwable t) {
            logger.log(Level.INFO, message, t);
        }

        @Override
        public void warn(String message) {
            logger.log(Level.WARNING, message);
        }

        @Override
        public void warn(String message, Throwable t) {
            logger.log(Level.WARNING, message, t);
        }

        @Override
        public boolean isDebugEnabled() {
            return logger.isLoggable(Level.FINE);
        }

        @Override
        public boolean isInfoEnabled() {
            return logger.isLoggable(Level.INFO);
        }

        @Override
        public boolean isWarnEnabled() {
            return logger.isLoggable(Level.WARNING);
        }

        @Override
        public boolean isErrorEnabled() {
            return logger.isLoggable(Level.SEVERE);
        }

        @Override
        public boolean isFatalEnabled() {
            return logger.isLoggable(Level.SEVERE);
        }
    }
}
