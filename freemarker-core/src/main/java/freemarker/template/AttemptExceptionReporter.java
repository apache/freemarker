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

package freemarker.template;

import freemarker.core.Configurable;
import freemarker.core.Environment;

/**
 * Used for the {@link Configurable#setAttemptExceptionReporter(AttemptExceptionReporter) attempt_exception_reported}
 * configuration setting.
 */
public interface AttemptExceptionReporter {
    
    /**
     * Logs the exception into the "freemarker.runtime" log category with "error" log level. This is the default
     * {@link AttemptExceptionReporter}. The error message will explain that the error was handled by an
     * {@code #attempt} block.
     */
    AttemptExceptionReporter LOG_ERROR_REPORTER = new LoggingAttemptExceptionReporter(false);

    /**
     * Like {@link #LOG_ERROR_REPORTER}, but it logs with "warn" log level.
     */
    AttemptExceptionReporter LOG_WARN_REPORTER = new LoggingAttemptExceptionReporter(true);
    
    /**
     * Called to log or otherwise report the error that has occurred inside an {@code #attempt} block.  
     */
    void report(TemplateException te, Environment env);
    
}
