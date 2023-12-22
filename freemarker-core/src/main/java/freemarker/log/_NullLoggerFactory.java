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

/**
 * Don't use this class; it's only public to work around Google App Engine Java
 * compliance issues. FreeMarker developers only: treat this class as package-visible.
 */
public class _NullLoggerFactory implements LoggerFactory {
    _NullLoggerFactory() {
    }
    
    @Override
    public Logger getLogger(String category) {
        return INSTANCE;
    }

    private static final Logger INSTANCE = new Logger()
    {
        @Override
        public void debug(String message) {
        }

        @Override
        public void debug(String message, Throwable t) {
        }

        @Override
        public void error(String message) {
        }

        @Override
        public void error(String message, Throwable t) {
        }

        @Override
        public void info(String message) {
        }

        @Override
        public void info(String message, Throwable t) {
        }

        @Override
        public void warn(String message) {
        }

        @Override
        public void warn(String message, Throwable t) {
        }

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public boolean isInfoEnabled() {
            return false;
        }

        @Override
        public boolean isWarnEnabled() {
            return false;
        }

        @Override
        public boolean isErrorEnabled() {
            return false;
        }

        @Override
        public boolean isFatalEnabled() {
            return false;
        }
    };
}
